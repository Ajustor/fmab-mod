package com.ajustor.fmab.alchemy.drawing;

import com.ajustor.fmab.alchemy.glyph.GlyphJson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Code texte d'un tracé, à copier entre le carnet et l'éditeur web : le JSON
 * {@code {"v":1,"primitives":[...]}} compressé (deflate brut) puis encodé en base64 URL.
 */
public final class DrawingCode {
	public static final int VERSION = 1;
	/** Un code plus long est refusé avant même d'être décompressé. */
	public static final int MAX_CODE_LENGTH = 16_384;
	private static final int MAX_JSON_BYTES = 256 * 1024;

	private DrawingCode() {
	}

	public static String encode(Drawing drawing) {
		JsonObject root = new JsonObject();
		root.addProperty("v", VERSION);
		JsonArray prims = new JsonArray();
		drawing.primitives().forEach(p -> prims.add(GlyphJson.toJson(p)));
		root.add("primitives", prims);
		byte[] json = root.toString().getBytes(StandardCharsets.UTF_8);

		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
		deflater.setInput(json);
		deflater.finish();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		while (!deflater.finished()) {
			out.write(buf, 0, deflater.deflate(buf));
		}
		deflater.end();
		return Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
	}

	/** @throws IllegalArgumentException si le code est illisible, trop gros ou d'une version future */
	public static Drawing decode(String code) {
		String trimmed = code.strip();
		if (trimmed.isEmpty() || trimmed.length() > MAX_CODE_LENGTH) {
			throw new IllegalArgumentException("code vide ou trop long");
		}
		byte[] compressed = Base64.getUrlDecoder().decode(trimmed);
		Inflater inflater = new Inflater(true);
		inflater.setInput(compressed);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		try {
			while (!inflater.finished()) {
				int n = inflater.inflate(buf);
				if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
					break;
				}
				out.write(buf, 0, n);
				if (out.size() > MAX_JSON_BYTES) {
					throw new IllegalArgumentException("code trop gros");
				}
			}
		} catch (DataFormatException e) {
			throw new IllegalArgumentException("code corrompu", e);
		} finally {
			inflater.end();
		}
		JsonObject root = JsonParser.parseString(out.toString(StandardCharsets.UTF_8)).getAsJsonObject();
		int version = root.get("v").getAsInt();
		if (version > VERSION) {
			throw new IllegalArgumentException("code d'une version plus récente (" + version + ")");
		}
		List<Primitive> prims = new ArrayList<>();
		for (JsonElement e : root.getAsJsonArray("primitives")) {
			prims.add(GlyphJson.primitive(e.getAsJsonObject()));
		}
		if (prims.size() > Drawing.MAX_PRIMITIVES) {
			throw new IllegalArgumentException("trop de traits");
		}
		return new Drawing(prims);
	}
}
