/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.values;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.builder.AbstractTableUpdateBuilder;
import org.hibernate.sql.model.internal.TableUpdateStandard;

/**
 * @author Marco Belladelli
 */
public class TableUpdateReturningBuilder<O extends MutationOperation> extends AbstractTableUpdateBuilder<O> {
	public TableUpdateReturningBuilder(
			MutationTarget<?> mutationTarget,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, mutationTarget.getIdentifierTableMapping(), sessionFactory );
	}

	@Override
	protected EntityPersister getMutationTarget() {
		return (EntityPersister) super.getMutationTarget();
	}

	@Override @SuppressWarnings("unchecked")
	public RestrictedTableMutation<O> buildMutation() {
		final List<ColumnReference> generatedColumns = getMutationTarget().getUpdateGeneratedProperties()
				.stream().map( prop -> {
					assert prop instanceof BasicValuedModelPart : "Unsupported non-basic generated value";
					return new ColumnReference( getMutatingTable(), ( (SelectableMapping) prop ) );
				} ).toList();

		return (RestrictedTableMutation<O>) new TableUpdateStandard(
				getMutatingTable(),
				getMutationTarget(),
				getSqlComment(),
				combine( getValueBindings(), getKeyBindings(), getLobValueBindings() ),
				getKeyRestrictionBindings(),
				getOptimisticLockBindings(),
				null,
				null,
				generatedColumns
		);
	}
}
