/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.InstanceIdentity;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * {@link Map} implementation of {@link InstanceIdentityStore}.
 * <p>
 * Methods accessing / modifying the map with {@link Object} typed parameters will need
 * to type check against the instance identity interface which might be inefficient,
 * so it's recommended to use the position (int) based variant of those methods.
 */
public class InstanceIdentityMap<K extends InstanceIdentity, V> extends InstanceIdentityStore<V> implements Map<K, V> {
	// Transient fields caching the views on this map the first time they're accessed.
	// The views are stateless, so there's no reason to create more than one of each
	private transient Set<K> keySet;
	private transient Collection<V> values;
	private transient Set<Map.Entry<K, V>> entrySet;

	@Override
	public int size() {
		return super.size();
	}

	@Override
	public boolean isEmpty() {
		return super.isEmpty();
	}

	/**
	 * @inheritDoc
	 * @implNote This only works for {@link InstanceIdentity} keys, and it's inefficient
	 * since we need to do a type check. Prefer using {@link #containsKey(int, Object)}.
	 */
	@Override
	public boolean containsKey(Object key) {
		if ( key instanceof InstanceIdentity instance ) {
			return super.containsKey( instance.$$_hibernate_getInstanceId(), instance );
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
	 * @inheritDoc
	 * @implNote This only works for {@link InstanceIdentity} keys, and it's inefficient
	 * since we need to do a type check. Prefer using {@link #get(int, Object)}.
	 */
	@Override
	public @Nullable V get(Object key) {
		if ( key instanceof InstanceIdentity instance ) {
			return super.get( instance.$$_hibernate_getInstanceId(), instance );
		}
		throw new ClassCastException( "Provided key does not support instance identity" );
	}

	@Override
	public @Nullable V put(K key, V value) {
		return super.add( key, value );
	}

	/**
	 * @inheritDoc
	 * @implNote This only works for {@link InstanceIdentity} keys, and it's inefficient
	 * since we need to do a type check. Prefer using {@link #remove(int, Object)}.
	 */
	@Override
	public @Nullable V remove(Object key) {
		if ( key instanceof InstanceIdentity instance ) {
			return super.remove( instance.$$_hibernate_getInstanceId(), instance );
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
	}

	/**
	 * Returns a read-only {@link Set} view of the keys contained in this map.
	 */
	@Override
	public @NonNull Set<K> keySet() {
		Set<K> ks = keySet;
		if ( ks == null ) {
			ks = new KeySet();
			keySet = ks;
		}
		return ks;
	}

	/**
	 * Returns a read-only {@link Collection} view of the values contained in this map.
	 */
	@Override
	public @NonNull Collection<V> values() {
		Collection<V> vs = values;
		if ( vs == null ) {
			vs = new Values();
			values = vs;
		}
		return vs;
	}

	/**
	 * Returns a read-only {@link Set} view of the mappings contained in this map
	 */
	@Override
	public @NonNull Set<Entry<K, V>> entrySet() {
		Set<Map.Entry<K, V>> es = entrySet;
		if ( es == null ) {
			es = new EntrySet();
			entrySet = es;
		}
		return es;
	}



	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		for ( Page page : elementPages ) {
			if ( page != null ) {
				for ( int j = 0; j <= page.lastNonEmptyOffset(); j += 2 ) {
					Object key;
					if ( (key = page.get( j )) != null ) {
						//noinspection unchecked
						action.accept( (K) key, (V) page.get( j + 1 ) );
					}
				}
			}
		}
	}

	public Map.Entry<K, V>[] toArray() {
		//noinspection unchecked
		return entrySet().toArray( new Map.Entry[0] );
	}

	private class KeyIterator extends InstanceIdentityIterator<K> {
		public K next() {
			return get( nextIndex() );
		}
	}

	private class ValueIterator extends InstanceIdentityIterator<V> {
		public V next() {
			return get( nextIndex() + 1 );
		}
	}

	private class EntryIterator extends InstanceIdentityIterator<Map.Entry<K, V>> {
		public Map.Entry<K, V> next() {
			return new Entry( nextIndex() );
		}

		private class Entry implements Map.Entry<K, V> {
			private final int index;

			private Entry(int index) {
				this.index = index;
			}

			public K getKey() {
				return get( index );
			}

			public V getValue() {
				return get( index + 1 );
			}

			public V setValue(V value) {
				throw new UnsupportedOperationException();
			}

			public boolean equals(Object o) {
				return o instanceof Map.Entry<?, ?> e
					   && Objects.equals( e.getKey(), getKey() )
					   && Objects.equals( e.getValue(), getValue() );
			}

			public int hashCode() {
				return castNonNull( getKey() ).hashCode() ^
					   Objects.hashCode( getValue() );
			}

			public String toString() {
				return getKey() + "=" + getValue();
			}
		}
	}

	private class KeySet extends AbstractSet<K> {
		@Override
		public @NonNull Iterator<K> iterator() {
			return new KeyIterator();
		}

		@Override
		public int size() {
			return InstanceIdentityMap.this.size();
		}

		@Override
		public boolean contains(Object o) {
			return containsKey( o );
		}
	}

	private class Values extends AbstractCollection<V> {
		@Override
		public @NonNull Iterator<V> iterator() {
			return new ValueIterator();
		}

		@Override
		public int size() {
			return InstanceIdentityMap.this.size();
		}
	}

	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		@Override
		public @NonNull Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@Override
		public int size() {
			return InstanceIdentityMap.this.size();
		}

		@Override
		public boolean contains(Object o) {
			return o instanceof Entry<?, ?> entry
				   && containsMapping( entry.getKey(), entry.getValue() );
		}

		@Override
		public @NonNull Object @NonNull [] toArray() {
			return toArray( new Object[0] );
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> @NonNull T @NonNull [] toArray(T[] a) {
			int size = size();
			if ( a.length < size ) {
				a = (T[]) Array.newInstance( a.getClass().getComponentType(), size );
			}
			int ti = 0;
			for ( Page page : elementPages ) {
				if ( page != null ) {
					for ( int j = 0; j <= page.lastNonEmptyOffset(); j += 2 ) {
						Object key;
						if ( (key = page.get( j )) != null ) {
							a[ti++] = (T) new AbstractMap.SimpleImmutableEntry<>( key, page.get( j + 1 ) );
						}
					}
				}
			}
			// fewer elements than expected or concurrent modification from other thread detected
			if ( ti < size ) {
				throw new ConcurrentModificationException();
			}
			// final null marker as per spec
			if ( ti < a.length ) {
				a[ti] = null;
			}
			return a;
		}
	}
}
