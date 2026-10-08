package com.ajustor.fmab.item;

import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.GateOfTruth;
import com.ajustor.fmab.gate.SoulBinding;
import com.ajustor.fmab.registry.FmabAttachments;
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
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * La Pierre philosophale : des âmes humaines condensées en une pierre rouge. Elle échappe à
 * l'échange équivalent : une âme fixée dans une armure s'en sert pour reprendre son corps à la
 * Vérité, et la Pierre se consume.
 */
public class PhilosopherStoneItem extends Item {
	/** Les éclairs rouges d'une transmutation par la Pierre. */
	private static final DustParticleOptions RED = new DustParticleOptions(0xD01020, 1.1f);

	public PhilosopherStoneItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server)) {
			return InteractionResult.SUCCESS;
		}
		GateState gate = server.getAttachedOrCreate(FmabAttachments.GATE);
		if (!gate.inArmor()) {
			server.sendOverlayMessage(Component.translatable("item.fmab.philosopher_stone.pulses"));
			return InteractionResult.SUCCESS;
		}
		restoreBody(server, gate);
		if (!player.isCreative()) {
			player.getItemInHand(hand).shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * La Pierre paie ce que la Vérité exigerait : le corps revient, l'âme quitte l'armure, qui reste
	 * debout, vide. Ce que la Vérité avait pris d'autre reste à elle.
	 */
	private static void restoreBody(ServerPlayer player, GateState gate) {
		ServerLevel level = player.level();
		SoulBinding.leaveShell(player, false);
		player.setAttached(FmabAttachments.GATE, gate.restore(BodyPart.BODY));
		GateOfTruth.returnPart(player, BodyPart.BODY);
		player.setHealth(player.getMaxHealth());
		level.sendParticles(RED, player.getX(), player.getY(0.5), player.getZ(), 120, 0.5, 1, 0.5, 0);
		level.playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 1, 1.2f);
		player.sendSystemMessage(Component.translatable("item.fmab.philosopher_stone.body_restored")
				.withStyle(ChatFormatting.RED));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.philosopher_stone.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
