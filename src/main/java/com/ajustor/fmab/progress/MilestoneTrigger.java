package com.ajustor.fmab.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//? if >=26.3 {
/*import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
*///?} else {
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
//?}
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Le déclencheur des progrès du mod : une étape du chemin de l'alchimiste, nommée
 * ({@code "fmab:milestone"}, condition {@code "milestone": "<nom>"}). Voir {@link Milestones}.
 */
public class MilestoneTrigger extends SimpleCriterionTrigger<MilestoneTrigger.TriggerInstance> {
	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer player, String milestone) {
		trigger(player, t -> t.milestone().equals(milestone));
	}

	// La condition sur le joueur : un prédicat d'entité jusqu'en 26.2, une condition de butin ensuite.
	//? if >=26.3 {
	/*public record TriggerInstance(Optional<Holder<LootItemCondition>> player, String milestone)
			implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
				LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
				Codec.STRING.fieldOf("milestone").forGetter(TriggerInstance::milestone)
		).apply(i, TriggerInstance::new));
	}
	*///?} else {
	public record TriggerInstance(Optional<ContextAwarePredicate> player, String milestone)
			implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
				Codec.STRING.fieldOf("milestone").forGetter(TriggerInstance::milestone)
		).apply(i, TriggerInstance::new));
	}
	//?}
}
