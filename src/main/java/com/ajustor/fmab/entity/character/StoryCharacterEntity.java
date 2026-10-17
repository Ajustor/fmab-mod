package com.ajustor.fmab.entity.character;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Un personnage de l'histoire ({@link StoryCharacter}) : il reste près de son lieu, dit une réplique à
 * chaque rencontre, puis ouvre sa boutique s'il en tient une (l'écran des villageois). On ne le
 * blesse pas : l'histoire a besoin de lui.
 */
public class StoryCharacterEntity extends PathfinderMob implements Merchant {
	/** Un jour : la boutique se réapprovisionne. */
	private static final int RESTOCK = 24000;

	private final StoryCharacter character;
	private int line;
	private @Nullable Player trading;
	private @Nullable MerchantOffers offers;

	public StoryCharacterEntity(EntityType<? extends PathfinderMob> type, Level level, StoryCharacter character) {
		super(type, level);
		this.character = character;
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20)
				.add(Attributes.MOVEMENT_SPEED, 0.2);
	}

	public StoryCharacter character() {
		return character;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.6));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.4));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer p) || trading != null) {
			return InteractionResult.SUCCESS;
		}
		talk(p);
		if (!character.trades().isEmpty()) {
			setTradingPlayer(p);
			openTradingScreen(p, getDisplayName(), 0);
		}
		return InteractionResult.SUCCESS;
	}

	/** Une réplique, la suivante à chaque rencontre. */
	protected void talk(ServerPlayer player) {
		player.sendSystemMessage(Component.translatable(character.lineKey(line)));
		boolean last = line == character.lines() - 1;
		line = (line + 1) % character.lines();
		if (last && character.ending() != null) {
			character.ending().play(this, player);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return source.isCreativePlayer() && super.hurtServer(level, source, damage);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		// Il ne quitte pas son lieu.
		if (!hasHome()) {
			setHomeTo(blockPosition(), 10);
		}
		if (offers != null && level.getGameTime() % RESTOCK == 0) {
			offers.forEach(MerchantOffer::resetUses);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	// --- la boutique -------------------------------------------------------------------------------

	@Override
	public void setTradingPlayer(@Nullable Player player) {
		trading = player;
	}

	@Override
	public @Nullable Player getTradingPlayer() {
		return trading;
	}

	@Override
	public MerchantOffers getOffers() {
		if (offers == null) {
			offers = character.offers();
		}
		return offers;
	}

	@Override
	public void overrideOffers(MerchantOffers replacement) {
		offers = replacement;
	}

	@Override
	public void notifyTrade(MerchantOffer offer) {
		offer.increaseUses();
		playSound(SoundEvents.VILLAGER_YES);
	}

	@Override
	public void notifyTradeUpdated(ItemStack stack) {
	}

	@Override
	public int getVillagerXp() {
		return 0;
	}

	@Override
	public void overrideXp(int xp) {
	}

	@Override
	public boolean showProgressBar() {
		return false;
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return SoundEvents.VILLAGER_YES;
	}

	@Override
	public boolean isClientSide() {
		return level().isClientSide();
	}

	@Override
	public boolean stillValid(Player player) {
		return trading == player && isAlive() && player.distanceToSqr(this) < 64;
	}
}
