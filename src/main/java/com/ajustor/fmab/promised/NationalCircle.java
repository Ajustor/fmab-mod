package com.ajustor.fmab.promised;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.FmabConfig;
import com.ajustor.fmab.progress.Milestones;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Le cercle de transmutation national de Père : chaque massacre d'Amestris en a marqué un point de
 * sang. Tant que le cercle tient, Père est servi par le Jour promis (à chaque éclipse, sa zone
 * double et il se régénère). Sceller {@link #POINTS} points de sang, n'importe lesquels, brise le
 * cercle : sa zone anti-alchimie s'effondre. L'état est partagé par tout le monde.
 *
 * <p>Le cercle ne s'éveille pas à la première éclipse venue : Père attend ses sacrifices, des
 * alchimistes qui ont ouvert leur Porte. Il en retient la liste ; une éclipse n'éveille le cercle qu'une
 * fois qu'il y en a assez ({@link FmabConfig#promisedDaySacrifices()}, voir {@link #awake}).
 */
public final class NationalCircle extends SavedData {
	/** Les sept points du cercle, comme les sept pointes de l'heptagone. */
	public static final int POINTS = 7;

	private static final Codec<NationalCircle> CODEC = Codec.withAlternative(
			RecordCodecBuilder.create(i -> i.group(
					BlockPos.CODEC.listOf().optionalFieldOf("sealed", List.of()).forGetter(c -> List.copyOf(c.sealed)),
					Codec.BOOL.optionalFieldOf("father_fallen", false).forGetter(c -> c.fatherFallen),
					UUIDUtil.CODEC.listOf().optionalFieldOf("sacrifices", List.of())
							.forGetter(c -> List.copyOf(c.sacrifices))
			).apply(i, NationalCircle::new)),
			// Format d'avant : la liste des points scellés, seule.
			BlockPos.CODEC.listOf().xmap(sealed -> new NationalCircle(sealed, false, List.of()),
					c -> List.copyOf(c.sealed)));
	private static final SavedDataType<NationalCircle> TYPE = new SavedDataType<>(Fmab.id("national_circle"),
			NationalCircle::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Set<BlockPos> sealed;
	/** Père est tombé : il n'y aura plus de Jour promis. */
	private boolean fatherFallen;
	/** Les joueurs qui ont ouvert leur Porte : les sacrifices que Père attend. */
	private final Set<UUID> sacrifices;

	public NationalCircle() {
		this(List.of(), false, List.of());
	}

	private NationalCircle(List<BlockPos> sealed, boolean fatherFallen, List<UUID> sacrifices) {
		this.sealed = new HashSet<>(sealed);
		this.fatherFallen = fatherFallen;
		this.sacrifices = new HashSet<>(sacrifices);
	}

	/** Ce joueur a ouvert sa Porte : un sacrifice de plus, compté une seule fois. */
	public void sacrifice(UUID player) {
		if (sacrifices.add(player)) {
			setDirty();
		}
	}

	/** Le nombre de joueurs qui ont ouvert leur Porte. */
	public int sacrifices() {
		return sacrifices.size();
	}

	/**
	 * Une éclipse éveillerait-elle le cercle ? Il faut qu'il tienne, que Père soit debout, et qu'assez
	 * de joueurs aient ouvert leur Porte.
	 */
	public boolean awake(int required) {
		return wakes(broken(), fatherFallen, sacrifices.size(), required);
	}

	/** {@link #awake(int)} avec le réglage du serveur. */
	public boolean awake() {
		return awake(FmabConfig.get().promisedDaySacrifices());
	}

	/** La règle d'éveil du cercle, en fonction pure. */
	public static boolean wakes(boolean broken, boolean fatherFallen, int sacrifices, int required) {
		return !broken && !fatherFallen && sacrifices >= required;
	}

	public boolean fatherFallen() {
		return fatherFallen;
	}

	public void fatherFalls() {
		fatherFallen = true;
		setDirty();
	}

	public static NationalCircle get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** Les points de sang scellés. */
	public java.util.Set<BlockPos> sealed() {
		return java.util.Set.copyOf(sealed);
	}

	public int sealedCount() {
		return Math.min(POINTS, sealed.size());
	}

	public boolean broken() {
		return sealed.size() >= POINTS;
	}

	/** Un point de sang vient d'être scellé : on le compte, on l'annonce à tous. */
	public void seal(MinecraftServer server, BlockPos pos, ServerPlayer by) {
		boolean wasBroken = broken();
		if (!sealed.add(pos.immutable())) {
			return;
		}
		setDirty();
		Milestones.reach(by, "crest_sealed");
		boolean nowBroken = broken() && !wasBroken;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (nowBroken) {
				p.sendSystemMessage(Component.translatable("promised.fmab.circle_broken", by.getDisplayName())
						.withStyle(ChatFormatting.GOLD));
				Milestones.reach(p, "circle_broken");
			} else if (!wasBroken) {
				p.sendSystemMessage(Component.translatable("promised.fmab.crest_sealed", by.getDisplayName(),
						sealedCount(), POINTS).withStyle(ChatFormatting.DARK_RED));
			}
		}
	}
}
