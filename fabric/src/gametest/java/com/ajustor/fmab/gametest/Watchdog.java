package com.ajustor.fmab.gametest;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;

/**
 * Le chien de garde des gametests. Un jeu figé (un fil qui en attend un autre, une scène qui attend une
 * condition qui ne vient pas) n'écrit plus rien, et le job attendait jusqu'à son délai d'une heure.
 * Ici, une scène trop longue, ou un client qui ne tourne plus, arrête tout : on écrit dans le journal
 * la scène en cours et la pile de chaque fil, puis le jeu s'arrête en échec.
 *
 * <p>Les scènes durent de quelques secondes à une minute et demie ; les délais se règlent avec
 * {@code -Dfmab.gametest.sceneTimeout} et {@code -Dfmab.gametest.stallTimeout} (en secondes).
 */
final class Watchdog {
	/** Une scène qui dure plus que ça est figée. */
	private static final long SCENE_TIMEOUT = seconds("fmab.gametest.sceneTimeout", 300);
	/** Un client qui ne tourne plus depuis ça est figé (même en chargeant un monde, il tourne). */
	private static final long STALL_TIMEOUT = seconds("fmab.gametest.stallTimeout", 120);
	private static final long CHECK_EVERY = 5_000;

	private static volatile String scene = "(démarrage)";
	private static volatile long sceneStart = System.currentTimeMillis();
	private static volatile long lastTick = System.currentTimeMillis();
	private static boolean started;

	private Watchdog() {
	}

	/** Une scène commence : elle a son propre délai, et le journal dit laquelle c'est. */
	static synchronized void scene(String name) {
		start();
		long now = System.currentTimeMillis();
		System.out.printf("[fmab-gametest] scène %s (la précédente, %s, a duré %d s)%n", name, scene,
				(now - sceneStart) / 1000);
		scene = name;
		sceneStart = now;
	}

	private static void start() {
		if (started) {
			return;
		}
		started = true;
		ClientTickEvents.END_CLIENT_TICK.register(client -> lastTick = System.currentTimeMillis());
		Thread thread = new Thread(Watchdog::watch, "fmab-gametest-watchdog");
		thread.setDaemon(true);
		thread.start();
	}

	private static void watch() {
		while (true) {
			try {
				Thread.sleep(CHECK_EVERY);
			} catch (InterruptedException e) {
				return;
			}
			long now = System.currentTimeMillis();
			if (now - sceneStart > SCENE_TIMEOUT * 1000) {
				fail("la scène " + scene + " dure depuis plus de " + SCENE_TIMEOUT + " s");
			} else if (now - lastTick > STALL_TIMEOUT * 1000) {
				fail("le client ne tourne plus depuis " + (now - lastTick) / 1000 + " s (scène " + scene + ")");
			}
		}
	}

	/** Écrit l'état de chaque fil, puis arrête le jeu en échec : le job le dit tout de suite. */
	private static void fail(String why) {
		StringBuilder out = new StringBuilder("[fmab-gametest] JEU FIGÉ : ").append(why).append('\n');
		for (ThreadInfo info : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
			out.append('"').append(info.getThreadName()).append("\" ").append(info.getThreadState());
			if (info.getLockName() != null) {
				out.append(" en attente de ").append(info.getLockName());
			}
			if (info.getLockOwnerName() != null) {
				out.append(", tenu par \"").append(info.getLockOwnerName()).append('"');
			}
			out.append('\n');
			for (StackTraceElement frame : info.getStackTrace()) {
				out.append("\tat ").append(frame).append('\n');
			}
		}
		System.err.print(out);
		System.err.flush();
		System.out.flush();
		Runtime.getRuntime().halt(1);
	}

	private static long seconds(String property, long fallback) {
		try {
			return Long.parseLong(System.getProperty(property, String.valueOf(fallback)));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
