/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * SqmPathSource implementation for entity discriminator
 *
 * @author Steve Ebersole
 */
public class EmbeddedDiscriminatorSqmPathSource<D> extends DiscriminatorSqmPathSource<D> {
	private final EmbeddableDomainType<?> embeddableDomainType;

	public EmbeddedDiscriminatorSqmPathSource(EmbeddableDomainType<D> embeddableDomainType) {
		super( embeddableDomainType );
		this.embeddableDomainType = embeddableDomainType;
	}

	@Override
	public SqmPath<D> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		final AttributeMapping attributeMapping = lhs.nodeBuilder()
				.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( lhs.findRoot().getEntityName() )
				.findAttributeMapping( lhs.getResolvedModel().getPathName() );
		assert attributeMapping instanceof EmbeddedAttributeMapping;
		return new EmbeddedDiscriminatorSqmPath<>(
				PathHelper.append( lhs, this, intermediatePathSource ),
				pathModel,
				lhs,
				embeddableDomainType,
				( (EmbeddedAttributeMapping) attributeMapping ).getMappedType().getDiscriminatorMapping(),
				lhs.nodeBuilder()
		);
	}
}
