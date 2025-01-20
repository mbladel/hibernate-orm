/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.InstanceIdentity;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Utility collection backed by {@link PagedArray} that takes advantage of {@link InstanceIdentity}'s
 * unique identifier to store objects and provide {@link Map}-like functionalities.
 * <p>
 * Methods accessing / modifying the map with {@link Object} typed parameters will need
 * to type check against the instance identity interface which might be inefficient,
 * so it's recommended to use the position (int) based variant of those methods.
 * <p>
 * Iterating through the whole map is most efficient with {@link #forEach}, and since
 * we simply iterate the underlying array, it's also concurrent and reentrant safe.
 */
public class InstanceIdentityMap<K extends InstanceIdentity, V> implements Map<K, V> {
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

		@Override
		public boolean equals(Object o) {
			if ( o == this ) {
				return true;
			}

			return o instanceof Map.Entry<?, ?> e
				&& Objects.equals( key, e.getKey() )
				&& Objects.equals( value, e.getValue() );
		}

		@Override
		public int hashCode() {
			int result = key.hashCode();
			result = 31 * result + Objects.hashCode( value );
			return result;
		}
	}

	private final PagedArray<Entry<K, V>> backingArray;

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


	/**
	 * Returns {@code true} if this map contains a mapping for the specified instance id.
	 *
	 * @param instanceId the instance id whose associated value is to be returned
	 * @param key key instance to double-check instance equality
	 * @return {@code true} if this map contains a mapping for the specified instance id
	 * @implNote This method accesses the backing array with the provided instance id, but performs an instance
	 * equality check ({@code ==}) with the provided key to ensure it corresponds to the mapped one
	 */
	public boolean containsKey(int instanceId, Object key) {
		return get( instanceId, key ) != null;
	}

	/**
	 * @inheritDoc
	 * @implNote This only works for {@link InstanceIdentity} keys, and it's inefficient
	 * since we need to do a type check. Prefer using {@link #containsKey(int, Object)}.
	 */
	@Override
	public boolean containsKey(Object key) {
		if ( key instanceof InstanceIdentity instance ) {
			return containsKey( instance.$$_hibernate_getInstanceId(), instance );
		}
		throw new ClassCastException( "Provided key does not support instance identity" );
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

	/**
	 * Returns the value to which the specified instance id is mapped, or {@code null} if this map
	 * contains no mapping for the instance id.
	 *
	 * @param instanceId the instance id whose associated value is to be returned
	 * @param key key instance to double-check instance equality
	 * @return the value to which the specified instance id is mapped,
	 * or {@code null} if this map contains no mapping for the instance id
	 * @implNote This method accesses the backing array with the provided instance id, but performs an instance
	 * equality check ({@code ==}) with the provided key to ensure it corresponds to the mapped one
	 */
	public @Nullable V get(int instanceId, Object key) {
		if ( instanceId < 0 ) {
			return null;
		}

		final Entry<K, V> entry = backingArray.get( instanceId );
		return entry != null && entry.getKey() == key ? entry.getValue() : null;
	}

	/**
	 * @inheritDoc
	 * @implNote This only works for {@link InstanceIdentity} keys, and it's inefficient
	 * since we need to do a type check. Prefer using {@link #get(int, Object)}.
	 */
	@Override
	public @Nullable V get(Object key) {
		if ( key instanceof InstanceIdentity instance ) {
			return get( instance.$$_hibernate_getInstanceId(), instance );
		}
		throw new ClassCastException( "Provided key does not support instance identity" );
	}

	@Override
	public @Nullable V put(K key, V value) {
		final int instanceId = key.$$_hibernate_getInstanceId();
		final Entry<K, V> old = backingArray.set( instanceId, new Entry<>( key, value ) );
		return old != null ? old.getValue() : null;
	}

	/**
	 * Removes the mapping for an instance id from this map if it is present (optional operation).
	 *
	 * @param instanceId the instance id whose associated value is to be returned
	 * @param key key instance to double-check instance equality
	 * @return the previous value associated with {@code instanceId}, or {@code null} if there was no mapping for it.
	 * @implNote This method accesses the backing array with the provided instance id, but performs an instance
	 * equality check ({@code ==}) with the provided key to ensure it corresponds to the mapped one
	 */
	public @Nullable V remove(int instanceId, Object key) {
		final Entry<K, V> old = backingArray.remove( instanceId );
		if ( old != null ) {
			// Check that the provided instance really matches with the key contained in the map
			if ( old.getKey() != key ) {
				// If it doesn't, reset the array value to the old entry
				backingArray.set( instanceId, old );
			}
			return old.getValue();
		}
		return null;
	}

	/**
	 * @inheritDoc
	 * @implNote This only works for {@link InstanceIdentity} keys, and it's inefficient
	 * since we need to do a type check. Prefer using {@link #remove(int, Object)}.
	 */
	@Override
	public @Nullable V remove(Object key) {
		if ( key instanceof InstanceIdentity instance ) {
			return remove( instance.$$_hibernate_getInstanceId(), instance );
		}
		throw new ClassCastException( "Provided key does not support instance identity" );
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for ( Map.Entry<? extends K, ? extends V> entry : m.entrySet() ) {
			put( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public @Nullable V putIfAbsent(K key, V value) {
		V v = get( key.$$_hibernate_getInstanceId(), key );
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
	public @NonNull Set<K> keySet() {
		return backingArray.stream().map( Entry::getKey ).collect( Collectors.toUnmodifiableSet() );
	}

	/**
	 * Returns a read-only Collection view of the values contained in this map.
	 */
	@Override
	public @NonNull Collection<V> values() {
		return backingArray.stream().map( Entry::getValue ).collect( Collectors.toUnmodifiableSet() );
	}

	/**
	 * Returns a read-only Set view of the mappings contained in this map.
	 */
	@Override
	public @NonNull Set<Map.Entry<K, V>> entrySet() {
		return backingArray.stream().collect( Collectors.toUnmodifiableSet() );
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		backingArray.forEach( element -> action.accept( element.getKey(), element.getValue() ) );
	}

	public Map.Entry<K, V>[] toArray() {
		//noinspection unchecked
		return backingArray.stream().toArray( Map.Entry[]::new );
	}
}
