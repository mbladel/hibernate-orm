/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = GenericCollectionConverterTest.PersonEntity.class )
@SessionFactory
public class GenericCollectionConverterTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Dog dog1 = new Dog( "harry", "jack-russel-terrier" );
			final Dog dog2 = new Dog( "dumbledore", "shepherd" );
			final PersonEntity person = new PersonEntity( 1L, List.of( dog1, dog2 ) );
			session.persist( person );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final PersonEntity person = session.find( PersonEntity.class, 1L );
			assertThat( person ).isNotNull();
		} );
	}

	public static class Dog {
		private String name;
		private String breed;

		public Dog() {
		}

		public Dog(String name, String breed) {
			this.name = name;
			this.breed = breed;
		}

		public String getName() {
			return name;
		}

		public String getBreed() {
			return breed;
		}
	}

	public static class DogListConverter implements AttributeConverter<Collection<Dog>, String> {
		@Override
		public String convertToDatabaseColumn(Collection<Dog> obj) {
			if ( obj == null ) {
				return null;
			}
			return obj.stream()
					.map( dog -> dog.getName() + ":" + dog.getBreed() )
					.collect( Collectors.joining( "," ) );
		}

		@Override
		public Collection<Dog> convertToEntityAttribute(String dbData) {
			if ( dbData == null || dbData.isEmpty() ) {
				return null;
			}
			return Arrays.stream( dbData.split( "," ) ).map( s -> {
				final String[] tokens = s.split( ":" );
				return new Dog( tokens[0], tokens[1] );
			} ).collect( Collectors.toList() );
		}
	}


	@Entity( name = "PersonEntity" )
	public static class PersonEntity {

		@Id
		private Long id;

		@Column
		@Convert( converter = DogListConverter.class )
		// Change declaration to List<Dog> to make the test pass
		private Collection<Dog> dogs = new ArrayList<>();

		public PersonEntity() {
		}

		public PersonEntity(Long id, List<Dog> dogs) {
			this.id = id;
			this.dogs = dogs;
		}

		public Long getId() {
			return id;
		}

		public Collection<Dog> getDogs() {
			return dogs;
		}
	}
}
