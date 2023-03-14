/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.dialect.JsonHelper;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Specialized type mapping for {@code JSON} and the BLOB SQL data type for H2.
 *
 * @author Christian Beikov
 * @author Marco Belladelli
 */
public class H2JsonBlobJdbcType implements AggregateJdbcType {
	/**
	 * Singleton access
	 */
	public static final H2JsonBlobJdbcType INSTANCE = new H2JsonBlobJdbcType( null );

	private final EmbeddableMappingType embeddableMappingType;

	protected H2JsonBlobJdbcType(EmbeddableMappingType embeddableMappingType) {
		this.embeddableMappingType = embeddableMappingType;
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.BLOB;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.JSON;
	}

	@Override
	public String toString() {
		return "JsonBlobJdbcType";
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		// No literal support for now
		return null;
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new H2JsonBlobJdbcType( mappingType );
	}

	@Override
	public EmbeddableMappingType getEmbeddableMappingType() {
		return embeddableMappingType;
	}

	protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) throws SQLException {
		if ( embeddableMappingType != null ) {
			return JsonHelper.fromString(
					embeddableMappingType,
					string,
					javaType.getJavaTypeClass() != Object[].class,
					options
			);
		}
		return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().fromString(
				string,
				javaType,
				options
		);
	}

	@Override
	public Object createJdbcValue(Object domainValue, WrapperOptions options) throws SQLException {
		assert embeddableMappingType != null;
		return JsonHelper.toString( embeddableMappingType, domainValue, options );
	}

	@Override
	public Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException {
		assert embeddableMappingType != null;
		return JsonHelper.fromString( embeddableMappingType, (String) rawJdbcValue, false, options );
	}

	protected <X> String toString(X value, JavaType<X> javaType, WrapperOptions options) {
		if ( embeddableMappingType != null ) {
			return JsonHelper.toString( embeddableMappingType, value, options );
		}
		return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().toString(
				value,
				javaType,
				options
		);
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final String json = H2JsonBlobJdbcType.this.toString(
						value,
						getJavaType(),
						options
				);
				st.setBytes( index, json.getBytes( StandardCharsets.UTF_8 ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final String json = H2JsonBlobJdbcType.this.toString(
						value,
						getJavaType(),
						options
				);
				st.setBytes( name, json.getBytes( StandardCharsets.UTF_8 ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return fromString( rs.getBytes( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return fromString( statement.getBytes( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return fromString( statement.getBytes( name ), options );
			}

			private X fromString(byte[] json, WrapperOptions options) throws SQLException {
				if ( json == null ) {
					return null;
				}
				return H2JsonBlobJdbcType.this.fromString(
						new String( json, StandardCharsets.UTF_8 ),
						getJavaType(),
						options
				);
			}
		};
	}
}
