package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.knowledge.Knowledge;
import com.ajustor.fmab.alchemy.knowledge.KnowledgeNode;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import com.ajustor.fmab.alchemy.rules.Combination;
import com.ajustor.fmab.alchemy.rules.CombinationTable;
import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.registry.FmabRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Le parseur et les règles construits à partir des glyphes et combinaisons chargés. Les registres
 * dynamiques sont recréés à chaque chargement de monde ou connexion : on garde une instance par
 * registre, sans retenir les anciens.
 */
public final class AlchemyRules {
	private static final Map<Registry<Glyph>, AlchemyRules> CACHE = new WeakHashMap<>();

	private final List<Glyph> glyphs;
	private final List<KnowledgeNode> nodes;
	private final CircleParser parser;
	private final CircleAnalyzer analyzer;

	private AlchemyRules(Registry<Glyph> glyphRegistry, Registry<Combination> combinationRegistry,
			Registry<KnowledgeNode> knowledgeRegistry) {
		this.glyphs = glyphRegistry.stream().toList();
		List<KnowledgeNode> nodes = new ArrayList<>();
		knowledgeRegistry.entrySet()
				.forEach(e -> nodes.add(e.getValue().withId(e.getKey().identifier().toString())));
		this.nodes = List.copyOf(nodes);
		List<Combination> combinations = new ArrayList<>();
		combinationRegistry.entrySet()
				.forEach(e -> combinations.add(e.getValue().withId(e.getKey().identifier().toString())));
		this.parser = new CircleParser(new GlyphRecognizer(glyphs));
		this.analyzer = new CircleAnalyzer(new CombinationTable(combinations), glyphs);
	}

	public static synchronized AlchemyRules of(RegistryAccess access) {
		Registry<Glyph> glyphs = access.lookupOrThrow(FmabRegistries.GLYPH);
		return CACHE.computeIfAbsent(glyphs, g -> new AlchemyRules(g,
				access.lookupOrThrow(FmabRegistries.COMBINATION), access.lookupOrThrow(FmabRegistries.KNOWLEDGE)));
	}

	public List<Glyph> glyphs() {
		return glyphs;
	}

	public List<KnowledgeNode> nodes() {
		return nodes;
	}

	public Knowledge knowledge(AlchemistData alchemist) {
		return new Knowledge(nodes, alchemist.mastery(), alchemist.granted());
	}

	/** Le cercle tel que cet alchimiste le comprend, avec son rang, ses glyphes et son savoir. */
	public Analysis analyze(Drawing drawing, AlchemistData alchemist) {
		return analyzer.analyze(parser.parse(drawing), alchemist.rank(), alchemist.known(), knowledge(alchemist));
	}
}
