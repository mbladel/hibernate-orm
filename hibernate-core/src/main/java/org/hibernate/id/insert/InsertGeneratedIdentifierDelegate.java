/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;

/**
 * Each implementation defines a strategy for retrieving a primary key
 * {@linkplain org.hibernate.generator.OnExecutionGenerator generated by
 * the database} from the database after execution of an {@code insert}
 * statement. The generated primary key is usually an {@code IDENTITY}
 * column, but in principle it might be something else, for example,
 * a value generated by a trigger.
 * <p>
 * An implementation controls:
 * <ul>
 * <li>building the SQL {@code insert} statement, and
 * <li>retrieving the generated identifier value using JDBC.
 * </ul>
 * <p>
 * The implementation should be written to handle any instance of
 * {@link org.hibernate.generator.OnExecutionGenerator}.
 *
 * @see org.hibernate.generator.OnExecutionGenerator
 *
 * @author Steve Ebersole
 *
 * @deprecated Use {@link GeneratedValuesMutationDelegate} instead.
 */
@Deprecated( forRemoval = true, since = "7.0" )
public interface InsertGeneratedIdentifierDelegate extends GeneratedValuesMutationDelegate {
	/**
	 * Create a {@link TableInsertBuilder} with any specific identity
	 * handling already built in.
	 * @deprecated Use {@link GeneratedValuesMutationDelegate#createTableMutationBuilder} instead.
	 */
	@Deprecated( since = "7.0" )
	default TableInsertBuilder createTableInsertBuilder(
			BasicEntityIdentifierMapping identifierMapping,
			Expectation expectation,
			SessionFactoryImplementor sessionFactory) {
		return (TableInsertBuilder) createTableMutationBuilder( expectation, sessionFactory );
	}

	@Override
	PreparedStatement prepareStatement(String insertSql, SharedSessionContractImplementor session);

	/**
	 * Perform the {@code insert} and extract the database-generated
	 * primary key value.
	 *
	 * @see #createTableInsertBuilder
	 *
	 * @deprecated Use {@link GeneratedValuesMutationDelegate#performMutation} instead
	 */
	@Deprecated( since = "7.0" )
	default Object performInsert(
			PreparedStatementDetails insertStatementDetails,
			JdbcValueBindings valueBindings,
			Object entity,
			SharedSessionContractImplementor session) {
		return performMutation( insertStatementDetails, valueBindings, entity, session );
	}

	/**
	 * Append SQL specific to this delegate's mode of handling generated
	 * primary key values to the given {@code insert} statement.
	 *
	 * @return The processed {@code insert} statement string
	 */
	default String prepareIdentifierGeneratingInsert(String insertSQL) {
		return insertSQL;
	}

	/**
	 * Execute the given {@code insert} statement and return the generated
	 * key value.
	 *
	 * @param insertSQL The {@code insert} statement string
	 * @param session The session in which we are operating
	 * @param binder The parameter binder
	 * 
	 * @return The generated identifier value
	 */
	GeneratedValues performInsert(String insertSQL, SharedSessionContractImplementor session, Binder binder);
}
