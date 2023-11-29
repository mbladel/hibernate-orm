/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.values;

import java.util.function.Consumer;

import org.hibernate.generator.EventType;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

import static org.hibernate.generator.values.GeneratedValuesHelper.createMappingProducer;

/**
 * @author Marco Belladelli
 */
public abstract class AbstractGeneratedValuesMutationDelegate implements GeneratedValuesMutationDelegate {
	private final PostInsertIdentityPersister persister;
	private final EventType timing;

	public AbstractGeneratedValuesMutationDelegate(PostInsertIdentityPersister persister, EventType timing) {
		this.persister = persister;
		this.timing = timing;
	}

	public PostInsertIdentityPersister getPersister() {
		return persister;
	}

	@Override
	public EventType getTiming() {
		return timing;
	}

	protected JdbcValuesMappingProducer getMappingProducer(Consumer<String> columnNameConsumer) {
		return createMappingProducer(
				persister,
				timing,
				supportsArbitraryValues(),
				supportsRowId(),
				columnNameConsumer
		);
	}
}
