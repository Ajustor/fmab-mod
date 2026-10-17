package com.ajustor.fmab.compat;

//? if >=26.3 {
/*import net.minecraft.util.Prediction;
*///?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Les gestes des entités dont la forme change d'une version de Minecraft à l'autre. C'est ici, et
 * seulement ici, que vivent les conditions de version (voir ARCHITECTURE.md) : le reste du mod
 * appelle ces fonctions sans savoir quelle version tourne.
 */
public final class EntityCompat {
	private EntityCompat() {
	}

	/** Le joueur lâche un objet devant lui (côté serveur). */
	public static void drop(Player player, ItemStack stack, boolean throwRandomly) {
		//? if >=26.3 {
		/*player.drop(stack, throwRandomly, Prediction.SERVER_ONLY);
		*///?} else {
		player.drop(stack, throwRandomly);
		//?}
	}

	/** Le joueur lâche un objet devant lui (côté serveur). */
	public static void drop(Player player, ItemStack stack) {
		drop(player, stack, false);
	}

	/** Remet un objet dans l'inventaire du joueur ; ce qui n'y tient pas tombe à ses pieds. */
	public static void placeBack(Player player, ItemStack stack) {
		//? if >=26.3 {
		/*player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		*///?} else {
		player.getInventory().placeItemBackInInventory(stack);
		//?}
	}

	/**
	 * Le bras frappe : l'animation de l'objet tenu.
	 *
	 * @param updateSelf le joueur voit aussi son propre geste (sinon, seuls les autres le voient)
	 */
	public static void swing(LivingEntity entity, InteractionHand hand, boolean updateSelf) {
		//? if >=26.3 {
		/*entity.swing(hand, entity.getItemInHand(hand).getAttackAnimation(), updateSelf);
		*///?} else {
		entity.swing(hand, updateSelf);
		//?}
	}

	public static void swing(LivingEntity entity, InteractionHand hand) {
		swing(entity, hand, false);
	}

	/** La vitesse de l'entité vient de changer de force (un coup, une poussée) : l'envoyer aux clients. */
	public static void syncVelocity(Entity entity) {
		//? if >=26.3 {
		/*entity.syncVelocity = true;
		*///?} else {
		entity.hurtMarked = true;
		//?}
	}

	/** Les ticks pendant lesquels l'entité ne peut plus être blessée. */
	public static void setInvulnerableTime(Entity entity, int ticks) {
		//? if >=26.3 {
		/*entity.setInvulnerableTime(ticks);
		*///?} else {
		entity.invulnerableTime = ticks;
		//?}
	}
}
