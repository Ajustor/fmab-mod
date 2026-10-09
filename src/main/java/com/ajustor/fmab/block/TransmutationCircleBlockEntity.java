package com.ajustor.fmab.block;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.data.FmabCodecs;
import com.ajustor.fmab.registry.FmabBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;
import java.util.UUID;

/**
 * Mémorise le tracé du cercle inscrit sur une surface. On ne relit jamais les blocs autour : le
 * cercle est ce que la craie, la peinture ou le burin a enregistré.
 */
public class TransmutationCircleBlockEntity extends BlockEntity {
	private Drawing drawing = Drawing.EMPTY;
	private CircleSize size = CircleSize.NORMAL;
	private CircleTrigger trigger = CircleTrigger.HAND;
	/** Qui a armé le déclencheur : le cercle puise dans sa concentration. */
	private UUID author;
	/** Dernier départ automatique (piège, redstone), en temps de jeu. */
	private long lastFired = Long.MIN_VALUE / 2;
	private boolean powered;
	/** Dernière fois qu'une créature se tenait sur le cercle (piège). */
	private long lastOccupied = Long.MIN_VALUE / 2;

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

	public CircleSize size() {
		return size;
	}

	public void setSize(CircleSize size) {
		this.size = size;
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public CircleTrigger trigger() {
		return trigger;
	}

	public Optional<UUID> author() {
		return Optional.ofNullable(author);
	}

	/** Arme un déclencheur ; celui qui le règle en devient l'auteur. */
	public void setTrigger(CircleTrigger trigger, UUID author) {
		this.trigger = trigger;
		this.author = author;
		setChanged();
	}

	/** Le cercle peut-il repartir tout seul ? Sinon, il attend. Note l'heure du départ. */
	public boolean rearm(long now) {
		if (now - lastFired < CircleTrigger.REARM_TICKS) {
			return false;
		}
		lastFired = now;
		return true;
	}

	/**
	 * Une créature se tient sur le piège.
	 *
	 * @return vrai si elle vient d'y entrer (il était vide juste avant)
	 */
	public boolean step(long now) {
		boolean entering = now - lastOccupied > 2;
		lastOccupied = now;
		return entering;
	}

	/** @return vrai si le signal vient d'arriver (front montant) */
	public boolean power(boolean signal) {
		boolean rising = signal && !powered;
		if (signal != powered) {
			powered = signal;
			setChanged();
		}
		return rising;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("drawing", FmabCodecs.DRAWING, drawing);
		output.putInt("size", size.blocks());
		output.putInt("trigger", trigger.ordinal());
		if (author != null) {
			output.store("author", UUIDUtil.CODEC, author);
		}
		output.putBoolean("powered", powered);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		drawing = input.read("drawing", FmabCodecs.DRAWING).orElse(Drawing.EMPTY);
		size = CircleSize.ofBlocks(input.getIntOr("size", CircleSize.NORMAL.blocks()));
		trigger = CircleTrigger.byOrdinal(input.getIntOr("trigger", 0));
		author = input.read("author", UUIDUtil.CODEC).orElse(null);
		powered = input.getBooleanOr("powered", false);
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
