package com.ajustor.fmab.item;

import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.entity.AlchemicalMineEntity;
import com.ajustor.fmab.homunculus.AntiAlchemy;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Les cercles écarlates de Kimblee, tatoués sur ses paumes (ici, gravés sur des plaques qu'on serre
 * dans la main). Clic droit sur un bloc : sa matière devient une mine alchimique. Accroupi, clic
 * droit : les mains claquent, toutes vos mines sautent.
 */
public class CrimsonSealsItem extends Item {
	private static final float COST = 3;
	private static final int MAX_MINES = 5;
	private static final double REACH = 64;
	private static final DustParticleOptions CRIMSON = new DustParticleOptions(0xC01030, 1.0f);

	public CrimsonSealsItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getPlayer() == null || context.getPlayer().isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer p)) {
			return InteractionResult.SUCCESS;
		}
		if (AntiAlchemy.blocks(level, context.getClickedPos(), p) || !pay(p)) {
			return InteractionResult.SUCCESS;
		}
		List<AlchemicalMineEntity> mine = mines(level, p);
		if (mine.size() >= MAX_MINES) {
			// La plus ancienne se défait.
			mine.stream().max(Comparator.comparingInt(e -> e.tickCount)).ifPresent(AlchemicalMineEntity::discard);
		}
		Vec3 at = context.getClickLocation();
		level.addFreshEntity(new AlchemicalMineEntity(level, at, p.getUUID()));
		level.sendParticles(CRIMSON, at.x, at.y, at.z, 20, 0.25, 0.25, 0.25, 0);
		level.playSound(null, context.getClickedPos(), FmabSounds.MINE_ARM, SoundSource.PLAYERS, 0.8f, 1);
		p.sendOverlayMessage(Component.translatable("item.fmab.crimson_seals.armed",
				Math.min(MAX_MINES, mine.size() + 1), MAX_MINES));
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server && player instanceof ServerPlayer p) {
			// Le claquement des mains : tout saute.
			server.playSound(null, p.blockPosition(), FmabSounds.CLAP, SoundSource.PLAYERS, 1, 0.9f);
			List<AlchemicalMineEntity> mines = mines(server, p);
			mines.forEach(m -> m.detonate(server));
			if (mines.isEmpty()) {
				p.sendOverlayMessage(Component.translatable("item.fmab.crimson_seals.none"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	private static List<AlchemicalMineEntity> mines(ServerLevel level, ServerPlayer owner) {
		return level.getEntitiesOfClass(AlchemicalMineEntity.class, new AABB(owner.blockPosition()).inflate(REACH),
				m -> m.owner().map(owner.getUUID()::equals).orElse(false));
	}

	private static boolean pay(ServerPlayer player) {
		if (player.isCreative()) {
			return true;
		}
		AlchemistData data = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		if (data.concentration() < COST) {
			player.sendOverlayMessage(Component.translatable("transmutation.fmab.tired", (int) COST,
					(int) data.concentration()));
			return false;
		}
		player.setAttached(FmabAttachments.ALCHEMIST, data.withConcentration(data.concentration() - COST));
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.crimson_seals.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
