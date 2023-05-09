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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		JoinFetchInheritanceTest.Animal.class,
		JoinFetchInheritanceTest.Cat.class,
		JoinFetchInheritanceTest.CatEmbedded.class,
		JoinFetchInheritanceTest.Kitten.class
} )
public class JoinFetchInheritanceTest {
	private final static String CAT = "cat";
	private final static String CAT_EMBEDDED = "cat_embedded";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Animal> animals = new ArrayList<>();
			final Kitten kitten1 = new Kitten( "kitten_1" );
			session.persist( kitten1 );
			animals.add( new Cat( 1L, List.of( kitten1 ), kitten1 ) );
			animals.add( new Cat( 2L, new ArrayList<>(), kitten1 ) );
			final Kitten kitten2 = new Kitten( "kitten_2" );
			session.persist( kitten2 );
			animals.add( new CatEmbedded( 3L, new KittensEmbeddable( List.of( kitten2 ), kitten2 ) ) );
			animals.add( new CatEmbedded( 4L, new KittensEmbeddable( new ArrayList<>(), kitten2 ) ) );
			animals.forEach( session::persist );
		} );
	}

	@Test
	public void testCollection(SessionFactoryScope scope) {
		final List<Animal> animals = scope.fromTransaction( session -> session.createQuery(
				"select animal from Animal animal left join fetch animal.kittens",
				Animal.class
		).getResultList() );
		assertThat( animals ).hasSize( 4 );
		for ( final Animal animal : animals ) {
			switch ( animal.getType() ) {
				case CAT:
					final Cat cat = (Cat) animal;
					final List<Kitten> kittens = cat.getKittens();
					assertThat( Hibernate.isInitialized( kittens ) ).isTrue();
					assertThat( kittens ).hasSizeBetween( 0, 1 );
					break;
				case CAT_EMBEDDED:
					final CatEmbedded catEmbedded = (CatEmbedded) animal;
					final List<Kitten> kittensEmbedded = catEmbedded.getKittensEmbeddable().getKittens();
					assertThat( Hibernate.isInitialized( kittensEmbedded ) ).isFalse();
			}
		}
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
		assertThat( animals ).hasSize( 4 );
		for ( final Animal animal : animals ) {
			switch ( animal.getType() ) {
				case CAT:
					final Cat cat = (Cat) animal;
					final Kitten kitten = cat.getSingleKitten();
					assertThat( Hibernate.isInitialized( kitten ) ).isTrue();
					assertThat( kitten.getName() ).isEqualTo( "kitten_1" );
					break;
				case CAT_EMBEDDED:
					final CatEmbedded catEmbedded = (CatEmbedded) animal;
					final Kitten kittenEmbedded = catEmbedded.getKittensEmbeddable().getSingleKitten();
					assertThat( Hibernate.isInitialized( kittenEmbedded ) ).isFalse();
			}
		}
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
		assertThat( animals ).hasSize( 4 );
		for ( final Animal animal : animals ) {
			switch ( animal.getType() ) {
				case CAT:
					final Cat cat = (Cat) animal;
					final List<Kitten> kittens = cat.getKittens();
					assertThat( Hibernate.isInitialized( kittens ) ).isFalse();
					break;
				case CAT_EMBEDDED:
					final CatEmbedded catEmbedded = (CatEmbedded) animal;
					final List<Kitten> kittensEmbedded = catEmbedded.getKittensEmbeddable().getKittens();
					assertThat( Hibernate.isInitialized( kittensEmbedded ) ).isTrue();
					assertThat( kittensEmbedded ).hasSizeBetween( 0, 1 );
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
		assertThat( animals ).hasSize( 4 );
		for ( final Animal animal : animals ) {
			switch ( animal.getType() ) {
				case CAT:
					final Cat cat = (Cat) animal;
					final Kitten kitten = cat.getSingleKitten();
					assertThat( Hibernate.isInitialized( kitten ) ).isFalse();
					break;
				case CAT_EMBEDDED:
					final CatEmbedded catEmbedded = (CatEmbedded) animal;
					final Kitten kittenEmbedded = catEmbedded.getKittensEmbeddable().getSingleKitten();
					assertThat( Hibernate.isInitialized( kittenEmbedded ) ).isTrue();
					assertThat( kittenEmbedded.getName() ).isEqualTo( "kitten_2" );
			}
		}
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

	@Entity( name = "Cat" )
	@DiscriminatorValue( CAT )
	public static class Cat extends Animal {
		@ManyToMany( fetch = FetchType.LAZY )
		@JoinTable( name = "Cat_Kitten" )
		public List<Kitten> kittens = new ArrayList<>();

		@ManyToOne( fetch = FetchType.LAZY )
		public Kitten singleKitten;

		public Cat() {
		}

		public Cat(Long id, List<Kitten> kittens, Kitten singleKitten) {
			super( id );
			this.kittens = kittens;
			this.singleKitten = singleKitten;
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
		@ManyToMany( fetch = FetchType.LAZY )
		@JoinTable( name = "CatEmbedded_Kitten" )
		public List<Kitten> kittens = new ArrayList<>();

		@ManyToOne( fetch = FetchType.LAZY )
		public Kitten singleKitten;

		public KittensEmbeddable() {
		}

		public KittensEmbeddable(List<Kitten> kittens, Kitten singleKitten) {
			this.kittens = kittens;
			this.singleKitten = singleKitten;
		}

		public List<Kitten> getKittens() {
			return kittens;
		}

		public Kitten getSingleKitten() {
			return singleKitten;
		}
	}

	@Entity( name = "CatEmbedded" )
	@DiscriminatorValue( CAT_EMBEDDED )
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

		public String getName() {
			return name;
		}
	}
}
