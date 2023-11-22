/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.values;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.EventType;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;
import org.hibernate.id.insert.InsertReturningDelegate;
import org.hibernate.id.insert.UniqueKeySelectingDelegate;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.WrapperOptions;

import static org.hibernate.generator.internal.NaturalIdHelper.getNaturalIdPropertyNames;

/**
 * Factory and helper methods for {@link GeneratedValuesMutationDelegate} framework.
 *
 * @author Marco Belladelli
 */
@Internal
public class GeneratedValuesHelper {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( IdentifierGeneratorHelper.class );

	/**
	 * Reads the {@link PostInsertIdentityPersister#getGeneratedProperties(EventType)}  generated values}
	 * for the specified {@link ResultSet}.
	 *
	 * @param resultSet The result set from which to extract the generated values
	 * @param persister The entity type which we're reading the generated values for
	 * @param wrapperOptions The session
	 *
	 * @return The generated values
	 *
	 * @throws SQLException Can be thrown while accessing the result set
	 * @throws HibernateException Indicates a problem reading back a generated value
	 */
	public static GeneratedValues getGeneratedValues(
			ResultSet resultSet,
			PostInsertIdentityPersister persister,
			EventType timing,
			WrapperOptions wrapperOptions) throws SQLException {
		if ( !resultSet.next() ) {
			throw new HibernateException(
					"The database returned no natively generated values : " + persister.getNavigableRole().getFullPath()
			);
		}

		final List<ModelPart> generatedModelParts;
		if ( timing == EventType.INSERT ) {
			generatedModelParts = new ArrayList<>(
					persister.getInsertDelegate().supportsArbitraryValues() ?
							persister.getInsertGeneratedProperties() :
							List.of( persister.getIdentifierMapping() )
			);

			if ( persister.getRowIdMapping() != null && persister.getInsertDelegate().supportsRowId() ) {
				generatedModelParts.add( persister.getRowIdMapping() );
			}
		}
		else {
			generatedModelParts = new ArrayList<>( persister.getUpdateGeneratedProperties() );
		}

		final GeneratedValuesImpl generatedValues = new GeneratedValuesImpl( generatedModelParts );
		for ( ModelPart modelPart : generatedModelParts ) {
			final SelectableMapping selectable = getActualSelectableMapping( modelPart, persister );
			final JdbcMapping jdbcMapping = selectable.getJdbcMapping();
			final Object value = jdbcMapping.getJdbcValueExtractor().extract( resultSet, columnIndex(
					resultSet,
					selectable,
					modelPart.isEntityIdentifierMapping() ? 1 : null,
					persister.getFactory().getFastSessionServices().dialect
			), wrapperOptions );
			generatedValues.addGeneratedValue( modelPart, value );

			LOG.debugf(
					"Extracted natively generated value (%s) : %s",
					selectable.getSelectionExpression(),
					value
			);
		}
		return generatedValues;
	}

	private static int columnIndex(
			ResultSet resultSet,
			SelectableMapping selectable,
			Integer defaultIndex,
			Dialect dialect) {
		final String columnName = selectable.getSelectionExpression();
		try {
			final ResultSetMetaData metaData = resultSet.getMetaData();
			for ( int i = 1; i <= metaData.getColumnCount(); i++ ) {
				if ( equal( columnName, metaData.getColumnName( i ), dialect ) ) {
					return i;
				}
			}
		}
		catch (SQLException e) {
			LOG.debugf( "Could not determine column index from JDBC metadata", e );
		}

		if ( defaultIndex != null ) {
			return defaultIndex;
		}
		else {
			throw new HibernateException( "Could not retrieve column index for column name : " + columnName );
		}
	}

	public static SelectableMapping getActualSelectableMapping(ModelPart modelPart, EntityPersister persister) {
		// todo marco : would be nice to avoid the cast here, but if we want to keep
		//  the options open (for Components and/or other attribute types) we're going to need it
		assert modelPart instanceof SelectableMapping : "Unsupported non-selectable generated value";
		final ModelPart actualModelPart = modelPart.isEntityIdentifierMapping() ?
				persister.getRootEntityDescriptor().getIdentifierMapping() :
				modelPart;
		return (SelectableMapping) actualModelPart;
	}

	/**
	 * Creates the {@link GeneratedValuesMutationDelegate delegate} used to retrieve
	 * {@linkplain org.hibernate.generator.OnExecutionGenerator database generated values} on
	 * mutation execution through e.g. {@link Dialect#supportsInsertReturning() insert ... returning}
	 * syntax or the JDBC {@link Dialect#supportsInsertReturningGeneratedKeys() getGeneratedKeys()} API.
	 * <p>
	 * If the current {@link Dialect} doesn't support any of the available delegates this method returns {@code null}.
	 */
	public static GeneratedValuesMutationDelegate getGeneratedValuesDelegate(
			PostInsertIdentityPersister persister,
			EventType timing) {
		final boolean hasGeneratedProperties = !persister.getGeneratedProperties( timing ).isEmpty();
		final boolean hasRowId = timing == EventType.INSERT && persister.getRowIdMapping() != null;
		final Dialect dialect = persister.getFactory().getJdbcServices().getDialect();

		if ( hasRowId && dialect.supportsInsertReturning() && dialect.supportsInsertReturningRowId() ) {
			// Special case for RowId on INSERT, since GetGeneratedKeysDelegate doesn't support it
			// make InsertReturningDelegate the preferred method if the dialect supports it
			return new InsertReturningDelegate( persister, dialect, timing );
		}

		if ( !hasGeneratedProperties ) {
			return null;
		}


		if ( dialect.supportsInsertReturningGeneratedKeys() ) {
			return new GetGeneratedKeysDelegate( persister, dialect, false, timing );
		}
		else if ( dialect.supportsInsertReturning() ) {
			return new InsertReturningDelegate( persister, dialect, timing );
		}
		else if ( timing == EventType.INSERT && persister.getNaturalIdentifierProperties() != null
				&& !persister.getEntityMetamodel().isNaturalIdentifierInsertGenerated() ) {
			return new UniqueKeySelectingDelegate(
					persister,
					dialect,
					getNaturalIdPropertyNames( persister ),
					timing
			);
		}
		return null;
	}

	/**
	 * Returns a list of strings containing the {@linkplain SelectableMapping#getSelectionExpression() column names}
	 * of the generated properties of the provided {@link EntityPersister persister}.
	 */
	public static List<String> getGeneratedColumnNames(
			EntityPersister persister,
			Dialect dialect,
			EventType timing,
			boolean unquote) {
		final List<? extends ModelPart> generated = persister.getGeneratedProperties( timing );
		return generated.stream().map( modelPart -> {
			final SelectableMapping selectableMapping = getActualSelectableMapping( modelPart, persister );
			final String selectionExpression = selectableMapping.getSelectionExpression();
			return unquote ? StringHelper.unquote( selectionExpression, dialect ) : selectionExpression;
		} ).toList();
	}

	private static boolean equal(String keyColumnName, String alias, Dialect dialect) {
		return alias.equalsIgnoreCase( keyColumnName )
				|| alias.equalsIgnoreCase( StringHelper.unquote( keyColumnName, dialect ) );
	}
}
