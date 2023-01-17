/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.util;

import java.util.NoSuchElementException;

import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
public class StandardStackTest {
	@Test
	public void testSimpleStackAccess() {
		final Stack<Integer> stack = allocateStack( 5 );
		assertEquals( 5, stack.depth() );
		assertEquals( 0, stack.getRoot() );
		assertEquals( 4, stack.getCurrent() );
		assertEquals( 4, stack.pop() );
		assertEquals( 4, stack.depth() );
	}

	@Test
	public void testClear() {
		final Stack<Integer> stack = allocateStack( 42 );
		assertEquals( 42, stack.depth() );
		assertFalse( stack.isEmpty() );
		stack.clear();
		assertEquals( 0, stack.depth() );
		assertTrue( stack.isEmpty() );
	}

	@Test
	public void testPopOnEmptyStackShouldThrow() {
		final Stack<Integer> emptyStack = allocateStack( 0 );
		assertTrue( emptyStack.isEmpty() );
		assertThrows( NoSuchElementException.class, emptyStack::pop );
		final Stack<Integer> clearedStack = allocateStack( 1 );
		clearedStack.pop();
		assertTrue( clearedStack.isEmpty() );
		assertThrows( NoSuchElementException.class, clearedStack::pop );
	}

	@Test
	public void testVisitRootFirst() {
		final Stack<Integer> clearedStack = allocateStack( 5 );
		final int[] i = { 0 };
		clearedStack.visitRootFirst( value -> {
			assertEquals( i[0], value );
			i[0]++;
		} );
	}

	@Test
	public void testFindCurrentFirst() {
		final Stack<Integer> clearedStack = allocateStack( 5 );
		final Integer result = clearedStack.findCurrentFirst( value -> value == 1 ? value : null );
		assertEquals( 1, result );
		final Integer nullResult = clearedStack.findCurrentFirst( value -> value == 42 ? value : null );
		assertNull( nullResult );
	}

	@Test
	public void testFindCurrentFirstWithParameter() {
		final Stack<Integer> clearedStack = allocateStack( 5 );
		final Integer result = clearedStack.findCurrentFirstWithParameter( 1, this::returnIfEquals );
		assertEquals( 1, result );
		final Integer nullResult = clearedStack.findCurrentFirstWithParameter( 42, this::returnIfEquals );
		assertNull( nullResult );
	}

	private Stack<Integer> allocateStack(int size) {
		final Stack<Integer> stack = new StandardStack<>( Integer.class );
		for ( int i = 0; i < size; i++ ) {
			stack.push( i );
		}
		return stack;
	}

	private Integer returnIfEquals(Integer value, Integer param) {
		return value.equals( param ) ? value : null;
	}
}
