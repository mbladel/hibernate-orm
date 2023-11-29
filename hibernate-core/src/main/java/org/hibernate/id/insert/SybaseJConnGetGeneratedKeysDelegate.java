/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.generator.values.GeneratedValuesHelper.getGeneratedValues;

/**
 * Specialized {@link IdentifierGeneratingInsert} which appends the database
 * specific clause which signifies to return generated {@code IDENTITY} values
 * to the end of the insert statement.
 * 
 * @author Christian Beikov
 */
public class SybaseJConnGetGeneratedKeysDelegate extends GetGeneratedKeysDelegate {
	private final Dialect dialect;

	public SybaseJConnGetGeneratedKeysDelegate(EntityPersister persister, Dialect dialect) {
		super( persister, dialect, true, EventType.INSERT );
		this.dialect = dialect;
	}

	@Override
	public String prepareIdentifierGeneratingInsert(String insertSQL) {
		return dialect.getIdentityColumnSupport().appendIdentitySelectToInsert( insertSQL );
	}

	@Override
	public GeneratedValues executeAndExtract(
			String sql,
			PreparedStatement preparedStatement,
			SharedSessionContractImplementor session) {
		JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final JdbcServices jdbcServices = session.getJdbcServices();

		ResultSet resultSet = jdbcCoordinator.getResultSetReturn().execute( preparedStatement, sql );
		try {
			return getGeneratedValues( resultSet, persister, getTiming(), session );
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to extract generated-keys ResultSet",
					sql
			);
		}
		finally {
			jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( resultSet, preparedStatement );
			jdbcCoordinator.afterStatementExecution();
		}
	}

	@Override
	public boolean supportsArbitraryValues() {
		return false;
	}
}
