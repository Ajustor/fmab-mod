package com.ajustor.fmab.client.compat;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.client.render.EclipseSky;
import com.ajustor.fmab.platform.Platform;

import java.lang.reflect.Method;

/**
 * Les shaders d'Iris : Iris remplace chaque pipeline du jeu par un programme du shader pack, et ne
 * sait pas quoi faire des nôtres (« Missing program … in override list ») : la silhouette de la lune
 * de l'éclipse ne se dessinait pas. On lui dit de la traiter comme les astres du ciel.
 *
 * <p>Par réflexion : Iris est facultatif, et son API suit la classe des pipelines, qui change d'une
 * version de Minecraft à l'autre.
 */
public final class IrisCompat {
	private static final String API = "net.irisshaders.iris.api.v0.IrisApi";
	private static final String PROGRAM = "net.irisshaders.iris.api.v0.IrisProgram";

	private IrisCompat() {
	}

	public static void register() {
		if (!Platform.get().isModLoaded("iris")) {
			return;
		}
		try {
			Class<?> api = Class.forName(API);
			Object iris = api.getMethod("getInstance").invoke(null);
			Object sky = Class.forName(PROGRAM).getField("SKY_TEXTURED").get(null);
			if (!assign(iris, EclipseSky.MASK, sky)) {
				Fmab.LOGGER.warn("Iris : pas de assignPipeline, la lune de l'éclipse risque de manquer sous shaders");
			}
		} catch (ReflectiveOperationException | LinkageError e) {
			Fmab.LOGGER.warn("Iris : impossible de déclarer la lune de l'éclipse", e);
		}
	}

	/** Confie le pipeline au programme d'Iris ; faux si l'API n'a pas de quoi le faire. */
	public static boolean assign(Object iris, Object pipeline, Object program) throws ReflectiveOperationException {
		for (Method method : iris.getClass().getMethods()) {
			if (method.getName().equals("assignPipeline") && method.getParameterCount() == 2
					&& method.getParameterTypes()[0].isInstance(pipeline)
					&& method.getParameterTypes()[1].isInstance(program)) {
				method.invoke(iris, pipeline, program);
				return true;
			}
		}
		return false;
	}
}
