package com.ajustor.fmab.gate;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.transmutation.EffectContext;
import com.ajustor.fmab.transmutation.Effects;
import com.ajustor.fmab.transmutation.TransmutationLightning;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Le cercle d'âme (Humain et Fixer) : il appelle une âme qui erre devant la Porte et la fixe dans
 * l'armure posée sur le cercle, sur un porte-armure ou à même le sol. Le plastron décide de l'âme :
 * scellé, il appelle son propriétaire ; nu, il appelle l'âme qui attend depuis le plus longtemps.
 *
 * <p>Une âme prévoyante prépare ses sceaux : un plastron scellé à la table d'alchimiste, posé sur un
 * porte-armure debout sur un cercle d'âme. Si son armure cède, elle demande à la Vérité de la
 * rappeler dans l'un d'eux.
 */
public final class SoulBinding {
	/** Temps laissé au monde pour charger un sceau éloigné, en ticks. */
	private static final int RECALL_PATIENCE = 100;
	/** Âmes errantes, par ordre d'arrivée devant la Porte. */
	private static final Map<UUID, Long> ADRIFT_SINCE = new HashMap<>();
	/** Rappels en cours : l'âme, les sceaux qui restent à essayer, le temps passé sur le premier. */
	private static final Map<UUID, Recall> RECALLS = new HashMap<>();

	private record Recall(List<Anchors.Anchor> left, int ticks) {
	}

	private SoulBinding() {
	}

