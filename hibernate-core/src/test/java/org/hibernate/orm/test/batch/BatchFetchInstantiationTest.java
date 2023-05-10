/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.batch;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		BatchFetchInstantiationTest.EntityA.class,
		BatchFetchInstantiationTest.EntityB.class,
		BatchFetchInstantiationTest.MyPojo.class
} )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "2" ) )
public class BatchFetchInstantiationTest {
	@Test
	public void testNormalSelect(SessionFactoryScope scope) {
		final EntityA entityA = scope.fromTransaction( session -> {
			final EntityB entityB = new EntityB();
			entityB.foo = 123;
			session.persist( entityB );

			final EntityA entityA1 = new EntityA();
			entityA1.entityB = entityB;
			session.persist( entityA1 );
			return entityA1;
		} );

		scope.inTransaction( session -> {
			final TypedQuery<EntityA> query = session.createQuery(
					"select t from EntityA t where t.id = ?1",
					EntityA.class
			).setParameter( 1, entityA.id );
			final EntityA result = query.getSingleResult();
			assertThat( result.entityB ).isNotNull();
		} );
	}

	@Test
	public void testDynamicInstantiation(SessionFactoryScope scope) {
		final EntityA entityA = scope.fromTransaction( session -> {
			final EntityB entityB = new EntityB();
			entityB.foo = 123;
			session.persist( entityB );

			final EntityA entityA1 = new EntityA();
			entityA1.entityB = entityB;
			session.persist( entityA1 );
			return entityA1;
		} );

		scope.inTransaction( session -> {
			final TypedQuery<MyPojo> query2 = session.createQuery(
					String.format( "select new %s(t) from EntityA t where t.id = ?1", MyPojo.class.getName() ),
					MyPojo.class
			).setParameter( 1, entityA.id );
			final MyPojo pojo = query2.getSingleResult();
			assertThat( pojo.getFoo() ).isEqualTo( 123 );
		} );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		@GeneratedValue
		private Integer id;

		@JoinColumn( name = "entityb_id" )
		@ManyToOne
		// @Fetch( FetchMode.SELECT )
		private EntityB entityB;
	}

	@Entity( name = "EntityB" )
	public static class EntityB {
		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		@Column( name = "ID" )
		private Integer id;

		@Column( name = "FOO" )
		private Integer foo;

		@OneToMany( mappedBy = "entityB" )
		@Fetch( FetchMode.SUBSELECT )
		private List<EntityA> listOfEntitiesA = new ArrayList<>();
	}

	public static class MyPojo {
		private final Integer foo;

		public MyPojo(EntityA a) {
			foo = a.entityB.foo;
		}

		public Integer getFoo() {
			return foo;
		}
	}
}
