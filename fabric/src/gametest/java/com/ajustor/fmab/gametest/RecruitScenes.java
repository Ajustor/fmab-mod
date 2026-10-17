package com.ajustor.fmab.gametest;

import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.stone.Recruitment;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;

import java.util.function.Function;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * La paix promise par Père : Gluttony ne s'en prend pas à une recrue qui passe près de lui ; il fond
 * sur celle qui a refusé l'offre, et sur la recrue qui le frappe. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=recruits}.
 */
public class RecruitScenes implements FabricClientGameTest {
	private static final int Y = -60;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("recruits")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("difficulty normal");
			sp.getServer().runCommand("tp @p 0.5 %d 0.5 0 0".formatted(Y));
			sp.getConnection().waitForChunksRender();
			Mob gluttony = server(sp, p -> {
				// Insensible aux coups, mais toujours une cible (un joueur invulnérable n'en est plus une).
				p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 600, 4, false, false));
				FmabAttachments.RECRUITMENT.set(p, Recruitment.ACCEPTED);
				return spawn(p);
			});
			context.waitTicks(60);
			check(server(sp, p -> gluttony.getTarget() != p), "Gluttony ne devrait pas s'en prendre à une recrue");

			server(sp, p -> {
				FmabAttachments.RECRUITMENT.set(p, Recruitment.REFUSED);
				return null;
			});
			check(targets(context, sp, gluttony), "qui a refusé l'offre est une proie");

			// Recrue de nouveau, mais qui frappe : il riposte. Gluttony a pu l'avaler entre-temps (son
			// estomac est ailleurs) : on repart d'un joueur ramené au point de départ et d'un Gluttony neuf.
			server(sp, p -> {
				gluttony.discard();
				FmabAttachments.RECRUITMENT.set(p, Recruitment.ACCEPTED);
				return null;
			});
			sp.getServer().runCommand("execute in minecraft:overworld run tp @p 0.5 %d 0.5 0 0".formatted(Y));
			context.waitTicks(5);
			Mob fresh = server(sp, p -> spawn(p));
			context.waitTicks(40);
			check(server(sp, p -> fresh.getTarget() != p), "recrue de nouveau, elle est épargnée");
			server(sp, p -> {
				fresh.hurtServer(p.level(), p.damageSources().playerAttack(p), 1);
				return null;
			});
			check(targets(context, sp, fresh), "une recrue qui frappe se fait frapper");
		}
	}

	/** Un Gluttony, à quatre blocs du joueur. */
	private static Mob spawn(ServerPlayer p) {
		Mob mob = FmabEntities.GLUTTONY.create(p.level(), EntitySpawnReason.COMMAND);
		mob.snapTo(p.getX() + 4, p.getY(), p.getZ(), 90, 0);
		p.level().addFreshEntity(mob);
		return mob;
	}

	/** Gluttony vise-t-il le joueur dans les trois secondes ? (Sans attendre qu'il l'avale.) */
	private static boolean targets(ClientGameTestContext context, TestSingleplayerContext sp, Mob gluttony) {
		for (int tick = 0; tick < 60; tick++) {
			if (server(sp, p -> gluttony.getTarget() == p)) {
				return true;
			}
			context.waitTick();
		}
		return false;
	}

	private static <T> T server(TestSingleplayerContext sp, Function<ServerPlayer, T> action) {
		return sp.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
	}
}
