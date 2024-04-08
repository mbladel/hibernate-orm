/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;

/**
 * Details about the discriminator for an embeddable hierarchy
 *
 * @see jakarta.persistence.DiscriminatorColumn
 * @see jakarta.persistence.DiscriminatorValue
 *
 * @author Marco Belladelli
 */
public interface EmbeddableDiscriminatorMapping extends DiscriminatorMapping, FetchOptions {
	default EmbeddableDiscriminatorConverter<?,?> getEmbeddableValueConverter() {
		return (EmbeddableDiscriminatorConverter<?, ?>) getValueConverter();
	}

	/**
	 * Retrieve the details for a particular discriminator value.
	 * <p>
	 * Returns {@code null} if there is no match.
	 */
	EmbeddableDiscriminatorValueDetails resolveEmbeddableDiscriminatorValue(Object value);

	/**
	 * Retrieve a {@link Set} of all subclasses represented by this discriminator mapping.
	 */
	Set<Class<?>> getEmbeddableClasses();

	default Object getDiscriminatorValue(Class<?> embeddableClass) {
		return getEmbeddableValueConverter().getDetailsForEmbeddableClass( embeddableClass ).getValue();
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
