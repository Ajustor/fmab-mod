package com.ajustor.fmab.entity;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Les voix des créatures du mod, prises dans les sons de Minecraft : ce qu'elles disent au repos,
 * quand on les frappe, quand elles meurent, et le bruit de leurs pas. Un mixin les branche sur les
 * méthodes de son des entités ; une créature absente de la table garde les sons ordinaires.
 *
 * @param ambient au repos, ou {@code null} pour se taire
 * @param hurt    touché
 * @param death   à la mort
 * @param step    ses pas, ou {@code null} pour ceux du bloc foulé
 * @param pitch   la hauteur de la voix (1 : celle du son d'origine)
 */
public record Voices(@Nullable SoundEvent ambient, SoundEvent hurt, SoundEvent death, @Nullable SoundEvent step,
		float pitch) {
	/** Un humain : il ne parle pas tout seul, il crie comme un joueur. */
	private static final Voices HUMAN = new Voices(null, SoundEvents.PLAYER_HURT, SoundEvents.PLAYER_DEATH, null, 1);
	private static final Voices SOLDIER = new Voices(null, SoundEvents.PILLAGER_HURT, SoundEvents.PILLAGER_DEATH, null, 1);

	private static Map<EntityType<?>, Voices> table;

	private static Map<EntityType<?>, Voices> table() {
		if (table == null) {
			Map<EntityType<?>, Voices> t = new IdentityHashMap<>();
			for (EntityType<?> type : new EntityType<?>[]{FmabEntities.IZUMI, FmabEntities.WINRY, FmabEntities.MAY_CHANG,
					FmabEntities.HOHENHEIM, FmabEntities.MARCOH, FmabEntities.CORNELLO, FmabEntities.SCAR,
					FmabEntities.STATE_EXAMINER}) {
				t.put(type, HUMAN);
			}
			t.put(FmabEntities.OLIVIER, new Voices(null, SoundEvents.PLAYER_HURT, SoundEvents.PLAYER_DEATH, null, 1.15f));
			t.put(FmabEntities.WRATH, new Voices(null, SoundEvents.PLAYER_HURT, SoundEvents.PLAYER_DEATH, null, 0.85f));
			t.put(FmabEntities.AMESTRIAN_SOLDIER, SOLDIER);
			t.put(FmabEntities.BRIGGS_SOLDIER, SOLDIER);
			t.put(FmabEntities.DRACHMA_SOLDIER, new Voices(SoundEvents.PILLAGER_AMBIENT, SoundEvents.PILLAGER_HURT,
					SoundEvents.PILLAGER_DEATH, null, 0.9f));
			// Les soldats immortels : des râles de mort-vivant, un pas traînant.
			t.put(FmabEntities.IMMORTAL_SOLDIER, new Voices(SoundEvents.ZOMBIE_VILLAGER_AMBIENT,
					SoundEvents.ZOMBIE_VILLAGER_HURT, SoundEvents.ZOMBIE_VILLAGER_DEATH, SoundEvents.ZOMBIE_STEP, 0.8f));
			// Les homoncules.
			t.put(FmabEntities.LUST, new Voices(SoundEvents.WITCH_AMBIENT, SoundEvents.WITCH_HURT,
					SoundEvents.WITCH_DEATH, null, 1.1f));
			t.put(FmabEntities.GLUTTONY, new Voices(SoundEvents.HOGLIN_AMBIENT, SoundEvents.HOGLIN_HURT,
					SoundEvents.HOGLIN_DEATH, null, 1.3f));
			t.put(FmabEntities.ENVY, new Voices(SoundEvents.VEX_AMBIENT, SoundEvents.PIGLIN_HURT,
					SoundEvents.PIGLIN_DEATH, null, 0.8f));
			t.put(FmabEntities.GREED, new Voices(SoundEvents.PIGLIN_BRUTE_AMBIENT, SoundEvents.PIGLIN_BRUTE_HURT,
					SoundEvents.PIGLIN_BRUTE_DEATH, null, 1));
			t.put(FmabEntities.SLOTH, new Voices(SoundEvents.RAVAGER_AMBIENT, SoundEvents.RAVAGER_HURT,
					SoundEvents.RAVAGER_DEATH, SoundEvents.RAVAGER_STEP, 0.7f));
			t.put(FmabEntities.PRIDE, new Voices(null, SoundEvents.PLAYER_HURT, SoundEvents.WARDEN_DEATH, null, 1.4f));
			t.put(FmabEntities.FATHER, new Voices(null, SoundEvents.EVOKER_HURT, SoundEvents.WITHER_DEATH, null, 0.6f));
			// Les armures vides : du métal qui sonne creux, des pas de chaînes.
			t.put(FmabEntities.HAUNTED_ARMOR, new Voices(null, SoundEvents.IRON_GOLEM_HURT, SoundEvents.IRON_GOLEM_DEATH,
					SoundEvents.CHAIN_STEP, 1.4f));
			t.put(FmabEntities.BARRY, new Voices(SoundEvents.VINDICATOR_AMBIENT, SoundEvents.IRON_GOLEM_HURT,
					SoundEvents.IRON_GOLEM_DEATH, SoundEvents.CHAIN_STEP, 1.3f));
			// Ce que l'alchimie a levé ou cousu.
			t.put(FmabEntities.STONE_GOLEM, new Voices(null, SoundEvents.IRON_GOLEM_HURT, SoundEvents.IRON_GOLEM_DEATH,
					SoundEvents.IRON_GOLEM_STEP, 0.7f));
			t.put(FmabEntities.CHIMERA_BEAST, new Voices(SoundEvents.POLAR_BEAR_WARNING, SoundEvents.POLAR_BEAR_HURT,
					SoundEvents.POLAR_BEAR_DEATH, SoundEvents.POLAR_BEAR_STEP, 0.8f));
			table = t;
		}
		return table;
	}

	/** La voix de cette créature, ou {@code null} si elle garde les sons ordinaires. */
	public static @Nullable Voices of(Entity entity) {
		return table().get(entity.getType());
	}
}
