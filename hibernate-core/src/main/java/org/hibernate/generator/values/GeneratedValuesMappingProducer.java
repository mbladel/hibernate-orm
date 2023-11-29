package org.hibernate.generator.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.JdbcValuesMappingImpl;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Marco Belladelli
 */
public class GeneratedValuesMappingProducer implements JdbcValuesMappingProducer {
	private final List<ResultBuilder> resultBuilders = new ArrayList<>();

	@Override
	public JdbcValuesMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor sessionFactory) {
		final int numberOfResults = resultBuilders.size();
		final int rowSize = jdbcResultsMetadata.getColumnCount();

		final List<SqlSelection> sqlSelections = new ArrayList<>( rowSize );
		final List<DomainResult<?>> domainResults = new ArrayList<>( numberOfResults );

		final DomainResultCreationStateImpl creationState = new DomainResultCreationStateImpl(
				null,
				jdbcResultsMetadata,
				null,
				sqlSelections::add,
				loadQueryInfluencers,
				sessionFactory
		);

		for ( int i = 0; i < numberOfResults; i++ ) {
			final ResultBuilder resultBuilder = resultBuilders.get( i );
			final DomainResult<?> domainResult = resultBuilder.buildResult(
					jdbcResultsMetadata,
					domainResults.size(),
					creationState.getLegacyFetchResolver()::resolve,
					creationState
			);

			if ( domainResult.containsAnyNonScalarResults() ) {
				creationState.disallowPositionalSelections();
			}

			domainResults.add( domainResult );
		}

		return new JdbcValuesMappingImpl(
				sqlSelections,
				domainResults,
				rowSize,
				creationState.getRegisteredLockModes()
		);
	}

	public void addResultBuilder(ResultBuilder resultBuilder) {
		resultBuilders.add( resultBuilder );
	}

	@Override
	public void addAffectedTableNames(Set<String> affectedTableNames, SessionFactoryImplementor sessionFactory) {
		// nothing to do
	}
}
