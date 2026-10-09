package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.progress.Milestones;
import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.LinkKind;
import com.ajustor.fmab.alchemy.circle.Stage;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.knowledge.Knowledge;
import com.ajustor.fmab.alchemy.knowledge.KnowledgeNode;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.TransmutationPose;
import com.ajustor.fmab.homunculus.AntiAlchemy;
import com.ajustor.fmab.homunculus.Belly;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabSounds;
import com.ajustor.fmab.stone.Karma;
import com.ajustor.fmab.stone.LivingStone;
import com.ajustor.fmab.stone.PhilosopherStones;
import com.ajustor.fmab.training.Trainings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Activation d'un cercle par un alchimiste : le serveur relit le tracé, vérifie le savoir, le
 * rang et la concentration, puis déclenche les effets ou le rebond.
 */
public final class Transmutation {
	/** Au-delà de cette gravité, le rebond fait exploser le support. */
	private static final double SHATTERING_SEVERITY = 0.9;

	private Transmutation() {
	}

	/** Ce qu'a donné l'activation, pour qui l'a demandée (un gant s'use, par exemple). */
	public enum Result {
		/** Le cercle n'a pas réagi : rien n'est dépensé. */
		INERT,
		/** Pas assez de concentration : rien n'est dépensé. */
		TIRED,
		REBOUND,
		/** L'énergie est partie, mais rien n'a trouvé de matière ou de cible. */
		NOTHING,
		DONE
	}

	/**
	 * Un cercle inscrit sur une surface : en cas de rebond, c'est lui qui brûle. Sa taille règle la
	 * puissance et le coût.
	 */
	/** Deux alchimistes qui posent les paumes sur le même cercle à moins de ce délai fusionnent. */
	private static final int FUSION_WINDOW = 30;
	private static final double FUSION_POWER = 1.75;
	/** Le dernier à avoir touché chaque cercle inscrit, et quand. */
	private record Touch(UUID player, long time) {
	}

	private static final Map<BlockPos, Touch> TOUCHES = new HashMap<>();

