package com.ajustor.fmab.alchemy.circle;

import java.util.List;

/** Ce que le parseur a lu dans un tracé, avant toute règle de rang ou de coût. */
public record ParsedCircle(List<Stage> stages, List<CircleIssue> issues) {
	public ParsedCircle {
		stages = List.copyOf(stages);
		issues = List.copyOf(issues);
	}
}
