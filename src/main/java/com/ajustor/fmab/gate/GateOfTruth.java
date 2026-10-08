package com.ajustor.fmab.gate;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.entity.TruthEntity;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * La visite de l'Espace blanc : l'alchimiste arrive devant sa Porte, face à sa Vérité (sa copie toute
 * blanche), qui lui parle ; la Porte s'ouvre, le savoir déferle, la Vérité prend son péage et le
 * porte désormais, et l'alchimiste se réveille sur son cercle. Chaque alchimiste a sa Porte, à son
 * propre emplacement de l'Espace blanc.
 *
 * <p>Une âme qui a perdu son armure erre ici jusqu'à ce qu'un cercle d'âme l'appelle (voir
 * {@link SoulBinding}).
 */
public final class GateOfTruth {
	public static final ResourceKey<Level> WHITE_SPACE = ResourceKey.create(Registries.DIMENSION, Fmab.id("white_space"));

	/** Distance entre les Portes de deux alchimistes. */
	private static final int SPACING = 64;
	private static final int SLOTS = 1024;
	/** Par rapport à l'emplacement de l'alchimiste : où il se tient, où est sa Vérité, sa Porte. */
	private static final Vec3 ARRIVAL = new Vec3(0.5, 1, 6.5);
	private static final Vec3 TRUTH = new Vec3(0.5, 1, 3.5);
	private static final int GATE_Z = -2;
	private static final int GATE_HALF_WIDTH = 4;
	private static final int GATE_HEIGHT = 12;

	private static final int GREETING = 40;
	private static final int PRESENTATION = 110;
	private static final int OPENING = 190;
	private static final int KNOWLEDGE = 250;
	private static final int TOLL = 330;
	private static final int RETURN = 400;

