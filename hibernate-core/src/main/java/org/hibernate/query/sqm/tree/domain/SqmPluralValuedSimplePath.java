/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.AbstractSqmPathSource;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * An SqmPath for plural attribute paths
 *
 * @param <C> The collection type
 *
 * @author Steve Ebersole
 */
public class SqmPluralValuedSimplePath<C> extends AbstractSqmExpression<C> implements SqmPath<C> {
	private final PluralPersistentAttribute<?, C, ?> pluralAttribute;
	private final NavigablePath navigablePath;
	private final SqmPath<?> lhs;

	private String alias;

	public SqmPluralValuedSimplePath(
			NavigablePath navigablePath,
			PluralPersistentAttribute<?, C, ?> pluralAttribute,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, pluralAttribute, lhs, null, nodeBuilder );
	}

	public SqmPluralValuedSimplePath(
			NavigablePath navigablePath,
			PluralPersistentAttribute<?, C, ?> pluralAttribute,
			SqmPath<?> lhs,
			String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( new SqmCollectionPathSource( pluralAttribute ), nodeBuilder );
		this.lhs = lhs;
		this.alias = explicitAlias;
		this.pluralAttribute = pluralAttribute;
		this.navigablePath = navigablePath;
	}

	@Override
	public PluralPersistentAttribute<?, C, ?> getReferencedPathSource() {
		return pluralAttribute;
	}

	@Override
	public String getExplicitAlias() {
		return alias;
	}

	@Override
	public void setExplicitAlias(String explicitAlias) {
		this.alias = explicitAlias;
	}

	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	public SqmPath<?> getLhs() {
		return lhs;
	}

	@Override
	public List<SqmPath<?>> getReusablePaths() {
		return List.of();
	}

	@Override
	public void visitReusablePaths(Consumer<SqmPath<?>> consumer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerReusablePath(SqmPath<?> path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmPath<?> getReusablePath(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S extends C> SqmTreatedPath<C, S> treatAs(Class<S> treatJavaType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S extends C> SqmTreatedPath<C, S> treatAs(EntityDomainType<S> treatTarget) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S extends C> SqmTreatedPath<C, S> treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S extends C> SqmTreatedPath<C, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S extends C> SqmTreatedPath<C, S> treatAs(Class<S> treatJavaType, String alias, boolean fetch) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S extends C> SqmTreatedPath<C, S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetch) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmPath<?> resolvePathPart(String name, boolean isTerminal, SqmCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
		final DomainType<?> lhsType;
		if ( pluralAttribute.isGeneric() && ( lhsType = getLhs().getResolvedModel().getSqmPathType() ) instanceof ManagedDomainType ) {
			final PersistentAttribute<?, ?> concreteAttribute = ( (ManagedDomainType<?>) lhsType ).findConcreteGenericAttribute(
					pluralAttribute.getPathName()
			);
			if ( concreteAttribute != null ) {
				return (SqmPathSource<?>) concreteAttribute;
			}
		}
		return pluralAttribute;
	}

	@Override
	public SqmPluralValuedSimplePath.SqmCollectionPathSource<C> getNodeType() {
		return (SqmCollectionPathSource<C>) super.getNodeType();
	}

	@Override
	public Bindable<C> getModel() {
		return getNodeType();
	}

	@Override
	public <Y> SqmPath<Y> get(SingularAttribute<? super C, Y> attribute) {
		return null;
	}

	@Override
	public <E, C1 extends Collection<E>> SqmExpression<C1> get(PluralAttribute<? super C, C1, E> collection) {
		return null;
	}

	@Override
	public <K, V, M extends Map<K, V>> SqmExpression<M> get(MapAttribute<? super C, K, V> map) {
		return null;
	}

	@Override
	public SqmExpression<Class<? extends C>> type() {
		return null;
	}

	@Override
	public <Y> SqmPath<Y> get(String attributeName) {
		return null;
	}

	@Override
	public SqmPluralValuedSimplePath<C> copy(SqmCopyContext context) {
		final SqmPluralValuedSimplePath<C> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmPath<?> lhsCopy = lhs.copy( context );
		final SqmPluralValuedSimplePath<C> path = context.registerCopy(
				this,
				new SqmPluralValuedSimplePath<>(
						// todo marco : extract getNavigablePathCopy from AbstractSqmPath
						navigablePath,
						pluralAttribute,
						lhsCopy,
						alias,
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPluralValuedPath( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql) {
		// todo marco
	}

	private static class SqmCollectionPathSource<C> extends AbstractSqmPathSource<C> {
		private final PluralPersistentAttribute<?, C, ?> pluralAttribute;

		public SqmCollectionPathSource(PluralPersistentAttribute<?, C, ?> pluralAttribute) {
			// todo marco : resolve the domain type in the plural attribute and cache it by java type
			super( pluralAttribute.getPathName(), null, new DomainType<>() {
				@Override
				public JavaType<C> getExpressibleJavaType() {
					return pluralAttribute.getAttributeJavaType();
				}

				@Override
				public Class<C> getBindableJavaType() {
					return pluralAttribute.getJavaType();
				}
			}, BindableType.PLURAL_ATTRIBUTE );
			this.pluralAttribute = pluralAttribute;
		}

		@Override
		public JavaType<C> getExpressibleJavaType() {
			return pluralAttribute.getAttributeJavaType();
		}

		@Override
		public BindableType getBindableType() {
			return BindableType.PLURAL_ATTRIBUTE;
		}

		@Override
		public SqmPathSource<?> findSubPathSource(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public SqmPath<C> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
			throw new UnsupportedOperationException();
		}
	}
}
