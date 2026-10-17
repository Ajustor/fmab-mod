package com.ajustor.fmab.gametest;

import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.Shell;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.Shells;
import com.ajustor.fmab.gate.SoulArmor;
import com.ajustor.fmab.network.ShellKeyPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;

import java.util.UUID;
import java.util.function.Function;

import static com.ajustor.fmab.gametest.GametestAsserts.check;
import com.ajustor.fmab.platform.client.ClientPlatform;
import com.ajustor.fmab.platform.event.LivingEvents;

/**
 * L'armure d'une âme comme coquille, jouée pour de vrai : le joueur est l'âme, une autre joueuse
 * (Winry, un faux joueur du serveur) la revêt. L'âme se fige avec sa touche ; Winry entre, fait
 * marcher l'armure et peut en sortir ; l'âme se remet à marcher et Winry est emportée, prisonnière,
 * à l'abri des coups ; la touche la relâche. Lancer avec {@code ./gradlew runClientGameTest -PfmabGametest=shell}.
 */
public class ShellScenes implements FabricClientGameTest {
	private static final int Y = -60;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("shell")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("gamerule doMobSpawning false");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("tp @p 0.5 %d 0.5 0 0".formatted(Y));
			sp.getConnection().waitForChunksRender();
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			});
			// Le joueur devient une âme dans une armure.
			server(sp, p -> {
				GateState everything = GateState.NONE;
				for (BodyPart part : BodyPart.values()) {
					everything = everything.pay(part);
				}
				FmabAttachments.GATE.set(p, everything);
				SoulArmor.bindFirstTime(p);
				return null;
			});
			context.waitTicks(10);
			check(server(sp, p -> FmabAttachments.GATE.getOrCreate(p).inArmor()), "le joueur devrait être une âme en armure");

			// 1. La touche fige l'âme : plus de vitesse à elle.
			key(context, sp);
			check(server(sp, p -> shell(p).still()), "la touche devrait figer l'âme");
			check(server(sp, p -> p.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0), "une âme figée ne devrait plus avancer");
			context.takeScreenshot("shell_0_still");

			// 2. Winry la revêt, main nue.
			FakePlayer winry = server(sp, p -> {
				FakePlayer fake = FakePlayer.get(p.level(), new GameProfile(UUID.nameUUIDFromBytes("winry".getBytes()), "Winry"));
				fake.snapTo(p.getX() + 2, p.getY(), p.getZ(), 0, 0);
				fake.setItemSlot(EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
				check(Shells.canEnter(fake, p), "une âme figée et vide devrait pouvoir être revêtue");
				Shells.enter(fake, p);
				return fake;
			});
			context.waitTicks(2);
			check(server(sp, p -> shell(p).worn() && Shells.inside(winry)), "Winry devrait être dans l'armure, et la mener");
			check(context.computeOnClient(mc -> !mc.player.isPushable()), "l'âme habitée ne devrait plus être poussée");

			// Winry marche : l'armure la suit.
			server(sp, p -> {
				winry.snapTo(p.getX() + 3, p.getY(), p.getZ() + 2, 90, 0);
				return null;
			});
			context.waitTicks(3);
			check(server(sp, p -> p.distanceTo(winry) < 0.1), "l'armure revêtue devrait suivre Winry");
			context.takeScreenshot("shell_1_worn_moved");

			// Winry peut sortir d'une armure figée…
			server(sp, p -> {
				winry.setLastClientInput(new Input(false, false, false, false, false, true, false));
				return null;
			});
			context.waitTicks(2);
			check(server(sp, p -> !Shells.inside(winry) && shell(p).still() && shell(p).occupant().isEmpty()),
					"Winry devrait sortir, l'armure rester figée");
			server(sp, p -> {
				winry.setLastClientInput(Input.EMPTY);
				Shells.enter(winry, p);
				return null;
			});

			// 3. L'âme se remet à marcher : Winry est emportée, prisonnière.
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			net.minecraft.world.phys.Vec3 start = server(sp, p -> p.position());
			context.getInput().holdKey(options -> options.keyUp);
			context.waitTicks(20);
			context.getInput().releaseKey(options -> options.keyUp);
			context.waitTicks(2);
			check(server(sp, p -> shell(p).trapped()), "l'âme qui bouge devrait enfermer Winry");
			check(server(sp, p -> p.position().distanceTo(start) > 1), "l'âme devrait avoir marché");
			check(server(sp, p -> p.distanceTo(winry) < 0.1), "Winry devrait être emportée avec l'armure");
			check(server(sp, p -> winry.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0), "prisonnière, Winry ne devrait plus avancer");
			context.takeScreenshot("shell_2_trapped_walking");
			server(sp, p -> {
				winry.setLastClientInput(new Input(false, false, false, false, false, true, false));
				return null;
			});
			context.waitTicks(2);
			check(server(sp, p -> Shells.inside(winry)), "prisonnière, Winry ne devrait pas pouvoir sortir");
			server(sp, p -> {
				winry.setLastClientInput(Input.EMPTY);
				return null;
			});

			// Les coups qui la visent frappent l'armure.
			// (Un faux joueur est invulnérable de lui-même : on pose la question à l'événement directement.)
			int[] wear = new int[1];
			boolean hurt = server(sp, p -> {
				wear[0] = p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue();
				return LivingEvents.ALLOW_DAMAGE.invoker().allowDamage(winry,
						p.level().damageSources().generic(), 6);
			});
			check(!hurt, "Winry, dans l'armure, ne devrait pas être blessée");
			check(server(sp, p -> p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() > wear[0]),
					"le coup devrait cabosser l'armure de l'âme");

			// 4. La touche la relâche.
			key(context, sp);
			check(server(sp, p -> !Shells.inside(winry) && shell(p).equals(Shell.NONE)), "la touche devrait relâcher Winry");
			check(server(sp, p -> winry.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0), "relâchée, Winry devrait pouvoir avancer");
			check(server(sp, p -> p.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0), "éveillée, l'âme devrait pouvoir avancer");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(5);
			context.takeScreenshot("shell_3_released");
		}
	}

	private static Shell shell(ServerPlayer p) {
		return FmabAttachments.SHELL.getOrElse(p, Shell.NONE);
	}

	/** La touche de l'âme, par le vrai paquet du client. */
	private static void key(ClientGameTestContext context, TestSingleplayerContext sp) {
		context.runOnClient(mc -> ClientPlatform.get().send(ShellKeyPayload.INSTANCE));
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(3);
	}

	private static <T> T server(TestSingleplayerContext sp, Function<ServerPlayer, T> action) {
		return sp.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
	}
}
