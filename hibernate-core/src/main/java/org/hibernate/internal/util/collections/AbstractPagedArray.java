package org.hibernate.internal.util.collections;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class AbstractPagedArray<E> {
	// It's important that capacity is a power of 2 to allow calculating page index and offset within the page
	// with simple division and modulo operations; also static final so JIT can inline these operations.
	private static final int PAGE_CAPACITY = 1 << 5; // 32, 16 key + value pairs

	/**
	 * Represents a page of {@link #PAGE_CAPACITY} objects in the overall array.
	 */
	protected static final class Page<E> {
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
		public E set(int offset, Object element) {
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
			//noinspection unchecked
			return (E) old;
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

		int lastNonEmptyOffset() {
			return lastNotEmptyOffset;
		}
	}

	private final ArrayList<Page<E>> elementPages;

	public AbstractPagedArray() {
		elementPages = new ArrayList<>();
	}

	protected static int toPageIndex(final int index) {
		return index / PAGE_CAPACITY;
	}

	protected static int toPageOffset(final int index) {
		return index % PAGE_CAPACITY;
	}

	/**
	 * Utility methods that retrieves an {@link Page} based on the absolute index in the array.
	 *
	 * @param index the absolute index of the array
	 * @return the page corresponding to the provided index, or {@code null}
	 */
	protected Page<E> getPage(int index) {
		final int pageIndex = toPageIndex( index );
		if ( pageIndex < elementPages.size() ) {
			return elementPages.get( pageIndex );
		}
		return null;
	}

	/**
	 * Access the underlying array with an absolute index, only meant to be used by internal iterator implementations
	 *
	 * @param index the absolute index in the underlying array
	 * @return the value contained in the array at the specified position or {@code null}
	 */
	protected E get(int index) {
		final Page<E> page = getPage( index );
		return page != null ? page.get( toPageOffset( index ) ) : null;
	}

	/**
	 * Utility methods that retrieves or initializes a {@link Page} based on the absolute index in the array.
	 *
	 * @param index the absolute index of the array
	 * @return the page corresponding to the provided index
	 */
	protected Page<E> getOrCreateEntryPage(int index) {
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

	public void clear() {
		for ( Page<E> entryPage : elementPages ) {
			entryPage.clear();
		}
		elementPages.clear();
		elementPages.trimToSize();
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
