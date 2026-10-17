package com.ajustor.fmab.platform;

import com.ajustor.fmab.platform.data.DataSlot;
import com.ajustor.fmab.platform.data.DataSlots;
import com.ajustor.fmab.platform.network.Network;
import com.ajustor.fmab.platform.registry.Content;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * La plateforme des tests unitaires, sans loader : registres du jeu inscrits directement, données
 * d'entité en mémoire, réseau muet. Elle prouve aussi que le cœur du mod ne dépend que de ses ports.
 */
public final class TestPlatform implements Platform {
	@Override
	public String loader() {
		return "test";
	}

	@Override
	public Path configDir() {
		return Path.of("build", "test-config");
	}

	@Override
	public boolean isModLoaded(String modId) {
		return false;
	}

	@Override
	public DataSlots dataSlots() {
		return MemorySlot::new;
	}

	@Override
	public Network network() {
		return NETWORK;
	}

	@Override
	public Content content() {
		return CONTENT;
	}

	private static final Network NETWORK = new Network() {
		@Override
		public <T extends CustomPacketPayload> void clientbound(CustomPacketPayload.Type<T> type,
				StreamCodec<? super RegistryFriendlyByteBuf, T> codec, int maxSize) {
		}

		@Override
		public <T extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<T> type,
				StreamCodec<? super RegistryFriendlyByteBuf, T> codec, int maxSize) {
		}

		@Override
		public <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, Receiver<T> receiver) {
		}

		@Override
		public void send(ServerPlayer player, CustomPacketPayload payload) {
		}
	};

	private static final Content CONTENT = new Content() {
		@Override
		public <T, V extends T> V register(Registry<T> registry, ResourceKey<T> key, V value) {
			return Registry.register(registry, key, value);
		}

		@Override
		public <T> void dataRegistry(ResourceKey<Registry<T>> key, Codec<T> codec, boolean synced) {
		}

		@Override
		public <T extends LivingEntity> void attributes(EntityType<T> type, AttributeSupplier.Builder attributes) {
		}

		@Override
		public CreativeModeTab.Builder creativeTab() {
			return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0);
		}

		@Override
		public void addSpawn(TagKey<Biome> biomes, Set<ResourceKey<Biome>> except, MobCategory category,
				EntityType<?> type, int weight, int minGroup, int maxGroup) {
		}
	};

	/** Une donnée d'entité gardée en mémoire, le temps du test. */
	private record MemorySlot<T>(Identifier id, DataSlot.Spec<T> spec, Map<Entity, T> values) implements DataSlot<T> {
		MemorySlot(Identifier id, DataSlot.Spec<T> spec) {
			this(id, spec, new WeakHashMap<>());
		}

		@Override
		public @Nullable T get(Entity holder) {
			return values.get(holder);
		}

		@Override
		public T getOrCreate(Entity holder) {
			return values.computeIfAbsent(holder, h -> spec.initializer().orElseThrow().get());
		}

		@Override
		public @Nullable T set(Entity holder, @Nullable T value) {
			return value == null ? values.remove(holder) : values.put(holder, value);
		}
	}
}
