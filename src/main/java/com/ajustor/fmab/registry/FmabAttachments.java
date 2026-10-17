package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.ExamProgress;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.data.Gloves;
import com.ajustor.fmab.data.NotebookContents;
import com.ajustor.fmab.data.Shell;
import com.ajustor.fmab.data.Tattoos;
import com.ajustor.fmab.data.Training;
import com.ajustor.fmab.data.TransmutationPose;
import com.ajustor.fmab.homunculus.Belly;
import com.ajustor.fmab.platform.data.DataSlot;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class FmabAttachments {
	/** Rang, savoir et concentration du joueur ; gardés à la mort, visibles de lui seul. */
	public static final DataSlot<AlchemistData> ALCHEMIST = DataSlot.<AlchemistData>builder()
			.persistent(AlchemistData.CODEC)
			.initializer(() -> AlchemistData.NEW)
			.copyOnDeath()
			.syncWith(AlchemistData.STREAM_CODEC, DataSlot.Sync.OWNER)
			.register(Fmab.id("alchemist"));

	/**
	 * Le Carnet de cercles : l'alchimiste le garde en tête (touche du carnet) et à sa mort, et le
	 * client le lit pour la roue des cercles.
	 */
	public static final DataSlot<NotebookContents> NOTEBOOK = DataSlot.<NotebookContents>builder()
			.persistent(NotebookContents.CODEC)
			.copyOnDeath()
			.syncWith(NotebookContents.STREAM_CODEC, DataSlot.Sync.OWNER)
			.register(Fmab.id("notebook"));

	/** Les gants portés, à gauche et à droite. */
	public static final DataSlot<Gloves> GLOVES = DataSlot.<Gloves>builder()
			.persistent(Gloves.CODEC)
			.initializer(() -> Gloves.NONE)
			.copyOnDeath()
			.syncWith(Gloves.STREAM_CODEC, DataSlot.Sync.ALL)
			.register(Fmab.id("gloves"));

	/** Les épreuves d'Izumi : gardées à la mort, inutiles au client. */
	public static final DataSlot<Training> TRAINING = DataSlot.<Training>builder()
			.persistent(Training.CODEC)
			.initializer(() -> Training.NONE)
			.copyOnDeath()
			.register(Fmab.id("training"));

	/** L'examen d'Alchimiste d'État : gardé à la mort, inutile au client. */
	public static final DataSlot<ExamProgress> EXAM = DataSlot.<ExamProgress>builder()
			.persistent(ExamProgress.CODEC)
			.initializer(() -> ExamProgress.NONE)
			.copyOnDeath()
			.register(Fmab.id("exam"));

	/** Cercles tatoués : permanents, gardés à la mort, visibles de tous (ils se dessinent sur le corps). */
	public static final DataSlot<Tattoos> TATTOOS = DataSlot.<Tattoos>builder()
			.persistent(Tattoos.CODEC)
			.initializer(() -> Tattoos.NONE)
			.copyOnDeath()
			.syncWith(Tattoos.STREAM_CODEC, DataSlot.Sync.ALL)
			.register(Fmab.id("tattoos"));

	/**
	 * Ce que la Porte a pris, et la visite en cours. Tous les clients voient ce qui manque : le joueur
	 * pour sa vue et ses mains, les autres pour dessiner ses membres perdus.
	 */
	public static final DataSlot<GateState> GATE = DataSlot.<GateState>builder()
			.persistent(GateState.CODEC)
			.initializer(() -> GateState.NONE)
			.copyOnDeath()
			.syncWith(GateState.STREAM_CODEC, DataSlot.Sync.ALL)
			.register(Fmab.id("gate"));

	/**
	 * L'heure de jeu à laquelle l'âme s'est mise à errer devant sa Porte : sa place dans la file des
	 * âmes errantes, gardée à la déconnexion, au redémarrage du serveur et à la mort.
	 */
	public static final DataSlot<Long> ADRIFT_SINCE = DataSlot.<Long>builder()
			.persistent(Codec.LONG)
			.copyOnDeath()
			.register(Fmab.id("adrift_since"));

	/** Les automails posés sur les membres perdus, visibles de tous sur le corps du joueur. */
	public static final DataSlot<Automail> AUTOMAIL = DataSlot.<Automail>builder()
			.persistent(Automail.CODEC)
			.initializer(() -> Automail.NONE)
			.copyOnDeath()
			.syncWith(Automail.STREAM_CODEC, DataSlot.Sync.ALL)
			.register(Fmab.id("automail"));

	/** Le joueur est dans le Ventre de Gluttony : perdu à la mort, inutile au client. */
	public static final DataSlot<Belly.Swallowed> SWALLOWED = DataSlot.<Belly.Swallowed>builder()
			.persistent(Belly.Swallowed.CODEC)
			.register(Fmab.id("swallowed"));

	/**
	 * La concentration, à part du reste de l'alchimiste : elle se recharge chaque seconde, et seul ce
	 * nombre part vers le client. Absente : pleine. Gardée à la mort, visible du joueur.
	 */
	public static final DataSlot<Float> CONCENTRATION = DataSlot.<Float>builder()
			.persistent(Codec.FLOAT)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.FLOAT, DataSlot.Sync.OWNER)
			.register(Fmab.id("concentration"));

	/** Le karma, de −100 à +100 : gardé à la mort, visible du joueur. */
	public static final DataSlot<Integer> KARMA = DataSlot.<Integer>builder()
			.persistent(Codec.INT)
			.initializer(() -> 0)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.VAR_INT, DataSlot.Sync.OWNER)
			.register(Fmab.id("karma"));

	/** Les âmes d'une Pierre philosophale vivante ; absent pour qui n'en est pas une. */
	public static final DataSlot<Integer> LIVING_STONE = DataSlot.<Integer>builder()
			.persistent(Codec.INT)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.VAR_INT, DataSlot.Sync.OWNER)
			.register(Fmab.id("living_stone"));

	/** L'alkahestry de Xing, apprise de May Chang : gardée à la mort. */
	public static final DataSlot<Boolean> ALKAHESTRY = DataSlot.<Boolean>builder()
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.register(Fmab.id("alkahestry"));

	/** Allié de Greed, par un pacte scellé d'or : gardé à la mort, même si le pacte est rompu depuis. */
	public static final DataSlot<Boolean> GREED_PACT = DataSlot.<Boolean>builder()
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.register(Fmab.id("greed_pact"));

	/**
	 * La réponse à l'offre de Père ({@link com.ajustor.fmab.stone.Recruitment}) : acceptée ou refusée,
	 * une fois pour toutes. Gardée à la mort.
	 */
	public static final DataSlot<String> RECRUITMENT = DataSlot.<String>builder()
			.persistent(Codec.STRING)
			.copyOnDeath()
			.register(Fmab.id("recruitment"));

	/** Reconnu allié de Briggs par Olivier Armstrong : gardé à la mort. */
	public static final DataSlot<Boolean> BRIGGS_ALLY = DataSlot.<Boolean>builder()
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.register(Fmab.id("briggs_ally"));

	/** Recherché par l'armée jusqu'à cette heure de jeu, pour avoir frappé un soldat. */
	public static final DataSlot<Long> WANTED = DataSlot.<Long>builder()
			.persistent(Codec.LONG)
			.copyOnDeath()
			.register(Fmab.id("wanted"));

	/** Le bras droit de Scar, tatoué : gardé à la mort, connu du client (il ne mine plus à mains nues). */
	public static final DataSlot<Boolean> SCAR_ARM = DataSlot.<Boolean>builder()
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.BOOL, DataSlot.Sync.OWNER)
			.register(Fmab.id("scar_arm"));

	/** Le temps passé d'affilée sur l'île de Yock, en ticks (l'épreuve d'Izumi). */
	public static final DataSlot<Integer> ISLAND_TIME = DataSlot.<Integer>builder()
			.persistent(Codec.INT)
			.register(Fmab.id("island_time"));

	/** Les cadeaux uniques déjà reçus des PNJ (notes de Hohenheim, de Scar…) : gardés à la mort. */
	public static final DataSlot<Set<String>> GIFTS = DataSlot.<Set<String>>builder()
			.persistent(Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf))
			.initializer(Set::of)
			.copyOnDeath()
			.register(Fmab.id("gifts"));

	/** Les homonculus qu'on a vus tomber : la clé du sceau de Père. Gardés à la mort. */
	public static final DataSlot<Set<String>> SLAIN = DataSlot.<Set<String>>builder()
			.persistent(Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf))
			.initializer(Set::of)
			.copyOnDeath()
			.register(Fmab.id("slain"));

	/** Le geste de transmutation en cours : éphémère, visible de tous pour animer le joueur. */
	public static final DataSlot<TransmutationPose> POSE = DataSlot.<TransmutationPose>builder()
			.syncWith(TransmutationPose.STREAM_CODEC, DataSlot.Sync.ALL)
			.register(Fmab.id("pose"));

	/**
	 * Le fauteuil roulant dont le joueur tient les poignées (son identifiant d'entité) : posé et retiré
	 * par le fauteuil, éphémère, visible de tous pour l'animer sans chercher le fauteuil autour de lui.
	 */
	public static final DataSlot<Integer> PUSHING = DataSlot.<Integer>builder()
			.syncWith(ByteBufCodecs.VAR_INT, DataSlot.Sync.ALL)
			.register(Fmab.id("pushing"));

	/** Le joueur a reçu son matériel de départ. */
	public static final DataSlot<Boolean> EQUIPPED = DataSlot.<Boolean>builder()
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.register(Fmab.id("equipped"));

	/**
	 * L'armure d'une âme comme coquille : figée ou non, et qui est dedans. Éphémère (on ne se
	 * reconnecte pas prisonnier), visible de tous pour savoir qui cacher et quelle armure ne bouge plus.
	 */
	public static final DataSlot<Shell> SHELL = DataSlot.<Shell>builder()
			.syncWith(Shell.STREAM_CODEC, DataSlot.Sync.ALL)
			.register(Fmab.id("shell"));

	/** Le joueur est dans l'armure de cette âme : on ne le dessine plus, il ne se vise plus. */
	public static final DataSlot<UUID> INSIDE = DataSlot.<UUID>builder()
			.syncWith(UUIDUtil.STREAM_CODEC, DataSlot.Sync.ALL)
			.register(Fmab.id("inside"));

	private FmabAttachments() {
	}

	/**
	 * Les objets dont l'alchimiste a compris la structure en les décomposant sur un cercle : lui seul
	 * peut les recomposer d'après modèle. Gardés à la mort, visibles de lui seul.
	 */
	public static final DataSlot<Set<String>> UNDERSTOOD = DataSlot.<Set<String>>builder()
			.persistent(Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf))
			.initializer(Set::of)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(Set::copyOf, List::copyOf),
					DataSlot.Sync.OWNER)
			.register(Fmab.id("understood"));

	/**
	 * L'objet auquel l'alchimiste pense en recomposant, choisi sur sa page de conception : sans
	 * modèle posé sur le cercle, c'est lui qu'un cercle Recomposer crée. Gardé à la mort.
	 */
	public static final DataSlot<String> DESIGN = DataSlot.<String>builder()
			.persistent(Codec.STRING)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.STRING_UTF8, DataSlot.Sync.OWNER)
			.register(Fmab.id("design"));

	/**
	 * L'alchimiste a donné sa Porte pour rendre son corps à un autre (voir SoulGifts) : il ne
	 * transmutera plus jamais. Gardé à la mort, visible de lui.
	 */
	public static final DataSlot<Boolean> GATELESS = DataSlot.<Boolean>builder()
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.BOOL, DataSlot.Sync.OWNER)
			.register(Fmab.id("gateless"));

	public static void register() {
	}
}
