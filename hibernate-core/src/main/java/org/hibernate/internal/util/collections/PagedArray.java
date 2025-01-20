/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Utility implementation of an array-like structure organized in sub-arrays (pages)
 * that can grow efficiently by a constant factor of {@link #PAGE_CAPACITY}.
 * Reading from and writing to the array is a simple O(2) operation.
 *
 * @param <E> the type of elements stored in this array
 * @implNote To optimize the memory footprint of the array, removed indexes should be reused
 * to have the least number of pages allocated as possible.
 */
public class PagedArray<E> {
	// It's important that capacity is a power of 2 to allow calculating page index and offset within the page
	// with simple division and modulo operations; also static final so JIT can inline these operations.
	private static final int PAGE_CAPACITY = 1 << 5; // 32

	/**
	 * Represents a page of {@link #PAGE_CAPACITY} objects in the overall array.
	 *
	 * @param <E>
	 */
	private static final class Page<E> {
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
		public E set(int offset, E element) {
			if ( offset >= PAGE_CAPACITY ) {
				throw new IllegalArgumentException( "The required offset is beyond page capacity" );
			}
			//noinspection unchecked
			final E old = (E) elements[offset];
			if ( element != null ) {
				if ( offset > lastNotEmptyOffset ) {
					lastNotEmptyOffset = offset;
				}
			}
			else if ( old != null && lastNotEmptyOffset == offset ) {
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
		public E get(final int offset) {
			if ( offset >= PAGE_CAPACITY ) {
				throw new IllegalArgumentException( "The required offset is beyond page capacity" );
			}
			if ( offset > lastNotEmptyOffset ) {
				return null;
			}
			//noinspection unchecked
			return (E) elements[offset];
		}
	}

	private final ArrayList<Page<E>> elementPages;
	private int size;

	private static int toPageIndex(final int index) {
		return index / PAGE_CAPACITY;
	}

	private static int toPageOffset(final int index) {
		return index % PAGE_CAPACITY;
	}

	public PagedArray() {
		elementPages = new ArrayList<>();
	}

	/**
	 * Returns the size of this array, intended as the number of elements contained in it.
	 *
	 * @return the number of elements in the array
	 */
	public int size() {
		return size;
	}

	/**
	 * Returns {@code true} if this array contains no elements.
	 *
	 * @return {@code true} if this array contains no elements
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	public boolean containsKey(int index) {
		return get( index ) != null;
	}

	/**
	 * Get the element at the specified index.
	 *
	 * @param index the index to retrieve the element from
	 * @return the element at {@code index} if one existed, or {@code null}
	 */
	public E get(int index) {
		final int pageIndex = toPageIndex( index );
		if ( pageIndex < elementPages.size() ) {
			final Page<E> page = elementPages.get( pageIndex );
			if ( page != null ) {
				return page.get( toPageOffset( index ) );
			}
		}
		return null;
	}

	/**
	 * Utility methods that retrieves or initializes a {@link Page} based on the absolute index in the array.
	 *
	 * @param index the absolute index of the array
	 * @return the page corresponding to the provided index
	 */
	private Page<E> getOrCreateEntryPage(int index) {
		final int pages = elementPages.size();
		final int pageIndex = toPageIndex( index );
		if ( pageIndex < pages ) {
			final Page<E> page = elementPages.get( pageIndex );
			if ( page != null ) {
				return page;
			}
			final Page<E> newPage = new Page<>();
			elementPages.set( pageIndex, newPage );
			return newPage;
		}
		elementPages.ensureCapacity( pageIndex + 1 );
		for ( int i = pages; i < pageIndex; i++ ) {
			elementPages.add( null );
		}
		final Page<E> page = new Page<>();
		elementPages.add( page );
		return page;
	}

	/**
	 * Set the provided element at the specified index.
	 *
	 * @param index the index of the array
	 * @param element the element to set
	 * @return the previous element at {@code index} if one existed, or {@code null}
	 */
	public @Nullable E set(int index, E element) {
		final Page<E> page = getOrCreateEntryPage( index );
		final int pageOffset = toPageOffset( index );
		final E old = page.set( pageOffset, element );
		if ( element != null ) {
			if ( old == null ) {
				size++;
			}
		}
		else if ( old != null ) {
			// this is effectively a remove
			size--;
			if ( page.lastNotEmptyOffset == -1 ) {
				// no need to keep the page around anymore
				elementPages.set( toPageIndex( index ), null );
			}
		}
		return old;
	}

	/**
	 * Remove the element at the specified index.
	 *
	 * @param index the index where to remove the element
	 * @return the previous element at {@code index} if one existed, or {@code null}
	 */
	public E remove(int index) {
		return set( index, null );
	}

	/**
	 * Clear the contents of the array.
	 */
	public void clear() {
		for ( Page<E> entryPage : elementPages ) {
			entryPage.clear();
		}
		elementPages.clear();
		elementPages.trimToSize();
		size = 0;
	}

	public @NonNull Stream<E> stream() {
		//noinspection unchecked
		return elementPages.stream().filter( Objects::nonNull )
				.flatMap( p -> Arrays.stream( p.elements, 0, p.lastNotEmptyOffset + 1 ) ).filter( Objects::nonNull )
				.map( e -> (E) e );
	}

	public void forEach(Consumer<? super E> action) {
		for ( final Page<E> page : elementPages ) {
			if ( page != null ) {
				for ( int j = 0; j <= page.lastNotEmptyOffset; j++ ) {
					//noinspection unchecked
					final E element = (E) page.elements[j];
					if ( element != null ) {
						action.accept( element );
					}
				}
			}
		}
	}
}
