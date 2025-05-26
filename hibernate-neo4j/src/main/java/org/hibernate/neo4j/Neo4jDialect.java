/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.neo4j;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SimpleDatabaseVersion;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.tool.schema.spi.Exporter;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * An SQL dialect for Neo4j.
 */
public class Neo4jDialect extends Dialect {

	private static final Pattern VERSION_PATTERN = Pattern.compile( "[\\d]+\\.[\\d]+\\.[\\d]+" );
	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 2, 5 );
	private static final Type[] VECTOR_JAVA_TYPES = {
			Float[].class,
			float[].class
	};
	private static final Type[] BINARY_VECTOR_JAVA_TYPES = {
			Byte[].class,
			byte[].class
	};

	@SuppressWarnings("unused")
	public Neo4jDialect() {
		this( MINIMUM_VERSION );
	}

	public Neo4jDialect(DialectResolutionInfo info) {
		this( determineVersion( info ) );
		registerKeywords( info );
	}

	public Neo4jDialect(DatabaseVersion version) {
		super( version );
	}

	protected static DatabaseVersion determineVersion(DialectResolutionInfo info) {
		String versionString = null;
		if ( info.getDatabaseMetadata() != null ) {
			try {
				versionString = info.getDatabaseMetadata().getDatabaseProductVersion();
			}
			catch (SQLException ex) {
				// Ignore
			}
		}
		return versionString != null ? parseVersion( versionString ) : info.makeCopyOrDefault( MINIMUM_VERSION );
	}

	public static DatabaseVersion parseVersion(String versionString) {
		DatabaseVersion databaseVersion = null;
		final Matcher matcher = VERSION_PATTERN.matcher( versionString == null ? "" : versionString );
		if ( matcher.matches() ) {
			final String[] versionParts = StringHelper.split( ".", versionString );
			// if we got to this point, there is at least a major version, so no need to check [].length > 0
			int majorVersion = parseInt( versionParts[0] );
			int minorVersion = versionParts.length > 1 ? parseInt( versionParts[1] ) : 0;
			int microVersion = versionParts.length > 2 ? parseInt( versionParts[2] ) : 0;
			databaseVersion = new SimpleDatabaseVersion( majorVersion, minorVersion, microVersion );
		}
		if ( databaseVersion == null ) {
			databaseVersion = MINIMUM_VERSION;
		}
		return databaseVersion;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		// todo neo4j : do we need anything here?
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		// todo neo4j : do we need anything here?
	}

	@Override
	public int getMaxIdentifierLength() {
		return 255;
	}

	@Override
	public boolean supportsStandardArrays() {
		return true;
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case CHAR,VARCHAR, NCHAR, NVARCHAR, CLOB, NCLOB -> "varchar";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	public void appendLiteral(SqlAppender appender, String literal) {
		appender.appendDoubleQuoteEscapedString( literal );
	}

	@Override
	public Exporter<Table> getTableExporter() {
		return Neo4jTableExporter.INSTANCE;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		// todo neo4j
		return super.getSqlAstTranslatorFactory();
//		return new StandardSqlAstTranslatorFactory() {
//			@Override
//			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
//					SessionFactoryImplementor sessionFactory, Statement statement, SqlParameterInfo parameterInfo) {
//				return new MilvusSqlAstTranslator<>( sessionFactory, statement, parameterInfo );
//			}
//		};
	}
}
