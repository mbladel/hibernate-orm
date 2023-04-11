/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = CircularEntityValuedPathInSubqueryTest.EntityA.class )
public class CircularEntityValuedPathInSubqueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA1 = new EntityA( "entitya_1", 1, null );
			session.persist( entityA1 );
			session.persist( new EntityA( "entitya_2", 2, entityA1 ) );
		} );
	}

	@Test
	public void testFkReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query<EntityA> query = session.createQuery(
					"select a from EntityA a where a.amount = " +
					"(select max(b.amount) from EntityA b where a.reference is null and b.reference is null)",
					EntityA.class
			);
			final List<EntityA> resultList = query.getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getName() ).isEqualTo( "entitya_1" );
		} );
	}

	@Test
	public void testJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query<EntityA> query = session.createQuery(
					"select a from EntityA a where a.amount = " +
					"(select max(b.amount) from EntityA b where a.reference.name = 'entitya_1' and b.reference is not null)",
					EntityA.class
			);
			final List<EntityA> resultList = query.getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getName() ).isEqualTo( "entitya_2" );
		} );
	}

	@Test
	public void testFkReferenceCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<EntityA> query = cb.createQuery( EntityA.class );
			final Root<EntityA> root = query.from( EntityA.class );

			final Subquery<Integer> subquery = query.subquery( Integer.class );
			final Root<EntityA> subRoot = subquery.from( EntityA.class );
			subquery.select( cb.max( subRoot.get( "amount" ) ) ).where( cb.and(
					cb.isNull( root.get( "reference" ) ),
					cb.isNull( subRoot.get( "reference" ) )
			) );

			query.select( root ).where( cb.equal( root.get( "amount" ), subquery ) );
			final List<EntityA> resultList = session.createQuery( query ).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getName() ).isEqualTo( "entitya_1" );
		} );
	}

	@Test
	public void testJoinCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<EntityA> query = cb.createQuery( EntityA.class );
			final Root<EntityA> root = query.from( EntityA.class );

			final Subquery<Integer> subquery = query.subquery( Integer.class );
			final Root<EntityA> subRoot = subquery.from( EntityA.class );
			subquery.select( cb.max( subRoot.get( "amount" ) ) ).where( cb.and(
					cb.equal( root.get( "reference" ).get( "name" ), "entitya_1" ),
					cb.isNotNull( subRoot.get( "reference" ) )
			) );

			query.select( root ).where( cb.equal( root.get( "amount" ), subquery ) );
			final List<EntityA> resultList = session.createQuery( query ).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getName() ).isEqualTo( "entitya_2" );
		} );
	}

	@Test
	public void testFkReferenceExistingTableGroup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query<EntityA> query = session.createQuery(
					"select a from EntityA a where a.reference is null and a.name = " +
					"(select b.name from EntityA b where (a.reference is null and b.reference is null))",
					EntityA.class
			);
			final List<EntityA> resultList = query.getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getName() ).isEqualTo( "entitya_1" );
		} );
	}

	@Test
	public void testJoinExistingTableGroup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query<EntityA> query = session.createQuery(
					"select a from EntityA a where a.reference is not null and a.amount = " +
					"(select max(b.amount) from EntityA b where a.reference.name = 'entitya_1' and b.reference is not null)",
					EntityA.class
			);
			final List<EntityA> resultList = query.getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getName() ).isEqualTo( "entitya_2" );
		} );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private Integer amount;

		@ManyToOne
		@JoinColumn( name = "reference" )
		private EntityA reference;

		public EntityA() {
		}

		public EntityA(String name, Integer amount, EntityA reference) {
			this.name = name;
			this.amount = amount;
			this.reference = reference;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Integer getAmount() {
			return amount;
		}

		public EntityA getReference() {
			return reference;
		}
	}
}
