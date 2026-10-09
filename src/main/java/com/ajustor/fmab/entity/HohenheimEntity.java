package com.ajustor.fmab.entity;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.Gifts;
import com.ajustor.fmab.item.Tomes;
import com.ajustor.fmab.promised.NationalCircle;
import com.ajustor.fmab.stone.LivingStone;
import com.ajustor.fmab.world.Maps;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;


/**
 * Van Hohenheim, Pierre philosophale vivante depuis la chute de Xerxès, dans les ruines de son
 * royaume. Il raconte ce qu'il a vu ; il parle du cercle national et de ses points de sang ; il
 * confie ses notes sur la transmutation humaine. Il reconnaît les Pierres vivantes comme lui.
 */
public class HohenheimEntity extends PathfinderMob {
	private static final DustParticleOptions RED = new DustParticleOptions(0xD01020, 0.8f);
	private int line;
	private static final TagKey<Structure> CREST_MAPS = TagKey.create(Registries.STRUCTURE, Fmab.id("on_blood_crest_maps"));

	public HohenheimEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40)
				.add(Attributes.MOVEMENT_SPEED, 0.2);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.6));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.4));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer p) || !(level() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		if (LivingStone.souls(p) > 0) {
			p.sendSystemMessage(Component.translatable("npc.fmab.hohenheim.kin"));
		} else {
			line = (line + 1) % 4;
			p.sendSystemMessage(Component.translatable("npc.fmab.hohenheim.line" + (line + 1)));
		}
		NationalCircle circle = NationalCircle.get(level.getServer());
		p.sendSystemMessage(Component.translatable(circle.fatherFallen() ? "npc.fmab.hohenheim.father_fallen"
				: circle.broken() ? "npc.fmab.hohenheim.circle_broken" : "npc.fmab.hohenheim.circle",
				NationalCircle.POINTS - circle.sealedCount()));
		// Il montre aussi où trouver un point de sang.
		if (!circle.broken() && !circle.fatherFallen() && !Gifts.received(p, "crest_map")) {
			ItemStack map = Maps.toStructure(level, blockPosition(), CREST_MAPS, MapDecorationTypes.RED_X,
					Component.translatable("filled_map.fmab.blood_crest"));
			if (!map.isEmpty() && Gifts.give(p, "crest_map", map)) {
				p.sendSystemMessage(Component.translatable("npc.fmab.hohenheim.crest_map"));
			}
		}
		if (Gifts.give(p, "hohenheim_notes", Tomes.stack(Tomes.HOHENHEIM))) {
			p.sendSystemMessage(Component.translatable("npc.fmab.hohenheim.notes"));
		}
		level.sendParticles(RED, getX(), getY(0.6), getZ(), 8, 0.3, 0.4, 0.3, 0);
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// Une Pierre vivante : la blessure se referme aussitôt.
		level.sendParticles(RED, getX(), getY(0.5), getZ(), 20, 0.3, 0.5, 0.3, 0);
		return (source.isCreativePlayer() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
				&& super.hurtServer(level, source, damage);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		// Il ne quitte pas son lieu.
		if (!hasHome()) {
			setHomeTo(blockPosition(), 10);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}
}
