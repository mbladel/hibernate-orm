/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.mapping.EmbeddableDiscriminatorMapping;
import org.hibernate.metamodel.model.domain.DiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.AbstractSqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.BasicType;

/**
 * {@link SqmPath} specialization for an embeddable discriminator
 *
 * @author Marco Belladelli
 */
public class EmbeddedDiscriminatorSqmPath<T> extends AbstractSqmPath<T> implements DiscriminatorSqmPath<T> {
	private final EmbeddableDomainType<T> embeddableDomainType;
	private final EmbeddableDiscriminatorMapping discriminator;

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	protected EmbeddedDiscriminatorSqmPath(
			NavigablePath navigablePath,
			SqmPathSource referencedPathSource,
			SqmPath<?> lhs,
			EmbeddableDomainType embeddableDomainType,
			EmbeddableDiscriminatorMapping discriminator,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );
		this.embeddableDomainType = embeddableDomainType;
		this.discriminator = discriminator;
	}

	public EmbeddableDomainType<T> getEmbeddableDomainType() {
		return embeddableDomainType;
	}

	public EmbeddableDiscriminatorMapping getDiscriminator() {
		return discriminator;
	}

	@Override
	public BasicType<T> getExpressible() {
		//noinspection unchecked
		return (BasicType<T>) discriminator.getJdbcMapping();
	}

	@Override
	public EmbeddedDiscriminatorSqmPath<T> copy(SqmCopyContext context) {
		final EmbeddedDiscriminatorSqmPath<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		//noinspection unchecked
		return context.registerCopy(
				this,
				(EmbeddedDiscriminatorSqmPath<T>) getLhs().copy( context ).type()
		);
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		assert embeddableDomainType.isPolymorphic();
		return walker.visitDiscriminatorPath( this );
	}
}
