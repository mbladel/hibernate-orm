/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import java.util.List;

import org.hibernate.engine.FetchTiming;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.VirtualTableGroup;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;

/**
 * @author Steve Ebersole
 */
public class EmbeddableFetchImpl extends AbstractFetchParent implements EmbeddableResultGraphNode, Fetch {

	private final FetchParent fetchParent;
	private final FetchTiming fetchTiming;
	private final TableGroup tableGroup;
	private final boolean hasTableGroup;

	public EmbeddableFetchImpl(
			NavigablePath navigablePath,
			EmbeddableValuedFetchable embeddedPartDescriptor,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean hasTableGroup,
			DomainResultCreationState creationState) {
		super( embeddedPartDescriptor.getEmbeddableTypeDescriptor(), navigablePath );

		this.fetchParent = fetchParent;
		this.fetchTiming = fetchTiming;
		this.hasTableGroup = hasTableGroup;

		this.tableGroup = creationState.getSqlAstCreationState().getFromClauseAccess().resolveTableGroup(
				getNavigablePath(),
				np -> {
					final TableGroup lhsTableGroup = creationState.getSqlAstCreationState()
							.getFromClauseAccess()
							.findTableGroup( fetchParent.getNavigablePath() );
					final TableGroupJoin tableGroupJoin = getReferencedMappingContainer().createTableGroupJoin(
							getNavigablePath(),
							lhsTableGroup,
							null,
							null,
							SqlAstJoinType.INNER,
							true,
							false,
							creationState.getSqlAstCreationState()
					);
					lhsTableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);

		afterInitialize( this, creationState );
	}

	/**
	 * For Hibernate Reactive
	 */
	protected EmbeddableFetchImpl(EmbeddableFetchImpl original) {
		super( original.getFetchContainer(), original.getNavigablePath() );
		fetchParent = original.fetchParent;
		fetchTiming = original.fetchTiming;
		tableGroup = original.tableGroup;
		hasTableGroup = original.hasTableGroup;
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}

	@Override
	public boolean hasTableGroup() {
		return hasTableGroup;
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public EmbeddableMappingType getFetchContainer() {
		return (EmbeddableMappingType) super.getFetchContainer();
	}

	@Override
	public EmbeddableValuedModelPart getReferencedMappingContainer() {
		return getFetchContainer().getEmbeddedValueMapping();
	}

	@Override
	public Fetchable getFetchedMapping() {
		return getReferencedMappingContainer();
	}


	@Override
	public NavigablePath resolveNavigablePath(Fetchable fetchable) {
		if ( fetchable instanceof TableGroupProducer ) {
			NavigablePath navigablePath = getNavigablePathFromJoins( tableGroup.getTableGroupJoins(), fetchable );
			if ( navigablePath == null && tableGroup instanceof VirtualTableGroup ) {
				navigablePath = getNavigablePathFromUnderlying( (VirtualTableGroup) tableGroup, fetchable );
			}
			if ( navigablePath != null ) {
				return navigablePath;
			}
		}
		return super.resolveNavigablePath( fetchable );
	}

	private NavigablePath getNavigablePathFromUnderlying(VirtualTableGroup tableGroup, Fetchable fetchable) {
		for ( TableGroupJoin tableGroupJoin : tableGroup.getUnderlyingTableGroup().getTableGroupJoins() ) {
			final NavigablePath navigablePath = tableGroupJoin.getNavigablePath();
			final TableGroup joinedGroup = tableGroupJoin.getJoinedGroup();
			if ( navigablePath.getLocalName().equals( tableGroup.getNavigablePath().getLocalName() )
				 && joinedGroup.getModelPart() == tableGroup.getModelPart() ) {
				// This table group is for the same embeddable but not treated,
				// check if its joins contain the fetchable too
				return getNavigablePathFromJoins( joinedGroup.getTableGroupJoins(), fetchable );
			}
		}
		return null;
	}

	private NavigablePath getNavigablePathFromJoins(List<TableGroupJoin> joins, Fetchable fetchable) {
		for ( TableGroupJoin tableGroupJoin : joins ) {
			final NavigablePath navigablePath = tableGroupJoin.getNavigablePath();
			final TableGroup joinedGroup = tableGroupJoin.getJoinedGroup();
			if ( joinedGroup.isFetched()
					&& fetchable.getFetchableName().equals( navigablePath.getLocalName() )
					&& joinedGroup.getModelPart() == fetchable ) {
				return navigablePath;
			}
		}
		return null;
	}

	@Override
	public EmbeddableMappingType getReferencedMappingType() {
		return getFetchContainer();
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final EmbeddableInitializer initializer = creationState.resolveInitializer(
				getNavigablePath(),
				getReferencedModePart(),
				() -> buildEmbeddableFetchInitializer( parentAccess, this, creationState )
		).asEmbeddableInitializer();

		assert initializer != null;

		return new EmbeddableAssembler( initializer );
	}

	protected Initializer buildEmbeddableFetchInitializer(
			FetchParentAccess parentAccess,
				EmbeddableResultGraphNode embeddableFetch,
			AssemblerCreationState creationState) {
		return new EmbeddableFetchInitializer( parentAccess, this, creationState );
	}

	@Override
	public boolean appliesTo(GraphImplementor graphImplementor) {
		return getFetchParent().appliesTo( graphImplementor );
	}

}
