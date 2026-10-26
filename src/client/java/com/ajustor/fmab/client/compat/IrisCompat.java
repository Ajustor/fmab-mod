package com.ajustor.fmab.client.compat;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.client.render.EclipseSky;
import com.ajustor.fmab.platform.Platform;

import org.jspecify.annotations.Nullable;

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

	/** L'API d'Iris et sa question « un shader pack est-il actif ? », ou rien sans Iris. */
	private static @Nullable Object iris;
	private static @Nullable Method inUse;

	private IrisCompat() {
	}

	/**
	 * Un shader pack dessine-t-il le monde ? Il calcule alors son ciel et sa lumière lui-même, sans
	 * l'éclipse : c'est au voile de l'écran de l'assombrir.
	 */
	public static boolean shaders() {
		if (iris == null || inUse == null) {
			return false;
		}
		try {
			return (boolean) inUse.invoke(iris);
		} catch (ReflectiveOperationException e) {
			inUse = null;
			return false;
		}
	}

	public static void register() {
		if (!Platform.get().isModLoaded("iris")) {
			return;
		}
		try {
			Class<?> api = Class.forName(API);
			Object iris = api.getMethod("getInstance").invoke(null);
			IrisCompat.iris = iris;
			inUse = api.getMethod("isShaderPackInUse");
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
