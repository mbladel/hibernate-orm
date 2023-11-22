/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.util.Locale;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.EntityMutationOperationGroup;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

import static org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.standardPreparation;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * Specialized form of {@link MutationExecutorPostInsert} for cases where there
 * is only the single identity table.  Allows us to skip references to things
 * we won't need (Batch, etc)
 *
 * @todo (mutation) : look to consolidate this into/with MutationExecutorStandard
 * 		- aside from the special handling for the IDENTITY table insert,
 * 	 			the code below is the same as MutationExecutorStandard.
 * 	 	- consolidating this into MutationExecutorStandard would simplify
 * 	 			creating "single table" variations - i.e. MutationExecutorStandard and
 * 	 			StandardSingleTableExecutor.  Otherwise we'd have MutationExecutorStandard,
 * 	 			StandardSingleTableExecutor, MutationExecutorPostInsert and
 * 	 			MutationExecutorPostInsertSingleTable variants
 *
 * @author Steve Ebersole
 *
 * @deprecated This was consolidated into {@link MutationExecutorSingleNonBatched}.
 */
@Deprecated( since = "7.0", forRemoval = true )
public class MutationExecutorPostInsertSingleTable implements MutationExecutor, JdbcValueBindingsImpl.JdbcValueDescriptorAccess {
	private final EntityMutationTarget mutationTarget;
	private final SharedSessionContractImplementor session;
	private final PreparableMutationOperation operation;
	private final PreparedStatementDetails statemementDetails;

	private final JdbcValueBindingsImpl valueBindings;

	public MutationExecutorPostInsertSingleTable(
			EntityMutationOperationGroup mutationOperationGroup,
			SharedSessionContractImplementor session) {
		this.mutationTarget = mutationOperationGroup.getMutationTarget();
		this.session = session;

		assert mutationOperationGroup.getNumberOfOperations() == 1;

		this.operation = (PreparableMutationOperation) mutationOperationGroup.getOperation( mutationTarget.getIdentifierTableName() );
		this.statemementDetails = standardPreparation( operation, mutationTarget.getMutationDelegate( operation.getMutationType() ) , session );

		this.valueBindings = new JdbcValueBindingsImpl(
				operation.getMutationType(),
				mutationTarget,
				this,
				session
		);

		prepareForNonBatchedWork( null, session );
	}

	@Override
	public JdbcValueDescriptor resolveValueDescriptor(String tableName, String columnName, ParameterUsage usage) {
		assert statemementDetails.getMutatingTableDetails().getTableName().equals( tableName );
		return operation.findValueDescriptor( columnName, usage );
	}

	@Override
	public JdbcValueBindings getJdbcValueBindings() {
		return valueBindings;
	}

	@Override
	public PreparedStatementDetails getPreparedStatementDetails(String tableName) {
		if ( mutationTarget.getIdentifierTableName().equals( tableName ) ) {
			return statemementDetails;
		}

		return null;
	}

	@Override
	public Object execute(
			Object modelReference,
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session) {
		final GeneratedValuesMutationDelegate delegate = mutationTarget.getMutationDelegate( operation.getMutationType() );
		final Object generatedValues = delegate.performMutation( statemementDetails, valueBindings, modelReference, session );

		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.tracef(
					"Post-insert generated values : `%s` (%s)",
					generatedValues,
					mutationTarget.getNavigableRole().getFullPath()
			);
		}

		return generatedValues;
	}

	@Override
	public void release() {
		statemementDetails.releaseStatement( session );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"MutationExecutorPostInsertSingleTable(`%s`)",
				mutationTarget.getNavigableRole().getFullPath()
		);
	}
}
