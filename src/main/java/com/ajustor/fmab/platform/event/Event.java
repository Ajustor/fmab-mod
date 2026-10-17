package com.ajustor.fmab.platform.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Un événement du mod : le jeu le déclenche par son {@link #invoker()}, le mod l'écoute par
 * {@link #register}. Le mod ne s'abonne jamais aux événements d'un loader : c'est l'adaptateur de la
 * plateforme (Fabric, et demain d'autres) qui relaie les siens vers ceux-ci, une fois pour toutes.
 *
 * @param <T> l'interface d'écoute
 */
public final class Event<T> {
	private final Function<List<T>, T> combiner;
	private final List<T> listeners = new ArrayList<>();
	private volatile T invoker;

	private Event(Function<List<T>, T> combiner) {
		this.combiner = combiner;
		this.invoker = combiner.apply(List.of());
	}

	/**
	 * @param combiner fait d'une liste d'écouteurs un seul, qui les appelle dans l'ordre et combine
	 *                 leurs réponses (le premier refus l'emporte, le premier résultat décisif…)
	 */
	public static <T> Event<T> create(Function<List<T>, T> combiner) {
		return new Event<>(combiner);
	}

	public synchronized void register(T listener) {
		listeners.add(listener);
		invoker = combiner.apply(List.copyOf(listeners));
	}

	/** L'écouteur qui représente tous les autres : c'est lui qu'on appelle pour déclencher l'événement. */
	public T invoker() {
		return invoker;
	}
}
