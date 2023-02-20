/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Custom {@link TruncFunction} for PostgreSQL which uses the dialect-specific function for numeric truncation
 *
 * @author Marco Belladelli
 */
public class PostgreSQLTruncFunction extends TruncFunction {
	private final boolean supportsTwoArguments;

	public PostgreSQLTruncFunction(boolean supportsTwoArguments) {
		super(
				"trunc(?1)",
				null,
				DatetimeTrunc.DATE_TRUNC,
				null
		);
		this.supportsTwoArguments = supportsTwoArguments;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		final List<SqmTypedNode<?>> args = new ArrayList<>( arguments );
		if ( arguments.size() == 2 && arguments.get( 1 ) instanceof SqmExtractUnit ) {
			// datetime truncation
			numericTrunc = false;
		}
		else {
			// numeric truncation
			return new PostgreSQLTruncRoundFunction( "trunc", supportsTwoArguments ).generateSqmFunctionExpression(
					arguments,
					impliedResultType,
					queryEngine,
					typeConfiguration
			);
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
}
