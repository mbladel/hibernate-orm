/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.BitSet;

import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;

/**
 * @author Marco Belladelli
 */
public class EntityDelayedFetchJoinedDiscriminatorImpl extends AbstractFetchParent implements EntityFetch,
		InitializerProducer<EntityDelayedFetchJoinedDiscriminatorImpl> {
	private final FetchParent fetchParent;
	private final ToOneAttributeMapping fetchContainer;
	final BasicFetch<?> discriminatorFetch;
	private final DomainResult<?> keyResult;
	private final boolean selectByUniqueKey;

	public EntityDelayedFetchJoinedDiscriminatorImpl(
			FetchParent fetchParent,
			ToOneAttributeMapping toOneMapping,
			DomainResult<?> keyResult,
			NavigablePath navigablePath,
			boolean selectByUniqueKey,
			DomainResultCreationState creationState) {
		super( navigablePath );
		this.fetchContainer = toOneMapping;
		this.fetchParent = fetchParent;
		this.keyResult = keyResult;
		this.selectByUniqueKey = selectByUniqueKey;
		discriminatorFetch = creationState.visitDiscriminatorFetch( this );
	}

	@Override
	public void afterInitialize(FetchParent fetchParent, DomainResultCreationState creationState) {
		super.afterInitialize( fetchParent, creationState ); // todo marco : should we do this ?
	}

	@Override
	public ToOneAttributeMapping getEntityValuedModelPart() {
		return fetchContainer;
	}

	@Override
	public FetchableContainer getFetchContainer() {
		return fetchContainer;
	}

	@Override
	public ToOneAttributeMapping getReferencedModePart() {
		return getEntityValuedModelPart();
	}

	@Override
	public EntityValuedFetchable getReferencedMappingType() {
		return getEntityValuedModelPart();
	}

	@Override
	public EntityMappingType getReferencedMappingContainer() {
		return getEntityValuedModelPart().getEntityMappingType();
	}

	@Override
	public EntityValuedFetchable getFetchedMapping() {
		return getEntityValuedModelPart();
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public DomainResultAssembler<?> createAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		return buildEntityAssembler(
				creationState.resolveInitializer( this, parentAccess, this ).asEntityInitializer()
		);
	}

	protected EntityAssembler buildEntityAssembler(EntityInitializer entityInitializer) {
		return new EntityAssembler( getFetchedMapping().getJavaType(), entityInitializer );
	}

	@Override
	public Initializer createInitializer(
			EntityDelayedFetchJoinedDiscriminatorImpl resultGraphNode,
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		return resultGraphNode.createInitializer( parentAccess, creationState );
	}

	@Override
	public EntityInitializer createInitializer(FetchParentAccess parentAccess, AssemblerCreationState creationState) {
		return new EntityDelayedFetchInitializer(
				parentAccess,
				getNavigablePath(),
				getReferencedModePart(),
				selectByUniqueKey,
				keyResult.createResultAssembler( parentAccess, creationState ),
				discriminatorFetch != null
						? (BasicResultAssembler<?>) discriminatorFetch.createAssembler( parentAccess, creationState )
						: null
		);
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public boolean hasTableGroup() {
		return true;
	}

	@Override
	public boolean containsCollectionFetches() {
		return false;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		final EntityPersister entityPersister = fetchContainer.getEntityMappingType().getEntityPersister();
		keyResult.collectValueIndexesToCache( valueIndexes );
		if ( !entityPersister.useShallowQueryCacheLayout() ) {
			if ( discriminatorFetch != null ) {
				discriminatorFetch.collectValueIndexesToCache( valueIndexes );
			}
			super.collectValueIndexesToCache( valueIndexes );
		}
		else if ( entityPersister.storeDiscriminatorInShallowQueryCacheLayout() && discriminatorFetch != null ) {
			discriminatorFetch.collectValueIndexesToCache( valueIndexes );
		}
	}
}
