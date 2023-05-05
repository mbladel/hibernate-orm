/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		JoinFetchInheritanceTest.Animal.class,
		JoinFetchInheritanceTest.Dog.class,
		JoinFetchInheritanceTest.Cat.class,
		JoinFetchInheritanceTest.CatEmbedded.class,
		JoinFetchInheritanceTest.Kitten.class
} )
public class JoinFetchInheritanceTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Animal> animals = new ArrayList<>();
			animals.add( new Dog( 1L, "Dog" ) );
			animals.add( new Cat( 2L, List.of( new Kitten( "Kitten" ) ) ) );
			animals.add( new Cat( 3L, new ArrayList<>() ) );
			animals.add( new CatEmbedded( 4L, new KittensEmbeddable( List.of( new Kitten( "EmbeddedKitten" ) ) ) ) );
			animals.add( new CatEmbedded( 5L, new KittensEmbeddable( new ArrayList<>() ) ) );
			animals.forEach( session::persist );
		} );
	}

	@Test
	public void testCollection(SessionFactoryScope scope) {
		final List<Animal> animals = scope.fromTransaction( session -> {
			final TypedQuery<Animal> query = session.createQuery(
					"select animal from Animal animal left join fetch animal.kittens",
					Animal.class
			);
			return query.getResultList();
		} );
		for ( final Animal animal : animals ) {
			if ( animal instanceof Cat ) {
				final Cat cat = (Cat) animal;
				final List<Kitten> kittens = cat.getKittens();
				assertThat( Hibernate.isInitialized( kittens ) ).isTrue();
				assertThat( kittens ).hasSizeBetween( 0, 1 );
			}
		}

		// todo marco : also test with Cat.embeddable.kittens to be extra sure
		// todo marco : would be nice to have a solution which creates the correct nav. path from the join side
	}

	@Test
	public void testSingle(SessionFactoryScope scope) {
		final List<Animal> animals = scope.fromTransaction( session -> {
			final TypedQuery<Animal> query = session.createQuery(
					"select animal from Animal animal left join fetch animal.singleKitten",
					Animal.class
			);
			return query.getResultList();
		} );
	}

	@Test
	public void testEmbeddedCollection(SessionFactoryScope scope) {
		final List<Animal> animals = scope.fromTransaction( session -> {
			final TypedQuery<Animal> query = session.createQuery(
					"select animal from Animal animal left join fetch animal.kittensEmbeddable.kittens",
					Animal.class
			);
			return query.getResultList();
		} );
		for ( final Animal animal : animals ) {
			if ( animal instanceof CatEmbedded ) {
				final CatEmbedded cat = (CatEmbedded) animal;
				final List<Kitten> kittens = cat.getKittensEmbeddable().getKittens();
				assertThat( Hibernate.isInitialized( kittens ) ).isTrue();
				assertThat( kittens ).hasSizeBetween( 0, 1 );
			}
		}
	}

	@Test
	public void testEmbeddedSingle(SessionFactoryScope scope) {
		final List<Animal> animals = scope.fromTransaction( session -> {
			final TypedQuery<Animal> query = session.createQuery(
					"select animal from Animal animal left join fetch animal.kittensEmbeddable.singleKitten",
					Animal.class
			);
			return query.getResultList();
		} );
	}

	@Entity( name = "Animal" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "type", discriminatorType = DiscriminatorType.STRING )
	public static class Animal {
		@Id
		private Long id;

		public Animal() {
		}

		public Animal(Long id) {
			this.id = id;
		}

		@Column( name = "type", insertable = false, updatable = false )
		private String type;

		public Long getId() {
			return id;
		}

		public String getType() {
			return type;
		}
	}

	@Entity( name = "Dog" )
	@DiscriminatorValue( "Dog" )
	public static class Dog extends Animal {
		@Column( name = "name" )
		private String name;

		public Dog() {
		}

		public Dog(Long id, String name) {
			super( id );
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}
	}

	@Entity( name = "Cat" )
	@DiscriminatorValue( "Cat" )
	public static class Cat extends Animal {
		@ManyToMany( cascade = CascadeType.ALL )
		@JoinTable( name = "Cat_Kitten" )
		public List<Kitten> kittens = new ArrayList<>();

		@ManyToOne
		public Kitten singleKitten;

		public Cat() {
		}

		public Cat(Long id, List<Kitten> kittens) {
			super( id );
			this.kittens = kittens;
		}

		public List<Kitten> getKittens() {
			return kittens;
		}

		public Kitten getSingleKitten() {
			return singleKitten;
		}
	}

	@Embeddable
	public static class KittensEmbeddable {
		@ManyToMany( cascade = CascadeType.ALL )
		@JoinTable( name = "CatEmbedded_Kitten" )
		public List<Kitten> kittens = new ArrayList<>();

		@ManyToOne
		public Kitten singleKitten;

		public KittensEmbeddable() {
		}

		public KittensEmbeddable(List<Kitten> kittens) {
			this.kittens = kittens;
		}

		public List<Kitten> getKittens() {
			return kittens;
		}

		public Kitten getSingleKitten() {
			return singleKitten;
		}
	}

	@Entity( name = "CatEmbedded" )
	@DiscriminatorValue( "CatEmbedded" )
	public static class CatEmbedded extends Animal {
		@Embedded
		private KittensEmbeddable kittensEmbeddable;

		public CatEmbedded() {
		}

		public CatEmbedded(Long id, KittensEmbeddable kittensEmbeddable) {
			super( id );
			this.kittensEmbeddable = kittensEmbeddable;
		}

		public KittensEmbeddable getKittensEmbeddable() {
			return kittensEmbeddable;
		}
	}

	@Entity( name = "Kitten" )
	public static class Kitten {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Kitten() {
		}

		public Kitten(String name) {
			this.name = name;
		}
	}
}
