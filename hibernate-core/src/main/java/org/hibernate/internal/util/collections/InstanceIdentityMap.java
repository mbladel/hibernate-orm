/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import org.hibernate.engine.spi.InstanceIdentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Utility collection backed by a simple array that takes advantage of {@link InstanceIdentity}'s
 * unique identifier to store objects and provider {@link Map} functionalities.
 * <p>
 * Methods accessing / modifying the map with {@link Object} typed paramters,
 * will need to type check against the instance identity interface which might be inefficient,
 * so it's recommended to use the position (int) based variant of those methods.
 * <p>
 * Iterating through the whole map is most efficient with {@link #forEach}, and since
 * we simply iterate the underlying array, it's also concurrent and reentrant safe.
 */
public class InstanceIdentityMap<K extends InstanceIdentity, V> implements Map<K, V> {
	private final PagedArray<Entry<K, V>> backingArray;

	private static final class Entry<K, V> implements Map.Entry<K, V> {
		private final K key;
		private final V value;

		public Entry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			throw new UnsupportedOperationException();
		}
	}

	public InstanceIdentityMap() {
		backingArray = new PagedArray<>();
	}

	@Override
	public int size() {
		return backingArray.size();
	}

	@Override
	public boolean isEmpty() {
		return backingArray.isEmpty();
	}

	public boolean containsKey(int instanceId) {
		return get( instanceId ) != null;
	}

	/**
	 * Returns true if this map contains a mapping for the specified key.
	 *
	 * @implNote This only works for {@link InstanceIdentity} keys, and it's inefficient
	 * since we need to do a type check. Prefer using {@link #containsKey(int)}.
	 */
	@Override
	public boolean containsKey(Object key) {
		if ( key instanceof InstanceIdentity instance ) {
			return containsKey( instance.$$_hibernate_getInstanceId() );
		}
		throw new IllegalArgumentException( "Provided key does not support instance identity" );
	}

	@Override
	public boolean containsValue(Object value) {
		for ( V v : values() ) {
			if ( Objects.equals( value, v ) ) {
				return true;
			}
		}
		return false;
	}

	public V get(int instanceId) {
		final Entry<K, V> entry = backingArray.get( instanceId );
		return entry == null ? null : entry.getValue();
	}

	/**
	 * Returns the value to which the specified key is mapped, or null if this map contains no mapping for the key.
	 *
	 * @implNote This only works for {@link InstanceIdentity} keys, and it's inefficient
	 * since we need to do a type check. Prefer using {@link #get(int)}.
	 */
	@Override
	public V get(Object key) {
		if ( key instanceof InstanceIdentity instance ) {
			return get( instance.$$_hibernate_getInstanceId() );
		}
		throw new IllegalArgumentException( "Provided key does not support instance identity" );
	}

	@Override
	public V put(K key, V value) {
		final int instanceId = key.$$_hibernate_getInstanceId();
		final Entry<K, V> old = backingArray.set( instanceId, new Entry<>( key, value ) );
		return old != null ? old.getValue() : null;
	}

	public V remove(int instanceId) {
		final Entry<K, V> old = backingArray.remove( instanceId );
		return old != null ? old.getValue() : null;
	}

	/**
	 * Removes the mapping for a key from this map if it is present (optional operation). Returns the value to which
	 * this map previously associated the key, or null if the map contained no mapping for the key.
	 *
	 * @implNote This only works for {@link InstanceIdentity} keys, and it's inefficient
	 * since we need to do a type check. Prefer using {@link #remove(int)}.
	 */
	@Override
	public V remove(Object key) {
		if ( key instanceof InstanceIdentity instance ) {
			return remove( instance.$$_hibernate_getInstanceId() );
		}
		throw new IllegalArgumentException( "Provided key does not support instance identity" );
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for ( Map.Entry<? extends K, ? extends V> entry : m.entrySet() ) {
			put( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public V putIfAbsent(K key, V value) {
		V v = get( key.$$_hibernate_getInstanceId() );
		if ( v == null ) {
			v = put( key, value );
		}
		return v;
	}

	@Override
	public void clear() {
		backingArray.clear();
	}

	/**
	 * Returns a read-only Set view of the keys contained in this map.
	 */
	@Override
	public Set<K> keySet() {
		return backingArray.stream().map( Entry::getKey ).collect( Collectors.toUnmodifiableSet() );
	}

	/**
	 * Returns a read-only Collection view of the values contained in this map.
	 */
	@Override
	public Collection<V> values() {
		return backingArray.stream().map( Entry::getValue ).collect( Collectors.toUnmodifiableSet() );
	}

	/**
	 * Returns a read-only Set view of the mappings contained in this map.
	 */
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return backingArray.stream().collect( Collectors.toUnmodifiableSet() );
	}

	public Map.Entry<K, V>[] toArray() {
		return backingArray.toArray();
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		backingArray.forEach( element -> action.accept( element.getKey(), element.getValue() ) );
	}
}