	public static void register() {
		Effects.register("fmab:soul_binding", SoulBinding::apply);
		// Poser un plastron scellé sur un porte-armure prépare un sceau.
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (entity instanceof ArmorStand stand && level instanceof ServerLevel server) {
				ItemStack held = player.getItemInHand(hand);
				SoulArmor.sealOf(held).ifPresent(owner -> Anchors.get(server.getServer())
						.add(owner, new Anchors.Anchor(server.dimension().identifier().toString(), stand.getUUID(),
								stand.blockPosition())));
			}
			return InteractionResult.PASS;
		});
		ServerTickEvents.END_SERVER_TICK.register(SoulBinding::tickRecalls);
	}

	/** Une âme vient de perdre son armure : elle prend son tour dans la file. */
	public static void drift(ServerPlayer soul) {
		ADRIFT_SINCE.putIfAbsent(soul.getUUID(), soul.level().getGameTime());
	}

	private static Effects.Result apply(EffectContext ctx) {
		ServerLevel level = ctx.level();
		AABB area = new AABB(ctx.circle()).inflate(1, 1, 1);
		ArmorStand stand = level.getEntitiesOfClass(ArmorStand.class, area,
				s -> s.getItemBySlot(EquipmentSlot.CHEST).is(ItemTags.CHEST_ARMOR)).stream()
				.findFirst().orElse(null);
		ItemEntity loose = stand != null ? null : level.getEntitiesOfClass(ItemEntity.class, area,
				e -> e.getItem().is(ItemTags.CHEST_ARMOR)).stream().findFirst().orElse(null);
		if (stand == null && loose == null) {
			ctx.caster().sendOverlayMessage(Component.translatable("gate.fmab.soul.no_armor"));
			return Effects.Result.NO_MATERIAL;
		}
		ItemStack chest = stand != null ? stand.getItemBySlot(EquipmentSlot.CHEST) : loose.getItem();
		Optional<ServerPlayer> soul = soulFor(level.getServer(), chest, ctx.caster());
		if (soul.isEmpty()) {
			ctx.caster().sendOverlayMessage(Component.translatable("gate.fmab.soul.nobody"));
			return Effects.Result.NO_TARGET;
		}
		summon(soul.get(), level, Vec3.atBottomCenterOf(ctx.circle()), stand, loose);
		if (soul.get() != ctx.caster()) {
			ctx.caster().sendSystemMessage(Component.translatable("gate.fmab.soul.summoned_other",
					soul.get().getDisplayName()));
		}
		return Effects.Result.DONE;
	}

	/**
	 * L'âme qu'appelle ce plastron : son propriétaire s'il est scellé ; nu, l'âme en armure qui active
	 * le cercle (elle change d'armure), sinon l'âme errante qui attend depuis le plus longtemps.
	 */
	private static Optional<ServerPlayer> soulFor(MinecraftServer server, ItemStack chest, ServerPlayer caster) {
		Optional<UUID> seal = SoulArmor.sealOf(chest);
		if (seal.isPresent()) {
			ServerPlayer owner = server.getPlayerList().getPlayer(seal.get());
			return Optional.ofNullable(owner).filter(p -> adrift(p) || p == caster && inArmor(p));
		}
		if (inArmor(caster)) {
			return Optional.of(caster);
		}
		return server.getPlayerList().getPlayers().stream()
				.filter(SoulBinding::adrift)
				.min((a, b) -> Long.compare(ADRIFT_SINCE.getOrDefault(a.getUUID(), Long.MAX_VALUE),
						ADRIFT_SINCE.getOrDefault(b.getUUID(), Long.MAX_VALUE)));
	}

	private static boolean inArmor(ServerPlayer player) {
		GateState gate = player.getAttached(FmabAttachments.GATE);
		return gate != null && gate.inArmor();
	}

	private static boolean adrift(ServerPlayer player) {
		GateState gate = player.getAttached(FmabAttachments.GATE);
		return gate != null && gate.adrift();
	}

	/**
	 * L'âme quitte l'Espace blanc et entre dans l'armure : le plastron devient son sceau, les autres
	 * pièces du porte-armure lui font un corps. Le porte-armure (ou le plastron posé) disparaît.
	 */
	private static void summon(ServerPlayer soul, ServerLevel level, Vec3 at, ArmorStand stand, ItemEntity loose) {
		if (inArmor(soul)) {
			leaveShell(soul);
		}
		Map<EquipmentSlot, ItemStack> pieces = new EnumMap<>(EquipmentSlot.class);
		if (stand != null) {
			for (EquipmentSlot slot : SoulArmor.SLOTS) {
				ItemStack piece = stand.getItemBySlot(slot);
				if (!piece.isEmpty() && SoulArmor.fits(slot, piece)) {
					pieces.put(slot, piece.copy());
				}
			}
			at = stand.position();
			stand.discard();
		} else {
			pieces.put(EquipmentSlot.CHEST, loose.getItem().copyWithCount(1));
			loose.getItem().shrink(1);
			if (loose.getItem().isEmpty()) {
				loose.discard();
			}
		}
		ItemStack chest = pieces.get(EquipmentSlot.CHEST);
		pieces.put(EquipmentSlot.CHEST, SoulArmor.sealOf(chest).isPresent() ? chest
				: SoulArmor.sealed(level.registryAccess(), soul.getUUID(), chest));
		soul.setAttached(FmabAttachments.GATE, soul.getAttachedOrCreate(FmabAttachments.GATE).withAdrift(false));
		ADRIFT_SINCE.remove(soul.getUUID());
		RECALLS.remove(soul.getUUID());
		GateOfTruth.leaveTruth(soul);
		soul.teleport(new TeleportTransition(level, at, Vec3.ZERO, soul.getYRot(), 0,
				TeleportTransition.DO_NOTHING));
		SoulArmor.inhabit(soul, pieces);
		soul.setHealth(soul.getMaxHealth());
		TransmutationLightning.discharge(level, BlockPos.containing(at), 1.5, 1);
		level.playSound(null, BlockPos.containing(at), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1, 0.6f);
		soul.sendSystemMessage(Component.translatable("gate.fmab.soul.summoned"));
	}

	/**
	 * L'âme quitte son armure pour une autre : l'ancienne reste debout, vide, sur un porte-armure. Son
	 * plastron garde le sceau : c'est un sceau de rechange de plus.
	 */
	private static void leaveShell(ServerPlayer soul) {
		ServerLevel level = soul.level();
		ArmorStand shell = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.TRIGGERED);
		for (EquipmentSlot slot : SoulArmor.SLOTS) {
			ItemStack piece = soul.getItemBySlot(slot);
			soul.setItemSlot(slot, ItemStack.EMPTY);
			if (piece.isEmpty()) {
				continue;
			}
			if (shell != null) {
				shell.setItemSlot(slot, piece.copy());
			} else {
				soul.drop(piece.copy(), false);
			}
		}
		if (shell == null) {
			return;
		}
		shell.snapTo(soul.getX(), soul.getY(), soul.getZ(), soul.getYRot(), 0);
		level.addFreshEntity(shell);
		Anchors.get(level.getServer()).add(soul.getUUID(), new Anchors.Anchor(level.dimension().identifier().toString(),
				shell.getUUID(), shell.blockPosition()));
	}

	/** L'âme demande à la Vérité de la rappeler dans un de ses sceaux préparés. */
	public static void recall(ServerPlayer soul) {
		if (RECALLS.containsKey(soul.getUUID())) {
			return;
		}
		List<Anchors.Anchor> anchors = new ArrayList<>(Anchors.get(soul.level().getServer()).of(soul.getUUID()));
		if (anchors.isEmpty()) {
			soul.sendSystemMessage(Component.translatable("gate.fmab.soul.no_anchor"));
			return;
		}
		soul.sendSystemMessage(Component.translatable("gate.fmab.soul.searching"));
		RECALLS.put(soul.getUUID(), new Recall(anchors, 0));
	}

	private static void tickRecalls(MinecraftServer server) {
		for (UUID id : List.copyOf(RECALLS.keySet())) {
			ServerPlayer soul = server.getPlayerList().getPlayer(id);
			Recall recall = RECALLS.get(id);
			if (soul == null || !adrift(soul) || recall.left().isEmpty()) {
				RECALLS.remove(id);
				if (soul != null && adrift(soul)) {
					soul.sendSystemMessage(Component.translatable("gate.fmab.soul.no_anchor"));
				}
				continue;
			}
			Anchors.Anchor anchor = recall.left().getFirst();
			ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION,
					Identifier.parse(anchor.dimension())));
			ChunkPos chunk = ChunkPos.containing(anchor.pos());
			if (level != null && recall.ticks() == 0) {
				level.setChunkForced(chunk.x(), chunk.z(), true);
			}
			Optional<ArmorStand> stand = level == null ? Optional.empty()
					: Optional.ofNullable(level.getEntity(anchor.stand())).filter(ArmorStand.class::isInstance)
							.map(ArmorStand.class::cast);
			if (stand.isPresent() && ready(level, stand.get(), id)) {
				level.setChunkForced(chunk.x(), chunk.z(), false);
				summon(soul, level, stand.get().position(), stand.get(), null);
				continue;
			}
			if (level == null || stand.isPresent() || recall.ticks() >= RECALL_PATIENCE) {
				// Ce sceau n'est plus bon (porte-armure déplacé, plastron ôté, cercle effacé) : on l'oublie.
				if (level != null) {
					level.setChunkForced(chunk.x(), chunk.z(), false);
				}
				Anchors.get(server).remove(id, anchor);
				RECALLS.put(id, new Recall(recall.left().subList(1, recall.left().size()), 0));
				continue;
			}
			RECALLS.put(id, new Recall(recall.left(), recall.ticks() + 1));
		}
	}

	/** Le porte-armure porte-t-il le sceau de l'âme, debout sur un cercle d'âme ? */
	private static boolean ready(ServerLevel level, ArmorStand stand, UUID soul) {
		if (SoulArmor.sealOf(stand.getItemBySlot(EquipmentSlot.CHEST)).filter(soul::equals).isEmpty()) {
			return false;
		}
		if (!(level.getBlockEntity(stand.blockPosition()) instanceof TransmutationCircleBlockEntity circle)) {
			return false;
		}
		AlchemistData reader = AlchemistData.NEW.withRank(Rank.GATE);
		return AlchemyRules.of(level.registryAccess()).analyze(circle.drawing(), reader).effects().stream()
				.anyMatch(e -> e.combination().effect().equals("fmab:soul_binding"));
	}

	/** Les sceaux préparés, par âme : un porte-armure et l'endroit où il se tenait. */
	public static final class Anchors extends SavedData {
		public record Anchor(String dimension, UUID stand, BlockPos pos) {
			static final Codec<Anchor> CODEC = RecordCodecBuilder.create(i -> i.group(
					Codec.STRING.fieldOf("dimension").forGetter(Anchor::dimension),
					UUIDUtil.STRING_CODEC.fieldOf("stand").forGetter(Anchor::stand),
					BlockPos.CODEC.fieldOf("pos").forGetter(Anchor::pos)
			).apply(i, Anchor::new));
		}

		private static final Codec<Anchors> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Anchor.CODEC.listOf())
				.xmap(Anchors::new, a -> a.anchors);
		private static final SavedDataType<Anchors> TYPE = new SavedDataType<>(Fmab.id("soul_anchors"), Anchors::new,
				CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

		private final Map<UUID, List<Anchor>> anchors;

		public Anchors() {
			this(Map.of());
		}

		private Anchors(Map<UUID, List<Anchor>> anchors) {
			this.anchors = new HashMap<>();
			anchors.forEach((k, v) -> this.anchors.put(k, new ArrayList<>(v)));
		}

		public static Anchors get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(TYPE);
		}

		public List<Anchor> of(UUID soul) {
			return List.copyOf(anchors.getOrDefault(soul, List.of()));
		}

		public void add(UUID soul, Anchor anchor) {
			List<Anchor> list = anchors.computeIfAbsent(soul, k -> new ArrayList<>());
			list.removeIf(a -> a.stand().equals(anchor.stand()));
			list.add(anchor);
			setDirty();
		}

		public void remove(UUID soul, Anchor anchor) {
			List<Anchor> list = anchors.get(soul);
			if (list != null && list.remove(anchor)) {
				setDirty();
			}
		}
	}
}
