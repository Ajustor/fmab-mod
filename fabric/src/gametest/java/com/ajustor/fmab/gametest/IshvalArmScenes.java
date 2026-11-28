package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.arts.ArmRecomposition;
import com.ajustor.fmab.arts.IshvalArm;
import com.ajustor.fmab.arts.ScarArm;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.data.Tattoos;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.tattoo.BodyArt;
import com.ajustor.fmab.tattoo.BodyCircles;
import com.ajustor.fmab.tattoo.TattooRitual;
import com.ajustor.fmab.tattoo.TattooSlot;
import com.ajustor.fmab.transmutation.AlchemyRules;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Le bras de Scar se tatoue : les Notes d'Ishval apprennent le motif sans armer le bras ; tatoué autour
 * du bras droit, il décompose ; la Vérité qui prend ce bras emporte le pouvoir avec le tatouage. Un
 * joueur qui avait reçu l'ancien bras le retrouve tatoué. La moitié de Xing, lisible avec
 * l'alkahestry, recrée l'objet auquel on pense avec la matière des blocs touchés.
 */
public class IshvalArmScenes implements FabricClientGameTest {
	private static final BlockPos TARGET = new BlockPos(0, -60, 2);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("ishval")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("tp @p 0.5 -60 -1.0 0 30");
			sp.getConnection().waitForChunksRender();
			server(context, sp, (level, p) -> {
				AlchemistData data = FmabAttachments.ALCHEMIST.getOrCreate(p).withRank(Rank.ALCHEMIST);
				for (Glyph g : AlchemyRules.of(level.registryAccess()).glyphs()) {
					data = data.learn(g.id());
				}
				FmabAttachments.ALCHEMIST.set(p, data);
				p.getInventory().clearContent();
				return null;
			});

			// 1. Les notes apprennent le motif et le recopient dans le carnet ; le bras n'est pas encore tatoué.
			String learned = server(context, sp, (level, p) -> {
				ItemStack notes = new ItemStack(FmabItems.ISHVAL_TATTOO);
				p.setItemInHand(InteractionHand.MAIN_HAND, notes);
				notes.use(level, p, InteractionHand.MAIN_HAND);
				List<Drawing> motif = IshvalArm.motif(AlchemyRules.of(level.registryAccess()));
				return "compris=" + IshvalArm.knows(p) + " motif=" + motif.size()
						+ " carnet=" + motif.stream().allMatch(c -> Notebooks.contains(p, c))
						+ " armé=" + ScarArm.armed(p);
			});
			check(learned.equals("compris=true motif=2 carnet=true armé=false"),
					"les notes apprennent le motif sans armer le bras : " + learned);

