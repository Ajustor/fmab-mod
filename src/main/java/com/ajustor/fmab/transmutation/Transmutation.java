package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.LinkKind;
import com.ajustor.fmab.alchemy.circle.Stage;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Activation d'un cercle par un alchimiste : le serveur relit le tracé, vérifie le savoir, le
 * rang et la concentration, puis déclenche les effets ou le rebond.
 */
public final class Transmutation {
	/** Au-delà de cette gravité, le rebond fait exploser le support. */
	private static final double SHATTERING_SEVERITY = 0.9;

	private Transmutation() {
	}

	public static void activate(ServerLevel level, BlockPos circle, Direction pageUp, Drawing drawing,
			ServerPlayer caster) {
		AlchemistData alchemist = caster.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		Analysis analysis = AlchemyRules.of(level.registryAccess())
				.analyze(drawing, alchemist.rank(), alchemist.known());

		if (analysis.outcome() == Analysis.Outcome.INERT) {
			CircleIssue first = analysis.issues().isEmpty() ? null : analysis.issues().getFirst();
			caster.sendOverlayMessage(first == null
					? Component.translatable("transmutation.fmab.inert")
					: Component.translatable(first.kind().translationKey()));
			level.playSound(null, circle, SoundEvents.SAND_STEP, SoundSource.BLOCKS, 0.6f, 0.8f);
			return;
		}
		if (alchemist.concentration() < analysis.concentration()) {
			caster.sendOverlayMessage(Component.translatable("transmutation.fmab.tired",
					analysis.concentration(), (int) alchemist.concentration()));
			return;
		}
		caster.setAttached(FmabAttachments.ALCHEMIST,
				alchemist.withConcentration(alchemist.concentration() - analysis.concentration()));

		if (analysis.outcome() == Analysis.Outcome.REBOUND) {
			rebound(level, circle, caster, analysis);
			return;
		}

		boolean anything = false;
		Effects.Result last = Effects.Result.DONE;
		// Résultat de l'étage précédent, pour les liaisons. Un étage sans effet est transparent.
		Effects.Result previous = Effects.Result.DONE;
		List<ItemStack> flow = new ArrayList<>();
		for (Stage stage : analysis.parsed().stages()) {
			List<Analysis.StageEffect> effects = analysis.effects().stream()
					.filter(e -> e.stage() == stage.index())
					.toList();
			if (effects.isEmpty() || !receives(stage, previous)) {
				continue;
			}
			if (stage.link() != LinkKind.SERIES) {
				// Seule une liaison en série transmet la matière ; le reste retombe sur le cercle.
				dropAll(level, circle, flow);
			}
			Effects.Result stageResult = Effects.Result.NO_TARGET;
			for (Analysis.StageEffect effect : effects) {
				EffectContext ctx = new EffectContext(level, circle, pageUp, caster, effect, flow);
				Effects.Result result = Effects.get(effect.combination().effect())
						.map(e -> e.apply(ctx))
						.orElse(Effects.Result.NO_TARGET);
				flow.removeIf(ItemStack::isEmpty);
				if (result == Effects.Result.DONE) {
					stageResult = result;
					anything = true;
				} else {
					last = result;
				}
			}
			previous = stageResult;
		}
		dropAll(level, circle, flow);
		if (anything) {
			sparks(level, circle, 40);
			level.playSound(null, circle, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1, 1.4f);
		} else {
			caster.sendOverlayMessage(Component.translatable(last == Effects.Result.NO_MATERIAL
					? "transmutation.fmab.no_material"
					: "transmutation.fmab.no_target"));
			sparks(level, circle, 8);
		}
	}

	/**
	 * L'énergie revient sur l'alchimiste : quelques dégâts pour un cercle à peine instable, une
	 * explosion qui détruit le support pour un cercle qui n'avait aucune chance.
	 */
	private static void rebound(ServerLevel level, BlockPos circle, ServerPlayer caster, Analysis analysis) {
		double severity = analysis.reboundSeverity();
		caster.hurtServer(level, level.damageSources().magic(), (float) (2 + 16 * severity));
		caster.sendOverlayMessage(Component.translatable("transmutation.fmab.rebound"));
		level.sendParticles(ParticleTypes.LARGE_SMOKE, circle.getX() + 0.5, circle.getY() + 0.2, circle.getZ() + 0.5,
				20, 0.6, 0.1, 0.6, 0.02);
		level.removeBlock(circle, false);
		if (severity >= SHATTERING_SEVERITY) {
			Vec3 c = Vec3.atCenterOf(circle);
			level.explode(null, c.x, c.y, c.z, 1.5f, Level.ExplosionInteraction.NONE);
			level.destroyBlock(circle.below(), false);
		} else {
			level.playSound(null, circle, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, 0.7f);
		}
	}

	/** L'étage reçoit-il l'énergie, compte tenu de sa liaison et du sort de l'étage précédent ? */
	private static boolean receives(Stage stage, Effects.Result previous) {
		if (stage.index() == 0) {
			return true;
		}
		return switch (stage.link()) {
			case NONE -> false;
			case SERIES -> previous == Effects.Result.DONE;
			case PARALLEL -> true;
			case CONDITIONAL -> previous != Effects.Result.DONE;
		};
	}

	/** Rien ne se perd : la matière que plus aucun étage n'utilise retombe sur le cercle. */
	private static void dropAll(ServerLevel level, BlockPos circle, List<ItemStack> flow) {
		Vec3 c = Vec3.atCenterOf(circle);
		for (ItemStack stack : flow) {
			if (!stack.isEmpty()) {
				level.addFreshEntity(new ItemEntity(level, c.x, c.y, c.z, stack));
			}
		}
		flow.clear();
	}

	/** Les éclairs bleus de la transmutation. */
	private static void sparks(ServerLevel level, BlockPos circle, int count) {
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, circle.getX() + 0.5, circle.getY() + 0.1,
				circle.getZ() + 0.5, count, 1.2, 0.2, 1.2, 0.05);
	}
}
