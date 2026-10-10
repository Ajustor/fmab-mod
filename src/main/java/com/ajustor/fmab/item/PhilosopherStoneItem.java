package com.ajustor.fmab.item;

import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.gate.GateOfTruth;
import com.ajustor.fmab.gate.Restoration;
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

import java.util.Map;
import java.util.function.Consumer;

/**
 * La Pierre philosophale : des âmes humaines condensées en une pierre rouge (sa barre d'usure
 * compte les âmes qui restent). Elle échappe à l'échange équivalent : qui a laissé quelque chose à
 * la Porte s'en sert pour le reprendre à la Vérité, et chaque partie rendue lui coûte des âmes (voir
 * {@link Restoration}). Vide, elle tombe en poussière.
 */
public class PhilosopherStoneItem extends Item {
	/** Les éclairs rouges d'une transmutation par la Pierre. */
	private static final DustParticleOptions RED = new DustParticleOptions(0xD01020, 1.1f);

	public PhilosopherStoneItem(Properties properties) {
		super(properties);
	}

	/** Les âmes qui restent dans la Pierre. */
	public static int souls(ItemStack stone) {
		return stone.getMaxDamage() - stone.getDamageValue();
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server)) {
			return InteractionResult.SUCCESS;
		}
		GateState gate = server.getAttachedOrCreate(FmabAttachments.GATE);
		if (gate.lost().isEmpty()) {
			server.sendOverlayMessage(Component.translatable("item.fmab.philosopher_stone.pulses"));
			return InteractionResult.SUCCESS;
		}
		ItemStack stone = player.getItemInHand(hand);
		Restoration.Plan plan = Restoration.plan(gate.lost(), player.isCreative() ? Integer.MAX_VALUE : souls(stone));
		if (plan.restored().isEmpty()) {
			server.sendOverlayMessage(Component.translatable("item.fmab.philosopher_stone.too_weak", souls(stone)));
			return InteractionResult.SUCCESS;
		}
		restore(server, gate, plan);
		if (!player.isCreative()) {
			stone.hurtAndBreak(plan.souls(), server.level(), server, broken -> server.sendSystemMessage(
					Component.translatable("item.fmab.philosopher_stone.spent").withStyle(ChatFormatting.DARK_RED)));
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * La Pierre paie ce que la Vérité exigerait. Une âme qui retrouve son corps quitte son armure,
	 * qui reste debout, vide ; les automails des membres rendus retournent au sac.
	 */
	public static void restore(ServerPlayer player, GateState gate, Restoration.Plan plan) {
		ServerLevel level = player.level();
		if (plan.restored().contains(BodyPart.BODY) && gate.inArmor()) {
			SoulBinding.leaveShell(player, false);
		}
		player.setAttached(FmabAttachments.GATE, gate.restore(plan.restored()));
		GateOfTruth.returnParts(player);
		Automail automail = player.getAttachedOrCreate(FmabAttachments.AUTOMAIL);
		for (Map.Entry<BodyPart, ItemStack> limb : automail.limbs().entrySet()) {
			if (plan.restored().contains(limb.getKey())) {
				if (!player.getInventory().add(limb.getValue().copy())) {
					player.drop(limb.getValue().copy(), false);
				}
				automail = automail.with(limb.getKey(), ItemStack.EMPTY);
			}
		}
		player.setAttached(FmabAttachments.AUTOMAIL, automail);
		player.setHealth(player.getMaxHealth());
		level.sendParticles(RED, player.getX(), player.getY(0.5), player.getZ(), 120, 0.5, 1, 0.5, 0);
		level.playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 1, 1.2f);
		for (BodyPart part : plan.restored()) {
			player.sendSystemMessage(Component.translatable("item.fmab.philosopher_stone.restored",
					Component.translatable(part.translationKey()), Restoration.cost(part)).withStyle(ChatFormatting.RED));
		}
		if (plan.restored().size() < gate.lost().size()) {
			player.sendSystemMessage(Component.translatable("item.fmab.philosopher_stone.not_all")
					.withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.fmab.philosopher_stone.souls", souls(stack))
				.withStyle(ChatFormatting.RED));
		tooltip.accept(Component.translatable("item.fmab.philosopher_stone.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
