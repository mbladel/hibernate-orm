/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.BitSet;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.entity.EntityInitializer;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityDelayedFetchImpl extends AbstractNonJoinedEntityFetch {
	final BasicFetch<?> discriminatorFetch;

	public EntityDelayedFetchImpl(
			FetchParent fetchParent,
			ToOneAttributeMapping fetchedAttribute,
			NavigablePath navigablePath,
			DomainResult<?> keyResult,
			boolean selectByUniqueKey,
			DomainResultCreationState creationState) {
		super( navigablePath, fetchedAttribute, fetchParent, keyResult, selectByUniqueKey );
		// todo marco : add check for @LazyOptions == CONCRETE_TYPE
		discriminatorFetch = creationState.visitDiscriminatorFetch( this );
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.DELAYED;
	}

	@Override
	public EntityInitializer createInitializer(FetchParentAccess parentAccess, AssemblerCreationState creationState) {
		return new EntityDelayedFetchInitializer(
				parentAccess,
				getNavigablePath(),
				getEntityValuedModelPart(),
				isSelectByUniqueKey(),
				getKeyResult().createResultAssembler( parentAccess, creationState ),
				discriminatorFetch != null
						? (BasicResultAssembler<?>) discriminatorFetch.createResultAssembler( parentAccess, creationState )
						: null
		);
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		if ( discriminatorFetch != null ) {
			discriminatorFetch.collectValueIndexesToCache( valueIndexes );
		}
		super.collectValueIndexesToCache( valueIndexes );
	}
}
