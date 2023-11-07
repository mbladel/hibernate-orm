/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import org.hibernate.dialect.Dialect;
import org.hibernate.generator.EventType;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Andrea Boriero
 */
public class Oracle12cIdentityColumnSupport extends IdentityColumnSupportImpl {

	public static final Oracle12cIdentityColumnSupport INSTANCE = new Oracle12cIdentityColumnSupport();

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentityColumnString(int type) {
		return "generated as identity";
	}

	@Override
	public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(EntityPersister persister) {
		return new GetGeneratedKeysDelegate( persister, false, EventType.INSERT );
	}

	@Override
	public String getIdentityInsertString() {
		return "default";
	}
}
