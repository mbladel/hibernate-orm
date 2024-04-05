/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.mapping;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Marco Belladelli
 */
public class EmbeddableDiscriminatorConverter<O, R> extends DiscriminatorConverter<O, R> {
	// todo marco : this is a very rough and quick draft, clean this up
	public static class EmbeddableDiscriminatorValueDetails implements DiscriminatorValueDetails {
		final Object value;
		final Class<?> embeddableClass;

		public EmbeddableDiscriminatorValueDetails(Object value, Class<?> embeddableClass) {
			this.value = value;
			this.embeddableClass = embeddableClass;
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public String getIndicatedEntityName() {
			return embeddableClass.getName();
		}

		@Override
		public EntityMappingType getIndicatedEntity() {
			throw new UnsupportedOperationException();
		}

		public Class<?> getEmbeddableClass() {
			return embeddableClass;
		}
	}

	public static <O, R> EmbeddableDiscriminatorConverter<O, R> fromValueMappings(
			NavigableRole role,
			JavaType<O> domainJavaType,
			BasicType<R> underlyingJdbcMapping,
			Map<Object, Class<?>> valueMappings) {
		final List<EmbeddableDiscriminatorValueDetails> valueDetailsList = CollectionHelper.arrayList( valueMappings.size() );
		valueMappings.forEach( (value, embeddableClass) -> {
			final EmbeddableDiscriminatorValueDetails valueDetails = new EmbeddableDiscriminatorValueDetails(
					value,
					embeddableClass
			);
			valueDetailsList.add( valueDetails );
		} );

		return new EmbeddableDiscriminatorConverter<>(
				role,
				domainJavaType,
				underlyingJdbcMapping.getJavaTypeDescriptor(),
				valueDetailsList
		);
	}

	private final Map<Object, EmbeddableDiscriminatorValueDetails> discriminatorValueToDetailsMap;
	private final Map<Class<?>, EmbeddableDiscriminatorValueDetails> embeddableClassToDetailsMap;

	public EmbeddableDiscriminatorConverter(
			NavigableRole discriminatorRole,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType,
			List<EmbeddableDiscriminatorValueDetails> valueMappings) {
		super( discriminatorRole, domainJavaType, relationalJavaType );

		this.discriminatorValueToDetailsMap = CollectionHelper.concurrentMap( valueMappings.size() );
		this.embeddableClassToDetailsMap = CollectionHelper.concurrentMap( valueMappings.size() );
		valueMappings.forEach( (valueDetails) -> {
			discriminatorValueToDetailsMap.put( valueDetails.getValue(), valueDetails );
			embeddableClassToDetailsMap.put( valueDetails.getEmbeddableClass(), valueDetails );
		} );
	}

	@Override
	public O toDomainValue(R relationalForm) {
		assert relationalForm == null || getRelationalJavaType().isInstance( relationalForm );

		final EmbeddableDiscriminatorValueDetails matchingValueDetails = getDetailsForDiscriminatorValue( relationalForm );
		if ( matchingValueDetails == null ) {
			throw new IllegalStateException( "Could not resolve discriminator value" );
		}

		//noinspection unchecked
		return (O) matchingValueDetails.getEmbeddableClass();
	}

	@Override
	public R toRelationalValue(O domainForm) {
		assert domainForm == null || domainForm instanceof Class;

		if ( domainForm == null ) {
			return null;
		}

		final Class<?> embeddableClass = (Class<?>) domainForm;

		final EmbeddableDiscriminatorValueDetails discriminatorValueDetails = getDetailsForEmbeddableClass( embeddableClass );
		//noinspection unchecked
		return (R) discriminatorValueDetails.getValue();
	}

	public EmbeddableDiscriminatorValueDetails getDetailsForEmbeddableClass(Class<?> embeddableClass) {
		EmbeddableDiscriminatorValueDetails valueDetails = embeddableClassToDetailsMap.get( embeddableClass );
		if ( valueDetails!= null) {
			return valueDetails;
		}

		throw new AssertionFailure( "Unrecognized embeddable class: " + embeddableClass );
	}

	@Override
	public EmbeddableDiscriminatorValueDetails getDetailsForDiscriminatorValue(Object value) {
		final EmbeddableDiscriminatorValueDetails valueMatch = discriminatorValueToDetailsMap.get( value );
		if ( valueMatch != null ) {
			return valueMatch;
		}

		throw new HibernateException( "Unrecognized discriminator value: " + value );
	}

	@Override
	public DiscriminatorValueDetails getDetailsForEntityName(String entityName) {
		for ( Map.Entry<Class<?>, EmbeddableDiscriminatorValueDetails> entry : embeddableClassToDetailsMap.entrySet() ) {
			if ( entry.getKey().getName().equals( entityName )) {
				return entry.getValue();
			}
		}

		throw new AssertionFailure( "Unrecognized embeddable class name: " + entityName );
	}

	@Override
	public void forEachValueDetail(Consumer<DiscriminatorValueDetails> consumer) {
		discriminatorValueToDetailsMap.forEach( (value, detail) -> consumer.accept( detail ) );
	}

	@Override
	public <X> X fromValueDetails(Function<DiscriminatorValueDetails,X> handler) {
		for ( DiscriminatorValueDetails detail : discriminatorValueToDetailsMap.values() ) {
			final X result = handler.apply( detail );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}
}
