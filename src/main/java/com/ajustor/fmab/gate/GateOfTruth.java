package com.ajustor.fmab.gate;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.entity.GateHandEntity;
import com.ajustor.fmab.entity.TruthEntity;
import com.ajustor.fmab.network.CinematicPayload;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabSounds;
import com.ajustor.fmab.stone.LivingStone;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
	/** Le temps que les bras noirs mettent à tirer l'alchimiste dans la Porte, depuis son cercle. */
	public static final int PULL = 50;
	/** Une âme errante qui s'éloigne plus loin que ça de sa Porte se fait rattraper. */
	private static final double STRAY = 16;
	/** Une âme errante entend la Vérité se moquer d'elle toutes les minutes. */
	private static final int MOCKERY_PERIOD = 1200;
	private static final int MOCKERIES = 5;

	private GateOfTruth() {
	}

	public static void register() {
		// Revenu en pleine visite : la cinématique reprend là où elle en était.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			GateState gate = player.getAttached(FmabAttachments.GATE);
			if (gate == null || gate.visit().isEmpty()) {
				return;
			}
			int t = gate.visit().get().ticks();
			if (t < -1) {
				CinematicPayload.play(player, CinematicPayload.GATE_PULL, -t);
			} else if (t >= 0 && t < RETURN) {
				CinematicPayload.play(player, CinematicPayload.GATE, RETURN + 2 - t);
			}
		});
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
		// Mort pendant qu'on l'aspire : la Porte attend qu'il revienne à la vie.
		if (!player.isAlive()) {
			return;
		}
		ServerLevel space = server.getLevel(WHITE_SPACE);
		if (space == null) {
			Fmab.LOGGER.error("Dimension {} introuvable : la Porte ne peut pas s'ouvrir", WHITE_SPACE.identifier());
			player.setAttached(FmabAttachments.GATE, gate.withVisit(null));
			return;
		}
		if (visit.ticks() < -1 && player.level() != space) {
			// Les bras noirs le tiennent : il ne bouge plus, le monde s'assombrit.
			player.setDeltaMovement(player.getDeltaMovement().multiply(0.2, 0.2, 0.2));
			player.hurtMarked = true;
			player.setAttached(FmabAttachments.GATE, gate.withVisit(visit.tick()));
			return;
		}
		if (visit.ticks() < 0 || player.level() != space) {
			if (visit.ticks() >= 0) {
				// Revenu d'ailleurs au milieu de la visite : on la reprend au début.
				visit = new GateState.Visit(visit.dimension(), visit.origin(), visit.ambition(), visit.severe(), -1);
			}
			if (arrive(space, player, gate)) {
				player.setAttached(FmabAttachments.GATE, gate.withVisit(visit.tick()));
			}
			return;
		}
		int t = visit.ticks();
		// Ce qu'elle dit dépend de combien de fois on est déjà venu la voir.
		String tier = familiarity(gate.openings());
		switch (t) {
			case GREETING -> say(player, "truth.fmab.greeting." + tier);
			case PRESENTATION -> say(player, "truth.fmab.presentation." + tier);
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
				mock(player);
			} else {
				leaveTruth(player);
				sendHome(server, player, visit);
			}
			return;
		}
		player.setAttached(FmabAttachments.GATE, gate.withVisit(visit.tick()));
	}

	/** Une âme sans armure attend devant sa Porte, face à sa Vérité, qui ne se prive pas de s'en moquer. */
	private static void drift(MinecraftServer server, ServerPlayer player, GateState gate) {
		ServerLevel space = server.getLevel(WHITE_SPACE);
		if (space == null) {
			return;
		}
		if (player.level() == space) {
			if (player.tickCount % MOCKERY_PERIOD == 0) {
				mock(player);
			}
			reclaim(space, player);
			return;
		}
		arrive(space, player, gate);
		SoulBinding.drift(player);
		mock(player);
	}

	/**
	 * Le piège de l'Espace blanc : une âme errante qui s'éloigne de sa Porte voit des bras noirs en
	 * jaillir et la ramener devant elle. On n'échappe pas à la Vérité.
	 */
	private static void reclaim(ServerLevel space, ServerPlayer player) {
		if (player.tickCount % 20 != 0 || player.position().distanceTo(at(player, ARRIVAL)) < STRAY) {
			return;
		}
		BlockPos o = origin(player);
		for (int i = 0; i < 3; i++) {
			double x = o.getX() + 0.5 + (space.getRandom().nextDouble() - 0.5) * 2 * (GATE_HALF_WIDTH - 1.5);
			GateHandEntity.reach(space, new Vec3(x, o.getY() + 1.5, o.getZ() + GATE_Z - 0.5), player, 50, 0.12);
		}
		space.playSound(null, player.blockPosition(), FmabSounds.GATE_HANDS, SoundSource.PLAYERS, 1.5f, 0.7f);
		say(player, "truth.fmab.reclaim");
	}

	/** Plus on revient, plus la Vérité devient familière. */
	private static String familiarity(int openings) {
		if (openings == 0) {
			return "first";
		}
		if (openings == 1) {
			return "again";
		}
		return openings < 4 ? "regular" : "friend";
	}

	/** Une âme sans corps qui revient devant sa Porte : la Vérité trouve ça très drôle. */
	private static void mock(ServerPlayer player) {
		say(player, "truth.fmab.mock." + (1 + player.getRandom().nextInt(MOCKERIES)));
		player.sendSystemMessage(Component.translatable("truth.fmab.mock_hint").withStyle(s -> s.withColor(0x707070)));
	}

	/** @return vrai si l'alchimiste est bien arrivé devant sa Porte */
	private static boolean arrive(ServerLevel space, ServerPlayer player, GateState gate) {
		build(space, player);
		if (player.teleport(new TeleportTransition(space, at(player, ARRIVAL), Vec3.ZERO, 180, 0,
				TeleportTransition.DO_NOTHING)) == null) {
			return false;
		}
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
		CinematicPayload.play(player, CinematicPayload.GATE, RETURN + 2);
		return true;
	}

	/** La Vérité de ce joueur, devant sa Porte. */
	private static Optional<TruthEntity> truth(ServerLevel space, ServerPlayer player) {
		return space.getEntitiesOfClass(TruthEntity.class, new AABB(BlockPos.containing(at(player, TRUTH))).inflate(8),
						t -> t.owner().filter(player.getUUID()::equals).isPresent())
				.stream().findFirst();
	}

	/** La Vérité rend ce qu'elle avait volé : elle ne le porte plus. */
	public static void returnParts(ServerPlayer player) {
		ServerLevel space = player.level().getServer().getLevel(WHITE_SPACE);
		if (space != null) {
			truth(space, player).ifPresent(t -> t.setStolen(player.getAttachedOrCreate(FmabAttachments.GATE).lost()));
		}
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
		space.playSound(null, o.offset(0, 4, GATE_Z), FmabSounds.GATE_OPEN, SoundSource.PLAYERS, 3, 1);
		say(player, "truth.fmab.opening");
		// Les bras noirs sortent des ténèbres derrière la Porte et viennent le chercher.
		for (int i = 0; i < 8; i++) {
			double x = o.getX() + 0.5 + (space.getRandom().nextDouble() - 0.5) * 2 * (GATE_HALF_WIDTH - 1.5);
			// À hauteur d'homme : ils le tirent vers la Porte, pas vers le ciel.
			double y = o.getY() + 1.2 + space.getRandom().nextDouble() * 2.5;
			GateHandEntity.reach(space, new Vec3(x, y, o.getZ() + GATE_Z - 0.5), player, TOLL - OPENING + 20, 0.035);
		}
	}

	/** Le savoir déferle : des images, trop, trop vite. */
	private static void knowledge(ServerLevel space, ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0, false, false));
		space.playSound(null, player.blockPosition(), FmabSounds.GATE_KNOWLEDGE, SoundSource.PLAYERS, 2, 1);
		say(player, "truth.fmab.knowledge");
		// Derrière la Porte : l'œil, le torrent des symboles, puis le blanc.
		CinematicPayload.play(player, CinematicPayload.GATE_KNOWLEDGE, TOLL - KNOWLEDGE);
	}

	/**
	 * L'alchimiste est happé par son propre cercle : des bras noirs en jaillissent tout autour et le
	 * tiennent, le monde s'éteint, puis la Porte l'avale (voir {@link #PULL}).
	 */
	public static void pullFromCircle(ServerLevel level, BlockPos circle, ServerPlayer caster) {
		Vec3 center = Vec3.atBottomCenterOf(circle);
		for (int i = 0; i < 7; i++) {
			double a = Math.PI * 2 * i / 7;
			Vec3 from = center.add(Math.cos(a) * 2.2, 0.05, Math.sin(a) * 2.2);
			GateHandEntity.reach(level, from, caster, PULL + 10, 0.05);
		}
		level.playSound(null, circle, FmabSounds.GATE_HANDS, SoundSource.PLAYERS, 2, 0.8f);
		CinematicPayload.play(caster, CinematicPayload.GATE_PULL, PULL);
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
		if (space.getRandom().nextInt(6) == 0) {
			space.playSound(null, player.blockPosition(), FmabSounds.GATE_HANDS, SoundSource.PLAYERS, 0.8f,
					0.8f + space.getRandom().nextFloat() * 0.4f);
		}
	}

	/**
	 * La Vérité prend son dû et le porte. La première fois qu'elle prend le corps, l'âme se retrouve
	 * dans une armure de fer ; à une âme déjà dans une armure, elle prend le sceau, et l'âme erre.
	 */
	private static GateState toll(ServerLevel space, ServerPlayer player, GateState gate, GateState.Visit visit) {
		if (LivingStone.souls(player) > 0) {
			// Une Pierre vivante a déjà tout payé.
			say(player, "truth.fmab.toll.living_stone");
			return gate;
		}
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
			say(player, "truth.fmab.toll." + familiarity(gate.openings()));
			player.sendSystemMessage(Component.translatable("gate.fmab.toll_taken",
					Component.translatable(part.translationKey())).withStyle(s -> s.withColor(0xB0201A)));
			if (part == BodyPart.BODY) {
				SoulArmor.bindFirstTime(player);
				player.sendSystemMessage(Component.translatable("gate.fmab.soul.first_armor"));
				SoulArmor.giveNotes(player);
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
