/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ListPersistentAttribute;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.PluralSqmPathSource;
import org.hibernate.query.NotIndexedCollectionException;
import org.hibernate.query.PathException;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.spi.NavigablePath;

/**
 * An SqmPath for plural attribute paths
 *
 * @param <C> The collection type
 * @param <E> The collection element type
 * @author Steve Ebersole
 */
public class SqmPluralValuedSimplePath<C, E> extends AbstractSqmSimplePath<C> {
	public SqmPluralValuedSimplePath(
			NavigablePath navigablePath,
			PluralSqmPathSource<?, C, E> referencedNavigable,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedNavigable, lhs, null, nodeBuilder );
	}

	public SqmPluralValuedSimplePath(
			NavigablePath navigablePath,
			PluralSqmPathSource<?, C, E> referencedNavigable,
			SqmPath<?> lhs,
			String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, lhs, explicitAlias, nodeBuilder );
	}

	@Override
	public SqmPluralValuedSimplePath<C, E> copy(SqmCopyContext context) {
		final SqmPluralValuedSimplePath<C, E> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmPath<?> lhsCopy = getLhs().copy( context );
		final SqmPluralValuedSimplePath<C, E> path = context.registerCopy(
				this,
				new SqmPluralValuedSimplePath<>(
						getNavigablePathCopy( lhsCopy ),
						getModel(),
						lhsCopy,
						getExplicitAlias(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public PluralSqmPathSource<?, C, E> getReferencedPathSource() {
		return (PluralSqmPathSource<?, C, E>) super.getReferencedPathSource();
	}

	@Override
	public PluralSqmPathSource<?, C, E> getModel() {
		return (PluralSqmPathSource<?, C, E>) super.getModel();
	}

	@Override
	public PluralSqmPathSource<?, C, E> getNodeType() {
		return getReferencedPathSource();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return null; // todo marco
//		return walker.visitPluralValuedPath( this );
	}

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		// this is a reference to a collection outside the from clause
		final CollectionPart.Nature nature = CollectionPart.Nature.fromNameExact( name );
		if ( nature == null ) {
			throw new PathException( "Plural path '" + getNavigablePath()
									 + "' refers to a collection and so element attribute '" + name
									 + "' may not be referenced directly (use element() function)" );
		}
		final SqmPath<?> sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPathRegistry pathRegistry = creationState.getCurrentProcessingState().getPathRegistry();
		final String alias = selector.toHqlString();
		final NavigablePath navigablePath = getNavigablePath().getParent().append(
				getNavigablePath().getLocalName(),
				alias
		).append( CollectionPart.Nature.ELEMENT.getName() );
		final SqmFrom<?, ?> indexedPath = pathRegistry.findFromByPath( navigablePath );
		if ( indexedPath != null ) {
			return indexedPath;
		}
		SqmFrom<?, ?> path = pathRegistry.findFromByPath( navigablePath.getParent() );
		if ( path == null ) {
			final PluralSqmPathSource<?, C, E> referencedPathSource = getReferencedPathSource();
			final SqmFrom<?, Object> parent = pathRegistry.resolveFrom( getLhs() );
			final SqmJoin<Object, ?> join;
			final SqmExpression<?> index;
			if ( referencedPathSource instanceof ListPersistentAttribute<?, ?> ) {
				//noinspection unchecked
				join = new SqmListJoin<>(
						parent,
						(ListPersistentAttribute<Object, ?>) referencedPathSource,
						alias,
						SqmJoinType.INNER,
						false,
						parent.nodeBuilder()
				);
				index = ((SqmListJoin<?, ?>) join).index();
			}
			else if ( referencedPathSource instanceof MapPersistentAttribute<?, ?, ?> ) {
				//noinspection unchecked
				join = new SqmMapJoin<>(
						parent,
						(MapPersistentAttribute<Object, ?, ?>) referencedPathSource,
						alias,
						SqmJoinType.INNER,
						false,
						parent.nodeBuilder()
				);
				index = ((SqmMapJoin<?, ?, ?>) join).key();
			}
			else {
				throw new NotIndexedCollectionException( "Index operator applied to path '" + getNavigablePath()
														 + "' which is not a list or map" );
			}
			join.setJoinPredicate( creationState.getCreationContext().getNodeBuilder().equal( index, selector ) );
			parent.addSqmJoin( join );
			pathRegistry.register( path = join );
		}
		final SqmIndexedCollectionAccessPath<Object> result = new SqmIndexedCollectionAccessPath<>(
				navigablePath,
				path,
				selector
		);
		pathRegistry.register( result );
		return result;
	}

	@Override
	public SqmExpression<Class<? extends C>> type() {
		throw new UnsupportedOperationException( "Cannot access the type of plural valued simple paths" );
	}

	@Override
	public <S extends C> SqmTreatedPath<C, S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new UnsupportedOperationException( "Cannot treat plural valued simple paths" );
	}

	@Override
	public <S extends C> SqmTreatedEntityValuedSimplePath<C, S> treatAs(EntityDomainType<S> treatTarget)
			throws PathException {
		throw new UnsupportedOperationException( "Cannot treat plural valued simple paths" );
	}
}
