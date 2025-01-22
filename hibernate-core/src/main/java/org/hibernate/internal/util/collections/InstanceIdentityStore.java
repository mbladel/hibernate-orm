/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.InstanceIdentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Utility collection that takes advantage of {@link InstanceIdentity}'s identifier to store objects.
 * The store is backed by an array-like structure, which automatically grows by a factor of
 * {@link #PAGE_CAPACITY} as needed to store new elements.
 */
public class InstanceIdentityStore<V> {
	// It's important that capacity is a power of 2 to allow calculating page index and offset within the page
	// with simple division and modulo operations; also static final so JIT can inline these operations.
	private static final int PAGE_CAPACITY = 1 << 5; // 32, 16 key + value pairs

	/**
	 * Represents a page of {@link #PAGE_CAPACITY} objects in the overall array.
	 */
	protected static final class Page {
		private final Object[] elements;
		private int lastNotEmptyOffset;

		public Page() {
			elements = new Object[PAGE_CAPACITY];
			lastNotEmptyOffset = -1;
		}

		/**
		 * Clears the contents of the page.
		 */
		public void clear() {
			// We need to null out everything to prevent GC nepotism (see https://hibernate.atlassian.net/browse/HHH-19047)
			Arrays.fill( elements, 0, lastNotEmptyOffset + 1, null );
			lastNotEmptyOffset = -1;
		}

		/**
		 * Set the provided element at the specified offset.
		 *
		 * @param offset the offset in the page where to set the element
		 * @param element the element to set
		 * @return the previous element at {@code offset} if one existed, or {@code null}
		 */
		public Object set(int offset, Object element) {
			if ( offset >= PAGE_CAPACITY ) {
				throw new IllegalArgumentException( "The required offset is beyond page capacity" );
			}
			final Object old = elements[offset];
			if ( element != null ) {
				if ( offset > lastNotEmptyOffset ) {
					lastNotEmptyOffset = offset;
				}
			}
			else if ( lastNotEmptyOffset == offset && old != null ) {
				// must search backward for the first not empty offset
				int i = offset;
				for ( ; i >= 0; i-- ) {
					if ( elements[i] != null ) {
						break;
					}
				}
				lastNotEmptyOffset = i;
			}
			elements[offset] = element;
			return old;
		}

		/**
		 * Get the element at the specified offset.
		 *
		 * @param offset the offset in the page where to set the element
		 * @return the element at {@code index} if one existed, or {@code null}
		 */
		public Object get(final int offset) {
			if ( offset >= PAGE_CAPACITY ) {
				throw new IllegalArgumentException( "The required offset is beyond page capacity" );
			}
			if ( offset > lastNotEmptyOffset ) {
				return null;
			}
			return elements[offset];
		}

		protected int lastNonEmptyOffset() {
			return lastNotEmptyOffset;
		}
	}

	protected final ArrayList<Page> elementPages;
	private int size;

	protected static int toPageIndex(final int index) {
		return index / PAGE_CAPACITY;
	}

	protected static int toPageOffset(final int index) {
		return index % PAGE_CAPACITY;
	}

	private static int toKeyIndex(int instanceId) {
		return instanceId * 2;
	}

	public InstanceIdentityStore() {
		elementPages = new ArrayList<>();
	}

	public int size() {
		return size;
	}

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
	 * Utility methods that retrieves an {@link Page} based on the absolute index in the array.
	 *
	 * @param index the absolute index of the array
	 * @return the page corresponding to the provided index, or {@code null}
	 */
	private Page getPage(int index) {
		final int pageIndex = toPageIndex( index );
		if ( pageIndex < elementPages.size() ) {
			return elementPages.get( pageIndex );
		}
		return null;
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
		final int keyIndex = toKeyIndex( instanceId );
		final Page page = getPage( keyIndex );
		if ( page != null ) {
			final int offset = toPageOffset( keyIndex );
			final Object k = page.get( offset );
			if ( k == key ) {
				//noinspection unchecked
				return (V) page.get( offset + 1 );
			}
		}

		return null;
	}

	/**
	 * Access the underlying array with an absolute index, only meant to be used by internal iterator implementations
	 *
	 * @param index the absolute index in the underlying array
	 * @return the value contained in the array at the specified position or {@code null}
	 */
	protected <T> T get(int index) {
		final Page page = getPage( index );
		//noinspection unchecked
		return page != null ? (T) page.get( toPageOffset( index ) ) : null;
	}

	/**
	 * Utility methods that retrieves or initializes a {@link Page} based on the absolute index in the array.
	 *
	 * @param index the absolute index of the array
	 * @return the page corresponding to the provided index
	 */
	private Page getOrCreateEntryPage(int index) {
		final int pages = elementPages.size();
		final int pageIndex = toPageIndex( index );
		if ( pageIndex < pages ) {
			final Page page = elementPages.get( pageIndex );
			if ( page != null ) {
				return page;
			}
			final Page newPage = new Page();
			elementPages.set( pageIndex, newPage );
			return newPage;
		}
		elementPages.ensureCapacity( pageIndex + 1 );
		for ( int i = pages; i < pageIndex; i++ ) {
			elementPages.add( null );
		}
		final Page page = new Page();
		elementPages.add( page );
		return page;
	}

	/**
	 * Associates the specified value with the specified key in this store (optional operation). If the store
	 * previously contained a mapping for the key, the old value is replaced by the specified value.
	 *
	 * @param key key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 * @return the previous value associated with {@code key}, or {@code null} if there was none
	 */
	public <K extends InstanceIdentity> @Nullable V add(K key, V value) {
		if ( key == null ) {
			throw new NullPointerException( "This store does not support null keys" );
		}

		final int instanceId = key.$$_hibernate_getInstanceId();
		final int keyIndex = toKeyIndex( instanceId );
		final Page page = getOrCreateEntryPage( keyIndex );
		final int pageOffset = toPageOffset( keyIndex );
		final Object old = page.set( pageOffset, key );
		if ( old == null ) {
			size++;
		}
		//noinspection unchecked
		return (V) page.set( pageOffset + 1, value );
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
		final int keyIndex = toKeyIndex( instanceId );
		final Page page = getPage( keyIndex );
		if ( page != null ) {
			final int pageOffset = toPageOffset( keyIndex );
			Object k = page.set( pageOffset, null );
			// Check that the provided instance really matches with the key contained in the store
			if ( k == key ) {
				size--;
				//noinspection unchecked
				return (V) page.set( pageOffset + 1, null );
			}
			else {
				// If it doesn't, reset the array value to the old key
				page.set( pageOffset, k );
			}
		}
		return null;
	}

	public void clear() {
		for ( Page entryPage : elementPages ) {
			entryPage.clear();
		}
		elementPages.clear();
		elementPages.trimToSize();
		size = 0;
	}

	protected abstract class InstanceIdentityIterator<T> implements Iterator<T> {
		int index = 0; // current absolute index in the array
		boolean indexValid; // To avoid unnecessary next computation

		public boolean hasNext() {
			for ( int i = toPageIndex( index ); i < elementPages.size(); i++ ) {
				final Page page = elementPages.get( i );
				if ( page != null ) {
					for ( int j = toPageOffset( index ); j <= page.lastNotEmptyOffset; j += 2 ) {
						if ( page.get( j ) != null ) {
							index = i * PAGE_CAPACITY + j;
							return indexValid = true;
						}
					}
				}
			}
			index = elementPages.size() * PAGE_CAPACITY;
			return false;
		}

		protected int nextIndex() {
			if ( !indexValid && !hasNext() ) {
				throw new NoSuchElementException();
			}

			indexValid = false;
			final int nextIndex = index;
			index += 2;
			return nextIndex;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
