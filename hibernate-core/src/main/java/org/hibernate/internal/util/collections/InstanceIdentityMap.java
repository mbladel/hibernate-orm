/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.InstanceIdentity;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * {@link Map} implementation of based on {@link InstanceIdentity}, similar to {@link InstanceIdentityStore}.
 * This collection also stores values using a growing array of {@link #PAGE_CAPACITY} but,
 * contrary to the store, it initializes {@link Map.Entry}s eagerly to optimize iteration
 * performance and avoid type-pollution issues when checking the type of contained objects.
 * <p>
 * Methods accessing / modifying the map with {@link Object} typed parameters will need
 * to type check against the instance identity interface which might be inefficient,
 * so it's recommended to use the position (int) based variant of those methods.
 */
public class InstanceIdentityMap<K extends InstanceIdentity, V> extends AbstractPagedArray<Map.Entry<K, V>>
		implements Map<K, V> {
	private int size;

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Returns {@code true} if this store contains a mapping for the specified instance id.
	 *
	 * @param instanceId the instance id whose associated value is to be returned
	 * @param key key instance to double-check instance equality
	 * @return {@code true} if this store contains a mapping for the specified instance id
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
	 * Tests if the specified key-value mapping is in the map.
	 *
	 * @param key possible key, must be an instance of {@link InstanceIdentity}
	 * @param value possible value
	 * @return {@code true} if and only if the specified key-value mapping is in the map
	 */
	private boolean containsMapping(Object key, Object value) {
		if ( key instanceof InstanceIdentity instance ) {
			return get( instance.$$_hibernate_getInstanceId(), instance ) == value;
		}
		throw new ClassCastException( "Provided key does not support instance identity" );
	}

	/**
	 * Returns the value to which the specified instance id is mapped, or {@code null} if this store
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
		final Page<Map.Entry<K, V>> page = getPage( instanceId );
		if ( page != null ) {
			final int offset = toPageOffset( instanceId );
			final Map.Entry<K, V> entry = page.get( offset );
			if ( entry != null && entry.getKey() == key ) {
				return entry.getValue();
			}
		}
		return null;
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
		if ( key == null ) {
			throw new NullPointerException( "This store does not support null keys" );
		}

		final int instanceId = key.$$_hibernate_getInstanceId();
		final Page<Map.Entry<K, V>> page = getOrCreateEntryPage( instanceId );
		final int pageOffset = toPageOffset( instanceId );
		final Map.Entry<K, V> old = page.set( pageOffset, new AbstractMap.SimpleImmutableEntry<>( key, value ) );
		if ( old == null ) {
			size++;
			return null;
		}
		else {
			return old.getValue();
		}
	}

	/**
	 * Removes the mapping for an instance id from this store if it is present (optional operation).
	 *
	 * @param instanceId the instance id whose associated value is to be returned
	 * @param key key instance to double-check instance equality
	 * @return the previous value associated with {@code instanceId}, or {@code null} if there was no mapping for it.
	 * @implNote This method accesses the backing array with the provided instance id, but performs an instance
	 * equality check ({@code ==}) with the provided key to ensure it corresponds to the mapped one
	 */
	public @Nullable V remove(int instanceId, Object key) {
		final Page<Map.Entry<K, V>> page = getPage( instanceId );
		if ( page != null ) {
			final int pageOffset = toPageOffset( instanceId );
			final Map.Entry<K, V> entry = page.set( pageOffset, null );
			// Check that the provided instance really matches with the key contained in the store
			if ( entry != null ) {
				if ( entry.getKey() == key ) {
					size--;
					return entry.getValue();
				}
				else {
					// If it doesn't, reset the array value to the old key
					page.set( pageOffset, entry );
				}
			}
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
		for ( Entry<? extends K, ? extends V> entry : m.entrySet() ) {
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
		super.clear();
		size = 0;
	}

	@Override
	public Set<K> keySet() {
		// todo marco : these absolutely do not work, need to implement custom iterators / sets here
		return stream().map( Entry::getKey ).collect( Collectors.toUnmodifiableSet() );
	}

	@Override
	public Collection<V> values() {
		return stream().map( Entry::getValue ).collect( Collectors.toUnmodifiableSet() );
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return stream().collect( Collectors.toUnmodifiableSet() );
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		super.forEach( element -> action.accept( element.getKey(), element.getValue() ) );
	}

	public Map.Entry<K, V>[] toArray() {
		//noinspection unchecked
		return stream().toArray( Map.Entry[]::new );
	}
}
