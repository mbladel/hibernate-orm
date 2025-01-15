package org.hibernate.internal.util.collections;

import org.hibernate.engine.spi.InstanceIdentity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private static final int INITIAL_CAPACITY = 8;

	private int size;
	private Entry<K, V>[] entries;

	public InstanceIdentityMap() {
		//noinspection unchecked
		entries = new Entry[INITIAL_CAPACITY];
	}

	public InstanceIdentityMap(int initialCapacity) {
		//noinspection unchecked
		entries = new Entry[Math.max( initialCapacity, INITIAL_CAPACITY )];
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	public boolean containsKey(int instanceId) {
		return entries.length > instanceId && entries[instanceId] != null;
	}

	/**
	 * Inefficient, prefer using {@link #containsKey(int)}
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
		for ( final Entry<K, V> entry : entries ) {
			if ( entry.getValue() == value || (value != null && value.equals( entry.getValue() )) ) {
				return true;
			}
		}
		return false;
	}

	public V get(int instanceId) {
		return entries[instanceId].getValue();
	}

	/**
	 * Inefficient, prefer using {@link #get(int)}
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
		if ( instanceId >= entries.length ) {
			grow( instanceId );
		}
		Entry<K, V> old = entries[instanceId];
		entries[instanceId] = new Entry<>( key, value );
		size++;
		return old != null ? old.getValue() : null;
	}

	static class Entry<K, V> implements Map.Entry<K, V> {
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

	public V remove(int instanceId) {
		final V old = entries[instanceId].getValue();
		entries[instanceId] = null;
		size--;
		return old;
	}

	/**
	 * Inefficient, prefer using {@link #remove(int)}
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
		entries = new Entry[INITIAL_CAPACITY];
		size = 0;
	}

	/**
	 * Returns a read-only Set view of the keys contained in this map.
	 */
	@Override
	public Set<K> keySet() {
		return Stream.of( entries ).map( Entry::getKey ).collect( Collectors.toUnmodifiableSet() );
	}

	/**
	 * Returns a read-only Collection view of the values contained in this map.
	 */
	@Override
	public Collection<V> values() {
		return Stream.of( entries ).map( Entry::getValue ).collect( Collectors.toUnmodifiableSet() );
	}

	/**
	 * Returns a read-only Set view of the mappings contained in this map.
	 */
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return Set.of( entries );
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		for ( final Entry<K, V> entry : entries ) {
			if ( entry != null ) {
				action.accept( entry.getKey(), entry.getValue() );
			}
		}
	}

	public Map.Entry<K, V>[] toArray() {
		return entries;
	}

	private void grow(int minimumCapacity) {
		final int oldCapacity = Math.max( entries.length, minimumCapacity );
		final int jump = (oldCapacity < 64) ? (oldCapacity + 2) : (oldCapacity >> 1);
		entries = Arrays.copyOf( entries, oldCapacity + jump );
	}
}
