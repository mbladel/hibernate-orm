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
 * Handles conversion of discriminator values for embeddable subtype classes
 * to their domain typed form.
 *
 * @author Marco Belladelli
 * @see EmbeddableDiscriminatorMapping
 */
public class EmbeddableDiscriminatorConverter<O, R> extends DiscriminatorConverter<O, R> {
	public static <O, R> EmbeddableDiscriminatorConverter<O, R> fromValueMappings(
			NavigableRole role,
			JavaType<O> domainJavaType,
			BasicType<R> underlyingJdbcMapping,
			Map<Object, Class<?>> valueMappings) {
		final List<EmbeddableDiscriminatorValueDetails> valueDetailsList = CollectionHelper.arrayList( valueMappings.size() );
		valueMappings.forEach( (value, embeddableClass) -> {
			final EmbeddableDiscriminatorValueDetails valueDetails = new EmbeddableDiscriminatorValueDetails(
					value,
					embeddableClass.getName()
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
	private final Map<String, EmbeddableDiscriminatorValueDetails> embeddableClassNameToDetailsMap;

	public EmbeddableDiscriminatorConverter(
			NavigableRole discriminatorRole,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType,
			List<EmbeddableDiscriminatorValueDetails> valueMappings) {
		super( discriminatorRole, domainJavaType, relationalJavaType );

		this.discriminatorValueToDetailsMap = CollectionHelper.concurrentMap( valueMappings.size() );
		this.embeddableClassNameToDetailsMap = CollectionHelper.concurrentMap( valueMappings.size() );
		valueMappings.forEach( (valueDetails) -> {
			discriminatorValueToDetailsMap.put( valueDetails.getValue(), valueDetails );
			embeddableClassNameToDetailsMap.put( valueDetails.getIndicatedEntityName(), valueDetails );
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
		return (O) matchingValueDetails.getIndicatedEntityName();
	}

	@Override
	public R toRelationalValue(O domainForm) {
		assert domainForm == null || domainForm instanceof String;

		if ( domainForm == null ) {
			return null;
		}

		final String embeddableClassName = (String) domainForm;

		//noinspection unchecked
		return (R) getDetailsForEntityName( embeddableClassName ).getValue();
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
	public DiscriminatorValueDetails getDetailsForEntityName(String embeddableClassName) {
		final EmbeddableDiscriminatorValueDetails valueDetails = embeddableClassNameToDetailsMap.get( embeddableClassName );
		if ( valueDetails != null ) {
			return valueDetails;
		}

		throw new AssertionFailure( "Unrecognized embeddable class: " + embeddableClassName );
	}

	@Override
	public void forEachValueDetail(Consumer<DiscriminatorValueDetails> consumer) {
		discriminatorValueToDetailsMap.forEach( (value, detail) -> consumer.accept( detail ) );
	}

	@Override
	public <X> X fromValueDetails(Function<DiscriminatorValueDetails, X> handler) {
		for ( DiscriminatorValueDetails detail : discriminatorValueToDetailsMap.values() ) {
			final X result = handler.apply( detail );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}
}
