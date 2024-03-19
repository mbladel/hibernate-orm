/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static java.util.Collections.singletonList;

/**
 * Utility class that caches the query needed to read the discriminator value
 * associated with the provided {@link EntityPersister} and returns the
 * resolved concrete entity type.
 *
 * @author Marco Belladelli
 * @see org.hibernate.annotations.ConcreteType
 */
public class EntityConcreteTypeLoader {
	private final EntityPersister entityPersister;
	private final SelectStatement sqlSelect;
	private final JdbcParametersList jdbcParameters;

	public EntityConcreteTypeLoader(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		this.entityPersister = entityPersister;
		final EntityDiscriminatorMapping discriminatorMapping = entityPersister.getDiscriminatorMapping();
		final JdbcParametersList.Builder jdbcParametersBuilder = JdbcParametersList.newBuilder();
		sqlSelect = LoaderSelectBuilder.createSelect(
				entityPersister,
				singletonList( discriminatorMapping ),
				entityPersister.getIdentifierMapping(),
				null,
				1,
				new LoadQueryInfluencers( sessionFactory ),
				LockOptions.NONE,
				jdbcParametersBuilder::add,
				sessionFactory
		);
		jdbcParameters = jdbcParametersBuilder.build();
	}

	public EntityPersister getConcreteType(Object id, SessionImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getSessionFactory();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = sessionFactory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory();

		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		int offset = jdbcParamBindings.registerParametersForEachJdbcValue(
				id,
				entityPersister.getIdentifierMapping(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();
		final JdbcOperationQuerySelect jdbcSelect =
				sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, sqlSelect )
						.translate( jdbcParamBindings, QueryOptions.NONE );

		final List<Object> results = session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParamBindings,
				new BaseExecutionContext( session ),
				RowTransformerStandardImpl.instance(),
				ListResultsConsumer.UniqueSemantic.NONE
		);
		if ( results.isEmpty() ) {
			throw new ObjectNotFoundException( entityPersister.getEntityName(), id );
		}
		else {
			assert results.size() == 1;
			final Class<?> subtype = (Class<?>) results.get( 0 );
			final EntityPersister entityDescriptor = sessionFactory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( subtype );
			assert entityDescriptor.isTypeOrSuperType( entityPersister );
			return entityDescriptor;
		}
	}
}
