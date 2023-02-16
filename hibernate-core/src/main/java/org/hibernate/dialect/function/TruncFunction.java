/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderingSupport;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.NUMERIC;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL_UNIT;

/**
 * Custom function that manages both numeric and datetime truncation
 *
 * @author Marco Belladelli
 */
public class TruncFunction extends AbstractSqmFunctionDescriptor implements FunctionRenderingSupport {
	private final String truncPattern;
	private final String twoArgTruncPattern;
	private final DatetimeTrunc datetimeTrunc;
	private final String toDateFunction;

	private boolean numericTrunc;

	public enum DatetimeTrunc {
		DATE_TRUNC( "date_trunc('?2',?1)" ),
		DATETRUNC( "datetrunc(?2,?1)" ),
		TRUNC( "trunc(?1,?2)" ),
		FORMAT( null );

		private final String pattern;

		DatetimeTrunc(String pattern) {
			this.pattern = pattern;
		}

		public String getPattern() {
			return pattern;
		}
	}

	public TruncFunction(
			String truncPattern,
			String twoArgTruncPattern,
			DatetimeTrunc datetimeTrunc,
			String toDateFunction) {
		super(
				"trunc",
				new TruncArgumentsValidator(),
				StandardFunctionReturnTypeResolvers.useArgType( 1 ),
				StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE
		);
		this.truncPattern = truncPattern;
		this.twoArgTruncPattern = twoArgTruncPattern;
		this.datetimeTrunc = datetimeTrunc;
		this.toDateFunction = toDateFunction;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		final String pattern;
		if ( numericTrunc ) {
			if ( sqlAstArguments.size() == 2 && twoArgTruncPattern != null ) {
				pattern = twoArgTruncPattern;
			}
			else {
				pattern = truncPattern;
			}
		}
		else {
			pattern = datetimeTrunc.getPattern();
		}
		new PatternRenderer( pattern, SqlAstNodeRenderingMode.DEFAULT ).render(
				sqlAppender,
				sqlAstArguments,
				walker
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		final NodeBuilder nodeBuilder = queryEngine.getCriteriaBuilder();
		final List<SqmTypedNode<?>> args = new ArrayList<>( arguments );
		if ( arguments.size() == 2 && arguments.get( 1 ) instanceof SqmExtractUnit ) {
			// datetime truncation
			numericTrunc = false;
			if ( datetimeTrunc == null ) {
				throw new UnsupportedOperationException( "Datetime truncation is not supported for this database" );
			}
			if ( datetimeTrunc.getPattern() == null ) {
				final boolean useConvertToFormat = nodeBuilder.getSessionFactory()
						.getJdbcServices()
						.getDialect() instanceof SybaseDialect;
				return new DateTruncEmulation(
						toDateFunction,
						useConvertToFormat,
						typeConfiguration
				).generateSqmFunctionExpression( arguments, impliedResultType, queryEngine, typeConfiguration );
			}
			else if ( datetimeTrunc == DatetimeTrunc.TRUNC ) {
				// the trunc() function requires translating the temporal_unit to a format string
				final TemporalUnit temporalUnit = ( (SqmExtractUnit<?>) arguments.get( 1 ) ).getUnit();
				final String pattern;
				switch ( temporalUnit ) {
					case YEAR:
						pattern = "YYYY";
						break;
					case MONTH:
						pattern = "MM";
						break;
					case WEEK:
						pattern = "IW";
						break;
					case DAY:
						pattern = "DD";
						break;
					case HOUR:
						pattern = "HH";
						break;
					case MINUTE:
						pattern = "MI";
						break;
					case SECOND:
						if ( nodeBuilder.getSessionFactory().getJdbcServices().getDialect() instanceof OracleDialect ) {
							// Oracle does not support truncating to seconds with the native function, use emulation
							return new DateTruncEmulation( "to_date", false, typeConfiguration )
									.generateSqmFunctionExpression(
											arguments,
											impliedResultType,
											queryEngine,
											typeConfiguration
									);
						}
						pattern = "SS";
						break;
					default:
						throw new UnsupportedOperationException( "Temporal unit not supported [" + temporalUnit + "]" );
				}
				// replace temporal_unit parameter with translated string format literal
				args.set( 1, new SqmLiteral<>(
						pattern,
						typeConfiguration.getBasicTypeForJavaType( String.class ),
						nodeBuilder
				) );
			}
		}
		else {
			// numeric truncation
			if ( nodeBuilder.getSessionFactory().getJdbcServices().getDialect() instanceof PostgreSQLDialect ) {
				return new PostgreSQLTruncRoundFunction( getName(), true ).generateSqmFunctionExpression(
						arguments,
						impliedResultType,
						queryEngine,
						typeConfiguration
				);
			}
			numericTrunc = true;
		}

		return new SelfRenderingSqmFunction<>(
				this,
				this,
				args,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}



	private static class TruncArgumentsValidator implements ArgumentsValidator {
		@Override
		public void validate(
				List<? extends SqmTypedNode<?>> arguments,
				String functionName,
				TypeConfiguration typeConfiguration) {
			if ( arguments.size() == 2 && arguments.get( 1 ) instanceof SqmExtractUnit ) {
				new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 2 ),
						TEMPORAL,
						TEMPORAL_UNIT
				).validate( arguments, functionName, typeConfiguration );
			}
			else {
				new ArgumentTypesValidator(
						StandardArgumentsValidators.between( 1, 2 ),
						NUMERIC,
						NUMERIC
				).validate( arguments, functionName, typeConfiguration );
			}
		}
	}
}
