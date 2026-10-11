package com.ajustor.fmab;

import com.ajustor.fmab.registry.FmabBlockEntities;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.registry.FmabSounds;
import com.ajustor.fmab.world.FmabStructures;
import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

/**
 * Minecraft et les registres du mod, pour les tests qui touchent aux blocs et aux créatures. Pas aux
 * piles d'objets : leurs composants ne sont liés qu'au chargement des données du serveur. Le
 * bootstrap de Minecraft gèle ses registres ; on les rouvre le temps d'y inscrire ceux du mod, dans
 * l'ordre de {@link Fmab#onInitialize}, et on les laisse ouverts (leurs étiquettes sont déjà liées,
 * on ne peut pas les geler une seconde fois). Les registres dynamiques (glyphes,
 * savoir) restent à part : ils passent par Fabric et le chargement des données.
 */
public final class TestBootstrap {
	private static boolean done;
	/** Les registres dont les entrées créent elles-mêmes leur « holder » (voir {@code registerSimpleWithIntrusiveHolders}). */
	private static final Set<ResourceKey<?>> INTRUSIVE = Set.of(Registries.BLOCK, Registries.ITEM, Registries.ENTITY_TYPE,
			Registries.FLUID, Registries.BLOCK_ENTITY_TYPE);

	private TestBootstrap() {
	}

	public static synchronized void init() {
		if (done) {
			return;
		}
		// Une seule tentative : rejouer une inscription à moitié faite ne donnerait que des doublons.
		done = true;
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		BuiltInRegistries.REGISTRY.forEach(TestBootstrap::thaw);
		FmabComponents.register();
		FmabSounds.register();
		FmabBlocks.register();
		FmabBlockEntities.register();
		// Les types de créatures s'inscrivent au chargement de la classe ; leurs attributs par défaut passent
		// par un mixin de Fabric, absent des tests : on ne charge que les types.
		Objects.requireNonNull(FmabEntities.IZUMI);
		FmabStructures.register();
		FmabItems.register();
	}

	/** Rouvre un registre gelé : il accepte de nouveau des entrées, et les blocs leurs « holders » intrusifs. */
	private static void thaw(Registry<?> registry) {
		if (!(registry instanceof MappedRegistry<?> mapped)) {
			return;
		}
		try {
			Field frozen = MappedRegistry.class.getDeclaredField("frozen");
			frozen.setAccessible(true);
			frozen.setBoolean(mapped, false);
			if (INTRUSIVE.contains(registry.key())) {
				Field intrusive = MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
				intrusive.setAccessible(true);
				intrusive.set(mapped, new IdentityHashMap<>());
			}
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Impossible de rouvrir " + registry.key(), e);
		}
	}
}
