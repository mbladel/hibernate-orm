/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.id.insert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.sql.model.ast.builder.AbstractTableInsertBuilder;
import org.hibernate.sql.model.internal.TableInsertStandard;

/**
 * @author Steve Ebersole
 */
public class TableInsertReturningBuilder extends AbstractTableInsertBuilder {
	public TableInsertReturningBuilder(
			PostInsertIdentityPersister mutationTarget,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, mutationTarget.getIdentifierTableMapping(), sessionFactory );
	}

	@Override
	protected PostInsertIdentityPersister getMutationTarget() {
		return (PostInsertIdentityPersister) super.getMutationTarget();
	}

	@Override
	public TableInsert buildMutation() {
		final List<? extends ValuedModelPart> insertGeneratedProperties = getMutationTarget().getInsertGeneratedProperties();
		final List<ColumnReference> generatedColumns = insertGeneratedProperties.stream().map( prop -> {
			assert prop instanceof BasicValuedModelPart; // todo marco : handle this differently
			return new ColumnReference( getMutatingTable(), ( (BasicValuedModelPart) prop ) );
		} ).collect( Collectors.toList() );

		return new TableInsertStandard(
				getMutatingTable(),
				getMutationTarget(),
				combine( getValueBindingList(), getKeyBindingList(), getLobValueBindingList() ),
				generatedColumns,
				getParameters()
		);
	}
}
