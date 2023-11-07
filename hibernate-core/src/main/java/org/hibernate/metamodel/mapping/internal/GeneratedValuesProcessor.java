/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.values.GeneratedValuesImpl;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.insert.BasicSelectingDelegate;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.loader.ast.internal.NoCallbackExecutionContext;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;

import static org.hibernate.sql.results.spi.ListResultsConsumer.UniqueSemantic.FILTER;

/**
 * Responsible for retrieving {@linkplain OnExecutionGenerator database-generated}
 * attribute values after an {@code insert} or {@code update} statement is executed.
 * <p>
 * Note that this class has responsibility for regular attributes of the entity. The
 * primary key / id attribute is handled separately, being the responsibility of an
 * instance of {@link org.hibernate.id.insert.InsertGeneratedIdentifierDelegate}.
 *
 * @see OnExecutionGenerator
 *
 * @author Steve Ebersole
 */
@Incubating
public class GeneratedValuesProcessor {
	private final SelectStatement selectStatement;
	private final JdbcOperationQuerySelect jdbcSelect;
	private final List<AttributeMapping> generatedValuesToSelect;
	private final JdbcParametersList jdbcParameters;

	private final EntityMappingType entityDescriptor;

	public GeneratedValuesProcessor(
			EntityMappingType entityDescriptor,
			List<AttributeMapping> generatedProperties,
			EventType timing,
			SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;

		generatedValuesToSelect = generatedProperties;
		if ( generatedProperties.isEmpty() || !needsSubsequentSelect( timing ) ) {
			selectStatement = null;
			jdbcSelect = null;
			jdbcParameters = null;
		}
		else {
			final JdbcParametersList.Builder builder = JdbcParametersList.newBuilder();

			selectStatement = LoaderSelectBuilder.createSelect(
					entityDescriptor,
					generatedProperties,
					entityDescriptor.getIdentifierMapping(),
					null,
					1,
					new LoadQueryInfluencers( sessionFactory ),
					LockOptions.READ,
					builder::add,
					sessionFactory
			);
			jdbcSelect = sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
							.buildSelectTranslator( sessionFactory, selectStatement )
							.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );
			jdbcParameters = builder.build();
		}
	}

	private boolean needsSubsequentSelect(EventType timing) {
		if ( timing == EventType.INSERT && ( (EntityMutationTarget) entityDescriptor ).getIdentityInsertDelegate() != null ) {
			// todo marco : we should avoid this cast here, deprecate method and move it to EntityPersister ?
			return !( (EntityMutationTarget) entityDescriptor ).getIdentityInsertDelegate().supportsRetrievingGeneratedValues();
		}
		return true;
	}

	/**
	 * Find attributes generated by an {@link OnExecutionGenerator}.
	 *
	 * @return a list of {@link AttributeMapping}s.
	 */
	public static List<AttributeMapping> getGeneratedAttributes(EntityMappingType entityDescriptor, EventType timing) {
		// todo (6.0): For now, we rely on the entity metamodel as composite attributes report
		//             GenerationTiming.NEVER even if they have attributes that would need generation
		final Generator[] generators = entityDescriptor.getEntityPersister().getEntityMetamodel().getGenerators();
		final List<AttributeMapping> generatedValuesToSelect = new ArrayList<>();
		entityDescriptor.forEachAttributeMapping( mapping -> {
			final Generator generator = generators[ mapping.getStateArrayPosition() ];
			if ( generator != null
					&& generator.generatedOnExecution()
					&& generator.getEventTypes().contains(timing) ) {
				generatedValuesToSelect.add( mapping );
			}
		} );
		return generatedValuesToSelect;
	}

	/**
	 * Obtain the generated values, and populate the snapshot and the fields of the entity instance.
	 */
	public void processGeneratedValues(
			Object entity,
			Object id,
			Object[] state,
			GeneratedValuesImpl generatedValues,
			SharedSessionContractImplementor session) {
		if ( hasActualGeneratedValuesToSelect( session, entity ) ) {
			if ( selectStatement != null ) {
				final List<Object[]> results = executeSelect( id, session );
				assert results.size() == 1;
				setEntityAttributes( entity, state, results.get( 0 ) );
			}
			else if ( generatedValues != null ) {
				setEntityAttributes( entity, state, generatedValues.getGeneratedValues( generatedValuesToSelect ) );
			}
		}
	}

	private boolean hasActualGeneratedValuesToSelect(SharedSessionContractImplementor session, Object entity) {
		for ( AttributeMapping attributeMapping : generatedValuesToSelect ) {
			if ( attributeMapping.getGenerator().generatedOnExecution( entity, session ) ) {
				return true;
			}
		}
		return false;
	}

	private List<Object[]> executeSelect(Object id, SharedSessionContractImplementor session) {
		final JdbcParameterBindings jdbcParamBindings = getJdbcParameterBindings( id, session );
		return session.getFactory().getJdbcServices().getJdbcSelectExecutor()
				.list( jdbcSelect, jdbcParamBindings, new NoCallbackExecutionContext(session), (row) -> row, FILTER );
	}

	private JdbcParameterBindings getJdbcParameterBindings(Object id, SharedSessionContractImplementor session) {
		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		int offset = jdbcParamBindings.registerParametersForEachJdbcValue(
				id,
				entityDescriptor.getIdentifierMapping(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();
		return jdbcParamBindings;
	}

	private void setEntityAttributes(Object entity, Object[] state, Object[] selectionResults) {
		for ( int i = 0; i < generatedValuesToSelect.size(); i++ ) {
			final AttributeMapping attribute = generatedValuesToSelect.get( i );
			final Object generatedValue = selectionResults[i];
			state[ attribute.getStateArrayPosition() ] = generatedValue;
			attribute.getAttributeMetadata().getPropertyAccess().getSetter().set( entity, generatedValue );
		}
	}

	public SelectStatement getSelectStatement() {
		return selectStatement;
	}

	public List<AttributeMapping> getGeneratedValuesToSelect() {
		return generatedValuesToSelect;
	}

	public JdbcParametersList getJdbcParameters() {
		return jdbcParameters;
	}

	public EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}
}
