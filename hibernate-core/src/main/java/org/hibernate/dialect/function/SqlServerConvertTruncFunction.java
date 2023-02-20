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
 * Custom {@link TruncFunction} for SQL Server versions < 16 which uses the custom {@link DateTruncConvertEmulation}
 *
 * @author Marco Belladelli
 */
public class SqlServerConvertTruncFunction extends TruncFunction {
	public SqlServerConvertTruncFunction() {
		super(
				"round(?1,0,1)",
				"round(?1,?2,1)",
				null,
				null
		);
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
			return new DateTruncConvertEmulation( typeConfiguration ).generateSqmExpression(
					arguments,
					impliedResultType,
					queryEngine,
					typeConfiguration
			);
		}
		else {
			// numeric truncation
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
}
