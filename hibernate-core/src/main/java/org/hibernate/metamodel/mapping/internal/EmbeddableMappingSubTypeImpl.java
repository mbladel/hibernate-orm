/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.CompositeType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Marco Belladelli
 */
public class EmbeddableMappingSubTypeImpl extends AbstractEmbeddableMapping {
	private final EmbeddableMappingType superMappingType;
	private final EmbeddableRepresentationStrategy representationStrategy;
	private final Object discriminatorValue;
	private final JavaType<?> embeddableJtd;
	private final Set<String> declaredAttributes;

	public EmbeddableMappingSubTypeImpl(
			Component bootDescriptor,
			Class<?> embeddableSubclass,
			EmbeddableMappingType superMappingType,
			RuntimeModelCreationContext creationContext) {
		super( new MutableAttributeMappingList( 5 ) );
		this.superMappingType = superMappingType;
		this.representationStrategy = creationContext
				.getBootstrapContext()
				.getRepresentationStrategySelector()
				.resolveStrategy( bootDescriptor, embeddableSubclass, () -> this, creationContext );
		this.discriminatorValue = bootDescriptor.getDiscriminatorValues().get( embeddableSubclass );
		this.embeddableJtd = representationStrategy.getMappedJavaType();
		this.declaredAttributes = new HashSet<>();
	}

	@Override
	public EmbeddableMappingType getSuperMappingType() {
		return superMappingType;
	}

	@Override
	protected void markDeclaredAttribute(String attributeName) {
		declaredAttributes.add( attributeName );
	}

	@Override
	protected boolean declaresAttribute(String attributeName) {
		return declaredAttributes.contains( attributeName );
	}

	@Override
	protected Object[] getAttributeValues(Object compositeInstance) {
		final Object[] results = new Object[getNumberOfAttributeMappings()];
		for ( int i = 0; i < getNumberOfAttributeMappings(); i++ ) {
			final AttributeMapping attributeMapping = getAttributeMapping( i );
			if ( declaredAttributes.contains( attributeMapping.getAttributeName() ) ) {
				final Getter getter = attributeMapping.getAttributeMetadata()
						.getPropertyAccess()
						.getGetter();
				results[i] = getter.get( compositeInstance );
			}
			else {
				results[i] = null;
			}
		}
		return results;
	}

	@Override
	protected void setAttributeValues(Object component, Object[] values) {
		for ( int i = 0; i < values.length; i++ ) {
			final AttributeMapping attributeMapping = getAttributeMapping( i );
			if ( declaredAttributes.contains( attributeMapping.getAttributeName() ) ) {
				attributeMapping.getPropertyAccess().getSetter().set( component, values[i] );
			}
			else {
				assert values[i] == null : "Unexpected non-null value for embeddable type " + getJavaType().getJavaTypeClass();
			}
		}
	}

	@Override
	public <X, Y> int decompose(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		// todo marco : support inheritance for aggregates?
		if ( shouldBindAggregateMapping() ) {
			valueConsumer.consume( offset, x, y, domainValue, getAggregateMapping() );
			return 1;
		}
		int span = 0;
		if ( domainValue instanceof Object[] ) {
			final Object[] values = (Object[]) domainValue;
			assert values.length == attributeMappings.size();

			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				if ( declaredAttributes.contains( attributeMapping.getAttributeName() ) ) {
					final Object attributeValue = values[i];
					span += attributeMapping.decompose( attributeValue, offset + span, x, y, valueConsumer, session );
				}
			}
		}
		else {
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				if ( declaredAttributes.contains( attributeMapping.getAttributeName() ) &&
						!( attributeMapping instanceof PluralAttributeMapping ) ) {
					final Object attributeValue = domainValue == null
							? null
							: attributeMapping.getPropertyAccess().getGetter().get( domainValue );
					span += attributeMapping.decompose( attributeValue, offset + span, x, y, valueConsumer, session );
				}
			}
			span += getDiscriminatorMapping().decompose( discriminatorValue, offset + span, x, y, valueConsumer, session );
		}
		return span;
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return embeddableJtd;
	}

	@Override
	public EntityDiscriminatorMapping getDiscriminatorMapping() {
		return superMappingType.getDiscriminatorMapping();
	}

	@Override
	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return superMappingType.getEmbeddedValueMapping();
	}

	@Override
	public EmbeddableRepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Override
	public boolean isCreateEmptyCompositesEnabled() {
		return superMappingType.isCreateEmptyCompositesEnabled();
	}

	@Override
	public SelectableMapping getAggregateMapping() {
		return superMappingType.getAggregateMapping();
	}

	@Override
	public EmbeddableMappingType createInverseMappingType(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			MappingModelCreationProcess creationProcess) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return superMappingType.getNavigableRole(); // todo marco : this ok?
	}

	@Override
	public String getPartName() {
		return superMappingType.getPartName();
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		// todo marco : this will probably need to change, no?
		return superMappingType.createDomainResult( navigablePath, tableGroup, resultVariable, creationState );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return superMappingType.findContainingEntityMapping();
	}
}
