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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		BasicEmbeddableInheritanceTest.TestEntity.class,
		SimpleEmbeddable.class,
		ParentEmbeddable.class,
		ChildOneEmbeddable.class,
		SubChildOneEmbeddable.class,
		ChildTwoEmbeddable.class,
} )
@SessionFactory
public class BasicEmbeddableInheritanceTest {
	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 1L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_1" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( ChildOneEmbeddable.class );
			assertThat( ( (ChildOneEmbeddable) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 1 );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 3L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_3" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( ParentEmbeddable.class );
		} );
	}

	@Test
	public void testQueryEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.createQuery(
					"from TestEntity where id = 2",
					TestEntity.class
			).getSingleResult();
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_2" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( ChildTwoEmbeddable.class );
			assertThat( ( (ChildTwoEmbeddable) result.getEmbeddable() ).getChildTwoProp() ).isEqualTo( 2L );
		} );
	}

	@Test
	public void testQueryEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentEmbeddable result = session.createQuery(
					"select embeddable from TestEntity where id = 4",
					ParentEmbeddable.class
			).getSingleResult();
			assertThat( result.getParentProp() ).isEqualTo( "embeddable_4" );
			assertThat( result ).isExactlyInstanceOf( SubChildOneEmbeddable.class );
			assertThat( ( (SubChildOneEmbeddable) result ).getChildOneProp() ).isEqualTo( 4 );
			assertThat( ( (SubChildOneEmbeddable) result ).getSubChildOneProp() ).isEqualTo( 4.0 );
		} );
	}

	@Test
	public void testQueryJoinedEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentEmbeddable result = session.createQuery(
					"select e from TestEntity t join t.embeddable e where t.id = 2",
					ParentEmbeddable.class
			).getSingleResult();
			assertThat( result.getParentProp() ).isEqualTo( "embeddable_2" );
			assertThat( result ).isExactlyInstanceOf( ChildTwoEmbeddable.class );
			assertThat( ( (ChildTwoEmbeddable) result ).getChildTwoProp() ).isEqualTo( 2L );
		} );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 5L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_5" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( ChildOneEmbeddable.class );
			assertThat( ( (ChildOneEmbeddable) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 5 );
			// update values
			result.getEmbeddable().setParentProp( "embeddable_5_new" );
			( (ChildOneEmbeddable) result.getEmbeddable() ).setChildOneProp( 55 );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 5L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_5_new" );
			assertThat( ( (ChildOneEmbeddable) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 55 );
			result.setEmbeddable( new SubChildOneEmbeddable( "embeddable_6", 6, 6.0 ) );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 5L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_6" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( SubChildOneEmbeddable.class );
			assertThat( ( (SubChildOneEmbeddable) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 6 );
			assertThat( ( (SubChildOneEmbeddable) result.getEmbeddable() ).getSubChildOneProp() ).isEqualTo( 6.0 );
		} );
	}

	@Test
	public void testType(SessionFactoryScope scope) {
		// todo marco : separate into dedicated test class (and maybe also test criteria queries?)
		scope.inTransaction( session -> {
			final Class<?> embeddableType = session.createQuery(
					"select type(t.embeddable) from TestEntity t where t.id = 1",
					Class.class
			).getSingleResult();
			assertThat( embeddableType ).isEqualTo( ChildOneEmbeddable.class );
			final TestEntity testEntity = session.createQuery(
					"from TestEntity t where type(t.embeddable) = SubChildOneEmbeddable",
					TestEntity.class
			).getSingleResult();
			assertThat( testEntity.getId() ).isEqualTo( 4L );
			assertThat( testEntity.getEmbeddable() ).isExactlyInstanceOf( SubChildOneEmbeddable.class );
			final Class<?> simpleEmbeddableType = session.createQuery(
					"select type(t.simpleEmbeddable) from TestEntity t where t.id = 1",
					Class.class
			).getSingleResult();
			assertThat( simpleEmbeddableType ).isEqualTo( SimpleEmbeddable.class );
			session.createQuery(
					"from TestEntity t where type(t.simpleEmbeddable) = SimpleEmbeddable",
					TestEntity.class
			).getResultList();
		} );
	}

	@Test
	public void testTreat(SessionFactoryScope scope) {
		// todo marco : separate into dedicated test class (and maybe also test criteria queries?)
		scope.inTransaction( session -> {
			// todo marco : we now interpret the treats correctly, now we need to add restrictions
			final ChildOneEmbeddable r1 = session.createQuery(
					"select treat(t.embeddable as ChildOneEmbeddable) from TestEntity t where t.id = 1",
					ChildOneEmbeddable.class
			).getSingleResult();
			assertThat( r1 ).isExactlyInstanceOf( ChildOneEmbeddable.class );
			final TestEntity r2 = session.createQuery(
					"from TestEntity t where treat(t.embeddable as ChildTwoEmbeddable).childTwoProp = 2",
					TestEntity.class
			).getSingleResult();
			assertThat( r2.getId() ).isEqualTo( 2L );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, new ChildOneEmbeddable( "embeddable_1", 1 ) ) );
			session.persist( new TestEntity( 2L, new ChildTwoEmbeddable( "embeddable_2", 2L ) ) );
			session.persist( new TestEntity( 3L, new ParentEmbeddable( "embeddable_3" ) ) );
			session.persist( new TestEntity( 4L, new SubChildOneEmbeddable( "embeddable_4", 4, 4.0 ) ) );
			session.persist( new TestEntity( 5L, new ChildOneEmbeddable( "embeddable_5", 5 ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate() );
	}

	//tag::embeddable-inheritance-entity-example[]
	@Entity( name = "TestEntity" )
	static class TestEntity {
		@Id
		private Long id;

		@Embedded
		private ParentEmbeddable embeddable;

		@Embedded
		private SimpleEmbeddable simpleEmbeddable;

		// ...
	//end::embeddable-inheritance-entity-example[]

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
	//tag::embeddable-inheritance-entity-example[]
	}
	//end::embeddable-inheritance-entity-example[]
}
