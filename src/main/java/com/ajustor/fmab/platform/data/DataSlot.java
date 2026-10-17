package com.ajustor.fmab.platform.data;

import com.ajustor.fmab.platform.Platform;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Une donnée du mod portée par une entité (le rang d'un alchimiste, ce que la Porte a pris…). La
 * plateforme la stocke, la sauvegarde, la garde à la mort et l'envoie aux clients selon sa
 * {@link Spec}. On la déclare avec {@link #builder()}.
 *
 * @param <T> la valeur, de préférence immuable : on la remplace avec {@link #set}, on ne la modifie pas
 */
public interface DataSlot<T> {
	Identifier id();

	/** La valeur, ou null si l'entité n'en a pas. */
	@Nullable T get(Entity holder);

	/** La valeur ; absente, elle est créée par la valeur initiale de la déclaration (et posée). */
	T getOrCreate(Entity holder);

	default T getOrElse(Entity holder, T fallback) {
		T value = get(holder);
		return value != null ? value : fallback;
	}

	default boolean has(Entity holder) {
		return get(holder) != null;
	}

	/** Pose la valeur (null l'enlève) ; rend l'ancienne. */
	@Nullable T set(Entity holder, @Nullable T value);

	/** Enlève la valeur ; rend l'ancienne. */
	default @Nullable T remove(Entity holder) {
		return set(holder, null);
	}

	static <T> Builder<T> builder() {
		return new Builder<>();
	}

	/** Qui, côté client, connaît la valeur. */
	enum Sync {
		/** Seulement le joueur qui la porte. */
		OWNER,
		/** Tous les joueurs qui voient l'entité. */
		ALL
	}

	/**
	 * Ce que la plateforme doit faire de la donnée.
	 *
	 * @param codec       sauvegardée avec l'entité ; vide : perdue au déchargement
	 * @param initializer la valeur créée par {@link #getOrCreate}
	 * @param copyOnDeath gardée par le joueur qui meurt et réapparaît
	 * @param syncCodec   envoyée aux clients ; vide : le serveur seul la connaît
	 */
	record Spec<T>(Optional<Codec<T>> codec, Optional<Supplier<T>> initializer, boolean copyOnDeath,
			Optional<StreamCodec<? super RegistryFriendlyByteBuf, T>> syncCodec, Sync sync) {
	}

	final class Builder<T> {
		private @Nullable Codec<T> codec;
		private @Nullable Supplier<T> initializer;
		private boolean copyOnDeath;
		private @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> syncCodec;
		private Sync sync = Sync.OWNER;

		private Builder() {
		}

		public Builder<T> persistent(Codec<T> codec) {
			this.codec = codec;
			return this;
		}

		public Builder<T> initializer(Supplier<T> initializer) {
			this.initializer = initializer;
			return this;
		}

		public Builder<T> copyOnDeath() {
			this.copyOnDeath = true;
			return this;
		}

		public Builder<T> syncWith(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Sync sync) {
			this.syncCodec = codec;
			this.sync = sync;
			return this;
		}

		public Spec<T> spec() {
			return new Spec<>(Optional.ofNullable(codec), Optional.ofNullable(initializer), copyOnDeath,
					Optional.ofNullable(syncCodec), sync);
		}

		/** Déclare la donnée auprès de la plateforme. */
		public DataSlot<T> register(Identifier id) {
			return Platform.get().dataSlots().register(id, spec());
		}
	}
}
