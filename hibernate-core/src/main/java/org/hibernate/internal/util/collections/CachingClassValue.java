package org.hibernate.internal.util.collections;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

/**
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
