package org.hibernate.orm.test.collection;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		OneToManyInheritanceTest.Food.class,
		OneToManyInheritanceTest.Cheese.class,
		OneToManyInheritanceTest.SmellyCheese.class,
		OneToManyInheritanceTest.Refrigerator.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17483" )
public class OneToManyInheritanceTest {
	private static final Integer REFRIGERATOR_ID = 42;

	@BeforeAll
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Refrigerator refrigerator = new Refrigerator( REFRIGERATOR_ID, "Fridge" );
			final Cheese cheese1 = new Cheese( 1, "Roquefort", refrigerator );
			final SmellyCheese cheese2 = new SmellyCheese( 2, "Maroilles", refrigerator );
			final SmellyCheese cheese3 = new SmellyCheese( 3, "Vieux Lille", refrigerator );
			refrigerator.getCheeses().add( cheese1 );
			refrigerator.getCheeses().add( cheese2 );
			refrigerator.getCheeses().add( cheese3 );
			session.persist( cheese1 );
			session.persist( cheese2 );
			session.persist( cheese3 );
			session.persist( refrigerator );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Food" ).executeUpdate();
			session.createMutationQuery( "delete from Refrigerator" ).executeUpdate();
		} );
	}

	@Test
	public void testCollectionLoad(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Refrigerator refrigerator = session.find( Refrigerator.class, REFRIGERATOR_ID );
			assertThat( refrigerator.getCheeses() ).hasSize( 3 );
		} );
	}

	@Test
	public void testCollectionQueryFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Refrigerator refrigerator = session.createQuery(
					"from Refrigerator r join fetch r.cheeses where r.id = :id",
					Refrigerator.class
			).setParameter( "id", REFRIGERATOR_ID ).getSingleResult();
			assertThat( refrigerator.getCheeses() ).hasSize( 3 );
		} );
	}

	@Entity( name = "Refrigerator" )
	public static class Refrigerator {
		@Id
		private Integer id;

		private String name;

		@OneToMany( mappedBy = "refrigerator" )
		private Set<Cheese> cheeses;

		public Refrigerator() {
		}

		public Refrigerator(Integer id, String name) {
			this.id = id;
			this.name = name;
			this.cheeses = new HashSet<>();
		}

		public Set<Cheese> getCheeses() {
			return cheeses;
		}
	}

	@Entity( name = "Food" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( discriminatorType = DiscriminatorType.STRING, name = "food_type" )
	@DiscriminatorValue( value = "Food" )
	public static class Food {
		@Id
		private Integer id;

		private String name;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "refrigerator_id" )
		private Refrigerator refrigerator;

		public Food() {
		}

		public Food(Integer id, String name, Refrigerator refrigerator) {
			this.id = id;
			this.name = name;
			this.refrigerator = refrigerator;
		}
	}

	@Entity( name = "Cheese" )
	@DiscriminatorValue( "Cheese" )
	public static class Cheese extends Food {
		public Cheese() {
			super();
		}

		public Cheese(Integer id, String name, Refrigerator refrigerator) {
			super( id, name, refrigerator );
		}
	}

	@Entity( name = "SmellyCheese" )
	@DiscriminatorValue( "SmellyCheese" )
	public static class SmellyCheese extends Cheese {
		public SmellyCheese() {
			super();
		}

		public SmellyCheese(Integer id, String name, Refrigerator refrigerator) {
			super( id, name, refrigerator );
		}
	}
}