			// 2. Tatoué autour du bras droit, le cercle qui décompose arme la main droite.
			boolean armed = server(context, sp, (level, p) -> {
				p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				List<Drawing> motif = IshvalArm.motif(AlchemyRules.of(level.registryAccess()));
				// Le motif entier sur le bras droit : la décomposition sur la face extérieure, la
				// recomposition à côté.
				BodyArt arm = TattooSlot.ARM_RIGHT.stamp(BodyArt.EMPTY, motif.get(0), BodyArt.SKIN_INK);
				arm = TattooSlot.ARM_RIGHT.stamp(arm, motif.get(1), BodyArt.SKIN_INK);
				BodyCircles.setArt(p, TattooSlot.ARM_RIGHT, arm);
				return ScarArm.armed(p) && IshvalArm.worn(p);
			});
			check(armed, "le motif tatoué autour du bras droit devrait armer la main nue");
			// Le tatouage est sous la couche des vêtements : on retire les manches de la peau pour le voir.
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				for (PlayerModelPart part : PlayerModelPart.values()) {
					if (part == PlayerModelPart.LEFT_SLEEVE || part == PlayerModelPart.RIGHT_SLEEVE
							|| part == PlayerModelPart.JACKET) {
						mc.options.setModelPart(part, false);
					}
				}
			});
			context.waitTicks(10);
			look(context, -90, "ishval_00_right_arm_outer");
			look(context, -45, "ishval_00b_three_quarter");
			look(context, 0, "ishval_00c_front");
			look(context, 180, "ishval_00d_back");
			look(context, -150, "ishval_00e_back_three_quarter");

			// 3. La Vérité prend le bras droit : le tatouage part avec lui, et le pouvoir aussi.
			boolean afterLoss = server(context, sp, (level, p) -> {
				FmabAttachments.GATE.set(p, new GateState(0, Set.of(BodyPart.RIGHT_ARM), false, Optional.empty()));
				TattooRitual.forgetLostArms(p);
				boolean still = ScarArm.armed(p);
				FmabAttachments.GATE.set(p, GateState.NONE);
				return still;
			});
			check(!afterLoss, "un bras perdu ne décompose plus");

			// 4. L'ancien bras de Scar, accordé d'un coup, se retrouve tatoué à la connexion.
			String migrated = server(context, sp, (level, p) -> {
				FmabAttachments.TATTOOS.set(p, Tattoos.NONE);
				FmabAttachments.ISHVAL_MOTIF.remove(p);
				FmabAttachments.SCAR_ARM.set(p, true);
				IshvalArm.migrate(p);
				return "ancien=" + FmabAttachments.SCAR_ARM.get(p) + " compris=" + IshvalArm.knows(p)
						+ " cercles=" + BodyCircles.circles(p, TattooSlot.ARM_RIGHT).size() + " armé=" + ScarArm.armed(p);
			});
			check(migrated.equals("ancien=null compris=true cercles=1 armé=true"),
					"l'ancien bras devrait être reporté en tatouage : " + migrated);

			// 5. La moitié de Xing, tatouée sur le bras gauche, ne se lit qu'avec l'alkahestry.
			String recompose = server(context, sp, (level, p) -> {
				Drawing circle = IshvalArm.motif(AlchemyRules.of(level.registryAccess())).get(1);
				BodyCircles.setArt(p, TattooSlot.ARM_LEFT, TattooSlot.ARM_LEFT.stamp(BodyArt.EMPTY, circle, BodyArt.SKIN_INK));
				boolean without = IshvalArm.has(p, true, IshvalArm.Half.RECOMPOSE);
				FmabAttachments.ALKAHESTRY.set(p, true);
				return "sans=" + without + " avec=" + IshvalArm.has(p, true, IshvalArm.Half.RECOMPOSE);
			});
			check(recompose.equals("sans=false avec=true"), "la recomposition se lit avec l'alkahestry : " + recompose);

			// 6. Pas assez de matière : rien n'est pris, rien n'est fait.
			String short_ = server(context, sp, (level, p) -> {
				new ItemStack(Items.IRON_PICKAXE).onCraftedBy(p, 1);
				FmabAttachments.DESIGN.set(p, "minecraft:iron_pickaxe");
				level.setBlockAndUpdate(TARGET, Blocks.OAK_PLANKS.defaultBlockState());
				boolean made = ArmRecomposition.recompose(p, level, TARGET, Direction.UP, () -> true);
				return "fait=" + made + " planche=" + level.getBlockState(TARGET).is(Blocks.OAK_PLANKS);
			});
			check(short_.equals("fait=false planche=true"), "sans fer, la planche reste : " + short_);

			// 7. Le bras touche un bloc de fer posé contre une planche : la pioche en sort, les blocs sont pris.
			String made = server(context, sp, (level, p) -> {
				level.setBlockAndUpdate(TARGET.east(), Blocks.IRON_BLOCK.defaultBlockState());
				boolean done = ArmRecomposition.recompose(p, level, TARGET, Direction.UP, () -> true);
				int pickaxes = level.getEntitiesOfClass(ItemEntity.class, new AABB(TARGET).inflate(2),
						e -> e.getItem().is(Items.IRON_PICKAXE)).size();
				return "fait=" + done + " pioches=" + pickaxes + " fer=" + level.getBlockState(TARGET.east()).isAir()
						+ " planche=" + level.getBlockState(TARGET).isAir();
			});
			check(made.equals("fait=true pioches=1 fer=true planche=true"),
					"le bras recompose la pioche avec la matière touchée : " + made);
			context.runOnClient(mc -> {
				for (PlayerModelPart part : PlayerModelPart.values()) {
					mc.options.setModelPart(part, true);
				}
				mc.options.fov().set(70);
				mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			});
			context.waitTicks(10);
			context.takeScreenshot("ishval_01_recomposed_pickaxe");
		}
	}

	/**
	 * Une prise de vue rapprochée du joueur, vu de face (caméra à la troisième personne, champ resserré),
	 * son corps tourné de {@code bodyYaw} : à −90°, son flanc droit fait face à la caméra.
	 */
	private static void look(ClientGameTestContext context, float bodyYaw, String name) {
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			mc.options.fov().set(30);
			mc.player.setYRot(0);
			mc.player.yRotO = 0;
			mc.player.setXRot(10);
			mc.player.xRotO = 10;
			mc.player.setYHeadRot(0);
			mc.player.yHeadRotO = 0;
			mc.player.yBodyRot = bodyYaw;
			mc.player.yBodyRotO = bodyYaw;
		});
		context.takeScreenshot(name);
	}

	@FunctionalInterface
	private interface OnServer<T> {
		T run(ServerLevel level, ServerPlayer player);
	}

	private static <T> T server(ClientGameTestContext context, TestSingleplayerContext sp, OnServer<T> action) {
		T result = sp.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			return action.run(player.level(), player);
		});
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(2);
		return result;
	}
}
