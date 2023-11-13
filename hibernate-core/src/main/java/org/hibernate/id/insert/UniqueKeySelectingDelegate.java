/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.type.Type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.hibernate.id.IdentifierGeneratorHelper.getGeneratedColumnNames;
import static org.hibernate.id.IdentifierGeneratorHelper.getGeneratedValues;

/**
 * Uses a unique key of the inserted entity to locate the newly inserted row.
 *
 * @author Gavin King
 */
public class UniqueKeySelectingDelegate extends AbstractSelectingDelegate {
	private final PostInsertIdentityPersister persister;

	private final String[] uniqueKeyPropertyNames;
	private final Type[] uniqueKeyTypes;

	private final String selectString;

	public UniqueKeySelectingDelegate(PostInsertIdentityPersister persister, Dialect dialect, String[] uniqueKeyPropertyNames) {
		super( persister );

		this.persister = persister;
		this.uniqueKeyPropertyNames = uniqueKeyPropertyNames;

		uniqueKeyTypes = new Type[ uniqueKeyPropertyNames.length ];
		for ( int i = 0; i < uniqueKeyPropertyNames.length; i++ ) {
			uniqueKeyTypes[i] = persister.getPropertyType( uniqueKeyPropertyNames[i] );
		}

		final EntityRowIdMapping rowIdMapping = persister.getRowIdMapping();
		if ( !persister.isIdentifierAssignedByInsert()
				|| persister.getInsertGeneratedProperties().size() > 1
				|| rowIdMapping != null ) {
			final List<String> columnNames = getGeneratedColumnNames( persister, dialect, EventType.INSERT );
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
		}
	}

	protected String getSelectSQL() {
		return selectString;
	}

	@Override @Deprecated
	public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert(SqlStringGenerationContext context) {
		return new IdentifierGeneratingInsert( persister.getFactory() );
	}

	@Override
	public TableInsertBuilder createTableInsertBuilder(
			Expectation expectation,
			SessionFactoryImplementor factory) {
		return new TableInsertBuilderStandard( persister, persister.getIdentifierTableMapping(), factory );
	}

	protected void bindParameters(Object entity, PreparedStatement ps, SharedSessionContractImplementor session)
			throws SQLException {
		int index = 1;
		for ( int i = 0; i < uniqueKeyPropertyNames.length; i++ ) {
			uniqueKeyTypes[i].nullSafeSet( ps, persister.getPropertyValue( entity, uniqueKeyPropertyNames[i] ), index, session );
			index += uniqueKeyTypes[i].getColumnSpan( session.getFactory() );
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
}
