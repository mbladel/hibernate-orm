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

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.generator.values.TableUpdateReturningBuilder;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

import static java.sql.Statement.NO_GENERATED_KEYS;
import static org.hibernate.generator.values.GeneratedValuesHelper.getGeneratedValues;

/**
 * Delegate for dealing with generated values where the dialect supports
 * returning the generated column directly from the mutation statement.
 * <p>
 * Supports both {@link EventType#INSERT insert} and {@link EventType#UPDATE update} statements.
 *
 * @see org.hibernate.generator.OnExecutionGenerator
 * @see GeneratedValuesMutationDelegate
 */
public class InsertReturningDelegate extends AbstractReturningDelegate {
	private final Dialect dialect;
	private final JdbcValuesMappingProducer jdbcValuesMappingProducer;

	public InsertReturningDelegate(EntityPersister persister, Dialect dialect, EventType timing) {
		super( persister, timing );
		this.dialect = dialect;
		this.jdbcValuesMappingProducer = getMappingProducer( null );
	}

	@Override
	public TableMutationBuilder<?> createTableMutationBuilder(
			Expectation expectation,
			SessionFactoryImplementor sessionFactory) {
		if ( getTiming() == EventType.INSERT ) {
			return new TableInsertReturningBuilder( persister, sessionFactory );
		}
		else {
			return new TableUpdateReturningBuilder<>( persister, sessionFactory );
		}
	}

	@Override
	protected GeneratedValues executeAndExtractReturning(
			String sql,
			PreparedStatement preparedStatement,
			SharedSessionContractImplementor session) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final JdbcServices jdbcServices = session.getJdbcServices();

		final ResultSet resultSet = jdbcCoordinator.getResultSetReturn().execute( preparedStatement, sql );
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
			jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( resultSet, preparedStatement );
		}
	}

	@Override
	public boolean supportsArbitraryValues() {
		return true;
	}

	@Override
	public boolean supportsRowId() {
		return dialect.supportsInsertReturningRowId();
	}

	@Override
	public JdbcValuesMappingProducer getGeneratedValuesMappingProducer() {
		return jdbcValuesMappingProducer;
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
