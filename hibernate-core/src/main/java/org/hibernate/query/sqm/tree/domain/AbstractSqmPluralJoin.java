/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.persistence.criteria.PluralJoin;

import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * Base support for joins to plural attributes
 *
 * @param <L> The left-hand side of the join
 * @param <C> The collection type
 * @param <E> The collection's element type
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmPluralJoin<L,C,E>
		extends AbstractSqmAttributeJoin<L,E>
		implements JpaJoin<L,E>, PluralJoin<L,C,E> {

	private final PluralPersistentAttribute<L,C,E> pluralAttribute;

	public AbstractSqmPluralJoin(
			SqmFrom<?, L> lhs,
			PluralPersistentAttribute<L, C, E> joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super(
				lhs,
				joinedNavigable.createNavigablePath( lhs, alias ),
				joinedNavigable.getElementPathSource(),
				alias,
				joinType,
				fetched,
				nodeBuilder
		);
		this.pluralAttribute = joinedNavigable;
	}

	protected AbstractSqmPluralJoin(
			SqmFrom<?, L> lhs,
			NavigablePath navigablePath,
			PluralPersistentAttribute<L, C, E> joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, navigablePath, joinedNavigable.getElementPathSource(), alias, joinType, fetched, nodeBuilder );
		this.pluralAttribute = joinedNavigable;
	}

	@Override
	public PluralPersistentAttribute<L, C, E> getModel() {
		return pluralAttribute;
	}

	@Override
	public PersistentAttribute<? super L, ?> getAttribute() {
		return pluralAttribute;
	}
}
