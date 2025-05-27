/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.neo4j;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.SqlParameterInfo;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.spi.Exporter;

import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.DURATION;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.REAL;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_UTC;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * An SQL dialect for Neo4j.
 */
public class Neo4jDialect extends Dialect {

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 5, 0 );

	private final boolean enterpriseEdition;

	@SuppressWarnings("unused")
	public Neo4jDialect() {
		this( MINIMUM_VERSION );
	}

	public Neo4jDialect(DatabaseVersion version) {
		super( version );
		this.enterpriseEdition = false;
	}

	@SuppressWarnings("unused")
	public Neo4jDialect(DialectResolutionInfo info) {
		this( info, Neo4jServerConfiguration.fromDialectResolutionInfo( info ) );
	}

	public Neo4jDialect(DialectResolutionInfo info, Neo4jServerConfiguration serverConfiguration) {
		super( serverConfiguration.getDatabaseVersion() );
		this.enterpriseEdition = serverConfiguration.isEnterpriseEdition();
		registerKeywords( info );
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		// todo neo4j : do we need anything here?
	}

	public boolean isEnterpriseEdition() {
		return enterpriseEdition;
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
			case CHAR, VARCHAR, NCHAR, NVARCHAR, CLOB, NCLOB, LONG32VARCHAR, LONG32NVARCHAR -> "string";
			case BOOLEAN -> "boolean";
			case TINYINT, SMALLINT, INTEGER, BIGINT -> "integer";
			case FLOAT, REAL, DOUBLE, NUMERIC, DECIMAL -> "float";

			case DATE -> "date";
			case TIME, TIME_UTC -> "local time";
			case TIME_WITH_TIMEZONE -> "zoned time";
			case TIMESTAMP, TIMESTAMP_UTC -> "local datetime";
			case TIMESTAMP_WITH_TIMEZONE -> "zoned datetime";
			case DURATION -> "duration";

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
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement, SqlParameterInfo parameterInfo) {
				return new Neo4jSqlAstTranslator<>( sessionFactory, statement, parameterInfo );
			}
		};
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public String[] getTruncateTableStatements(String[] tableNames) {
		return new String[] {"match(n:" + String.join( "|", tableNames ) + ") detach delete n"};
	}

	@Override
	public boolean hasAlterTable() {
		return false;
	}
}
