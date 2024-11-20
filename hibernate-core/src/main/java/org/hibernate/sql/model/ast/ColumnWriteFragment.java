/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Models a column's value expression within the SQL AST. Used to model:<ul>
 *     <li>a column's new value in a SET clause</li>
 *     <li>a column's new value in a SET clause</li>
 *     <li>a column's old value in a restriction (optimistic locking)</li>
 * </ul>
 *
 * @see ColumnTransformer#write()
 *
 * @author Steve Ebersole
 */
public class ColumnWriteFragment implements Expression {
	private final String fragment;
	private final List<ColumnValueParameter> parameters;
	private final JdbcMapping jdbcMapping;

	public ColumnWriteFragment(String fragment, JdbcMapping jdbcMapping) {
		this( fragment, Collections.emptyList(), jdbcMapping );
	}

	public ColumnWriteFragment(String fragment, ColumnValueParameter parameter, JdbcMapping jdbcMapping) {
		this( fragment, Collections.singletonList( parameter ), jdbcMapping );
		assert !fragment.contains( "?" ) || parameter != null;
	}

	public ColumnWriteFragment(String fragment, List<ColumnValueParameter> parameters, JdbcMapping jdbcMapping) {
		this.fragment = fragment;
		this.parameters = parameters;
		this.jdbcMapping = jdbcMapping;
	}

	public String getFragment() {
		return fragment;
	}

	public Collection<ColumnValueParameter> getParameters() {
		return parameters;
	}

	@Override
	public @Nullable ColumnReference getColumnReference() {
		return parameters.size() == 1 ? parameters.get( 0 ).getColumnReference() : null;
	}

	@Override
	public JdbcMapping getExpressionType() {
		return jdbcMapping;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitColumnWriteFragment( this );
	}

	@Override
	public String toString() {
		return switch ( parameters.size() ) {
			case 0 -> String.format(
					Locale.ROOT,
					"ColumnWriteFragment(%s)@%s",
					fragment,
					hashCode()
			);
			case 1 -> String.format(
					Locale.ROOT,
					"ColumnWriteFragment(%s = %s (%s))@%s",
					parameters.get( 0 ).getColumnReference().getColumnExpression(),
					fragment,
					parameters.get( 0 ).getUsage(),
					hashCode()
			);
			default -> String.format(
					Locale.ROOT,
					"ColumnWriteFragment(%s = %s (%s))@%s",
					parameters,
					fragment,
					parameters.get( 0 ).getUsage(),
					hashCode()
			);
		};
	}
}
