/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;

import static java.sql.Statement.NO_GENERATED_KEYS;
import static org.hibernate.generator.values.GeneratedValuesHelper.getGeneratedValues;

/**
 * Delegate for dealing with generated values where the dialect supports
 * returning the generated column directly from the mutation statement.
 *
 * @see org.hibernate.generator.OnExecutionGenerator
 * @see org.hibernate.generator.values.MutationGeneratedValuesDelegate
 */
public class InsertReturningDelegate extends AbstractReturningDelegate {
	private final PostInsertIdentityPersister persister;
	private final Dialect dialect;

	public InsertReturningDelegate(PostInsertIdentityPersister persister, Dialect dialect, EventType timing) {
		super( persister, timing );
		this.persister = persister;
		this.dialect = dialect;
	}

	@Override @Deprecated
	public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert(SqlStringGenerationContext context) {
		InsertSelectIdentityInsert insert = new InsertSelectIdentityInsert( persister.getFactory() );
		insert.addGeneratedColumns( persister.getRootTableKeyColumnNames(), (OnExecutionGenerator) persister.getGenerator() );
		return insert;
	}

	@Override
	public TableMutationBuilder<?> createTableMutationBuilder(
			Expectation expectation,
			SessionFactoryImplementor sessionFactory) {
		return new TableInsertReturningBuilder( persister, sessionFactory );
	}

	@Override
	protected GeneratedValues executeAndExtract(
			String sql,
			PreparedStatement insertStatement,
			SharedSessionContractImplementor session) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final JdbcServices jdbcServices = session.getJdbcServices();

		final ResultSet resultSet = jdbcCoordinator.getResultSetReturn().execute( insertStatement, sql );
		try {
			return getGeneratedValues( resultSet, persister, getTiming(), session );
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to extract generated key(s) from generated-keys ResultSet",
					sql
			);
		}
		finally {
			jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( resultSet, insertStatement );
		}
	}

	@Override
	public boolean supportsRetrievingGeneratedValues() {
		return true;
	}

	@Override
	public boolean supportsRetrievingRowId() {
		return true;
	}

	@Override
	public String prepareIdentifierGeneratingInsert(String insertSQL) {
		return dialect.getIdentityColumnSupport().appendIdentitySelectToInsert( insertSQL );
	}

	@Override
	public PreparedStatement prepareStatement(String sql, SharedSessionContractImplementor session) {
		return session.getJdbcCoordinator().getMutationStatementPreparer().prepareStatement( sql, NO_GENERATED_KEYS );
	}
}
