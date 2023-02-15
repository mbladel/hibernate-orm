/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmDurationUnit;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.NUMERIC;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL_UNIT;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * Trunc function manages both numeric and datetime implementations
 *
 * @author Marco Belladelli
 */
public class TruncFunction extends AbstractSqmFunctionDescriptor {

	public enum NumericTruncType {
		TRUNC,
		TRUNC_TRUNCATE,
		ROUND,
		FLOOR_POWER,
		TRUNC_FLOOR,
		FLOOR,
		TRUNCATE,
		TRUNCATE_ROUND,
		TRUNCATE_ROUND_MODE
	}

	public enum DatetimeTruncType {
		TRUNC,
		DATETRUNC,
		TRUNC_TRUNC,
		TRUNC_FORMAT
	}

	private final NumericTruncType numericTruncType;

	private final DatetimeTruncType datetimeTruncType;

	private final String toDateFunction;

	private final Boolean useConvertToFormatDatetimes;

	public TruncFunction(
			NumericTruncType numericTruncType,
			DatetimeTruncType datetimeTruncType,
			String toDateFunction,
			Boolean useConvertToFormatDatetimes) {
		super(
				"trunc",
				new ArgumentTypesValidator( StandardArgumentsValidators.between( 1, 2 ), ANY, ANY ),
				StandardFunctionReturnTypeResolvers.useArgType( 1 ),
				StandardFunctionArgumentTypeResolvers.argumentsOrImplied( 1 )
		);
		this.numericTruncType = numericTruncType;
		this.datetimeTruncType = datetimeTruncType;
		this.toDateFunction = toDateFunction;
		this.useConvertToFormatDatetimes = useConvertToFormatDatetimes;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		// The second argument is what determines if this trunc function
		// is applied with numeric or datetime semantic
		if ( arguments.get( 1 ) instanceof SqmDurationUnit ) {
			return generateDatetimeTruncFunction( arguments, impliedResultType, queryEngine, typeConfiguration );
		}
		else {
			return generateNumericTruncFunction( arguments, impliedResultType, queryEngine, typeConfiguration );
		}
	}

