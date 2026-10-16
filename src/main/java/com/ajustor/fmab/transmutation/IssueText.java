package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Le texte d'un problème de cercle, avec ses détails quand le joueur en a besoin. */
public final class IssueText {
	private IssueText() {
	}

	public static Component of(CircleIssue issue) {
		if (issue.kind() == CircleIssue.Kind.INCOMPLETE_FORMULA) {
			// Les éléments qui manquent à la formule, par leur nom : « eau, carbone ».
			MutableComponent missing = Component.empty();
			for (String element : issue.detail().split("\\+")) {
				if (!missing.getSiblings().isEmpty()) {
					missing.append(", ");
				}
				missing.append(Component.translatable("material.fmab." + element));
			}
			return Component.translatable(issue.kind().translationKey(), missing);
		}
		return Component.translatable(issue.kind().translationKey());
	}
}
