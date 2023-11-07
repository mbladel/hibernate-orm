/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.values;

import java.util.function.Consumer;

import org.hibernate.generator.EventType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

import static org.hibernate.generator.values.internal.GeneratedValuesHelper.createMappingProducer;

/**
 * @author Marco Belladelli
 */
public abstract class AbstractGeneratedValuesMutationDelegate implements GeneratedValuesMutationDelegate {
	protected final EntityPersister persister;
	private final EventType timing;

	public AbstractGeneratedValuesMutationDelegate(EntityPersister persister, EventType timing) {
		this.persister = persister;
		this.timing = timing;
	}

	@Override
	public EventType getTiming() {
		return timing;
	}

	protected JdbcValuesMappingProducer getMappingProducer(Consumer<ModelPart> modelPartConsumer) {
		return getMappingProducer( modelPartConsumer, true );
	}

	protected JdbcValuesMappingProducer getMappingProducer(Consumer<ModelPart> modelPartConsumer, boolean useIndex) {
		return createMappingProducer(
				persister,
				timing,
				supportsArbitraryValues(),
				supportsRowId(),
				useIndex,
				modelPartConsumer
		);
	}
}
