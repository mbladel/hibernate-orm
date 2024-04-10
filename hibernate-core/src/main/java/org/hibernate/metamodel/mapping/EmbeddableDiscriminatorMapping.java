/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.HibernateException;
import org.hibernate.engine.FetchTiming;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;

/**
 * Details about the discriminator for an embeddable hierarchy.
 *
 * @author Marco Belladelli
 * @see EmbeddableMappingType#getDiscriminatorMapping()
 * @see EmbeddableDiscriminatorConverter
 */
public interface EmbeddableDiscriminatorMapping extends DiscriminatorMapping, FetchOptions {
	default EmbeddableDiscriminatorConverter<?, ?> getEmbeddableValueConverter() {
		return (EmbeddableDiscriminatorConverter<?, ?>) getValueConverter();
	}

	/**
	 * Retrieve the {@linkplain EmbeddableDiscriminatorValueDetails details} for a particular discriminator value.
	 * @throws HibernateException if there is no match
	 */
	default EmbeddableDiscriminatorValueDetails resolveEmbeddableDiscriminatorValue(Object value) {
		return getEmbeddableValueConverter().getDetailsForDiscriminatorValue( value );
	}

	default Object getDiscriminatorValue(String embeddableClassName) {
		return getEmbeddableValueConverter().getDetailsForEntityName( embeddableClassName ).getValue();
	}

	@Override
	BasicFetch<?> generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState);
}
