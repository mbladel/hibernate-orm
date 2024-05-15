/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.util.List;

import org.hibernate.query.sqm.tree.domain.SqmPath;

import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * Specialization of {@link SqmPath} for types that can be treated.
 *
 * @author Marco Belladelli
 */
public interface SqmTreatablePath<T> extends SqmPath<T> {
	/**
	 * The treats associated with this SqmFrom
	 */
	List<? extends SqmTreatablePath<?>> getSqmTreats();

	default boolean hasTreats() {
		return !isEmpty( getSqmTreats() );
	}
}
