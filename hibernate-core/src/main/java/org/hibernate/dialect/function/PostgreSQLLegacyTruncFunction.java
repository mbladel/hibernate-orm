/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.NUMERIC;

/**
 * PostgreSQL only supports the two-argument {@code trunc} function with the signature:
 * {@code trunc(numeric, integer)}.
 * <p>
 * This custom function falls back to using {@code floor} as a workaround only when necessary:
 * <ul>
 *     <li>The first argument is of type {@code double precision}</li>
 *     <li>The dialect doesn't support the two-argument {@code trunc} function</li>
 * </ul>
 *
 * @see https://www.postgresql.org/docs/current/functions-math.html
 *
 * @author Marco Belladelli
 */
public class PostgreSQLLegacyTruncFunction extends AbstractSqmSelfRenderingFunctionDescriptor {
	private boolean supportsTwoArgumentTrunc;

	public PostgreSQLLegacyTruncFunction(boolean supportsTwoArgumentTrunc) {
		super(
				"trunc",
				new ArgumentTypesValidator( StandardArgumentsValidators.between( 1, 2 ), NUMERIC, INTEGER ),
				StandardFunctionReturnTypeResolvers.useArgType( 1 ),
				StandardFunctionArgumentTypeResolvers.invariant( NUMERIC, INTEGER )
		);
		this.supportsTwoArgumentTrunc = supportsTwoArgumentTrunc;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			SqlAstTranslator<?> walker) {
		final int numberOfArguments = arguments.size();
		Expression firstArg = (Expression) arguments.get( 0 );
		final JdbcType jdbcType = firstArg.getExpressionType().getJdbcMappings().get( 0 ).getJdbcType();
		if ( numberOfArguments == 1 || supportsTwoArgumentTrunc && jdbcType.isDecimal() ) {
			// use native trunc function
			sqlAppender.appendSql( "trunc(" );
			firstArg.accept( walker );
			if ( numberOfArguments > 1 ) {
				sqlAppender.appendSql( ", " );
				arguments.get( 1 ).accept( walker );
			}
			sqlAppender.appendSql( ")" );
		}
		else {
			// workaround using floor
			sqlAppender.appendSql( "sign(" );
			firstArg.accept( walker );
			sqlAppender.appendSql( ")*floor(abs(" );
			firstArg.accept( walker );
			sqlAppender.appendSql( ")*1e" );
			arguments.get( 1 ).accept( walker );
			sqlAppender.appendSql( ")/1e" );
			arguments.get( 1 ).accept( walker );
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "(NUMERIC number[, INTEGER places])";
	}
}
