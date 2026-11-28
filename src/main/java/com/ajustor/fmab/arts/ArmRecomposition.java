package com.ajustor.fmab.arts;

import com.ajustor.fmab.alchemy.exchange.Composition;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.registry.FmabSounds;
import com.ajustor.fmab.transmutation.Recomposition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * La moitié de Xing du motif d'Ishval : la main pose le bras sur la matière, et l'objet auquel
 * l'alchimiste pense (sa conception, comme pour un cercle Recomposer) en sort. Pas de cercle à tracer :
 * le cercle est dans le bras.
 *
 * <p>La matière vient des blocs que le bras touche : celui qu'on vise d'abord, puis les plus proches
 * autour, jusqu'à {@link #REACH} blocs, tant qu'il manque un élément de l'objet. Un bloc pris l'est tout
 * entier : l'échange est équivalent, ce qui dépasse est perdu. Ce qui manque, et rien n'est pris. Une
 * Pierre philosophale crée l'objet de toutes pièces.
 */
public final class ArmRecomposition {
	/** Jusqu'où, autour du bloc visé, le bras va chercher sa matière. */
	static final int REACH = 2;
	/** Blocs au plus défaits pour un objet. */
	static final int MAX_BLOCKS = 64;

	private ArmRecomposition() {
	}

	/**
	 * Les blocs à défaire pour payer {@code cost}, dans l'ordre où ils viennent (le plus proche
	 * d'abord) : un bloc n'est pris que s'il apporte un élément qui manque encore.
	 *
	 * @return les indices des blocs pris, ou vide s'ils ne suffisent pas
	 */
	static Optional<List<Integer>> pick(List<Map<String, Double>> blocks, Map<String, Integer> cost) {
		Map<String, Double> need = new HashMap<>();
		cost.forEach((element, mass) -> {
			if (mass > 0) {
				need.put(element, (double) mass);
			}
		});
		List<Integer> taken = new ArrayList<>();
		for (int i = 0; i < blocks.size() && !need.isEmpty() && taken.size() < MAX_BLOCKS; i++) {
			Map<String, Double> block = blocks.get(i);
			if (block.keySet().stream().noneMatch(need::containsKey)) {
				continue;
			}
			taken.add(i);
			block.forEach((element, mass) -> need.computeIfPresent(element,
					(e, left) -> left - mass <= 1e-9 ? null : left - mass));
		}
		return need.isEmpty() ? Optional.of(taken) : Optional.empty();
	}

	/** Ce que les blocs contiennent en tout, élément par élément. */
	static Map<String, Double> available(List<Map<String, Double>> blocks) {
		Map<String, Double> out = new HashMap<>();
		for (Map<String, Double> block : blocks) {
			block.forEach((element, mass) -> out.merge(element, mass, Double::sum));
		}
		return out;
	}

	/**
	 * Le bras touche {@code target} : l'objet auquel l'alchimiste pense en sort, fait de la matière
	 * alentour.
	 *
	 * @param pay le bras paie (sa concentration), une fois la matière trouvée ; faux, rien ne se fait
	 * @return vrai si l'objet a été créé
	 */
	public static boolean recompose(ServerPlayer player, ServerLevel level, BlockPos target, Direction face,
			BooleanSupplier pay) {
		Optional<Recomposition.Blueprint> found = Recomposition.blueprint(player);
		if (found.isEmpty()) {
			return false;
		}
		Recomposition.Blueprint blueprint = found.get();
		ItemStack made = new ItemStack(blueprint.item());
		Vec3 out = Vec3.atCenterOf(target).add(face.getStepX() * 0.6, face.getStepY() * 0.6,
				face.getStepZ() * 0.6);
		if (Recomposition.fromNothing(player)) {
			if (!pay.getAsBoolean()) {
				return false;
			}
			finish(player, level, target, out, made);
			return true;
		}
		List<BlockPos> positions = new ArrayList<>();
		List<Map<String, Double>> blocks = new ArrayList<>();
		for (BlockPos p : around(target)) {
			Optional<Composition> composition = material(player, level, p);
			if (composition.isPresent()) {
				positions.add(p);
				blocks.add(composition.get().masses());
			}
		}
		Map<String, Integer> cost = blueprint.composition().cost();
		Optional<List<Integer>> taken = pick(blocks, cost);
		if (taken.isEmpty()) {
			player.sendSystemMessage(Component.translatable("transmutation.fmab.copy.missing", made.getHoverName(),
					missing(cost, available(blocks))));
			return false;
		}
		if (!pay.getAsBoolean()) {
			return false;
		}
		for (int i : taken.get()) {
			BlockPos p = positions.get(i);
			BlockState state = level.getBlockState(p);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.getX() + 0.5, p.getY() + 0.5,
					p.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.1);
			level.destroyBlock(p, false, player);
		}
		finish(player, level, target, out, made);
		return true;
	}

	private static void finish(ServerPlayer player, ServerLevel level, BlockPos target, Vec3 at, ItemStack made) {
		ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, made);
		item.setDefaultPickUpDelay();
		level.addFreshEntity(item);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 20, 0.3, 0.3, 0.3, 0.1);
		level.playSound(null, target, FmabSounds.TRANSMUTE, SoundSource.PLAYERS, 0.8f, 1.2f);
		player.sendOverlayMessage(Component.translatable("scar_arm.fmab.recomposed", made.getHoverName()));
	}

	/** Les blocs à portée du bras, du bloc visé aux plus lointains. */
	private static List<BlockPos> around(BlockPos target) {
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(target.offset(-REACH, -REACH, -REACH),
				target.offset(REACH, REACH, REACH))) {
			out.add(p.immutable());
		}
		out.sort(Comparator.comparingDouble(p -> p.distSqr(target)));
		return out;
	}

	/** Ce dont un bloc est fait, s'il peut servir de matière : ni air, ni incassable, ni conteneur, ni protégé. */
	private static Optional<Composition> material(ServerPlayer player, ServerLevel level, BlockPos p) {
		BlockState state = level.getBlockState(p);
		if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, p) < 0
				|| state.getBlock() instanceof TransmutationCircleBlock
				|| !player.mayInteract(level, p) || player.blockActionRestricted(level, p, player.gameMode())) {
			return Optional.empty();
		}
		return Recomposition.composition(level, state.getBlock().asItem());
	}

	private static MutableComponent missing(Map<String, Integer> cost, Map<String, Double> available) {
		MutableComponent out = Component.empty();
		Map<String, Integer> sorted = new LinkedHashMap<>(cost);
		sorted.forEach((element, mass) -> {
			int have = (int) Math.floor(available.getOrDefault(element, 0.0) + 1e-9);
			if (have >= mass) {
				return;
			}
			if (!out.getSiblings().isEmpty()) {
				out.append(", ");
			}
			out.append(Component.translatable("transmutation.fmab.copy.amount", Recomposition.element(element), have,
					mass));
		});
		return out;
	}
}
