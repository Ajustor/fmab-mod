package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.SimpleCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphLayer;
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
import java.util.Optional;
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
	private final List<Combination> combinations;

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
		this.combinations = List.copyOf(combinations);
		Fmab.LOGGER.info("Règles alchimiques chargées : {} glyphes, {} combinaisons, {} nœuds de savoir",
				glyphs.size(), combinations.size(), nodes.size());
	}

	public static synchronized AlchemyRules of(RegistryAccess access) {
		Registry<Glyph> glyphs = access.lookupOrThrow(FmabRegistries.GLYPH);
		return CACHE.computeIfAbsent(glyphs, g -> new AlchemyRules(g,
				access.lookupOrThrow(FmabRegistries.COMBINATION), access.lookupOrThrow(FmabRegistries.KNOWLEDGE)));
	}

	public List<Glyph> glyphs() {
		return glyphs;
	}

	public List<Combination> combinations() {
		return combinations;
	}

	/**
	 * Le cercle simple (un élément, une action) d'une combinaison, s'il en existe un : anneau,
	 * polygone juste assez grand, élément en haut, action en bas.
	 */
	public Optional<Drawing> simpleCircle(Combination c) {
		if (c.elements().size() != 1) {
			return Optional.empty();
		}
		Optional<Glyph> element = glyph(GlyphLayer.ELEMENT, c.elements().iterator().next());
		Optional<Glyph> action = glyph(GlyphLayer.ACTION, c.action());
		if (element.isEmpty() || action.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(SimpleCircles.of(element.get(), action.get(),
				SimpleCircles.sidesFor(element.get(), action.get())));
	}

	/** La première combinaison simple où figure ce glyphe. */
	public Optional<Combination> simpleCombinationWith(Glyph glyph) {
		return combinations.stream()
				.filter(c -> c.elements().size() == 1)
				.filter(c -> glyph.layer() == GlyphLayer.ELEMENT ? c.elements().contains(glyph.role())
						: glyph.layer() == GlyphLayer.ACTION && c.action().equals(glyph.role()))
				.filter(c -> c.requires().isEmpty())
				.findFirst();
	}

	public Optional<Glyph> glyph(String id) {
		return glyphs.stream().filter(g -> g.id().equals(id)).findFirst();
	}

	private Optional<Glyph> glyph(GlyphLayer layer, String role) {
		return glyphs.stream().filter(g -> g.is(layer, role)).findFirst();
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
