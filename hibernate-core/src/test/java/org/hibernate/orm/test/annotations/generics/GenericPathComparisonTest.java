/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.generics;

import java.io.Serializable;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
public class GenericPathComparisonTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new UserEntity( 1L, "user_1", 1L ) );
			session.persist( new UserEntity( 2L, "user_2", 99L ) );
		} );
	}

	@Test
	public void testParamComparison(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<UserEntity> query = cb.createQuery( UserEntity.class );
			final JpaRoot<UserEntity> from = query.from( UserEntity.class );
			query.where( cb.equal( from.get( "id" ), cb.parameter( Long.class ) ) );
			final UserEntity singleResult = session.createQuery( query ).setParameter( 1, 1L ).getSingleResult();
			assertThat( singleResult ).isNotNull();
		} );
	}

	@Test
	public void testLiteralComparison(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<UserEntity> query = cb.createQuery( UserEntity.class );
			final JpaRoot<UserEntity> from = query.from( UserEntity.class );
			query.where( cb.equal( from.get( "id" ), 1L ) );
			final UserEntity singleResult = session.createQuery( query ).setParameter( 1, 1L ).getSingleResult();
			assertThat( singleResult ).isNotNull();
		} );
	}

	@Test
	public void testPathComparison(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<UserEntity> query = cb.createQuery( UserEntity.class );
			final JpaRoot<UserEntity> from = query.from( UserEntity.class );
			query.where( cb.equal( from.get( "id" ), from.get( "longProp" ) ) );
			final UserEntity singleResult = session.createQuery( query ).setParameter( 1, 1L ).getSingleResult();
			assertThat( singleResult ).isNotNull();
		} );
	}

	@MappedSuperclass
	public static class IdEntity<PK extends Serializable> {
		@Id
		private PK id;

		public IdEntity() {
		}

		public IdEntity(final PK id) {
			this.id = id;
		}

		public PK getId() {
			return id;
		}
	}

	@Entity( name = "UserEntity" )
	public static class UserEntity extends IdEntity<Long> {
		private String name;

		private Long longProp;

		public UserEntity() {
		}

		public UserEntity(final Long id, final String name, final Long longProp) {
			super( id );
			this.name = name;
			this.longProp = longProp;
		}

		public String getName() {
			return name;
		}

		public Long getLongProp() {
			return longProp;
		}
	}
}