	/**
	 * Un autre alchimiste vient-il de poser ses paumes sur ce cercle ? Alors leurs énergies se
	 * mêlent. Note ce contact pour le suivant.
	 */
	private static boolean fuses(ServerLevel level, BlockPos circle, ServerPlayer caster) {
		long now = level.getGameTime();
		TOUCHES.values().removeIf(t -> now - t.time() > FUSION_WINDOW);
		Touch before = TOUCHES.put(circle.immutable(), new Touch(caster.getUUID(), now));
		if (before == null || before.player().equals(caster.getUUID())) {
			return false;
		}
		ServerPlayer partner = level.getServer().getPlayerList().getPlayer(before.player());
		Vec3 c = Vec3.atCenterOf(circle);
		level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.3, c.z, 40, 1, 0.2, 1, 0.05);
		caster.sendOverlayMessage(Component.translatable("transmutation.fmab.fused"));
		if (partner != null) {
			partner.sendOverlayMessage(Component.translatable("transmutation.fmab.fused"));
			Milestones.reach(partner, "fused");
		}
		Milestones.reach(caster, "fused");
		return true;
	}

	public static Result activate(ServerLevel level, BlockPos circle, BlockState state, Drawing drawing,
			ServerPlayer caster, CircleSize size) {
		return activate(level, circle, CircleFrame.of(state), drawing, caster, true, Integer.MAX_VALUE, size);
	}

	/** Un cercle porté (gant, tatouage) ou les mains jointes : de taille normale. */
	public static Result activate(ServerLevel level, BlockPos circle, CircleFrame frame, Drawing drawing,
			ServerPlayer caster, boolean inscribed, int maxStages) {
		return activate(level, circle, frame, drawing, caster, inscribed, maxStages, CircleSize.NORMAL);
	}

	/**
	 * @param circle    où le cercle agit : le bloc inscrit, ou la case devant la surface visée par un
	 *                  gant
	 * @param inscribed vrai pour un cercle inscrit, qui disparaît en cas de rebond
	 * @param maxStages étages que le support peut porter (un gant de tissu n'en porte qu'un)
	 * @param size      taille du cercle : portée, dégâts et coût en dépendent
	 */
	public static Result activate(ServerLevel level, BlockPos circle, CircleFrame frame, Drawing drawing,
			ServerPlayer caster, boolean inscribed, int maxStages, CircleSize size) {
		// Père capte l'énergie tectonique : autour de lui, rien ne s'allume.
		if (AntiAlchemy.blocks(level, circle, caster)) {
			return Result.INERT;
		}
		AlchemistData alchemist = caster.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		AlchemyRules rules = AlchemyRules.of(level.registryAccess());
		Knowledge knowledge = rules.knowledge(alchemist);
		Analysis analysis = rules.analyze(drawing, alchemist);

		if (analysis.outcome() == Analysis.Outcome.INERT) {
			CircleIssue first = analysis.issues().isEmpty() ? null : analysis.issues().getFirst();
			caster.sendOverlayMessage(first == null
					? Component.translatable("transmutation.fmab.inert")
					: Component.translatable(first.kind().translationKey()));
			level.playSound(null, circle, SoundEvents.SAND_STEP, SoundSource.BLOCKS, 0.6f, 0.8f);
			return Result.INERT;
		}
		if (analysis.parsed().stages().size() > maxStages) {
			caster.sendOverlayMessage(Component.translatable("transmutation.fmab.support_too_small", maxStages));
			return Result.INERT;
		}
		boolean watch = StateWatch.empowers(caster);
		// Une Pierre philosophale en main (ou dans le corps) : ni concentration, ni rebond d'instabilité.
		Optional<ItemStack> stone = PhilosopherStones.held(caster);
		boolean living = LivingStone.souls(caster) > 0;
		boolean amplified = stone.isPresent() || living;
		// Une pierre rouge impure amplifie, mais ne tient rien : le cercle rebondit comme sans elle.
		boolean steady = living || stone.filter(PhilosopherStones::pure).isPresent();
		// À deux sur le même cercle, les énergies se mêlent : plus fort, moitié moins cher.
		boolean fused = inscribed && fuses(level, circle, caster);
		double fusion = fused ? FUSION_POWER : 1;
		int cost = amplified ? 0 : (int) Math.ceil(StateWatch.cost(analysis.concentration(), watch) * size.cost()
				* (fused ? 0.5 : 1));
		if (alchemist.concentration() < cost) {
			caster.sendOverlayMessage(Component.translatable("transmutation.fmab.tired",
					cost, (int) alchemist.concentration()));
			return Result.TIRED;
		}
		caster.setAttached(FmabAttachments.ALCHEMIST,
				alchemist.withConcentration(alchemist.concentration() - cost));
		// L'énergie est partie : quoi qu'il arrive, on s'est familiarisé avec les glyphes tracés.
		practiceGlyphs(caster, rules, analysis);

		if (analysis.outcome() == Analysis.Outcome.REBOUND && !(steady && PhilosopherStones.steadies(analysis))) {
			rebound(level, circle, frame, caster, analysis.reboundSeverity(), inscribed);
			return Result.REBOUND;
		}
		// Un glyphe qu'on ne comprend pas peut tout faire basculer (sauf si la Pierre le tient).
		if (!steady && analysis.risk() > 0 && caster.getRandom().nextDouble() < analysis.risk()) {
			caster.sendSystemMessage(Component.translatable("transmutation.fmab.misunderstood"));
			rebound(level, circle, frame, caster, analysis.risk(), inscribed);
			return Result.REBOUND;
		}

		boolean anything = false;
		List<String> practiced = new ArrayList<>();
		List<String> done = new ArrayList<>();
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
				EffectContext ctx = new EffectContext(level, circle, frame, caster, effect, flow, knowledge,
						watch ? StateWatch.RANGE_BONUS : 0,
						size.power() * (amplified ? PhilosopherStones.AMPLIFICATION : 1) * fusion, 0);
				Effects.Result result = Effects.get(effect.combination().effect())
						.map(e -> e.apply(ctx))
						.orElse(Effects.Result.NO_TARGET);
				flow.removeIf(ItemStack::isEmpty);
				if (result == Effects.Result.DONE) {
					stageResult = result;
					anything = true;
					practiced.add(effect.combination().school());
					done.add(effect.combination().effect());
				} else {
					last = result;
				}
			}
			previous = stageResult;
		}
		dropAll(level, circle, flow);
		// Le geste : la paume plaquée sur le cercle (les mains jointes, l'appelant les remplace).
		caster.swing(InteractionHand.MAIN_HAND, true);
		TransmutationPose.strike(caster, TransmutationPose.Kind.PALM, 14);
		if (anything) {
			if (inscribed) {
				CHANNELS.put(caster.getUUID(), new Channel(circle, level.getGameTime(), 0));
			}
			TransmutationLightning.discharge(level, circle, size.blocks() / 2.0, 1);
			level.playSound(null, circle, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1, 1.4f);
			practice(caster, rules, knowledge, practiced);
			Trainings.onTransmutation(caster, analysis, frame, inscribed, done);
			Belly.transmuted(caster);
			stone.ifPresent(s -> PhilosopherStones.drain(caster, s, PhilosopherStones.cost(analysis)));
			if (living && stone.isEmpty() && analysis.parsed().stages().size() >= 2) {
				LivingStone.spend(caster, 1);
			}
			if (done.contains("fmab:repair")) {
				// Réparer ce qui est cassé : un peu de bien en ce monde.
				Karma.add(caster, 1);
			}
			return Result.DONE;
		}
		caster.sendOverlayMessage(Component.translatable(last == Effects.Result.NO_MATERIAL
				? "transmutation.fmab.no_material"
				: "transmutation.fmab.no_target"));
		TransmutationLightning.discharge(level, circle, size.blocks() / 2.0, 0.3);
		return Result.NOTHING;
	}

	/**
	 * Une transmutation qu'on prolonge en gardant la main sur le cercle.
	 *
	 * @param circle le cercle inscrit
	 * @param last   dernier tick où l'énergie a coulé
	 * @param growth combien de fois elle a été prolongée
	 */
	private record Channel(BlockPos circle, long last, int growth) {
	}

	private static final Map<UUID, Channel> CHANNELS = new HashMap<>();
	/** Le jeu répète le geste toutes les quatre ticks tant qu'on maintient le clic. */
	private static final int CHANNEL_GAP = 8;
	/** Au-delà, l'ouvrage ne grandit plus. */
	private static final int MAX_GROWTH = 12;

	/**
	 * L'alchimiste garde la main sur le cercle qu'il vient d'activer : l'énergie continue de couler.
	 * Les ouvrages grandissent (un mur monte, une pique s'allonge) tant qu'il reste de la matière,
	 * les jets se répètent. Chaque prolongation coûte un peu de concentration (rien avec une Pierre).
	 *
	 * @return vrai si le geste prolonge une transmutation (il ne faut pas en lancer une nouvelle)
	 */
	public static boolean channel(ServerLevel level, BlockPos circle, BlockState state, Drawing drawing,
			ServerPlayer caster, CircleSize size) {
		Channel channel = CHANNELS.get(caster.getUUID());
		long now = level.getGameTime();
		if (channel == null || !channel.circle().equals(circle) || now - channel.last() > CHANNEL_GAP) {
			CHANNELS.remove(caster.getUUID());
			return false;
		}
		if (AntiAlchemy.blocks(level, circle, caster)) {
			CHANNELS.remove(caster.getUUID());
			return true;
		}
		if (channel.growth() >= MAX_GROWTH) {
			return true;
		}
		AlchemistData alchemist = caster.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		AlchemyRules rules = AlchemyRules.of(level.registryAccess());
		Analysis analysis = rules.analyze(drawing, alchemist);
		boolean amplified = PhilosopherStones.held(caster).isPresent() || LivingStone.souls(caster) > 0;
		if (!amplified) {
			if (alchemist.concentration() < 1) {
				caster.sendOverlayMessage(Component.translatable("transmutation.fmab.tired", 1, 0));
				CHANNELS.remove(caster.getUUID());
				return true;
			}
			caster.setAttached(FmabAttachments.ALCHEMIST, alchemist.withConcentration(alchemist.concentration() - 1));
		}
		int growth = channel.growth() + 1;
		CircleFrame frame = CircleFrame.of(state);
		Knowledge knowledge = rules.knowledge(alchemist);
		boolean watch = StateWatch.empowers(caster);
		boolean anything = false;
		for (Analysis.StageEffect effect : analysis.effects()) {
			Effects.Channel mode = Effects.channel(effect.combination().effect());
			if (mode == Effects.Channel.ONCE) {
				continue;
			}
			EffectContext ctx = new EffectContext(level, circle, frame, caster, effect, new ArrayList<>(), knowledge,
					watch ? StateWatch.RANGE_BONUS : 0,
					size.power() * (amplified ? PhilosopherStones.AMPLIFICATION : 1),
					mode == Effects.Channel.GROW ? growth : 0);
			Effects.Result result = Effects.get(effect.combination().effect())
					.map(e -> e.apply(ctx))
					.orElse(Effects.Result.NO_TARGET);
			anything |= result == Effects.Result.DONE;
		}
		caster.swing(InteractionHand.MAIN_HAND, true);
		TransmutationPose.strike(caster, TransmutationPose.Kind.HOLD, CHANNEL_GAP + 2);
		if (!anything) {
			// Plus de matière, ou rien qui puisse grandir : l'énergie s'arrête.
			CHANNELS.remove(caster.getUUID());
			return true;
		}
		CHANNELS.put(caster.getUUID(), new Channel(circle, now, growth));
		TransmutationLightning.discharge(level, circle, size.blocks() / 2.0, 0.4);
		return true;
	}

	/**
	 * L'énergie revient sur l'alchimiste : quelques dégâts pour un cercle à peine instable, une
	 * explosion qui détruit le support pour un cercle inscrit qui n'avait aucune chance.
	 */
	private static void rebound(ServerLevel level, BlockPos circle, CircleFrame frame, ServerPlayer caster,
			double severity, boolean inscribed) {
		caster.hurtServer(level, level.damageSources().magic(), (float) (2 + 16 * severity));
		caster.sendOverlayMessage(Component.translatable("transmutation.fmab.rebound"));
		level.sendParticles(ParticleTypes.LARGE_SMOKE, circle.getX() + 0.5, circle.getY() + 0.2, circle.getZ() + 0.5,
				20, 0.6, 0.1, 0.6, 0.02);
		if (inscribed) {
			level.removeBlock(circle, false);
		}
		if (severity >= SHATTERING_SEVERITY && inscribed) {
			Vec3 c = Vec3.atCenterOf(circle);
			level.explode(null, c.x, c.y, c.z, 1.5f, Level.ExplosionInteraction.NONE);
			level.destroyBlock(circle.relative(frame.normal().getOpposite()), false);
		} else {
			level.playSound(null, circle, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, 0.7f);
		}
		level.playSound(null, circle, FmabSounds.REBOUND, SoundSource.BLOCKS, 1, 1);
	}

	/**
	 * On apprend en pratiquant : chaque effet réussi fait progresser la maîtrise de son école. Les
	 * nœuds atteints sont annoncés, et un Apprenti assez expérimenté devient Alchimiste.
	 */
	private static void practice(ServerPlayer caster, AlchemyRules rules, Knowledge before, List<String> schools) {
		AlchemistData data = caster.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		for (String school : schools) {
			data = data.addMastery(school, Knowledge.MASTERY_PER_EFFECT);
		}
		Knowledge after = rules.knowledge(data);
		for (KnowledgeNode node : after.nodes()) {
			if (after.has(node.id()) && !before.has(node.id())) {
				caster.sendSystemMessage(Component.translatable("knowledge.fmab.unlocked",
						Component.translatable(node.nameKey())));
			}
		}
		if (data.rank() == Rank.APPRENTICE && after.totalMastery() >= Knowledge.ALCHEMIST_MASTERY) {
			data = data.withRank(Rank.ALCHEMIST);
			caster.sendSystemMessage(Component.translatable("knowledge.fmab.promoted",
					Component.translatable(Rank.ALCHEMIST.translationKey())));
			caster.level().playSound(null, caster.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
					1, 1);
		}
		caster.setAttached(FmabAttachments.ALCHEMIST, data);
	}

	/**
	 * Chaque usage d'un glyphe pas encore compris rapproche de sa compréhension ; au bout de
	 * {@link AlchemistData#USES_TO_LEARN} usages, on le comprend.
	 */
	private static void practiceGlyphs(ServerPlayer caster, AlchemyRules rules, Analysis analysis) {
		AlchemistData data = caster.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		for (CircleIssue issue : analysis.issues()) {
			if (issue.kind() != CircleIssue.Kind.GLYPH_NOT_LEARNED) {
				continue;
			}
			String id = issue.detail();
			data = data.practiceGlyph(id);
			if (data.known().contains(id)) {
				rules.glyph(id).ifPresent(g -> caster.sendSystemMessage(Component.translatable(
						"transmutation.fmab.glyph_understood", Component.translatable(g.nameKey()))));
			}
		}
		caster.setAttached(FmabAttachments.ALCHEMIST, data);
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

}
