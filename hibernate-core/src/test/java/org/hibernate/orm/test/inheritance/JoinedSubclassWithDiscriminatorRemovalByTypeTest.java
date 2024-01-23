/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.inheritance;

import java.io.Serializable;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.inheritance.JoinedSubclassWithDiscriminatorRemovalByTypeTest.BaseEntity;
import static org.hibernate.orm.test.inheritance.JoinedSubclassWithDiscriminatorRemovalByTypeTest.BaseVehicle;
import static org.hibernate.orm.test.inheritance.JoinedSubclassWithDiscriminatorRemovalByTypeTest.Car;
import static org.hibernate.orm.test.inheritance.JoinedSubclassWithDiscriminatorRemovalByTypeTest.Truck;

@DomainModel( annotatedClasses = { Car.class, Truck.class, BaseVehicle.class, BaseEntity.class, } )
@SessionFactory
public class JoinedSubclassWithDiscriminatorRemovalByTypeTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( Car.create( 1L ) );
			session.persist( Car.create( 2L ) );
			session.persist( Car.create( 3L ) );
			session.persist( Truck.create( 4L ) );
			session.persist( Truck.create( 5L ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BaseEntity" ).executeUpdate() );
	}

	@Test
	public void testInPredicateParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery(
				"delete from BaseVehicle e where type(e) in(:type)"
		).setParameter( "type", Car.class ).executeUpdate() );
		assertRemaining( scope, 4L, 5L );
	}

	@Test
	public void testInPredicateLiteral(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery(
				"delete from BaseVehicle e where type(e) in(Truck)"
		).executeUpdate() );
		assertRemaining( scope, 1L, 2L, 3L );
	}

	@Test
	public void testComparisonPredicateParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery(
				"delete from BaseVehicle e where type(e) = :type"
		).setParameter( "type", Truck.class ).executeUpdate() );
		assertRemaining( scope, 1L, 2L, 3L );
	}

	@Test
	public void testComparisonPredicateLiteral(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery(
				"delete from BaseVehicle e where type(e) = Car"
		).executeUpdate() );
		assertRemaining( scope, 4L, 5L );
	}

	private <T extends BaseVehicle> void assertRemaining(SessionFactoryScope scope, Long... ids) {
		scope.inSession( session -> {
			final List<BaseVehicle> resultList = session.createQuery(
					"from BaseVehicle ",
					BaseVehicle.class
			).getResultList();
			assertThat( resultList ).hasSize( ids.length );
			assertThat( resultList.stream().map( BaseEntity::getId ) ).containsOnly( ids );
		} );
	}

	@Entity( name = "Car" )
	public static class Car extends BaseVehicle {
		public static Car create(Long id) {
			final Car car = new Car();
			car.setId( id );
			return car;
		}
	}

	@Entity( name = "Truck" )
	public static class Truck extends BaseVehicle {
		public static Truck create(Long id) {
			final Truck truck = new Truck();
			truck.setId( id );
			return truck;
		}
	}

	@Entity( name = "BaseVehicle" )
	public static abstract class BaseVehicle extends BaseEntity {
	}

	@Entity( name = "BaseEntity" )
	@DiscriminatorColumn( name = "type" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static abstract class BaseEntity implements Serializable {
		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
