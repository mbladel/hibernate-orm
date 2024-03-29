/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.mutation.internal;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.persister.entity.mutation.UpdateCoordinatorStandard;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * @author Marco Belladelli
 */
public class UpdateCoordinatorForceBatch extends UpdateCoordinatorStandard {
	public UpdateCoordinatorForceBatch(UpdateCoordinatorStandard delegate) {
		super(
				delegate.entityPersister(),
				delegate.factory(),
				resolveStaticMutationOperationGroup( delegate ),
				new BasicBatchKey( delegate.entityPersister().getEntityName() + "#UPDATE", null ),
				delegate.getVersionUpdateGroup(),
				new BasicBatchKey(
						delegate.entityPersister().getEntityName() + "#UPDATE_VERSION",
						null
				)
		);
	}

	private static MutationOperationGroup resolveStaticMutationOperationGroup(UpdateCoordinatorStandard delegate) {
		final GeneratedValuesMutationDelegate updateDelegate = delegate.entityPersister().getUpdateDelegate();
		if ( updateDelegate != null && !updateDelegate.supportsBatching() ) {
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
