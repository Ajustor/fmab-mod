package com.ajustor.fmab.state;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.block.CircleMedium;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.ExamProgress;
import com.ajustor.fmab.data.Gifts;
import com.ajustor.fmab.entity.StoneGolemEntity;
import com.ajustor.fmab.item.Tomes;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.world.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * L'examen d'Alchimiste d'État : remettre à l'examinateur des objets qui prouvent la maîtrise,
 * puis vaincre en arène un golem de pierre lié à son cercle.
 */
public final class StateExam {
	/**
	 * Un objet demandé : l'un des items, en quantité.
	 *
	 * @param key clé de traduction de la ligne
	 */
	public record Requirement(String key, Set<Item> items, int count) {
		public int held(ServerPlayer player) {
			int n = 0;
			for (ItemStack stack : player.getInventory()) {
				if (items.contains(stack.getItem())) {
					n += stack.getCount();
				}
			}
			return n;
		}
	}

	/** Une arme transmutée prouve Recomposer et le savoir ; la glace, une seconde école ; le fer paie l'examen. */
	public static List<Requirement> requirements() {
		return List.of(
				new Requirement("exam.fmab.req.weapon", Set.of(FmabItems.STONE_LANCE, FmabItems.ARM_BLADE), 1),
				new Requirement("exam.fmab.req.ice", Set.of(Items.PACKED_ICE, Items.ICE, Items.BLUE_ICE, Items.SNOW_BLOCK), 8),
				new Requirement("exam.fmab.req.fee", Set.of(Items.IRON_INGOT), 4));
	}

	/** Distance entre l'examinateur et le cercle du golem. */
	private static final int ARENA_DISTANCE = 6;

	private StateExam() {
	}

	/** Le candidat remet les objets : tous d'un coup, ou rien. */
	public static boolean giveItems(ServerPlayer player) {
		AlchemistData me = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		ExamProgress exam = player.getAttachedOrCreate(FmabAttachments.EXAM);
		if (!me.rank().atLeast(Rank.ALCHEMIST) || exam.itemsGiven()) {
			return false;
		}
		for (Requirement r : requirements()) {
			if (r.held(player) < r.count()) {
				player.sendSystemMessage(Component.translatable("exam.fmab.missing", Component.translatable(r.key())));
				return false;
			}
		}
		for (Requirement r : requirements()) {
			int left = r.count();
			for (ItemStack stack : player.getInventory()) {
				if (left > 0 && r.items().contains(stack.getItem())) {
					int taken = Math.min(left, stack.getCount());
					stack.shrink(taken);
					left -= taken;
				}
			}
		}
		player.setAttached(FmabAttachments.EXAM, new ExamProgress(true, false));
		player.sendSystemMessage(Component.translatable("exam.fmab.items_accepted"));
		return true;
	}

	/**
	 * L'examinateur trace le cercle du golem devant lui et l'éveille. Il faut une place libre et
	 * plane à quelques pas.
	 */
	public static void startFight(Entity examiner, ServerPlayer player) {
		ServerLevel level = player.level();
		ExamProgress exam = player.getAttachedOrCreate(FmabAttachments.EXAM);
		if (!exam.itemsGiven() || exam.passed()) {
			return;
		}
		boolean ongoing = !level.getEntitiesOfClass(StoneGolemEntity.class, new AABB(examiner.blockPosition()).inflate(32))
				.isEmpty();
		if (ongoing) {
			player.sendSystemMessage(Component.translatable("exam.fmab.arena_busy"));
			return;
		}
		Direction facing = examiner.getDirection();
		BlockPos circle = null;
		for (int dy = 2; dy >= -2 && circle == null; dy--) {
			BlockPos p = examiner.blockPosition().relative(facing, ARENA_DISTANCE).above(dy);
			BlockState state = ((TransmutationCircleBlock) FmabBlocks.TRANSMUTATION_CIRCLE)
					.stateFor(Direction.UP, facing, CircleMedium.ENGRAVING);
			if (level.getBlockState(p).canBeReplaced() && state.canSurvive(level, p)) {
				level.setBlockAndUpdate(p, state);
				circle = p;
			}
		}
		if (circle == null) {
			player.sendSystemMessage(Component.translatable("exam.fmab.no_room"));
			return;
		}
		if (level.getBlockEntity(circle) instanceof TransmutationCircleBlockEntity be) {
			be.setDrawing(golemCircle());
		}
		StoneGolemEntity golem = FmabEntities.STONE_GOLEM.create(level, EntitySpawnReason.EVENT);
		if (golem == null) {
			return;
		}
		BlockPos spawn = circle.relative(facing, 2);
		golem.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, facing.getOpposite().toYRot(), 0);
		golem.bind(circle, player);
		level.addFreshEntity(golem);
		level.playSound(null, circle, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.5f, 0.6f);
		player.sendSystemMessage(Component.translatable("exam.fmab.fight_start"));
	}

	/**
	 * Le golem est tombé : le candidat devient Alchimiste d'État et reçoit sa montre, avec les notes
	 * sur la combustion des archives de l'armée.
	 */
	public static void pass(ServerPlayer player) {
		ExamProgress exam = player.getAttachedOrCreate(FmabAttachments.EXAM);
		if (exam.passed() || !exam.itemsGiven()) {
			return;
		}
		player.setAttached(FmabAttachments.EXAM, new ExamProgress(true, true));
		AlchemistData me = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		if (!me.rank().atLeast(Rank.STATE)) {
			player.setAttached(FmabAttachments.ALCHEMIST, me.withRank(Rank.STATE));
		}
		for (ItemStack reward : new ItemStack[]{new ItemStack(FmabItems.STATE_WATCH), Tomes.stack(Tomes.FLAME)}) {
			if (!player.getInventory().add(reward)) {
				player.drop(reward, false);
			}
		}
		// Sa première mission : le Laboratoire 5.
		ItemStack map = Maps.toStructure(player.level(), player.blockPosition(),
				TagKey.create(Registries.STRUCTURE, Fmab.id("on_laboratory_5_maps")), MapDecorationTypes.RED_X,
				Component.translatable("filled_map.fmab.laboratory_5"));
		if (!map.isEmpty() && Gifts.give(player, "lab5_map", map)) {
			player.sendSystemMessage(Component.translatable("exam.fmab.mission"));
		}
		player.sendSystemMessage(Component.translatable("exam.fmab.passed",
				Component.translatable(Rank.STATE.translationKey())));
		player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS,
				1, 1);
	}

	/** Le cercle gravé du golem : Terre et Recomposer dans un carré, l'anneau doublé. */
	private static Drawing golemCircle() {
		List<Primitive> p = new ArrayList<>();
		p.add(new Primitive.Circle(Drawing.CENTER_POINT, 14));
		p.add(new Primitive.Circle(Drawing.CENTER_POINT, 12));
		p.add(new Primitive.Polygon(List.of(new Vec2(16, 4), new Vec2(28, 16), new Vec2(16, 28), new Vec2(4, 16))));
		p.add(new Primitive.Polygon(List.of(new Vec2(13, 10), new Vec2(19, 10), new Vec2(16, 15))));
		p.add(new Primitive.Line(new Vec2(14, 12), new Vec2(18, 12)));
		p.add(new Primitive.Polygon(List.of(new Vec2(14, 18), new Vec2(18, 18), new Vec2(18, 22), new Vec2(14, 22))));
		p.add(new Primitive.Line(new Vec2(14, 18), new Vec2(18, 22)));
		p.add(new Primitive.Line(new Vec2(18, 18), new Vec2(14, 22)));
		return new Drawing(p);
	}
}
