package com.ajustor.fmab.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * L'armure d'une âme, vue comme une coquille : figée, elle n'est plus qu'une armure qu'un autre
 * joueur peut revêtir (il la fait marcher, et en ressort quand il veut) ; si l'âme se remet à bouger,
 * celui qui est dedans est prisonnier de son corps jusqu'à ce qu'elle le relâche. Une seule place.
 *
 * @param still    l'âme s'est figée : son armure ne bouge plus d'elle-même
 * @param occupant le joueur qui est dedans
 */
public record Shell(boolean still, Optional<UUID> occupant) {
	public static final Shell NONE = new Shell(false, Optional.empty());

	public static final StreamCodec<ByteBuf, Shell> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, Shell::still,
			ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Shell::occupant,
			Shell::new);

	/** Celui qui est dedans ne peut plus sortir : l'âme a repris son corps. */
	public boolean trapped() {
		return occupant.isPresent() && !still;
	}

	/** Celui qui est dedans mène l'armure : l'âme est figée, elle se laisse porter. */
	public boolean worn() {
		return occupant.isPresent() && still;
	}

	/** Une armure figée et vide se revêt. */
	public boolean canEnter() {
		return still && occupant.isEmpty();
	}

	public Shell enter(UUID player) {
		return canEnter() ? new Shell(true, Optional.of(player)) : this;
	}

	/** On ne sort que d'une armure figée : prisonnier, on attend d'être relâché. */
	public boolean canLeave() {
		return worn();
	}

	public Shell leave() {
		return canLeave() ? NONE.withStill(true) : this;
	}

	/**
	 * La touche de l'âme : avec quelqu'un dedans, elle le relâche (et reprend son corps) ; sinon elle
	 * se fige, ou se réveille.
	 */
	public Shell press() {
		return occupant.isPresent() ? NONE : withStill(!still);
	}

	/** L'âme bouge : elle se réveille, et qui est dedans y reste enfermé. */
	public Shell move() {
		return still ? new Shell(false, occupant) : this;
	}

	/** L'âme ou son occupant s'en va (déconnexion, mort, autre dimension) : l'armure se vide et se réveille. */
	public Shell release() {
		return NONE;
	}

	private Shell withStill(boolean value) {
		return new Shell(value, occupant);
	}
}
