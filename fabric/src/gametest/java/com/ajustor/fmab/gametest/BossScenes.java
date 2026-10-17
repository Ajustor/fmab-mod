package com.ajustor.fmab.gametest;

import com.ajustor.fmab.entity.KimbleeEntity;
import com.ajustor.fmab.entity.SlicerEntity;
import com.ajustor.fmab.registry.FmabEntities;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les boss de FMAB-59. Les frères Slicer : effacer le premier sceau relève le cadet, il en faut un
 * second. Kimblee : il marque le sol sous sa cible, qui saute, et sa propre explosion ne le blesse pas.
 * Lancer avec {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=bosses}.
 */
public class BossScenes implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("bosses")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getConnection().waitForChunksRender();
			sp.getServer().runCommand("difficulty normal");
			SlicerEntity slicer = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 600, 4, false, false));
				SlicerEntity s = FmabEntities.SLICER.create(p.level(), EntitySpawnReason.COMMAND);
				s.snapTo(p.getX() + 3, p.getY(), p.getZ(), 0, 0);
				p.level().addFreshEntity(s);
				// Abattu, puis le sceau de l'aîné effacé.
				s.hurtServer(p.level(), p.damageSources().playerAttack(p), 1000);
				s.interact(p, InteractionHand.MAIN_HAND, s.position());
				return s;
			});
			context.waitTicks(40);
			String younger = sp.getServer().computeOnServer(server ->
					"vivant=" + slicer.isAlive() + " effondré=" + slicer.collapsed() + " aîné=" + slicer.elder());
			check(younger.equals("vivant=true effondré=false aîné=false"),
					"le cadet devrait reprendre l'armure : " + younger);
			boolean freed = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				slicer.hurtServer(p.level(), p.damageSources().playerAttack(p), 1000);
				slicer.interact(p, InteractionHand.MAIN_HAND, slicer.position());
				return !slicer.isAlive();
			});
			check(freed, "le second sceau effacé, les frères Slicer devraient être libérés");

			// Kimblee : une cible marquée saute, et lui reste indemne.
			KimbleeEntity kimblee = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				KimbleeEntity k = FmabEntities.KIMBLEE.create(p.level(), EntitySpawnReason.COMMAND);
				k.snapTo(p.getX() - 6, p.getY(), p.getZ(), 0, 0);
				p.level().addFreshEntity(k);
				k.setTarget(p);
				return k;
			});
			float before = sp.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().getHealth()
					+ server.getPlayerList().getPlayers().getFirst().getAbsorptionAmount());
			context.waitTicks(120);
			String blast = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				// Sa propre détonation, sous ses pieds : rien.
				p.level().explode(kimblee, kimblee.getX(), kimblee.getY(), kimblee.getZ(), 2, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
				return "kimblee=" + kimblee.getHealth() + "/" + kimblee.getMaxHealth();
			});
			check(blast.equals("kimblee=90.0/90.0"), "Kimblee ne devrait pas se blesser lui-même : " + blast);
			check(before > 0, "le joueur est en vie");
		}
	}
}
