package com.ajustor.fmab.item;

import com.ajustor.fmab.entity.ThrowingKnifeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Les couteaux de lancer de Hughes : légers, rapides, bon marché. Clic droit pour lancer. */
public class ThrowingKnifeItem extends Item {
	public ThrowingKnifeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server) {
			ThrowingKnifeEntity knife = new ThrowingKnifeEntity(server, player, stack);
			knife.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 2.4f, 0.4f);
			if (player.getAbilities().instabuild) {
				knife.pickup = ThrowingKnifeEntity.Pickup.CREATIVE_ONLY;
			}
			server.addFreshEntity(knife);
			server.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.5f,
					1.9f);
		}
		player.getCooldowns().addCooldown(stack, 5);
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