	private <T> SelfRenderingSqmFunction<T> generateDatetimeTruncFunction(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		final SqmFunctionRegistry functionRegistry = queryEngine.getSqmFunctionRegistry();
		final String pattern;
		if ( datetimeTruncType == DatetimeTruncType.TRUNC ) {
			pattern = "date_trunc('?1',?2)";
		}
		else if ( datetimeTruncType == DatetimeTruncType.DATETRUNC ) {
			pattern = "datetrunc(?1,?2)";
		}
		else {
			pattern = null;
		}
		final SqmFunctionDescriptor descriptor;
		if ( pattern != null ) {
			descriptor = functionRegistry.patternDescriptorBuilder( "date_trunc", pattern )
					.setReturnTypeResolver( useArgType( 1 ) )
					.setExactArgumentCount( 2 )
					.setParameterTypes( TEMPORAL, TEMPORAL_UNIT )
					.setArgumentListSignature( "(TEMPORAL_UNIT field, TEMPORAL datetime)" )
					.descriptor();
		}
		else if ( datetimeTruncType == DatetimeTruncType.TRUNC_TRUNC ) {
			descriptor = new DateTruncTrunc( typeConfiguration );
		}
		else if ( datetimeTruncType == DatetimeTruncType.TRUNC_FORMAT ) {
			descriptor = new DateTruncEmulation( toDateFunction, useConvertToFormatDatetimes, typeConfiguration );
		}
		else {
			throw new IllegalArgumentException( "Invalid DatetimeTruncType [" + datetimeTruncType + "]" );
		}
		return descriptor.generateSqmExpression(
				arguments,
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}

	private <T> SelfRenderingSqmFunction<T> generateNumericTruncFunction(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		final SqmFunctionRegistry functionRegistry = queryEngine.getSqmFunctionRegistry();
		final SqmFunctionDescriptor descriptor;
		String pattern;
		switch ( numericTruncType ) {
			case TRUNC:
				descriptor = functionRegistry.namedDescriptorBuilder( "trunc" )
						.setReturnTypeResolver( useArgType( 1 ) )
						.setArgumentCountBetween( 1, 2 )
						.setParameterTypes( NUMERIC, INTEGER )
						.setArgumentListSignature( "(NUMERIC number[, INTEGER places])" )
						.descriptor();
				break;
			case TRUNC_TRUNCATE:
				pattern = arguments.size() == 1 ? "0" : "?2";
				descriptor = functionRegistry.patternDescriptorBuilder( "trunc", "truncate(?1," + pattern + ")" )
						.setExactArgumentCount( arguments.size() )
						.setParameterTypes( NUMERIC, INTEGER )
						.setReturnTypeResolver( useArgType( 1 ) )
						.setArgumentListSignature( "(NUMERIC number[, INTEGER places])" )
						.descriptor();
				break;
			case ROUND:
				pattern = arguments.size() == 1 ? "0" : "?2";
				descriptor = functionRegistry.patternDescriptorBuilder( "trunc", "round(?1," + pattern + ",1)" )
						.setExactArgumentCount( arguments.size() )
						.setParameterTypes( NUMERIC, INTEGER )
						.setReturnTypeResolver( useArgType( 1 ) )
						.setArgumentListSignature( "(NUMERIC number[, INTEGER places])" )
						.descriptor();
				break;
			case FLOOR_POWER:
				pattern = arguments.size() == 1 ?
						"sign(?1)*floor(abs(?1))" :
						"sign(?1)*floor(abs(?1)*power(10,?2))/power(10,?2)";
				descriptor = functionRegistry.patternDescriptorBuilder( "trunc", pattern )
						.setExactArgumentCount( arguments.size() )
						.setParameterTypes( NUMERIC, INTEGER )
						.setReturnTypeResolver( useArgType( 1 ) )
						.setArgumentListSignature( "(NUMERIC number[, INTEGER places])" )
						.descriptor();
				break;
			case TRUNC_FLOOR:
				pattern = arguments.size() == 1 ? "trunc(?1)" : "sign(?1)*floor(abs(?1)*1e?2)/1e?2";
				descriptor = functionRegistry.patternDescriptorBuilder( "trunc", pattern )
						.setExactArgumentCount( arguments.size() )
						.setParameterTypes( NUMERIC, INTEGER )
						.setReturnTypeResolver( useArgType( 1 ) )
						.setArgumentListSignature( "(NUMERIC number[, INTEGER places])" )
						.descriptor();
				break;
			case FLOOR:
				pattern = arguments.size() == 1 ? "sign(?1)*floor(abs(?1))" : "sign(?1)*floor(abs(?1)*1e?2)/1e?2";
				descriptor = functionRegistry.patternDescriptorBuilder( "trunc", pattern )
						.setExactArgumentCount( arguments.size() )
						.setParameterTypes( NUMERIC, INTEGER )
						.setReturnTypeResolver( useArgType( 1 ) )
						.setArgumentListSignature( "(NUMERIC number[, INTEGER places])" )
						.descriptor();
				break;
			case TRUNCATE:
				descriptor = functionRegistry.namedDescriptorBuilder( "truncate" )
						.setExactArgumentCount( 2 ) //some databases allow 1 arg but in these it's a synonym for trunc()
						.setParameterTypes( NUMERIC, INTEGER )
						.setInvariantType( typeConfiguration.standardBasicTypeForJavaType( Double.class ) )
						.setArgumentListSignature( "(NUMERIC number, INTEGER places)" )
						.descriptor();
				break;
			case TRUNCATE_ROUND:
				descriptor = functionRegistry.patternDescriptorBuilder( "truncate", "round(?1,?2,1)" )
						.setExactArgumentCount( 2 )
						.setParameterTypes( NUMERIC, INTEGER )
						.setInvariantType( typeConfiguration.standardBasicTypeForJavaType( Double.class ) )
						.setArgumentListSignature( "(NUMERIC number, INTEGER places)" )
						.descriptor();
				break;
			case TRUNCATE_ROUND_MODE:
				pattern = arguments.size() == 1 ? "round(?1,0,round_down)" : "round(?1,?2,round_down)";
				descriptor = functionRegistry.patternDescriptorBuilder( "trunc", pattern )
						.setExactArgumentCount( arguments.size() )
						.setParameterTypes( NUMERIC, INTEGER )
						.setReturnTypeResolver( useArgType( 1 ) )
						.setArgumentListSignature( "(NUMERIC number[, INTEGER places])" )
						.descriptor();
				break;
			default:
				throw new IllegalArgumentException( "Invalid NumericTruncType [" + numericTruncType + "]" );
		}
		return descriptor.generateSqmExpression(
				arguments,
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}
}
