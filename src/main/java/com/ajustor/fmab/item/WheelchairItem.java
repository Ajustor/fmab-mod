package com.ajustor.fmab.item;

import com.ajustor.fmab.entity.WheelchairEntity;
import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Le fauteuil plié : on le déplie sur le sol, tourné comme soi, prêt à s'y asseoir. */
public class WheelchairItem extends Item {
	public WheelchairItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		WheelchairEntity chair = FmabEntities.WHEELCHAIR.create(server, EntitySpawnReason.SPAWN_ITEM_USE);
		if (chair == null) {
			return InteractionResult.FAIL;
		}
		Player player = context.getPlayer();
		float yaw = player == null ? 0 : player.getYRot();
		chair.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
		chair.setYHeadRot(yaw);
		chair.yBodyRot = yaw;
		if (!level.noCollision(chair, chair.getBoundingBox())) {
			return InteractionResult.FAIL;
		}
		server.addFreshEntity(chair);
		level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
		context.getItemInHand().consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
