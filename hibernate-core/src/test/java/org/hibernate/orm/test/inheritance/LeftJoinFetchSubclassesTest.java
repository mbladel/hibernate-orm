/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		LeftJoinFetchSubclassesTest.Entity1.class,
		LeftJoinFetchSubclassesTest.SuperClass.class,
		LeftJoinFetchSubclassesTest.SubClass1.class,
		LeftJoinFetchSubclassesTest.SubClass2.class,
} )
public class LeftJoinFetchSubclassesTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Entity1 entity1 = new Entity1();
			session.persist( entity1 );

			final SubClass1 subClass1 = new SubClass1();
			subClass1.setId( 1L );
			subClass1.setEntity1( entity1 );
			session.persist( subClass1 );

			final SubClass2 subClass2 = new SubClass2();
			subClass2.setId( 2L );
			subClass2.setEntity1( entity1 );
			session.persist( subClass2 );
		} );
	}

	@Test
	public void testJoinFetchSub1(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Entity1 result = session.createQuery(
					"select e from Entity1 e left join fetch e.subClass1",
					Entity1.class
			).getSingleResult();
			assertThat( result ).isNotNull();
		} );
	}

	@Test
	public void testJoinFetchSub2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Entity1 result = session.createQuery(
					"select e from Entity1 e left join fetch e.subClass2",
					Entity1.class
			).getSingleResult();
			assertThat( result ).isNotNull();
		} );
	}

	@Test
	public void testJoinFetchBoth(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Entity1 result = session.createQuery(
					"select e from Entity1 e left join fetch e.subClass1 left join fetch e.subClass2",
					Entity1.class
			).getSingleResult();
			assertThat( result ).isNotNull();
		} );
	}

	@Test
	public void testJoinBoth(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Entity1 result = session.createQuery(
					"select e from Entity1 e left join e.subClass1 left join e.subClass2",
					Entity1.class
			).getSingleResult();
			assertThat( result ).isNotNull();
		} );
	}

	@Entity( name = "Entity1" )
	public static class Entity1 {
		@Id
		@GeneratedValue
		private Long id;

		@OneToOne( fetch = FetchType.LAZY, mappedBy = "entity1" )
		private SubClass1 subClass1;

		@OneToOne( fetch = FetchType.LAZY, mappedBy = "entity1" )
		private SubClass2 subClass2;

		public SubClass1 getSubClass1() {
			return subClass1;
		}

		public SubClass2 getSubClass2() {
			return subClass2;
		}
	}

	@Entity( name = "SuperClass" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	public static abstract class SuperClass implements Serializable {
		@Id
		private Long id;

		@ManyToOne
		private Entity1 entity1;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Entity1 getEntity1() {
			return entity1;
		}

		public void setEntity1(Entity1 entity1) {
			this.entity1 = entity1;
		}
	}

	@Entity( name = "SubClass1" )
	@DiscriminatorValue( "1" )
	public static class SubClass1 extends SuperClass {
	}

	@Entity( name = "SubClass2" )
	@DiscriminatorValue( "2" )
	public static class SubClass2 extends SuperClass {
	}
}
