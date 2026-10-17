package com.ajustor.fmab.gametest;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.ImmortalSoldierEntity;
import com.ajustor.fmab.promised.NationalCircle;
import com.ajustor.fmab.promised.PromisedDay;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * La fin du mod. Le Jour promis se ressent (l'alchimie faiblit, des soldats immortels sortent de
 * terre) ; puis, Père tombé, Hohenheim se repose : l'épilogue passe, le progrès « Fin » tombe et sa
 * montre gravée reste au joueur. Lancer avec
 * {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=ending}.
 */
public class EndingScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("ending")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getConnection().waitForChunksRender();
			sp.getServer().runCommand("difficulty normal");
			// Le jour de l'éclipse, en plein milieu.
			sp.getServer().runCommand("time set " + (7 * 24000 + 6000));
			context.waitTicks(5);
			double alchemy = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 600, 4, false, false));
				return PromisedDay.alchemy(p.level(), p.blockPosition());
			});
			check(alchemy == PromisedDay.WEAKENED, "le Jour promis devrait affaiblir l'alchimie : " + alchemy);
			context.waitTicks(220);
			int soldiers = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				return p.level().getEntitiesOfClass(ImmortalSoldierEntity.class, new AABB(p.blockPosition()).inflate(32))
						.size();
			});
			check(soldiers > 0, "des soldats immortels devraient sortir de terre pendant le Jour promis");

			// Père tombe ; Hohenheim attendait ce jour.
			String ending = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				var level = p.level();
				level.getEntitiesOfClass(ImmortalSoldierEntity.class, new AABB(p.blockPosition()).inflate(64))
						.forEach(s -> s.discard());
				NationalCircle.get(server).fatherFalls();
				var hohenheim = FmabEntities.HOHENHEIM.create(level, EntitySpawnReason.COMMAND);
				hohenheim.snapTo(p.getX() + 2, p.getY(), p.getZ(), 90, 0);
				level.addFreshEntity(hohenheim);
				hohenheim.interact(p, InteractionHand.MAIN_HAND, hohenheim.position());
				var end = server.getAdvancements().get(Fmab.id("alchemist/epilogue"));
				boolean done = end != null && p.getAdvancements().getOrStartProgress(end).isDone();
				boolean watch = p.getInventory().contains(stack -> stack.is(FmabItems.ENGRAVED_WATCH));
				return "parti=" + hohenheim.isRemoved() + " fin=" + done + " montre=" + watch
						+ " jour=" + PromisedDay.active(level);
			});
			check(ending.equals("parti=true fin=true montre=true jour=false"),
					"Hohenheim devrait se reposer et laisser la fin au joueur : " + ending);
			context.waitTicks(150);
			context.takeScreenshot("ending_epilogue");
		}
	}
}
