/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.mutation.internal;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.internal.NoBatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.InsertCoordinatorStandard;

/**
 * @author Marco Belladelli
 */
public class InsertCoordinatorForceBatch extends InsertCoordinatorStandard {
	public InsertCoordinatorForceBatch(InsertCoordinator delegate) {
		super(
				delegate.entityPersister(),
				delegate.factory(),
				delegate.getStaticMutationOperationGroup(),
				new BasicBatchKey( delegate.entityPersister().getEntityName() + "#INSERT", null )
		);
	}
}
