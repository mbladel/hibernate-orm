/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

/**
 * A {@link ReadOnlyMap} which relies on {@link ClassValue}s to cache entries, useful to store
 * information relative to {@link Class class types} like what interfaces they implement
 * to avoid type pollution.
 *
 * @implNote it's crucial that the cache be {@link #dispose() disposed} of when no longer needed,
 * as failing to do so would cause ClassLoader leaks.
 *
 * @author Marco Belladelli
 */
public class CachingClassValue<V> extends ClassValue<V> implements ReadOnlyMap<Class<?>, V> {
	private final Function<Class<?>, V> provider;
	private volatile CopyOnWriteArraySet<Class<?>> typeCache = new CopyOnWriteArraySet<>();

	public CachingClassValue(Function<Class<?>, V> provider) {
		this.provider = provider;
	}

	@Override
	protected V computeValue(Class<?> type) {
		final CopyOnWriteArraySet<Class<?>> t = typeCache;
		if ( t == null ) {
			throw new IllegalStateException( "This CachingClassValue has been disposed" );
		}
		else {
			t.add( type );
			return provider.apply( type );
		}
	}

	@Override
	public void dispose() {
		for ( Class<?> clazz : typeCache ) {
			remove( clazz );
		}
		typeCache = null;
	}
}
