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
		ParentEmbeddable.class,
		ChildEmbeddableOne.class,
		SubChildEmbeddableOne.class,
		ChildEmbeddableTwo.class,
} )
@SessionFactory
public class BasicEmbeddableInheritanceTest {
	// todo marco : test nested embeddable inheritance (?)
	// todo marco : test embeddable inheritance with composite identifiers
	// todo marco : test embeddable inheritance with foreign keys (will need some work)
	// todo marco : also test aggregate embeddables (will need some work)

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 1L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_1" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( ChildEmbeddableOne.class );
			assertThat( ( (ChildEmbeddableOne) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 1 );
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
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( ChildEmbeddableTwo.class );
			assertThat( ( (ChildEmbeddableTwo) result.getEmbeddable() ).getChildTwoProp() ).isEqualTo( 2L );
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
			assertThat( result ).isExactlyInstanceOf( SubChildEmbeddableOne.class );
			assertThat( ( (SubChildEmbeddableOne) result ).getChildOneProp() ).isEqualTo( 4 );
			assertThat( ( (SubChildEmbeddableOne) result ).getSubChildOneProp() ).isEqualTo( 4.0 );
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
			assertThat( result ).isExactlyInstanceOf( ChildEmbeddableTwo.class );
			assertThat( ( (ChildEmbeddableTwo) result ).getChildTwoProp() ).isEqualTo( 2L );
		} );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 5L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_5" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( ChildEmbeddableOne.class );
			assertThat( ( (ChildEmbeddableOne) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 5 );
			// update values
			result.getEmbeddable().setParentProp( "embeddable_5_new" );
			( (ChildEmbeddableOne) result.getEmbeddable() ).setChildOneProp( 55 );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 5L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_5_new" );
			assertThat( ( (ChildEmbeddableOne) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 55 );
			result.setEmbeddable( new SubChildEmbeddableOne( "embeddable_6", 6, 6.0 ) );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 5L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_6" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( SubChildEmbeddableOne.class );
			assertThat( ( (SubChildEmbeddableOne) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 6 );
			assertThat( ( (SubChildEmbeddableOne) result.getEmbeddable() ).getSubChildOneProp() ).isEqualTo( 6.0 );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, new ChildEmbeddableOne( "embeddable_1", 1 ) ) );
			session.persist( new TestEntity( 2L, new ChildEmbeddableTwo( "embeddable_2", 2L ) ) );
			session.persist( new TestEntity( 3L, new ParentEmbeddable( "embeddable_3" ) ) );
			session.persist( new TestEntity( 4L, new SubChildEmbeddableOne( "embeddable_4", 4, 4.0 ) ) );
			session.persist( new TestEntity( 5L, new ChildEmbeddableOne( "embeddable_5", 5 ) ) );
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

		public TestEntity() {
		}

		public TestEntity(Long id, ParentEmbeddable embeddable) {
			this.id = id;
			this.embeddable = embeddable;
		}

		public ParentEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(ParentEmbeddable embeddable) {
			this.embeddable = embeddable;
		}
	}
}
