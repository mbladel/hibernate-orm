/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.lang.reflect.Array;

import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * RowTransformer used when an array is explicitly specified as the return type
 *
 * @author Steve Ebersole
 */
public class RowTransformerArrayImpl<R> implements RowTransformer<R[]> {
	private final Class<R> componentType;

	public static RowTransformerArrayImpl<Object> instance() {
		return instance( Object.class );
	}

	public static <R> RowTransformerArrayImpl<R> instance(Class<R> elementType) {
		return new RowTransformerArrayImpl<>( elementType );
	}

	public RowTransformerArrayImpl(Class<R> componentType) {
		this.componentType = componentType;
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public R[] transformRow(Object[] row) {
		if ( componentType == Object.class ) {
			return (R[]) row;
		}

		final R[] result = (R[]) Array.newInstance( componentType, row.length );
		for ( int i = 0; i < row.length; i++ ) {
			if ( row[i] == null || componentType.isInstance( row[i] ) ) {
				result[i] = (R) row[i];
			}
			else {
				throw new QueryTypeMismatchException(
						"Result type is array of '" + componentType.getSimpleName() +
								"' but the query returned a '" + row[i].getClass().getSimpleName() + "'"
				);
			}
		}
		return result;
	}
}
