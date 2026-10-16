package com.ajustor.fmab.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Un cache qui oublie les entrées les moins récemment servies au-delà de sa capacité, sauf celles
 * qu'on a épinglées : une entrée épinglée (autant de fois qu'on l'a épinglée, tant qu'on ne l'a pas
 * désépinglée autant) n'est jamais évincée, quitte à dépasser la capacité. Le cache ne libère rien
 * lui-même : il rend ce qu'il évince, à l'appelant de s'en défaire.
 */
public final class PinnedLru<K, V> {
	private final int capacity;
	private final LinkedHashMap<K, V> entries = new LinkedHashMap<>(16, 0.75f, true);
	private final Map<K, Integer> pins = new HashMap<>();

	public PinnedLru(int capacity) {
		this.capacity = capacity;
	}

	/** La valeur, marquée comme la plus récemment servie ; null si absente. */
	public V get(K key) {
		return entries.get(key);
	}

	/**
	 * Range une valeur et rend celles qu'il a fallu évincer pour tenir dans la capacité. Celle qu'on
	 * vient de ranger reste toujours : l'appelant s'en sert aussitôt.
	 */
	public List<V> put(K key, V value) {
		entries.put(key, value);
		return evict(key);
	}

	/** Épingle une entrée présente : elle ne sera pas évincée tant qu'on ne l'a pas désépinglée. */
	public void pin(K key) {
		if (entries.containsKey(key)) {
			pins.merge(key, 1, Integer::sum);
		}
	}

	/** Retire une épingle, et rend ce qui dépasse désormais la capacité. */
	public List<V> unpin(K key) {
		pins.computeIfPresent(key, (k, n) -> n > 1 ? n - 1 : null);
		return evict(null);
	}

	public boolean pinned(K key) {
		return pins.containsKey(key);
	}

	public int size() {
		return entries.size();
	}

	/** Évince les plus anciennes, sauf les épinglées et {@code kept}. */
	private List<V> evict(K kept) {
		List<V> out = new ArrayList<>();
		Iterator<Map.Entry<K, V>> it = entries.entrySet().iterator();
		while (entries.size() > capacity && it.hasNext()) {
			Map.Entry<K, V> oldest = it.next();
			if (!pins.containsKey(oldest.getKey()) && !oldest.getKey().equals(kept)) {
				out.add(oldest.getValue());
				it.remove();
			}
		}
		return out;
	}
}
