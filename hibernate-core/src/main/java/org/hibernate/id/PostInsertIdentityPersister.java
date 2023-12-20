/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;

/**
 * A persister that may have an identity assigned by execution of 
 * a SQL {@code INSERT}.
 *
 * @author Gavin King
 */
public interface PostInsertIdentityPersister extends EntityPersister {
	/**
	 * Get the database-specific SQL command to retrieve the last
	 * generated IDENTITY value.
	 *
	 * @return The SQL command string
	 */
	String getIdentitySelectString();

	String[] getIdentifierColumnNames();

	/**
	 * @deprecated Use {@link EntityPersister#getSelectByUniqueKeyString(String)} instead.
	 */
	@Override
	@Deprecated( since = "6.5" )
	String getSelectByUniqueKeyString(String propertyName);

	/**
	 * @deprecated Use {@link EntityPersister#getSelectByUniqueKeyString(String[])} instead.
	 */
	@Override
	@Deprecated( since = "6.5" )
	default String getSelectByUniqueKeyString(String[] propertyNames) {
		return EntityPersister.super.getSelectByUniqueKeyString( propertyNames );
	}

	/**
	 * @deprecated Use {@link EntityPersister#getRootTableKeyColumnNames()} instead.
	 */
	@Override
	@Deprecated( since = "6.5" )
	String[] getRootTableKeyColumnNames();
}
