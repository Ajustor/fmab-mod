package com.ajustor.fmab.gate;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.drawing.SoulSeal;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.data.Notebooks;
import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.AlchemyRules;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * L'armure où vit l'âme d'un joueur qui a perdu son corps à la Porte, comme Alphonse. Seul le
 * plastron compte : il porte le sceau de sang, maudit de lien. Le heaume, les jambières et les
 * solerets sont des pièces d'armure ordinaires, qu'on refait quand elles cèdent.
 */
public final class SoulArmor {
	/** L'apparence portée du plastron : l'acier bleuté de l'armure d'Alphonse. */
	public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Fmab.id("soul"));
	/** L'acier d'un plastron d'âme. */
	public static final ArmorMaterial MATERIAL = new ArmorMaterial(24,
			Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 5, ArmorType.CHESTPLATE, 6, ArmorType.HELMET, 2,
					ArmorType.BODY, 5),
			9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, ItemTags.REPAIRS_IRON_ARMOR, ASSET);

	/** Les emplacements que l'armure occupe, le plastron (le sceau) compris. */
	public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
			EquipmentSlot.LEGS, EquipmentSlot.FEET);

	private SoulArmor() {
	}

	/** Un plastron d'âme scellé au nom de cette âme, maudit de lien. */
	public static ItemStack sealed(HolderLookup.Provider registries, UUID soul, ItemStack base) {
		ItemStack chest = new ItemStack(FmabItems.SOUL_CHESTPLATE);
		if (base.isDamageableItem() && base.getMaxDamage() > 0) {
			// L'usure du plastron d'origine se retrouve, à proportion, dans le plastron d'âme.
			chest.setDamageValue(base.getDamageValue() * chest.getMaxDamage() / base.getMaxDamage());
		}
		chest.enchant(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE), 1);
		chest.set(FmabComponents.BLOOD_SEAL, soul.toString());
		return chest;
	}

	public static Optional<UUID> sealOf(ItemStack stack) {
		String seal = stack.get(FmabComponents.BLOOD_SEAL);
		return seal == null ? Optional.empty() : Optional.of(UUID.fromString(seal));
	}

	/**
	 * La première fois que la Vérité prend le corps, l'âme se retrouve aussitôt dans une armure de
	 * fer : ce que le joueur portait passe dans son sac.
	 */
	public static void bindFirstTime(ServerPlayer player) {
		inhabit(player, Map.of(
				EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET),
				EquipmentSlot.CHEST, sealed(player.level().registryAccess(), player.getUUID(), ItemStack.EMPTY),
				EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS),
				EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS)));
	}

	/**
	 * Ce qu'une âme doit savoir, écrit dans son carnet : son sceau de sang, pour préparer d'autres
	 * armures, et le cercle Fer et Réparer qui redresse la sienne.
	 */
	public static void giveNotes(ServerPlayer player) {
		AlchemyRules rules = AlchemyRules.of(player.level().registryAccess());
		List<NotebookContents.Page> pages = new ArrayList<>();
		pages.add(new NotebookContents.Page(Component.translatable("gate.fmab.page.seal",
				player.getName().getString()).getString(), SoulSeal.of(player.getUUID())));
		rules.combinations().stream()
				.filter(c -> c.effect().equals("fmab:repair"))
				.findFirst()
				.flatMap(rules::simpleCircle)
				.ifPresent(d -> pages.add(new NotebookContents.Page("@gate.fmab.page.repair", d)));
		boolean full = false;
		for (NotebookContents.Page page : pages) {
			full |= Notebooks.add(player, page.name(), page.drawing()) == Notebooks.Added.FULL;
		}
		player.sendSystemMessage(Component.translatable(full ? "gate.fmab.soul.notes_full" : "gate.fmab.soul.notes"));
	}

	/** L'âme entre dans une armure : chaque pièce prend sa place, ce qui était porté va au sac. */
	public static void inhabit(ServerPlayer player, Map<EquipmentSlot, ItemStack> pieces) {
		for (EquipmentSlot slot : SLOTS) {
			ItemStack piece = pieces.getOrDefault(slot, ItemStack.EMPTY);
			ItemStack worn = player.getItemBySlot(slot);
			if (!worn.isEmpty()) {
				if (!player.getInventory().add(worn.copy())) {
					player.drop(worn.copy(), false);
				}
				player.setItemSlot(slot, ItemStack.EMPTY);
			}
			if (!piece.isEmpty()) {
				player.setItemSlot(slot, piece.copy());
			}
		}
	}

	/** Le sceau tient-il encore : le plastron scellé au nom du joueur est-il porté ? */
	public static boolean sealIntact(ServerPlayer player) {
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		return chest.is(FmabItems.SOUL_CHESTPLATE)
				&& sealOf(chest).filter(player.getUUID()::equals).isPresent();
	}

	/** Une pièce d'armure qui peut servir de corps à cet emplacement. */
	public static boolean fits(EquipmentSlot slot, ItemStack stack) {
		return switch (slot) {
			case HEAD -> stack.is(ItemTags.HEAD_ARMOR);
			case CHEST -> stack.is(ItemTags.CHEST_ARMOR);
			case LEGS -> stack.is(ItemTags.LEG_ARMOR);
			case FEET -> stack.is(ItemTags.FOOT_ARMOR);
			default -> false;
		};
	}
}
