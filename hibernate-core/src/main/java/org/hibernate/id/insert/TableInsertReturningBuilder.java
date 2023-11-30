/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.id.insert;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.sql.model.ast.builder.AbstractTableInsertBuilder;
import org.hibernate.sql.model.internal.TableInsertStandard;

/**
 * @author Steve Ebersole
 */
public class TableInsertReturningBuilder extends AbstractTableInsertBuilder {
	public TableInsertReturningBuilder(
			EntityPersister mutationTarget,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, mutationTarget.getIdentifierTableMapping(), sessionFactory );
	}

	@Override
	protected EntityPersister getMutationTarget() {
		return (EntityPersister) super.getMutationTarget();
	}

	@Override
	public TableInsert buildMutation() {
		final List<? extends ModelPart> insertGeneratedProperties = getMutationTarget().getInsertGeneratedProperties();
		final List<ColumnReference> generatedColumns = insertGeneratedProperties.stream().map( prop -> {
			assert prop instanceof BasicValuedModelPart : "Unsupported non-basic generated value";
			return new ColumnReference( getMutatingTable(), ( (BasicValuedModelPart) prop ) );
		} ).collect( Collectors.toList() );

		// special case for rowid when the dialect supports it
		final EntityRowIdMapping rowIdMapping = getMutationTarget().getRowIdMapping();
		if ( rowIdMapping != null && getJdbcServices().getDialect().supportsInsertReturningRowId() ) {
			generatedColumns.add( new ColumnReference( getMutatingTable(), rowIdMapping ) );
		}

		return new TableInsertStandard(
				getMutatingTable(),
				getMutationTarget(),
				combine( getValueBindingList(), getKeyBindingList(), getLobValueBindingList() ),
				generatedColumns,
				getParameters()
		);
	}
}
