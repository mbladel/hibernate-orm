/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.type.Type;

import static org.hibernate.generator.values.GeneratedValuesHelper.createMappingProducer;

/**
 * Uses a unique key of the inserted entity to locate the newly inserted row.
 *
 * @author Gavin King
 */
public class UniqueKeySelectingDelegate extends AbstractSelectingDelegate {
	private final String[] uniqueKeyPropertyNames;
	private final Type[] uniqueKeyTypes;

	private final String selectString;
	private final JdbcValuesMappingProducer jdbcValuesMappingProducer;

	public UniqueKeySelectingDelegate(
			PostInsertIdentityPersister persister,
			Dialect dialect,
			String[] uniqueKeyPropertyNames,
			EventType timing) {
		super( persister, timing );

		this.uniqueKeyPropertyNames = uniqueKeyPropertyNames;

		uniqueKeyTypes = new Type[ uniqueKeyPropertyNames.length ];
		for ( int i = 0; i < uniqueKeyPropertyNames.length; i++ ) {
			uniqueKeyTypes[i] = persister.getPropertyType( uniqueKeyPropertyNames[i] );
		}

		final EntityRowIdMapping rowIdMapping = persister.getRowIdMapping();
		if ( !persister.isIdentifierAssignedByInsert()
				|| persister.getInsertGeneratedProperties().size() > 1
				|| rowIdMapping != null ) {
			final List<String> columnNames = new ArrayList<>();
			this.jdbcValuesMappingProducer = getMappingProducer( columnNames::add );
			if ( rowIdMapping != null ) {
				columnNames.add( rowIdMapping.getSelectionExpression() );
			}
			selectString = persister.getSelectByUniqueKeyString(
					uniqueKeyPropertyNames,
					columnNames.toArray( new String[0] )
			);
		}
		else {
			selectString = persister.getSelectByUniqueKeyString( uniqueKeyPropertyNames );
			this.jdbcValuesMappingProducer = getMappingProducer( null );
		}
	}

	protected String getSelectSQL() {
		return selectString;
	}

	@Override @Deprecated
	public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert(SqlStringGenerationContext context) {
		return new IdentifierGeneratingInsert( getPersister().getFactory() );
	}

	@Override
	public TableMutationBuilder<?> createTableMutationBuilder(
			Expectation expectation,
			SessionFactoryImplementor factory) {
		return new TableInsertBuilderStandard( getPersister(), getPersister().getIdentifierTableMapping(), factory );
	}

	protected void bindParameters(Object entity, PreparedStatement ps, SharedSessionContractImplementor session)
			throws SQLException {
		int index = 1;
		for ( int i = 0; i < uniqueKeyPropertyNames.length; i++ ) {
			uniqueKeyTypes[i].nullSafeSet( ps, getPersister().getPropertyValue( entity, uniqueKeyPropertyNames[i] ), index, session );
			index += uniqueKeyTypes[i].getColumnSpan( session.getFactory() );
		}
	}

	@Override
	public boolean supportsArbitraryValues() {
		return true;
	}

	@Override
	public boolean supportsRowId() {
		return true;
	}

	@Override
	public JdbcValuesMappingProducer getGeneratedValuesMappingProducer() {
		return jdbcValuesMappingProducer;
	}
}
