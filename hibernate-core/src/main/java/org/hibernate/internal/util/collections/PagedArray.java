/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Utility implementation of an array-like structure that can grow efficiently by a constant factor
 * of {@link #PAGE_CAPACITY}.
 *
 * @param <E> the types of elements stored in this array
 */
public class PagedArray<E> {
	// important that capacity is static final so JIT can inline converting indexes to page / offset
	private static final int PAGE_CAPACITY = 1 << 5; // 32
	private static final int PAGE_SHIFT = Integer.numberOfTrailingZeros( PAGE_CAPACITY );
	private static final int PAGE_MASK = PAGE_CAPACITY - 1;

	/**
	 * Represents a page of {@link #PAGE_CAPACITY} in the overall array
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

		public void clear() {
			// We need to null out everything to prevent GC nepotism (see https://github.com/jbossas/jboss-threads/pull/74)
			Arrays.fill( elements, 0, lastNotEmptyOffset + 1, null );
			lastNotEmptyOffset = -1;
		}

		public E set(int offset, E element) {
			if ( offset >= PAGE_CAPACITY ) {
				throw new IllegalArgumentException( "The required offset is beyond page capacity" );
			}
			//noinspection unchecked
			final E old = (E) elements[offset];
			if ( element != null ) {
				if ( old == null ) {
					if ( offset > lastNotEmptyOffset ) {
						lastNotEmptyOffset = offset;
					}
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

		public E get(final int pageOffset) {
			if ( pageOffset >= PAGE_CAPACITY ) {
				throw new IllegalArgumentException( "The required pageOffset is beyond page capacity" );
			}
			if ( pageOffset > lastNotEmptyOffset ) {
				return null;
			}
			//noinspection unchecked
			return (E) elements[pageOffset];
		}
	}

	private final ArrayList<Page<E>> elementPages;
	private int size;

	private static int toPageIndex(final int cacheIndex) {
		return cacheIndex >> PAGE_SHIFT;
	}

	private static int toPageOffset(final int cacheIndex) {
		return cacheIndex & PAGE_MASK;
	}

	public PagedArray() {
		elementPages = new ArrayList<>();
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public boolean containsKey(int index) {
		return get( index ) != null;
	}

	public E get(int instanceId) {
		final int pageIndex = toPageIndex( instanceId );
		if ( pageIndex < elementPages.size() ) {
			final Page<E> page = elementPages.get( pageIndex );
			if ( page != null ) {
				return page.get( toPageOffset( instanceId ) );
			}
		}
		return null;
	}

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

	public E set(int index, E element) {
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

	public E remove(int index) {
		return set( index, null );
	}

	public void clear() {
		for ( Page<E> entryPage : elementPages ) {
			entryPage.clear();
		}
		elementPages.clear();
		elementPages.trimToSize(); // todo marco : should we do this ?
		size = 0;
	}

	public Stream<E> stream() {
		//noinspection unchecked
		return elementPages.stream().filter( Objects::nonNull )
				.flatMap( p -> Arrays.stream( p.elements, 0, p.lastNotEmptyOffset + 1 ) ).filter( Objects::nonNull )
				.map( e -> (E) e );
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	public void forEach(Consumer<? super E> action) {
		// We want a regular for here to avoid concurrency problems with list iterators
		for ( int i = 0; i < elementPages.size(); i++ ) {
			final Page<E> page = elementPages.get( i );
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
