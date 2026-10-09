package com.ajustor.fmab.promised;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.progress.Milestones;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Le cercle de transmutation national de Père : chaque massacre d'Amestris en a marqué un point de
 * sang. Tant que le cercle tient, Père est servi par le Jour promis (à chaque éclipse, sa zone
 * double et il se régénère). Sceller {@link #POINTS} points de sang, n'importe lesquels, brise le
 * cercle : sa zone anti-alchimie s'effondre. L'état est partagé par tout le monde.
 */
public final class NationalCircle extends SavedData {
	/** Les sept points du cercle, comme les sept pointes de l'heptagone. */
	public static final int POINTS = 7;

	private static final Codec<NationalCircle> CODEC = Codec.withAlternative(
			RecordCodecBuilder.create(i -> i.group(
					BlockPos.CODEC.listOf().optionalFieldOf("sealed", List.of()).forGetter(c -> List.copyOf(c.sealed)),
					Codec.BOOL.optionalFieldOf("father_fallen", false).forGetter(c -> c.fatherFallen)
			).apply(i, NationalCircle::new)),
			// Format d'avant : la liste des points scellés, seule.
			BlockPos.CODEC.listOf().xmap(sealed -> new NationalCircle(sealed, false), c -> List.copyOf(c.sealed)));
	private static final SavedDataType<NationalCircle> TYPE = new SavedDataType<>(Fmab.id("national_circle"),
			NationalCircle::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Set<BlockPos> sealed;
	/** Père est tombé : il n'y aura plus de Jour promis. */
	private boolean fatherFallen;

	public NationalCircle() {
		this(List.of(), false);
	}

	private NationalCircle(List<BlockPos> sealed, boolean fatherFallen) {
		this.sealed = new HashSet<>(sealed);
		this.fatherFallen = fatherFallen;
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
