package com.ajustor.fmab.item;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Une arme à feu de l'armée d'Amestris, celles de Riza Hawkeye : le tir part en ligne droite et
 * touche le premier obstacle. Chaque tir consomme une cartouche. Les balles sont des projectiles :
 * King Bradley les voit venir.
 */
public class FirearmItem extends Item {
	public static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, Fmab.id("bullet"));

	private final float damage;
	private final double range;
	private final int cooldown;
	private final float spread;

	public FirearmItem(float damage, double range, int cooldown, float spread, Properties properties) {
		super(properties);
		this.damage = damage;
		this.range = range;
		this.cooldown = cooldown;
		this.spread = spread;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack gun = player.getItemInHand(hand);
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		if (!player.isCreative() && !consumeCartridge(player)) {
			server.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.6f);
			player.sendOverlayMessage(Component.translatable("item.fmab.firearm.empty"));
			return InteractionResult.FAIL;
		}
		player.getCooldowns().addCooldown(gun, cooldown);
		fire(server, player);
		gun.hurtAndBreak(1, player, hand);
		return InteractionResult.SUCCESS;
	}

	private static boolean consumeCartridge(Player player) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(FmabItems.CARTRIDGE)) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	private void fire(ServerLevel level, Player shooter) {
		Vec3 eye = shooter.getEyePosition();
		Vec3 look = shooter.getLookAngle().add(shooter.getRandom().triangle(0, spread),
				shooter.getRandom().triangle(0, spread), shooter.getRandom().triangle(0, spread)).normalize();
		Vec3 end = eye.add(look.scale(range));
		BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
				shooter));
		if (block.getType() != HitResult.Type.MISS) {
			end = block.getLocation();
		}
		EntityHitResult entity = ProjectileUtil.getEntityHitResult(shooter, eye, end,
				new AABB(eye, end).inflate(1), e -> e instanceof LivingEntity && e.isPickable() && !e.isSpectator(),
				eye.distanceToSqr(end));
		if (entity != null) {
			end = entity.getLocation();
		}
		// La flamme du canon, la traînée, l'impact.
		Vec3 muzzle = eye.add(look.scale(0.8)).subtract(0, 0.15, 0);
		level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 6, 0.05, 0.05, 0.05, 0.02);
		level.sendParticles(ParticleTypes.SMALL_FLAME, muzzle.x, muzzle.y, muzzle.z, 3, 0.03, 0.03, 0.03, 0.01);
		Vec3 step = end.subtract(muzzle);
		int points = (int) (step.length() * 1.5);
		for (int i = 0; i < points; i++) {
			Vec3 p = muzzle.add(step.scale((double) i / points));
			level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
		level.playSound(null, shooter.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.6f, 0.6f);
		if (entity != null) {
			Entity target = entity.getEntity();
			DamageSource bullet = new DamageSource(level.damageSources().damageTypes.getOrThrow(BULLET), shooter, shooter);
			target.hurtServer(level, bullet, damage);
		} else if (block.getType() == HitResult.Type.BLOCK) {
			level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 4, 0.05, 0.05, 0.05, 0.01);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.firearm.stats", (int) damage, (int) range)
				.withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.fmab.firearm.tooltip").withStyle(ChatFormatting.DARK_GRAY));
	}
}
