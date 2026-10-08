package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import com.ajustor.fmab.alchemy.rules.Combination;
import com.ajustor.fmab.alchemy.rules.CombinationTable;
import com.ajustor.fmab.registry.FmabRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Le parseur et les règles construits à partir des glyphes et combinaisons chargés. Les registres
 * dynamiques sont recréés à chaque chargement de monde ou connexion : on garde une instance par
 * registre, sans retenir les anciens.
 */
public final class AlchemyRules {
	private static final Map<Registry<Glyph>, AlchemyRules> CACHE = new WeakHashMap<>();

	private final List<Glyph> glyphs;
	private final CircleParser parser;
	private final CircleAnalyzer analyzer;

	private AlchemyRules(Registry<Glyph> glyphRegistry, Registry<Combination> combinationRegistry) {
		this.glyphs = glyphRegistry.stream().toList();
		List<Combination> combinations = new ArrayList<>();
		combinationRegistry.entrySet().forEach(e -> {
			Combination c = e.getValue();
			combinations.add(new Combination(e.getKey().identifier().toString(), c.elements(), c.action(), c.effect(),
					c.range()));
		});
		this.parser = new CircleParser(new GlyphRecognizer(glyphs));
		this.analyzer = new CircleAnalyzer(new CombinationTable(combinations), glyphs);
	}

	public static synchronized AlchemyRules of(RegistryAccess access) {
		Registry<Glyph> glyphs = access.lookupOrThrow(FmabRegistries.GLYPH);
		return CACHE.computeIfAbsent(glyphs,
				g -> new AlchemyRules(g, access.lookupOrThrow(FmabRegistries.COMBINATION)));
	}

	public List<Glyph> glyphs() {
		return glyphs;
	}

	public Analysis analyze(Drawing drawing, Rank rank, Set<String> known) {
		return analyzer.analyze(parser.parse(drawing), rank, known);
	}
}
