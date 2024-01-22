/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = DistinctQueryLimitTest.TestEntity.class )
@SessionFactory
public class DistinctQueryLimitTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( createEntity( 1, 10, "A", "TEST1234" ) );
			session.persist( createEntity( 2, 10, "A", "TEST1234" ) );
			session.persist( createEntity( 3, 10, "B", "TEST1234" ) );
			session.persist( createEntity( 4, 11, "C", "TEST1234" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from TestEntity " ).executeUpdate();
		} );
	}

	@Test
	public void testSingleColumnDistinctSelect(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final var resultList = session.createQuery(
							"select distinct t.attributeA, t.attributeB, t.attributeC from TestEntity t where t.attributeA = 10",
							Object[].class
					).setMaxResults( 4 )
					.setFirstResult( 0 ).getResultList();
			assertEquals( 2, resultList.size() ); //breaks when using oracle Dialect, returns 3
		} );
	}

	@Test
	public void testSingleColumnGroupBySelect(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final var resultList = session.createQuery(
							"select distinct t.attributeA, t.attributeB, t.attributeC from TestEntity t where t.attributeA = 10 group by t.attributeA, t.attributeB, t.attributeC",
							Object[].class
					).setMaxResults( 4 )
					.setFirstResult( 0 ).getResultList();
			assertEquals( 2, resultList.size() ); //breaks when using oracle Dialect, leads to broken SQL
		} );
	}

	private static TestEntity createEntity(long id, long attributeA, String attributeB, String attributeC) {
		final var childA = new TestEntity();
		childA.setId( id );
		childA.setAttributeA( attributeA );
		childA.setAttributeB( attributeB );
		childA.setAttributeC( attributeC );
		return childA;
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity {
		@Id
		private Long id;
		private Long attributeA;
		private String attributeB;
		private String attributeC;

		public Long getAttributeA() {
			return attributeA;
		}

		public void setAttributeA(Long attributeA) {
			this.attributeA = attributeA;
		}

		public String getAttributeB() {
			return attributeB;
		}

		public void setAttributeB(String attributeB) {
			this.attributeB = attributeB;
		}

		public String getAttributeC() {
			return attributeC;
		}

		public void setAttributeC(String attributeC) {
			this.attributeC = attributeC;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}