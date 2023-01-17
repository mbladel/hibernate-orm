/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util.collections;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A general-purpose stack impl supporting null values.
 * <p>
 * Most of the implementation was inspired by {@link ArrayDeque} methods
 * and follows the same logic.
 *
 * @param <T> The type of things stored in the stack
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 * @author Marco Belladelli
 */
public final class StandardStack<T> implements Stack<T> {
	private T[] elements;
	private int head = 0;
	private int tail = 0;

	@SuppressWarnings("unchecked")
	public StandardStack(Class<T> type) {
		elements = (T[]) Array.newInstance( type, 8 );
	}

	public StandardStack(Class<T> type, T initial) {
		this( type );
		push( initial );
	}

	@Override
	public void push(T e) {
		head = dec( head );
		elements[head] = e;
		if ( head == tail ) {
			grow();
		}
	}

	@Override
	public T pop() {
		if ( isEmpty() ) {
			throw new NoSuchElementException();
		}
		T e = elements[head];
		elements[head] = null;
		head = inc( head );
		return e;
	}

	@Override
	public T getCurrent() {
		return elements[head];
	}

	@Override
	public T getRoot() {
		return elements[dec( tail )];
	}

	@Override
	public int depth() {
		int length = tail - head;
		if ( length < 0 ) {
			length += elements.length;
		}
		return length;
	}

	@Override
	public boolean isEmpty() {
		return head == tail;
	}

	@Override
	public void clear() {
		circularClear( elements, head, tail );
		head = tail = 0;
	}

	private static void circularClear(Object[] es, int i, int end) {
		for ( int to = ( i <= end ) ? end : es.length; ; i = 0, to = end ) {
			for ( ; i < to; i++ ) {
				es[i] = null;
			}
			if ( to == end ) {
				break;
			}
		}
	}

	@Override
	public void visitRootFirst(Consumer<T> action) {
		for ( int i = dec( tail ), remaining = depth();
				remaining > 0; i = dec( i ), remaining-- ) {
			action.accept( elements[i] );
		}
	}

	@Override
	public <X> X findCurrentFirst(Function<T, X> function) {
		for ( int i = head, remaining = depth();
				remaining > 0; i = inc( i ), remaining-- ) {
			final X result = function.apply( elements[i] );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	@Override
	public <X, Y> X findCurrentFirstWithParameter(Y parameter, BiFunction<T, Y, X> biFunction) {
		for ( int i = head, remaining = depth();
				remaining > 0; i = inc( i ), remaining-- ) {
			final X result = biFunction.apply( elements[i], parameter );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	private int inc(int i) {
		if ( ++i >= elements.length ) {
			i = 0;
		}
		return i;
	}

	private int dec(int i) {
		if ( --i < 0 ) {
			i = elements.length - 1;
		}
		return i;
	}

	private void grow() {
		final int oldCapacity = elements.length;
		int jump = ( oldCapacity < 64 ) ? ( oldCapacity + 2 ) : ( oldCapacity >> 1 );
		int newCapacity = oldCapacity + jump;
		elements = Arrays.copyOf( elements, newCapacity );
		if ( tail < head || ( tail == head && elements[head] != null ) ) {
			int newSpace = newCapacity - oldCapacity;
			System.arraycopy( elements, head, elements, head + newSpace, oldCapacity - head );
			for ( int i = head, to = ( head += newSpace ); i < to; i++ ) {
				elements[i] = null;
			}
		}
	}
}
