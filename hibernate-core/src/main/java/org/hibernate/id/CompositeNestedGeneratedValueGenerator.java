/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.factory.spi.StandardGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import static org.hibernate.generator.EventTypeSets.INSERT_ONLY;

/**
 * For composite identifiers, defines a number of "nested" generations that
 * need to happen to "fill" the identifier property(s).
 * <p>
 * This generator is used implicitly for all composite identifier scenarios if an
 * explicit generator is not in place.  So it make sense to discuss the various 
 * potential scenarios:<ul>
 * <li>
 * <i>"embedded" composite identifier</i> - this is possible only in HBM mappings
 * as {@code <composite-id/>} (notice the lack of both a name and class attribute
 * declarations).  The term {@link org.hibernate.mapping.Component#isEmbedded() "embedded"}
 * here refers to the Hibernate usage which is actually the exact opposite of the JPA
 * meaning of "embedded".  Essentially this means that the entity class itself holds
 * the named composite pk properties.  This is very similar to the JPA {@code @IdClass}
 * usage, though without a separate pk-class for loading.
 * </li>
 * <li>
 * <i>pk-class as entity attribute</i> - this is possible in both annotations ({@code @EmbeddedId})
 * and HBM mappings ({@code <composite-id name="idAttributeName" class="PkClassName"/>})
 * </li>
 * <li>
 * <i>"embedded" composite identifier with a pk-class</i> - this is the JPA {@code @IdClass} use case
 * and is only possible in annotations
 * </li>
 * </ul>
 * <p>
 * Most of the grunt work is done in {@link org.hibernate.mapping.Component}.
 *
 * @author Steve Ebersole
 */
@Internal
public class CompositeNestedGeneratedValueGenerator
		implements IdentifierGenerator, PostInsertIdentifierGenerator, StandardGenerator, IdentifierGeneratorAggregator, Serializable {
	/**
	 * Contract for declaring how to locate the context for sub-value injection.
	 */
	public interface GenerationContextLocator {
		/**
		 * Given the incoming object, determine the context for injecting back its generated
		 * id sub-values.
		 *
		 * @param session The current session
		 * @param incomingObject The entity for which we are generating id
		 *
		 * @return The injection context
		 */
		Object locateGenerationContext(SharedSessionContractImplementor session, Object incomingObject);
	}

	/**
	 * Contract for performing the actual sub-value generation, usually injecting it into the
	 * determined {@linkplain GenerationContextLocator#locateGenerationContext context}
	 */
	public interface GenerationPlan extends ExportableProducer {

		/**
		 * Initializes this instance, in particular pre-generates SQL as necessary.
		 * <p>
		 * This method is called after {@link #registerExportables(Database)}, before first use.
		 *
		 * @param context A context to help generate SQL strings
		 */
		void initialize(SqlStringGenerationContext context);

		/**
		 * Execute the value generation.
		 *
		 * @param session The current session
		 * @param incomingObject The entity for which we are generating id
		 * @param injectionContext The context into which the generated value can be injected
		 */
		void execute(SharedSessionContractImplementor session, Object incomingObject, Object injectionContext);

		/**
		 * Retrieves the underlying {@link Generator}
		 */
		Generator getSubgenerator();
	}

	private final GenerationContextLocator generationContextLocator;
	private final List<GenerationPlan> generationPlans = new ArrayList<>();

	public CompositeNestedGeneratedValueGenerator(GenerationContextLocator generationContextLocator) {
		this.generationContextLocator = generationContextLocator;
	}

	public void addGeneratedValuePlan(GenerationPlan plan) {
		generationPlans.add( plan );
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		final Object context = generationContextLocator.locateGenerationContext( session, object );

		for ( GenerationPlan generationPlan : generationPlans ) {
			generationPlan.execute( session, object, context );
		}

		return context;
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return INSERT_ONLY;
	}

	@Override
	public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry) {
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return getOnExecutionSubgenerators().stream()
				.anyMatch( generator -> generator.referenceColumnsInSql( dialect ) );
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		final List<OnExecutionGenerator> subgenerators = getOnExecutionSubgenerators();
		assert !subgenerators.isEmpty();

		final List<String> referencedColumnValues = new ArrayList<>( subgenerators.size() );
		for ( OnExecutionGenerator subgenerator : subgenerators ) {
			referencedColumnValues.addAll( List.of( subgenerator.getReferencedColumnValues( dialect ) ) );
		}
		return referencedColumnValues.toArray( new String[0] );
	}

	@Override
	public boolean generatedOnExecution() {
		return !getOnExecutionSubgenerators().isEmpty();
	}

	private List<OnExecutionGenerator> getOnExecutionSubgenerators() {
		return generationPlans.stream()
				.map( GenerationPlan::getSubgenerator )
				.filter( Generator::generatedOnExecution )
				.map( OnExecutionGenerator.class::cast )
				.collect( Collectors.toList() );
	}

	@Override
	public void registerExportables(Database database) {
		for ( GenerationPlan plan : generationPlans ) {
			plan.registerExportables( database );
		}
	}

	@Override
	public void initialize(SqlStringGenerationContext context) {
		for ( GenerationPlan plan : generationPlans ) {
			plan.initialize( context );
		}
	}
}
