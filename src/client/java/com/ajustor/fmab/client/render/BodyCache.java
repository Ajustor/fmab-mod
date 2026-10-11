package com.ajustor.fmab.client.render;

import com.ajustor.fmab.data.Automail;
import com.ajustor.fmab.data.GateState;
import com.ajustor.fmab.gate.Automails;
import com.ajustor.fmab.gate.BodyPart;
import com.ajustor.fmab.item.AutomailItem;
import com.ajustor.fmab.registry.FmabAttachments;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Le corps d'un joueur tel que le rendu le dessine : les membres que la Porte lui a pris, les automails
 * posés dessus, ceux dont la lame est sortie. Lu à chaque image, recalculé seulement quand l'état de
 * la Porte ou les automails changent : ces attachments sont immuables et remplacés à chaque
 * changement, il suffit de comparer leurs références. Utilisé depuis le fil de rendu seulement.
 */
public final class BodyCache {
	/** Le corps d'un joueur : membres perdus, automails, lames sorties. */
	public record Body(Set<BodyPart> lost, Map<BodyPart, AutomailItem.Model> automails, Set<BodyPart> blades) {
		/** Un corps entier, sans rien de perdu. */
		public static final Body WHOLE = new Body(Set.of(), Map.of(), Set.of());
	}

	private record Entry(GateState gate, Automail automail, Body body) {
	}

	/** Au-delà, on repart de zéro : les joueurs partis ne s'accumulent pas. */
	private static final int LIMIT = 256;
	private static final Int2ObjectMap<Entry> CACHE = new Int2ObjectOpenHashMap<>();

	private BodyCache() {
	}

	public static Body of(Entity entity) {
		GateState gate = entity instanceof Player ? entity.getAttached(FmabAttachments.GATE) : null;
		if (gate == null || gate.lost().isEmpty()) {
			return Body.WHOLE;
		}
		Automail automail = entity.getAttached(FmabAttachments.AUTOMAIL);
		Entry entry = CACHE.get(entity.getId());
		if (entry != null && entry.gate() == gate && entry.automail() == automail) {
			return entry.body();
		}
		if (CACHE.size() >= LIMIT) {
			CACHE.clear();
		}
		Body body = compute(gate, automail);
		CACHE.put(entity.getId(), new Entry(gate, automail, body));
		return body;
	}

	private static Body compute(GateState gate, Automail automail) {
		Set<BodyPart> lost = EnumSet.noneOf(BodyPart.class);
		Map<BodyPart, AutomailItem.Model> automails = new EnumMap<>(BodyPart.class);
		Set<BodyPart> blades = EnumSet.noneOf(BodyPart.class);
		for (BodyPart part : gate.lost()) {
			if (!part.limb()) {
				continue;
			}
			lost.add(part);
			if (automail != null && automail.get(part).getItem() instanceof AutomailItem item && item.fits(part)) {
				automails.put(part, item.model());
				if (Automails.bladed(automail.get(part))) {
					blades.add(part);
				}
			}
		}
		return new Body(Collections.unmodifiableSet(lost), Collections.unmodifiableMap(automails),
				Collections.unmodifiableSet(blades));
	}
}
