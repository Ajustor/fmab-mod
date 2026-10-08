package com.ajustor.fmab.block;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.data.FmabCodecs;
import com.ajustor.fmab.registry.FmabBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Mémorise le tracé du cercle inscrit sur une surface. On ne relit jamais les blocs autour : le
 * cercle est ce que la craie, la peinture ou le burin a enregistré.
 */
public class TransmutationCircleBlockEntity extends BlockEntity {
	private Drawing drawing = Drawing.EMPTY;

	public TransmutationCircleBlockEntity(BlockPos pos, BlockState state) {
		super(FmabBlockEntities.TRANSMUTATION_CIRCLE, pos, state);
	}

	public Drawing drawing() {
		return drawing;
	}

	public void setDrawing(Drawing drawing) {
		this.drawing = drawing;
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("drawing", FmabCodecs.DRAWING, drawing);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		drawing = input.read("drawing", FmabCodecs.DRAWING).orElse(Drawing.EMPTY);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
