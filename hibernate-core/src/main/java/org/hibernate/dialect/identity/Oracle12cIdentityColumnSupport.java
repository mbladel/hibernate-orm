/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;

/**
 * @author Andrea Boriero
 */
public class Oracle12cIdentityColumnSupport extends IdentityColumnSupportImpl {
	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) throws MappingException {
		return "select " + column + " from " + table;
	}

	@Override
	public String getIdentityColumnString(int type) {
		return "generated as identity";
	}

	@Override
	public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(
			PostInsertIdentityPersister persister, Dialect dialect) {
		return new GetGeneratedKeysDelegate( persister, dialect, false );
	}

	@Override
	public String getIdentityInsertString() {
		return "default";
	}
}
