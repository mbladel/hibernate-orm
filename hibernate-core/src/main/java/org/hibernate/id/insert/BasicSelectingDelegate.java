/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.id.insert;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.internal.CoreLogging;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

import static org.hibernate.generator.values.GeneratedValuesHelper.createMappingProducer;

/**
 * Delegate for dealing with {@code IDENTITY} columns where the dialect requires an
 * additional command execution to retrieve the generated {@code IDENTITY} value
 */
public class BasicSelectingDelegate extends AbstractSelectingDelegate {
	private final Dialect dialect;
	private final JdbcValuesMappingProducer jdbcValuesMappingProducer;

	public BasicSelectingDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
		super( persister, EventType.INSERT );
		this.dialect = dialect;
		this.jdbcValuesMappingProducer = getMappingProducer( null );
	}

	@Override @Deprecated
	public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert(SqlStringGenerationContext context) {
		IdentifierGeneratingInsert insert = new IdentifierGeneratingInsert( getPersister().getFactory() );
		insert.addGeneratedColumns( getPersister().getRootTableKeyColumnNames(), (OnExecutionGenerator) getPersister().getGenerator() );
		return insert;
	}

	@Override
	public TableMutationBuilder<?> createTableMutationBuilder(
			Expectation expectation,
			SessionFactoryImplementor factory) {
		final TableInsertBuilder builder =
				new TableInsertBuilderStandard( getPersister(), getPersister().getIdentifierTableMapping(), factory );

		final OnExecutionGenerator generator = (OnExecutionGenerator) getPersister().getGenerator();
		if ( generator.referenceColumnsInSql( dialect ) ) {
			final BasicEntityIdentifierMapping identifierMapping = (BasicEntityIdentifierMapping) getPersister().getIdentifierMapping();
			final String[] columnNames = getPersister().getRootTableKeyColumnNames();
			final String[] columnValues = generator.getReferencedColumnValues( dialect );
			if ( columnValues.length != columnNames.length ) {
				throw new MappingException("wrong number of generated columns");
			}
			for ( int i = 0; i < columnValues.length; i++ ) {
				builder.addKeyColumn( columnNames[i], columnValues[i], identifierMapping.getJdbcMapping() );
			}
		}

		return builder;
	}

	@Override
	protected String getSelectSQL() {
		if ( getPersister().getIdentitySelectString() == null && !dialect.getIdentityColumnSupport().supportsInsertSelectIdentity() ) {
			throw CoreLogging.messageLogger( BasicSelectingDelegate.class ).nullIdentitySelectString();
		}
		return getPersister().getIdentitySelectString();
	}

	@Override
	public JdbcValuesMappingProducer getGeneratedValuesMappingProducer() {
		return jdbcValuesMappingProducer;
	}
}
