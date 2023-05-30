/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree;

import java.util.List;

import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * Specialization of Statement for mutation (DML) statements
 *
 * @author Steve Ebersole
 */
public interface MutationStatement extends Statement {
	default TableGroup getTargetTableGroup() {
		return null;
	}

	NamedTableReference getTargetTable();
	List<ColumnReference> getReturningColumns();
}
