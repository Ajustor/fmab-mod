package com.ajustor.fmab.fabric;

import com.ajustor.fmab.platform.data.DataSlot;
import com.ajustor.fmab.platform.data.DataSlots;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/** Les données d'entité du mod, en attachements de Fabric API. */
final class FabricDataSlots implements DataSlots {
	@Override
	public <T> DataSlot<T> register(Identifier id, DataSlot.Spec<T> spec) {
		AttachmentRegistry.Builder<T> builder = AttachmentRegistry.builder();
		spec.codec().ifPresent(builder::persistent);
		spec.initializer().ifPresent(builder::initializer);
		if (spec.copyOnDeath()) {
			builder.copyOnDeath();
		}
		spec.syncCodec().ifPresent(codec -> builder.syncWith(codec, switch (spec.sync()) {
			case OWNER -> AttachmentSyncPredicate.targetOnly();
			case ALL -> AttachmentSyncPredicate.all();
		}));
		return new Slot<>(id, builder.buildAndRegister(id));
	}

	private record Slot<T>(Identifier id, AttachmentType<T> type) implements DataSlot<T> {
		@Override
		public @Nullable T get(Entity holder) {
			return holder.getAttached(type);
		}

		@Override
		public T getOrCreate(Entity holder) {
			return holder.getAttachedOrCreate(type);
		}

		@Override
		public @Nullable T set(Entity holder, @Nullable T value) {
			return holder.setAttached(type, value);
		}

		@Override
		public boolean has(Entity holder) {
			return holder.hasAttached(type);
		}
	}
}
