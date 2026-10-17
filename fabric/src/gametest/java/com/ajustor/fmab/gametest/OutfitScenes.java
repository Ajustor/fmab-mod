package com.ajustor.fmab.gametest;

import com.ajustor.fmab.client.render.OutfitLayer;
import com.ajustor.fmab.registry.FmabItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Les tenues d'Amestris portées : le manteau d'Edward, l'armure d'Alphonse, l'uniforme et le manteau
 * d'officier, chacun sur un porte-armure, puis le manteau rouge sur le joueur, de face, de dos et en
 * marchant (les pans suivent les jambes). Lancer avec {@code ./gradlew runClientGameTest -PfmabGametest=outfits}.
 */
public class OutfitScenes implements FabricClientGameTest {
	private static final int Y = -60;
	private static final List<Map<EquipmentSlot, Item>> OUTFITS = List.of(
			Map.of(EquipmentSlot.CHEST, FmabItems.ALCHEMIST_COAT, EquipmentSlot.LEGS, FmabItems.ALCHEMIST_TROUSERS,
					EquipmentSlot.FEET, FmabItems.ALCHEMIST_BOOTS),
			Map.of(EquipmentSlot.HEAD, FmabItems.STEEL_HELMET, EquipmentSlot.CHEST, FmabItems.STEEL_CHESTPLATE,
					EquipmentSlot.LEGS, FmabItems.STEEL_LEGGINGS, EquipmentSlot.FEET, FmabItems.STEEL_BOOTS),
			Map.of(EquipmentSlot.HEAD, FmabItems.OFFICER_CAP, EquipmentSlot.CHEST, FmabItems.OFFICER_JACKET,
					EquipmentSlot.LEGS, FmabItems.OFFICER_TROUSERS, EquipmentSlot.FEET, FmabItems.OFFICER_BOOTS),
			Map.of(EquipmentSlot.HEAD, FmabItems.OFFICER_CAP, EquipmentSlot.CHEST, FmabItems.OFFICER_GREATCOAT,
					EquipmentSlot.LEGS, FmabItems.OFFICER_TROUSERS, EquipmentSlot.FEET, FmabItems.OFFICER_BOOTS));

	private static String slotName(EquipmentSlot slot) {
		return slot.getName();
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("outfits")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getServer().runCommand("time set 6000");
			sp.getServer().runCommand("gamerule doDaylightCycle false");
			sp.getServer().runCommand("weather clear");

			// Chaque pièce se porte à sa place.
			for (Map<EquipmentSlot, Item> outfit : OUTFITS) {
				outfit.forEach((slot, item) -> {
					Equippable equippable = new ItemStack(item).get(DataComponents.EQUIPPABLE);
					check(equippable != null && equippable.slot() == slot, item + " devrait se porter en " + slot);
				});
			}
			// Les volumes des tenues se dessinent sur le joueur comme sur les porte-armures.
			check(context.computeOnClient(mc -> hasOutfitLayer(mc.getEntityRenderDispatcher().getRenderer(mc.player))),
					"le joueur devrait avoir la couche des tenues");

			for (int i = 0; i < OUTFITS.size(); i++) {
				StringBuilder equipment = new StringBuilder();
				OUTFITS.get(i).forEach((slot, item) -> equipment.append(equipment.isEmpty() ? "" : ",")
						.append(slotName(slot)).append(":{id:\"").append(net.minecraft.core.registries.BuiltInRegistries.ITEM
								.getKey(item)).append("\",count:1}"));
				sp.getServer().runCommand(String.format(Locale.ROOT,
						"summon armor_stand %.1f %d 0.5 {Rotation:[160f,0f],ShowArms:1b,equipment:{%s}}",
						i * 2.5 + 0.5, Y, equipment));
			}
			sp.getServer().runCommand("gamemode spectator @p");
			context.runOnClient(mc -> {
				RenderShowcase.hideGui(mc, true);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
			sp.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %.1f -4.2 0 10", OUTFITS.size() * 1.25 - 0.75,
					Y + 1.2));
			context.waitTicks(40);
			context.takeScreenshot("outfits_0_stands");
			sp.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %.1f 4.5 180 10", OUTFITS.size() * 1.25 - 0.75,
					Y + 1.2));
			context.waitTicks(10);
			context.takeScreenshot("outfits_1_stands_back");

			// Le manteau rouge sur le joueur : de face, de dos.
			sp.getServer().runCommand("gamemode creative @p");
			sp.getServer().runCommand("kill @e[type=armor_stand]");
			sp.getServer().runCommand(String.format(Locale.ROOT, "tp @p 0.5 %d 0.5 0 0", Y));
			sp.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				OUTFITS.getFirst().forEach((slot, item) -> p.setItemSlot(slot, new ItemStack(item)));
			});
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(10);
			context.takeScreenshot("outfits_2_coat_front");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(5);
			context.takeScreenshot("outfits_3_coat_back");
			// En marchant : les pans suivent les jambes.
			context.getInput().holdKey(options -> options.keyUp);
			context.waitTicks(8);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(2);
			context.takeScreenshot("outfits_4_coat_walking");
			context.getInput().releaseKey(options -> options.keyUp);
		}
	}

	private static boolean hasOutfitLayer(Object renderer) {
		try {
			var field = LivingEntityRenderer.class.getDeclaredField("layers");
			field.setAccessible(true);
			return ((List<?>) field.get(renderer)).stream().anyMatch(l -> l instanceof OutfitLayer<?, ?>);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
