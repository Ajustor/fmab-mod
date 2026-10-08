package com.ajustor.fmab.gate;

import com.ajustor.fmab.registry.FmabComponents;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/**
 * L'armure où l'âme d'un joueur qui a perdu son corps à la Porte est fixée, comme Alphonse. Le
 * sceau de sang est tracé à l'intérieur du plastron : effacé (plastron brisé ou retiré), l'âme s'en
 * va.
 */
public final class SoulArmor {
	/** Acier épais, sous l'apparence d'une armure de fer : solide comme du diamant. */
	public static final ArmorMaterial MATERIAL = new ArmorMaterial(33,
			Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 3,
					ArmorType.BODY, 11),
			0, SoundEvents.ARMOR_EQUIP_IRON, 2.0F, 0.1F, ItemTags.REPAIRS_IRON_ARMOR, EquipmentAssets.IRON);

	private SoulArmor() {
	}

	private static Map<EquipmentSlot, Item> pieces() {
		return Map.of(EquipmentSlot.HEAD, FmabItems.SOUL_HELMET, EquipmentSlot.CHEST, FmabItems.SOUL_CHESTPLATE,
				EquipmentSlot.LEGS, FmabItems.SOUL_LEGGINGS, EquipmentSlot.FEET, FmabItems.SOUL_BOOTS);
	}

	/**
	 * L'âme entre dans l'armure : ce que le joueur portait tombe dans son inventaire, l'armure prend
	 * sa place, maudite de lien (on ne l'ôte pas), le sceau tracé dans le plastron.
	 */
	public static void bind(ServerPlayer player) {
		var binding = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.BINDING_CURSE);
		for (Map.Entry<EquipmentSlot, Item> piece : pieces().entrySet()) {
			ItemStack worn = player.getItemBySlot(piece.getKey());
			if (!worn.isEmpty() && !player.getInventory().add(worn.copy())) {
				player.drop(worn.copy(), false);
			}
			ItemStack armor = new ItemStack(piece.getValue());
			armor.enchant(binding, 1);
			if (piece.getKey() == EquipmentSlot.CHEST) {
				armor.set(FmabComponents.BLOOD_SEAL, player.getUUID().toString());
			}
			player.setItemSlot(piece.getKey(), armor);
		}
	}

	/** Le sceau tient-il encore : le plastron scellé au nom du joueur est-il porté ? */
	public static boolean sealIntact(ServerPlayer player) {
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		return chest.is(FmabItems.SOUL_CHESTPLATE)
				&& player.getUUID().toString().equals(chest.get(FmabComponents.BLOOD_SEAL));
	}
}
