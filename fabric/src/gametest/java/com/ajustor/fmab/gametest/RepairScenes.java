package com.ajustor.fmab.gametest;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.CircleSize;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.Concentration;
import com.ajustor.fmab.transmutation.Transmutation;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

import static com.ajustor.fmab.gametest.GametestAsserts.check;

/**
 * Le bon cercle répare tout ce que l'enclume répare : chaque élément et Réparer, sur l'outil ou
 * l'armure dont la matière est de cet élément (le cuivre pour une pioche en cuivre, le carbone pour
 * une épée en diamant, le bois pour une hache en bois…). Un cercle d'un autre élément n'y touche
 * pas. Lancer avec {@code ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=repair}.
 */
public class RepairScenes implements FabricClientGameTest {
	private static final BlockPos CIRCLE = new BlockPos(0, -60, 3);

	/** Un objet abîmé, la matière qui le répare, et l'élément du cercle qui doit le réparer. */
	private record Case(Item tool, Item material, String element) {
	}

	private static final List<Case> CASES = List.of(
			new Case(Items.IRON_PICKAXE, Items.IRON_INGOT, "iron"),
			new Case(Items.COPPER_PICKAXE, Items.COPPER_INGOT, "copper"),
			new Case(Items.DIAMOND_SWORD, Items.DIAMOND, "carbon"),
			new Case(Items.WOODEN_AXE, Items.OAK_PLANKS, "wood"),
			new Case(Items.STONE_SHOVEL, Items.COBBLESTONE, "earth"),
			new Case(Items.GOLDEN_HOE, Items.GOLD_INGOT, "gold"),
			new Case(Items.NETHERITE_PICKAXE, Items.NETHERITE_INGOT, "netherite"),
			new Case(Items.LEATHER_CHESTPLATE, Items.LEATHER, "fiber"),
			new Case(Items.ELYTRA, Items.PHANTOM_MEMBRANE, "flesh"),
			new Case(Items.TURTLE_HELMET, Items.TURTLE_SCUTE, "calcium"));

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!RenderShowcase.selected("repair")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			sp.getServer().runCommand("difficulty peaceful");
			sp.getConnection().waitForChunksRender();
			context.waitTicks(20);
			// Un Alchimiste d'État qui comprend tous les glyphes (l'Or est de son rang), les mains vides.
			sp.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				AlchemistData data = AlchemistData.NEW.withRank(Rank.STATE);
				for (Glyph g : AlchemyRules.of(p.level().registryAccess()).glyphs()) {
					data = data.learn(g.id());
				}
				FmabAttachments.ALCHEMIST.set(p, data);
				p.getInventory().clearContent();
			});
			sp.getServer().runCommand("tp @p 0.5 -60 0.5 0 50");
			for (Case c : CASES) {
				clear(context, sp);
				int max = new ItemStack(c.tool()).getMaxDamage();
				drop(sp, worn(c.tool()));
				drop(sp, new ItemStack(c.material(), 2));
				context.waitTicks(5);
				check(cast(context, sp, circle(sp, c.element())), c.element() + " et Réparer devrait réparer " + c.tool());
				int damage = damageOf(sp, c.tool());
				check(damage < max / 2, c.tool() + " devrait être réparé : usure " + damage + " sur " + max);
				if (c.tool() == Items.DIAMOND_SWORD) {
					context.takeScreenshot("repair_diamond_sword");
				}
			}
			// Un cercle de cuivre ne répare pas une épée en diamant.
			clear(context, sp);
			drop(sp, worn(Items.DIAMOND_SWORD));
			drop(sp, new ItemStack(Items.COPPER_INGOT, 2));
			context.waitTicks(5);
			check(!cast(context, sp, circle(sp, "copper")), "le cuivre ne devrait pas réparer le diamant");
			int max = new ItemStack(Items.DIAMOND_SWORD).getMaxDamage();
			check(damageOf(sp, Items.DIAMOND_SWORD) == max / 2, "l'épée en diamant devrait rester abîmée");
		}
	}

	/** L'objet, usé de moitié. */
	private static ItemStack worn(Item item) {
		ItemStack stack = new ItemStack(item);
		stack.setDamageValue(stack.getMaxDamage() / 2);
		return stack;
	}

	/** Le cercle simple de cet élément et de Réparer. */
	private static Drawing circle(TestSingleplayerContext sp, String element) {
		return sp.getServer().computeOnServer(server -> {
			AlchemyRules rules = AlchemyRules.of(server.overworld().registryAccess());
			return rules.combinations().stream()
					.filter(c -> c.effect().equals("fmab:repair") && c.elements().equals(java.util.Set.of(element)))
					.findFirst()
					.flatMap(rules::simpleCircle)
					.orElseThrow(() -> new AssertionError("pas de cercle " + element + " et Réparer"));
		});
	}

	/** L'usure de l'objet de ce type posé sur le cercle. */
	private static int damageOf(TestSingleplayerContext sp, Item item) {
		return sp.getServer().computeOnServer(server -> server.overworld()
				.getEntitiesOfClass(ItemEntity.class, new AABB(CIRCLE).inflate(2.5), e -> e.getItem().is(item))
				.stream().mapToInt(e -> e.getItem().getDamageValue()).findFirst().orElse(-1));
	}

	/** Un sol de pierre neuf, sans objet qui traîne. */
	private static void clear(ClientGameTestContext context, TestSingleplayerContext sp) {
		sp.getServer().runCommand("fill -4 -60 -1 4 -56 7 air");
		sp.getServer().runCommand("fill -4 -61 -1 4 -61 7 stone");
		sp.getServer().runCommand("kill @e[type=item]");
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(5);
	}

	/** Trace le cercle à la craie, face au sud, et pose la paume dessus. */
	private static boolean cast(ClientGameTestContext context, TestSingleplayerContext sp, Drawing drawing) {
		Transmutation.Result result = sp.getServer().computeOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
			var level = p.level();
			BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
					.stateFor(Direction.UP, Direction.SOUTH, CircleMedium.CHALK);
			level.setBlockAndUpdate(CIRCLE, state);
			if (level.getBlockEntity(CIRCLE) instanceof TransmutationCircleBlockEntity be) {
				be.setDrawing(drawing);
			}
			Concentration.set(p, Concentration.max(p));
			return Transmutation.activate(level, CIRCLE, state, drawing, p, CircleSize.NORMAL);
		});
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(20);
		return result == Transmutation.Result.DONE;
	}

	/** Pose des objets au centre du cercle, immobiles et hors de portée du joueur. */
	private static void drop(TestSingleplayerContext sp, ItemStack stack) {
		sp.getServer().runOnServer(server -> {
			var level = server.overworld();
			ItemEntity entity = new ItemEntity(level, CIRCLE.getX() + 0.5, CIRCLE.getY() + 0.1, CIRCLE.getZ() + 0.5, stack);
			entity.setDeltaMovement(0, 0, 0);
			entity.setPickUpDelay(32767);
			level.addFreshEntity(entity);
		});
	}
}
