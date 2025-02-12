/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralElementValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;

/**
 * @author Steve Ebersole
 */
public class PluralSqmPathSource<X, C, E> extends AbstractSqmPathSource<C> implements SqmJoinable<X, E> {
	private final PluralPersistentAttribute<X, C, E> pluralPersistentAttribute;
	private final boolean isGeneric;

	public PluralSqmPathSource(
			String localPathName,
			PluralPersistentAttribute<X, C, E> pluralPersistentAttribute,
			BindableType jpaBindableType,
			boolean isGeneric) {
		super( localPathName, null, null, jpaBindableType );
		this.pluralPersistentAttribute = pluralPersistentAttribute;
		this.isGeneric = isGeneric;
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return pluralPersistentAttribute.findSubPathSource( name );
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name, boolean includeSubtypes) {
		return pluralPersistentAttribute.findSubPathSource( name, includeSubtypes );
	}

	@Override
	public boolean isGeneric() {
		return isGeneric;
	}

	@Override
	public SqmPath<C> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		return new SqmPluralValuedSimplePath<>(
				PathHelper.append( lhs, this, intermediatePathSource ),
				this,
				lhs,
				null,
				lhs.nodeBuilder()
		);
	}

	@Override
	public SqmJoin<X, E> createSqmJoin(
			SqmFrom<?, X> lhs,
			SqmJoinType joinType,
			String alias,
			boolean fetched,
			SqmCreationState creationState) {
		return pluralPersistentAttribute.createSqmJoin( lhs, joinType, alias, fetched, creationState );
	}

	@Override
	public String getName() {
		return getPathName();
	}
}
