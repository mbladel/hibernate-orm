/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.criteria.cte;

import org.hibernate.Session;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = BasicEntity.class )
public class CriteriaCteCopyTest {
	@Test
	public void testCteCriteria(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final HibernateCriteriaBuilder cb = entityManager.unwrap( Session.class ).getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> cq = cb.createQuery( Integer.class );
			final Subquery<Integer> subquery = cq.subquery( Integer.class );
			final Root<BasicEntity> subRoot = subquery.from( BasicEntity.class );
			final Path<Integer> id = subRoot.get( "id" );
			subquery.select( id ).where( cb.equal( id, 1 ) ).alias( "sub_id" );
			final JpaCteCriteria<Integer> cte = cq.with( subquery );
			final JpaRoot<Integer> root = cq.from( cte );
			cq.select( root.get( "sub_id" ) );
			assertThat( entityManager.createQuery( cq ).getSingleResult() ).isEqualTo( 1 );
		} );
	}

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new BasicEntity( 1, "data_1" ) );
			entityManager.persist( new BasicEntity( 2, "data_2" ) );
			entityManager.persist( new BasicEntity( 3, "data_3" ) );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.createQuery( "delete from BasicEntity" ).executeUpdate() );
	}
}
