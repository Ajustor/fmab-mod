package com.ajustor.fmab.world;

import com.ajustor.fmab.TestBootstrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'empreinte de tous les plans, bloc à bloc, à toutes les tailles où les lieux les posent (voir
 * {@link BlueprintsTest#plots}), coffres et créatures compris. Un remaniement qui ne doit rien
 * changer aux structures garde la même empreinte ; un changement voulu de plan la met à jour ici.
 */
class BlueprintsFingerprintTest {
	/** L'empreinte des plans après les coffres des lieux qui n'en avaient pas (FMAB-58). */
	private static final String EXPECTED = "9d459874655e58acf8c773ab37d110a7b4e99e90de455f219d278ecf4a761e95";

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	static String fingerprint() throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		Map<String, List<Blueprint.Plot>> plots = new TreeMap<>(BlueprintsTest.plots());
		StringBuilder line = new StringBuilder();
		for (Map.Entry<String, List<Blueprint.Plot>> entry : plots.entrySet()) {
			Blueprint plan = Blueprints.get(entry.getKey());
			for (Blueprint.Plot plot : entry.getValue()) {
				line.setLength(0);
				line.append(entry.getKey()).append(' ').append(plot).append('\n');
				for (int x = 0; x < plot.sizeX(); x++) {
					for (int z = 0; z < plot.sizeZ(); z++) {
						for (int y = -plot.ground(); y < plot.sizeY(); y++) {
							BlockState state = plan.at(x, y, z, plot);
							line.append(state == null ? "-" : state.toString()).append(';');
						}
					}
				}
				plan.chests(plot).forEach(c -> line.append("chest ").append(c.local()).append('\n'));
				plan.spawns(plot).forEach(s -> line.append("spawn ").append(BuiltInRegistries.ENTITY_TYPE.getKey(s.type()))
						.append(' ').append(s.local()).append('\n'));
				digest.update(line.toString().getBytes(StandardCharsets.UTF_8));
			}
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	@Test
	void everyPlanStaysTheSameBlockForBlock() throws Exception {
		assertEquals(EXPECTED, fingerprint());
	}
}
