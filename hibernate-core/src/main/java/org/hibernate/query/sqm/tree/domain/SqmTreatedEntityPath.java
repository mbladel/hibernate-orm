/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * Specialization of {@link SqmTreatedPath} for entity-typed paths
 *
 * @author Marco Belladelli
 */
public interface SqmTreatedEntityPath<T, S extends T> extends SqmTreatedPath<T, S> {
	@Override
	EntityDomainType<S> getTreatTarget();
}
