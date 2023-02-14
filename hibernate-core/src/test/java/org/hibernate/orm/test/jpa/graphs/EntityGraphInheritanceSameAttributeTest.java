/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.graphs;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		EntityGraphInheritanceSameAttributeTest.Animal.class,
		EntityGraphInheritanceSameAttributeTest.Dog.class,
		EntityGraphInheritanceSameAttributeTest.Cat.class,
		EntityGraphInheritanceSameAttributeTest.RescueDog.class
})
@JiraKey("HHH-15972")
public class EntityGraphInheritanceSameAttributeTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Dog( "Dog" ) );
			session.persist( new Cat( "Cat" ) );
			session.persist( new RescueDog( "RescueDog", "Mountain" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Animal" ).executeUpdate() );
	}

	@Test
	public void testFindAbstract(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Map<String, Object> properties = new HashMap<>();
			properties.put( "jakarta.persistence.loadgraph", session.createEntityGraph( Animal.class ) );
			final Animal dog = session.find( Animal.class, 1, properties );
			assertThat( dog ).isInstanceOf( Dog.class );
			assertThat( dog.getName() ).isEqualTo( "Dog" );
			final Animal cat = session.find( Animal.class, 2, properties );
			assertThat( cat ).isInstanceOf( Cat.class );
			assertThat( cat.getName() ).isEqualTo( "Cat" );
		} );
	}

	@Test
	public void testFindSpecific(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Map<String, Object> properties = new HashMap<>();
			properties.put( "jakarta.persistence.loadgraph", session.createEntityGraph( Animal.class ) );
			final Dog dog = session.find( Dog.class, 1, properties );
			assertThat( dog.getName() ).isEqualTo( "Dog" );
		} );
	}

	@Test
	public void testFindSubclass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Map<String, Object> properties = new HashMap<>();
			properties.put( "jakarta.persistence.loadgraph", session.createEntityGraph( Animal.class ) );
			final Animal animal = session.find( Animal.class, 3, properties );
			assertThat( animal ).isInstanceOf( RescueDog.class );
			assertThat( animal.getName() ).isEqualTo( "RescueDog" );
		} );
	}

	@Entity(name = "Animal")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class Animal {
		@Id
		@GeneratedValue
		public Integer id;

		public String name;

		public Animal() {
		}

		public Animal(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "Dog")
	public static class Dog extends Animal {
		public Integer numberOfLegs;

		public Dog() {
		}

		public Dog(String name) {
			super( name );
		}

		public Integer getNumberOfLegs() {
			return numberOfLegs;
		}

		public void setNumberOfLegs(Integer numberOfLegs) {
			this.numberOfLegs = numberOfLegs;
		}
	}

	@Entity(name = "RescueDog")
	public static class RescueDog extends Dog {
		private String function;

		public RescueDog() {
		}

		public RescueDog(String name, String function) {
			super( name );
			this.function = function;
		}

		public String getFunction() {
			return function;
		}

		public void setFunction(String function) {
			this.function = function;
		}
	}

	@Entity(name = "Cat")
	public static class Cat extends Animal {
		public Integer numberOfLegs;

		public Cat() {
		}

		public Cat(String name) {
			super( name );
		}

		public Integer getNumberOfLegs() {
			return numberOfLegs;
		}

		public void setNumberOfLegs(Integer numberOfLegs) {
			this.numberOfLegs = numberOfLegs;
		}
	}
}
