package com.ajustor.fmab.alchemy.knowledge;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ce que l'alchimiste sait dans chaque école : sa maîtrise, les nœuds qu'elle a débloqués et ceux
 * qu'on lui a accordés, et les bonus qui en découlent.
 *
 * <p>Un nœud est acquis quand la maîtrise de son école atteint son seuil et que son parent est
 * acquis, ou quand un livre ou un maître l'a accordé.
 */
public final class Knowledge {
	/** Maîtrise totale à partir de laquelle un Apprenti devient Alchimiste. */
	public static final int ALCHEMIST_MASTERY = 60;
	/** Maîtrise gagnée par effet réussi. */
	public static final int MASTERY_PER_EFFECT = 3;

	/** Personne ne sait rien : sert à l'éditeur web et aux tests. */
	public static final Knowledge NONE = new Knowledge(List.of(), Map.of(), Set.of());

	private final Map<String, Integer> mastery;
	private final Set<String> unlocked;
	private final List<KnowledgeNode> nodes;

	public Knowledge(Collection<KnowledgeNode> nodes, Map<String, Integer> mastery, Set<String> granted) {
		this.nodes = List.copyOf(nodes);
		this.mastery = Map.copyOf(mastery);
		Set<String> out = new LinkedHashSet<>(granted);
		// Les parents peuvent être déclarés après leurs enfants : on itère jusqu'à stabilité.
		boolean changed = true;
		while (changed) {
			changed = false;
			for (KnowledgeNode node : nodes) {
				if (!out.contains(node.id())
						&& mastery(node.school()) >= node.mastery()
						&& node.parent().map(out::contains).orElse(true)) {
					out.add(node.id());
					changed = true;
				}
			}
		}
		this.unlocked = Set.copyOf(out);
	}

	public int mastery(String school) {
		return mastery.getOrDefault(school, 0);
	}

	public int totalMastery() {
		return mastery.values().stream().mapToInt(Integer::intValue).sum();
	}

	public boolean has(String node) {
		return unlocked.contains(node);
	}

	public Set<String> unlocked() {
		return unlocked;
	}

	public List<KnowledgeNode> nodes() {
		return nodes;
	}

	/** Somme des bonus d'un type, pour une école. */
	public double perk(String school, String type) {
		double total = 0;
		for (KnowledgeNode node : nodes) {
			if (node.school().equals(school) && unlocked.contains(node.id())) {
				for (KnowledgeNode.Perk perk : node.perks()) {
					if (perk.type().equals(type)) {
						total += perk.value();
					}
				}
			}
		}
		return total;
	}
}
