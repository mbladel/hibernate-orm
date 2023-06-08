/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.internal.CacheEntityLoaderHelper;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Base support for FetchParentAccess implementations.  Mainly adds support for
 * registering and managing resolution listeners
 */
public abstract class AbstractFetchParentAccess implements FetchParentAccess {
	private List<Consumer<Object>> listeners;

	@Override
	public void registerResolutionListener(Consumer<Object> listener) {
		if ( listeners == null ) {
			listeners = new ArrayList<>();
		}

		listeners.add( listener );
	}

	protected void clearResolutionListeners() {
		if ( listeners != null ) {
			listeners.clear();
		}
	}

	protected void notifyResolutionListeners(Object resolvedInstance) {
		if ( listeners == null ) {
			return;
		}

		for ( Consumer<Object> listener : listeners ) {
			listener.accept( resolvedInstance );
		}

		listeners.clear();
	}

	// todo marco : in entity initializers, we always only look in the persistence context
	//  and never through the cache - I think this might be on purpose and due to expensive cache access for queries ?
	protected static Object existingOrCached(EntityKey keyToLoad, EntityPersister persister, SharedSessionContractImplementor session) {
		final Object existingEntity = session.getPersistenceContextInternal().getEntity( keyToLoad );
		if ( existingEntity != null ) {
			return existingEntity;
		}
		return CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
				session.asEventSource(),
				null,
				LockMode.NONE,
				persister,
				keyToLoad
		);
	}
}