	private GateOfTruth() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				GateState gate = player.getAttached(FmabAttachments.GATE);
				if (gate != null && gate.visit().isPresent()) {
					step(server, player, gate, gate.visit().get());
				} else if (gate != null && gate.adrift()) {
					drift(server, player, gate);
				} else if (player.level().dimension() == WHITE_SPACE && player.tickCount % 20 == 0) {
					// Personne ne reste dans l'Espace blanc sans y avoir été appelé.
					sendHome(server, player, null);
				}
			}
		});
	}

	/** L'emplacement de la Porte de ce joueur dans l'Espace blanc. */
	private static BlockPos origin(ServerPlayer player) {
		return new BlockPos(Math.floorMod(player.getUUID().hashCode(), SLOTS) * SPACING, 0, 0);
	}

	private static Vec3 at(ServerPlayer player, Vec3 local) {
		return Vec3.atLowerCornerOf(origin(player)).add(local);
	}

	private static void step(MinecraftServer server, ServerPlayer player, GateState gate, GateState.Visit visit) {
		ServerLevel space = server.getLevel(WHITE_SPACE);
		if (space == null) {
			Fmab.LOGGER.error("Dimension {} introuvable : la Porte ne peut pas s'ouvrir", WHITE_SPACE.identifier());
			player.setAttached(FmabAttachments.GATE, gate.withVisit(null));
			return;
		}
		if (visit.ticks() < 0 || player.level() != space) {
			if (visit.ticks() >= 0) {
				// Revenu d'ailleurs au milieu de la visite : on la reprend au début.
				visit = new GateState.Visit(visit.dimension(), visit.origin(), visit.ambition(), visit.severe(), -1);
			}
			arrive(space, player, gate);
			player.setAttached(FmabAttachments.GATE, gate.withVisit(visit.tick()));
			return;
		}
		int t = visit.ticks();
		switch (t) {
			case GREETING -> say(player, "truth.fmab.greeting");
			case PRESENTATION -> say(player, "truth.fmab.presentation");
			case OPENING -> open(space, player);
			case KNOWLEDGE -> knowledge(space, player);
			case TOLL -> gate = toll(space, player, gate, visit);
			default -> {
			}
		}
		if (t > OPENING && t < TOLL && t % 4 == 0) {
			blackArms(space, player);
		}
		if (t >= RETURN) {
			close(space, player);
			player.setAttached(FmabAttachments.GATE, gate.withVisit(null));
			if (gate.adrift()) {
				SoulBinding.drift(player);
				say(player, "truth.fmab.adrift");
			} else {
				leaveTruth(player);
				sendHome(server, player, visit);
			}
			return;
		}
		player.setAttached(FmabAttachments.GATE, gate.withVisit(visit.tick()));
	}

	/** Une âme sans armure attend devant sa Porte, face à sa Vérité. */
	private static void drift(MinecraftServer server, ServerPlayer player, GateState gate) {
		ServerLevel space = server.getLevel(WHITE_SPACE);
		if (space == null || player.level() == space) {
			return;
		}
		arrive(space, player, gate);
		SoulBinding.drift(player);
		say(player, "truth.fmab.adrift");
	}

	private static void arrive(ServerLevel space, ServerPlayer player, GateState gate) {
		build(space, player);
		player.teleport(new TeleportTransition(space, at(player, ARRIVAL), Vec3.ZERO, 180, 0,
				TeleportTransition.DO_NOTHING));
		truth(space, player).ifPresentOrElse(t -> t.mirror(player, gate.lost()), () -> {
			TruthEntity truth = FmabEntities.TRUTH.create(space, EntitySpawnReason.TRIGGERED);
			if (truth != null) {
				Vec3 pos = at(player, TRUTH);
				truth.snapTo(pos.x, pos.y, pos.z, 0, 0);
				truth.mirror(player, gate.lost());
				space.addFreshEntity(truth);
			}
		});
		space.playSound(null, BlockPos.containing(at(player, ARRIVAL)), SoundEvents.AMETHYST_BLOCK_RESONATE,
				SoundSource.PLAYERS, 1, 0.5f);
	}

	/** La Vérité de ce joueur, devant sa Porte. */
	private static Optional<TruthEntity> truth(ServerLevel space, ServerPlayer player) {
		return space.getEntitiesOfClass(TruthEntity.class, new AABB(BlockPos.containing(at(player, TRUTH))).inflate(8),
						t -> t.owner().filter(player.getUUID()::equals).isPresent())
				.stream().findFirst();
	}

	/** On quitte l'Espace blanc : la Vérité ne reste pas seule devant une Porte fermée. */
	public static void leaveTruth(ServerPlayer player) {
		ServerLevel space = player.level().getServer().getLevel(WHITE_SPACE);
		if (space != null) {
			truth(space, player).ifPresent(TruthEntity::discard);
		}
	}

	/** La Porte fermée : un mur de pierre sculptée, et les ténèbres derrière. */
	private static void build(ServerLevel space, ServerPlayer player) {
		BlockPos o = origin(player);
		for (int x = -GATE_HALF_WIDTH; x < GATE_HALF_WIDTH; x++) {
			for (int y = 1; y <= GATE_HEIGHT; y++) {
				space.setBlockAndUpdate(o.offset(x, y, GATE_Z), FmabBlocks.GATE_STONE.defaultBlockState());
				space.setBlockAndUpdate(o.offset(x, y, GATE_Z - 1), FmabBlocks.GATE_DARKNESS.defaultBlockState());
			}
		}
	}

	private static void close(ServerLevel space, ServerPlayer player) {
		build(space, player);
	}

	private static void open(ServerLevel space, ServerPlayer player) {
		BlockPos o = origin(player);
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int x = -GATE_HALF_WIDTH + 1; x < GATE_HALF_WIDTH - 1; x++) {
			for (int y = 1; y < GATE_HEIGHT; y++) {
				space.setBlockAndUpdate(o.offset(x, y, GATE_Z), air);
			}
		}
		space.playSound(null, o.offset(0, 4, GATE_Z), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2, 0.5f);
		say(player, "truth.fmab.opening");
	}

	/** Le savoir déferle : des images, trop, trop vite. */
	private static void knowledge(ServerLevel space, ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, false));
		space.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1, 0.5f);
		say(player, "truth.fmab.knowledge");
	}

	/** Les bras noirs sortent de la Porte et tirent l'alchimiste vers elle. */
	private static void blackArms(ServerLevel space, ServerPlayer player) {
		BlockPos o = origin(player);
		for (int i = 0; i < 6; i++) {
			double x = o.getX() + 0.5 + (space.getRandom().nextDouble() - 0.5) * 2 * (GATE_HALF_WIDTH - 1);
			double y = 1 + space.getRandom().nextDouble() * (GATE_HEIGHT - 2);
			double z = o.getZ() + GATE_Z + 0.5;
			Vec3 toward = player.position().add(0, 1, 0).subtract(x, y, z).normalize().scale(0.6);
			space.sendParticles(ParticleTypes.SQUID_INK, x, y, z, 0, toward.x, toward.y, toward.z, 1);
		}
		player.push(0, 0, -0.02);
		player.hurtMarked = true;
	}

	/**
	 * La Vérité prend son dû et le porte. La première fois qu'elle prend le corps, l'âme se retrouve
	 * dans une armure de fer ; à une âme déjà dans une armure, elle prend le sceau, et l'âme erre.
	 */
	private static GateState toll(ServerLevel space, ServerPlayer player, GateState gate, GateState.Visit visit) {
		BodyPart part = TollChooser.choose(gate.lost(), visit.ambition(), visit.severe(), player.getRandom().nextDouble());
		GateState paid;
		if (part == BodyPart.BODY && gate.soulBound()) {
			paid = gate.payWithSeal();
			for (EquipmentSlot slot : SoulArmor.SLOTS) {
				player.setItemSlot(slot, ItemStack.EMPTY);
			}
			say(player, "truth.fmab.toll.seal");
		} else {
			paid = gate.pay(part);
			say(player, "truth.fmab.toll");
			player.sendSystemMessage(Component.translatable("gate.fmab.toll_taken",
					Component.translatable(part.translationKey())).withStyle(s -> s.withColor(0xB0201A)));
			if (part == BodyPart.BODY) {
				SoulArmor.bindFirstTime(player);
				player.sendSystemMessage(Component.translatable("gate.fmab.soul.first_armor"));
			}
		}
		player.setAttached(FmabAttachments.GATE, paid);
		truth(space, player).ifPresent(t -> t.setStolen(paid.lost()));
		space.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1, 0.5f);
		Tolls.dropFromLostHands(player, paid);
		AlchemistData me = player.getAttachedOrCreate(FmabAttachments.ALCHEMIST);
		if (!me.rank().atLeast(Rank.GATE)) {
			player.setAttached(FmabAttachments.ALCHEMIST, me.withRank(Rank.GATE));
			player.sendSystemMessage(Component.translatable("gate.fmab.initiated",
					Component.translatable(Rank.GATE.translationKey())));
		}
		return paid;
	}

	private static void say(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable(key).withStyle(s -> s.withItalic(true).withColor(0x9A9A9A)));
	}

	/** Le réveil sur le cercle, ou au point d'apparition si le cercle n'est plus accessible. */
	private static void sendHome(MinecraftServer server, ServerPlayer player, GateState.Visit visit) {
		ServerLevel home = null;
		Vec3 target = null;
		if (visit != null) {
			home = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(visit.dimension())));
			target = Vec3.atBottomCenterOf(visit.origin());
		}
		if (home == null) {
			home = server.overworld();
			target = Vec3.atBottomCenterOf(home.getRespawnData().pos());
		}
		player.teleport(new TeleportTransition(home, target, Vec3.ZERO, player.getYRot(), 0,
				TeleportTransition.DO_NOTHING));
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2, false, false));
	}
}
