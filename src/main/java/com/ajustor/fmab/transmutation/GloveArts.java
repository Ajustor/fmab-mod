package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.item.GloveItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Ce que certains gants font sans cercle, comme dans l'anime. Les gants à étincelles de Mustang : un
 * claquement de doigts, et une gerbe de feu éclate là où l'on regarde. Les gantelets d'Armstrong : un
 * coup de poing au sol, et une ligne de piques jaillit devant lui.
 */
public final class GloveArts {
	static final float SNAP_COST = 4;
	static final float PUNCH_COST = 3;
	private static final double SNAP_REACH = 24;
	private static final float SNAP_DAMAGE = 6;
	private static final float SPIKE_DAMAGE = 5;
	/** La ligne de piques, en blocs. */
	static final int PUNCH_LENGTH = 5;

	private GloveArts() {
	}

	/**
	 * Là où l'on vise : la créature dans le regard, sinon le bloc d'air devant la surface visée (et sa
	 * face), sinon le bout du regard, en plein ciel.
	 *
	 * @param at   la case où agir
	 * @param face la face de la surface visée, ou null s'il n'y en a pas (créature, ou rien)
	 */
	record Aim(BlockPos at, @Nullable Direction face) {
	}

	static Aim aim(ServerPlayer player, double reach) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(reach));
		BlockHitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, player));
		boolean surface = block.getType() == HitResult.Type.BLOCK;
		if (surface) {
			end = block.getLocation();
		}
		EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, eye, end, new AABB(eye, end).inflate(1),
				e -> e instanceof LivingEntity && e.isPickable() && !e.isSpectator(), eye.distanceToSqr(end));
		if (entity != null) {
			return new Aim(BlockPos.containing(entity.getLocation()), null);
		}
		if (surface) {
			return new Aim(block.getBlockPos().relative(block.getDirection()), block.getDirection());
		}
		return new Aim(BlockPos.containing(end), null);
	}

	/** Porte-t-il un gant de cette sorte, sans cercle, dans une main libre ? */
	static boolean bare(ServerPlayer player, GloveItem.Kind kind) {
		Gloves gloves = FmabAttachments.GLOVES.getOrCreate(player);
		for (boolean left : new boolean[]{false, true}) {
			if (gloves.get(left).getItem() instanceof GloveItem item && item.kind() == kind
					&& !gloves.get(left).has(com.ajustor.fmab.registry.FmabComponents.GLOVE_CIRCLE)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Le claquement de Mustang : une gerbe de feu, au loin, là où l'on regarde. L'étincelle enflamme
	 * l'air même : sur une surface, une créature, ou en plein ciel.
	 */
	static void snap(ServerPlayer player) {
		ServerLevel level = player.level();
		if (player.isInWaterOrRain()) {
			player.sendOverlayMessage(Component.translatable("transmutation.fmab.glove_wet"));
			level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.6f);
			return;
		}
		if (!pay(player, SNAP_COST)) {
			return;
		}
		BlockPos at = aim(player, SNAP_REACH).at();
		level.playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1, 1.4f);
		level.playSound(null, at, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2f, 0.9f);
		Vec3 center = Vec3.atCenterOf(at);
		level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 60, 1.2, 0.8, 1.2, 0.05);
		for (BlockPos p : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
			if (level.getBlockState(p).isAir() && BaseFireBlock.canBePlacedAt(level, p, Direction.UP)) {
				level.setBlockAndUpdate(p, BaseFireBlock.getState(level, p));
			}
		}
		DamageSource fire = new DamageSource(level.damageSources().damageTypes.getOrThrow(DamageTypes.IN_FIRE), player);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at).inflate(2.5),
				e -> e.isAlive() && !Allies.spares(player, e))) {
			e.hurtServer(level, fire, SNAP_DAMAGE);
			e.igniteForSeconds(5);
		}
	}

	/** Le coup de poing d'Armstrong : une ligne de piques de terre, de la face frappée vers l'avant. */
	static void punch(ServerPlayer player, BlockPos struck) {
		ServerLevel level = player.level();
		if (!pay(player, PUNCH_COST)) {
			return;
		}
		Direction d = player.getDirection();
		DamageSource stone = level.damageSources().indirectMagic(player, player);
		for (int i = 1; i <= PUNCH_LENGTH; i++) {
			BlockPos top = surface(level, struck.relative(d, i));
			if (top == null) {
				continue;
			}
			// La terre se soulève d'un bloc : la pique, faite du sol même.
			BlockState ground = level.getBlockState(top);
			// La terre au sens large : celle de l'alchimie, et l'herbe ou la terre qu'on frappe d'ordinaire.
			boolean earth = ground.is(FmabTags.elementBlocks("earth")) || ground.is(BlockTags.DIRT)
					|| ground.is(Blocks.GRASS_BLOCK);
			if (!earth || ground.hasBlockEntity() || !level.getBlockState(top.above()).isAir()) {
				continue;
			}
			level.setBlockAndUpdate(top.above(), ground);
			level.setBlockAndUpdate(top, Blocks.AIR.defaultBlockState());
			level.sendParticles(ParticleTypes.POOF, top.getX() + 0.5, top.getY() + 1.2, top.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0);
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(top.above()).inflate(0.3, 1, 0.3),
					e -> e.isAlive() && !Allies.spares(player, e))) {
				e.hurtServer(level, stone, SPIKE_DAMAGE);
				e.push(0, 0.6, 0);
			}
		}
		level.playSound(null, struck, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.5f, 0.6f);
	}

	/** Le bloc de surface de cette colonne, à deux blocs près. */
	private static BlockPos surface(ServerLevel level, BlockPos around) {
		for (int dy = 2; dy >= -2; dy--) {
			BlockPos p = around.above(dy);
			if (!level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) {
				return p;
			}
		}
		return null;
	}

	private static boolean pay(ServerPlayer player, float cost) {
		if (player.isCreative()) {
			return true;
		}
		float concentration = Concentration.of(player);
		if (concentration < cost) {
			player.sendOverlayMessage(Component.translatable("transmutation.fmab.tired", (int) Math.ceil(cost),
					(int) concentration));
			return false;
		}
		Concentration.set(player, concentration - cost);
		return true;
	}
}
