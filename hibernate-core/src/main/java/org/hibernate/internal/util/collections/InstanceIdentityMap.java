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
	private static final int PAGE_CAPACITY = 1 << 5; // 32
	private static final int PAGE_SHIFT = Integer.numberOfTrailingZeros( PAGE_CAPACITY );
	private static final int PAGE_MASK = PAGE_CAPACITY - 1;

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

	private static final class EntryPage<K, V> {
		private final Entry<K, V>[] entries;
		private int lastNotEmptyOffset;

		public EntryPage() {
			entries = new Entry[PAGE_CAPACITY];
			lastNotEmptyOffset = -1;
		}

		public void clear() {
			Arrays.fill( entries, 0, lastNotEmptyOffset + 1, null );
			lastNotEmptyOffset = -1;
		}

		public Entry<K, V> set(int offset, Entry<K, V> entry) {
			if ( offset >= PAGE_CAPACITY ) {
				throw new IllegalArgumentException( "The required offset is beyond page capacity" );
			}
			final Entry<K, V> old = entries[offset];
			if ( entry != null ) {
				if ( old == null ) {
					if ( offset > lastNotEmptyOffset ) {
						lastNotEmptyOffset = offset;
					}
				}
			}
			else if ( lastNotEmptyOffset == offset ) {
				// must search backward for the first not empty slot, to mark it
				int i = offset;
				for ( ; i >= 0; i-- ) {
					if ( entries[i] != null ) {
						break;
					}
				}
				lastNotEmptyOffset = i;
			}
			entries[offset] = entry;
			return old;
		}

		public Entry<K, V> get(final int pageOffset) {
			if ( pageOffset >= PAGE_CAPACITY ) {
				throw new IllegalArgumentException( "The required pageOffset is beyond page capacity" );
			}
			if ( pageOffset > lastNotEmptyOffset ) {
				return null;
			}
			return entries[pageOffset];
		}
	}

	private final ArrayList<EntryPage<K, V>> entryPages;
	private int size;

	private static int toPageIndex(final int cacheIndex) {
		return cacheIndex >> PAGE_SHIFT;
	}

	private static int toPageOffset(final int cacheIndex) {
		return cacheIndex & PAGE_MASK;
	}

	public InstanceIdentityMap() {
		entryPages = new ArrayList<>();
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
		final int pageIndex = toPageIndex( instanceId );
		if ( pageIndex < entryPages.size() ) {
			final EntryPage<K, V> page = entryPages.get( pageIndex );
			if ( page != null ) {
				final Entry<K, V> entry = page.get( toPageOffset( instanceId ) );
				return entry == null ? null : entry.getValue();
			}
		}
		return null;
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

	private EntryPage<K, V> getOrCreateEntryPage(int instanceId) {
		final int pages = entryPages.size();
		final int pageIndex = toPageIndex( instanceId );
		if ( pageIndex < pages ) {
			final EntryPage<K, V> page = entryPages.get( pageIndex );
			if ( page != null ) {
				return page;
			}
			final EntryPage<K, V> newPage = new EntryPage<>();
			entryPages.set( pageIndex, newPage );
			return newPage;
		}
		entryPages.ensureCapacity( pageIndex + 1 );
		for ( int i = pages; i < pageIndex; i++ ) {
			entryPages.add( null );
		}
		final EntryPage<K, V> page = new EntryPage<>();
		entryPages.add( page );
		return page;
	}

	@Override
	public V put(K key, V value) {
		final int instanceId = key.$$_hibernate_getInstanceId();
		final EntryPage<K, V> page = getOrCreateEntryPage( instanceId );
		final int pageOffset = toPageOffset( instanceId );
		final Entry<K, V> old = page.get( pageOffset );
		page.set( pageOffset, new Entry<>( key, value ) );
		size++;
		return old != null ? old.getValue() : null;
	}

	public V remove(int instanceId) {
		V old = null;
		final int pageIndex = toPageIndex( instanceId );
		if ( pageIndex < entryPages.size() ) {
			final EntryPage<K, V> page = entryPages.get( pageIndex );
			final int pageOffset = toPageOffset( instanceId );
			Entry<K, V> entry = page.set( pageOffset, null );
			if ( entry != null ) {
				old = entry.getValue();
				size--;
			}
			if ( page.lastNotEmptyOffset == -1 ) {
				// no need to keep the page around anymore
				entryPages.set( pageIndex, null );
			}
		}
		return old;
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
		// We need to null out everything to prevent GC nepotism (see https://github.com/jbossas/jboss-threads/pull/74)
		for ( EntryPage<K, V> entryPage : entryPages ) {
			entryPage.clear();
		}
		entryPages.clear();
		entryPages.trimToSize(); // todo marco : should we do this ?
		size = 0;
	}

	/**
	 * Returns a read-only Set view of the keys contained in this map.
	 */
	@Override
	public Set<K> keySet() {
		return entryPages.stream().filter( Objects::nonNull )
				.flatMap( p -> Arrays.stream( p.entries, 0, p.lastNotEmptyOffset + 1 ) ).filter( Objects::nonNull )
				.map( Entry::getKey ).collect( Collectors.toUnmodifiableSet() );
	}

	/**
	 * Returns a read-only Collection view of the values contained in this map.
	 */
	@Override
	public Collection<V> values() {
		return entryPages.stream().filter( Objects::nonNull )
				.flatMap( p -> Arrays.stream( p.entries, 0, p.lastNotEmptyOffset + 1 ) ).filter( Objects::nonNull )
				.map( Entry::getValue ).collect( Collectors.toUnmodifiableSet() );
	}

	/**
	 * Returns a read-only Set view of the mappings contained in this map.
	 */
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return entryPages.stream().filter( Objects::nonNull )
				.flatMap( p -> Arrays.stream( p.entries, 0, p.lastNotEmptyOffset + 1 ) ).filter( Objects::nonNull )
				.collect( Collectors.toUnmodifiableSet() );
	}

	public Map.Entry<K, V>[] toArray() {
		final List<Entry<K, V>> list = new ArrayList<>();
		for ( EntryPage<K, V> p : entryPages ) {
			if ( p != null ) {
				for ( int i = 0; i <= p.lastNotEmptyOffset; i++ ) {
					final Entry<K, V> entry = p.entries[i];
					if ( entry != null ) {
						list.add( entry );
					}
				}
			}
		}
		//noinspection unchecked
		return list.toArray( new Map.Entry[0] );
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		for ( EntryPage<K, V> entryPage : entryPages ) {
			if ( entryPage != null ) {
				for ( int i = 0; i <= entryPage.lastNotEmptyOffset; i++ ) {
					final Entry<K, V> entry = entryPage.entries[i];
					if ( entry != null ) {
						action.accept( entry.getKey(), entry.getValue() );
					}
				}
			}
		}
	}
}
