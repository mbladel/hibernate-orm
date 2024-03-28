/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.mutation.internal;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.InsertCoordinatorStandard;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * @author Marco Belladelli
 */
public class InsertCoordinatorForceBatch extends InsertCoordinatorStandard {
	public InsertCoordinatorForceBatch(InsertCoordinator delegate) {
		super(
				delegate.entityPersister(),
				delegate.factory(),
				resolveStaticMutationOperationGroup( delegate ),
				new BasicBatchKey( delegate.entityPersister().getEntityName() + "#INSERT", null )
		);
	}

	private static MutationOperationGroup resolveStaticMutationOperationGroup(InsertCoordinator delegate) {
		final GeneratedValuesMutationDelegate insertDelegate = delegate.entityPersister().getInsertDelegate();
		if ( insertDelegate != null && !insertDelegate.supportsBatching() ) {
			// todo marco : this is not efficient
			// By returning null, we trigger creation of an ad-hoc mutation operation
			// that ignored the mutation delegate. This is not efficient, but the
			// only way to correctly support batching in this case
			return null;
		}
		else {
			return delegate.getStaticMutationOperationGroup();
		}
	}
}
