/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.internal;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.ast.AbstractTableInsert;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.MutatingTableReference;

/**
 * @author Steve Ebersole
 */
public class TableInsertStandard extends AbstractTableInsert {
	private final List<ColumnReference> returningColumns;

	public TableInsertStandard(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			List<ColumnValueBinding> valueBindings,
			List<ColumnReference> returningColumns,
			List<ColumnValueParameter> parameters) {
		super( mutatingTable, mutationTarget, parameters, valueBindings );
		this.returningColumns = returningColumns;
	}

	@Override
	public boolean isCustomSql() {
		return false;
	}

	@Override
	public List<ColumnReference> getReturningColumns() {
		return returningColumns;
	}

	@Override
	public void forEachReturningColumn(BiConsumer<Integer,ColumnReference> consumer) {
		forEachThing( returningColumns, consumer );
	}

	@Override
	public Expectation getExpectation() {
		// todo marco : maybe this is too much ?
		if ( !returningColumns.isEmpty() ) {
			return new Expectation.None();
		}
		return super.getExpectation();
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitStandardTableInsert( this );
	}
}
