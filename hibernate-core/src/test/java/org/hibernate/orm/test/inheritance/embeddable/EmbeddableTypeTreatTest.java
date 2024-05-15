/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EmbeddableTypeTreatTest.TestEntity.class,
		SimpleEmbeddable.class,
		ParentEmbeddable.class,
		ChildOneEmbeddable.class,
		SubChildOneEmbeddable.class,
		ChildTwoEmbeddable.class,
} )
@SessionFactory
public class EmbeddableTypeTreatTest {
	@Test
	public void testType(SessionFactoryScope scope) {
		// todo marco : add significant assertions
		scope.inTransaction( session -> {
			final Class<?> embeddableType = session.createQuery(
					"select type(t.embeddable) from TestEntity t where t.id = 1",
					Class.class
			).getSingleResult();
			final TestEntity testEntity = session.createQuery(
					"from TestEntity t where type(t.embeddable) = SubChildOneEmbeddable",
					TestEntity.class
			).getSingleResult();
			final Class<?> simpleEmbeddableType = session.createQuery(
					"select type(t.simpleEmbeddable) from TestEntity t where t.id = 1",
					Class.class
			).getSingleResult();
			session.createQuery(
					"from TestEntity t where type(t.simpleEmbeddable) = SimpleEmbeddable",
					TestEntity.class
			).getResultList();
		} );
	}

	@Test
	public void testTreat(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// todo marco : add significant assertions
			final SubChildOneEmbeddable r1 = session.createQuery(
					"select treat(t.embeddable as SubChildOneEmbeddable) from TestEntity t",
					SubChildOneEmbeddable.class
			).getSingleResult();
			final SubChildOneEmbeddable r11 = session.createQuery(
					"select treat(e as SubChildOneEmbeddable) from TestEntity t join t.embeddable e",
					SubChildOneEmbeddable.class
			).getSingleResult();
			final SubChildOneEmbeddable r111 = session.createQuery(
					"select e from TestEntity t join treat(t.embeddable as SubChildOneEmbeddable) e",
					SubChildOneEmbeddable.class
			).getSingleResult();
			final TestEntity r2 = session.createQuery(
					"from TestEntity t where treat(t.embeddable as ChildTwoEmbeddable).childTwoProp = 1",
					TestEntity.class
			).getSingleResult();
			final TestEntity r22 = session.createQuery(
					"from TestEntity t join t.embeddable e where treat(e as ChildTwoEmbeddable).childTwoProp = 1",
					TestEntity.class
			).getSingleResult();
			final TestEntity r222 = session.createQuery(
					"from TestEntity t join treat(t.embeddable as ChildTwoEmbeddable) e where e.childTwoProp = 1",
					TestEntity.class
			).getSingleResult();
		} );
	}

	@Test
	public void testTreatJunctions(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"from TestEntity t " +
							"where treat(t.embeddable as SubChildOneEmbeddable).subChildOneProp = 2.0 or id = 1",
					TestEntity.class
			).getResultList() ).isNotEmpty();

			assertThat( session.createQuery(
					"from TestEntity t " +
							"where treat(t.embeddable as SubChildOneEmbeddable).subChildOneProp = 2.0 and id = 1",
					TestEntity.class
			).getResultList() ).hasSize( 0 );

			assertThat( session.createQuery(
					"from TestEntity t where id = 1 or treat(t.embeddable as SubChildOneEmbeddable).subChildOneProp = 2.0",
					TestEntity.class
			).getResultList() ).hasSize( 2 );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, new ChildTwoEmbeddable( "embeddable_2", 1L ) ) );
			session.persist( new TestEntity( 2L, new SubChildOneEmbeddable( "embeddable_4", 2, 2.0 ) ) );
			session.persist( new TestEntity( 3L, new ChildOneEmbeddable( "embeddable_4", 3 ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate() );
	}

	@Entity( name = "TestEntity" )
	static class TestEntity {
		@Id
		private Long id;

		@Embedded
		private ParentEmbeddable embeddable;

		@Embedded
		private SimpleEmbeddable simpleEmbeddable;

		public TestEntity() {
		}

		public TestEntity(Long id, ParentEmbeddable embeddable) {
			this.id = id;
			this.embeddable = embeddable;
		}

		public Long getId() {
			return id;
		}

		public ParentEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(ParentEmbeddable embeddable) {
			this.embeddable = embeddable;
		}
	}
}
