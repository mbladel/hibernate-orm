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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		BasicEmbeddableInheritanceTest.TestEntity.class,
		BasicEmbeddableInheritanceTest.ParentEmbeddable.class,
		BasicEmbeddableInheritanceTest.ChildEmbeddableOne.class,
		BasicEmbeddableInheritanceTest.SubChildEmbeddableOne.class,
		BasicEmbeddableInheritanceTest.ChildEmbeddableTwo.class,
} )
@SessionFactory
public class BasicEmbeddableInheritanceTest {
	// todo marco : test nested embeddable inheritance (?)
	// todo marco : test embeddable inheritance with composite identifiers
	// todo marco : test embeddable inheritance with foreign keys (will need some work)
	// todo marco : also test aggregate embeddables (will need some work)

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, new ChildEmbeddableOne( "embeddable_1", 1 ) ) );
			session.persist( new TestEntity( 2L, new ChildEmbeddableTwo( "embeddable_2", 2L ) ) );
			session.persist( new TestEntity( 3L, new ParentEmbeddable( "embeddable_3" ) ) );
			session.persist( new TestEntity( 4L, new SubChildEmbeddableOne( "embeddable_4", 4, 4.0 ) ) );
		} );
	}

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
	}

	@Embeddable
	@DiscriminatorValue( "parent" )
	static class ParentEmbeddable {
		private String parentProp;

		public ParentEmbeddable() {
		}

		public ParentEmbeddable(String parentProp) {
			this.parentProp = parentProp;
		}

		public String getParentProp() {
			return parentProp;
		}
	}

	@Embeddable
	@DiscriminatorValue( "child_one" )
	static class ChildEmbeddableOne extends ParentEmbeddable {
		private Integer childOneProp;

		public ChildEmbeddableOne() {
		}

		public ChildEmbeddableOne(String parentProp, Integer childOneProp) {
			super( parentProp );
			this.childOneProp = childOneProp;
		}

		public Integer getChildOneProp() {
			return childOneProp;
		}
	}

	@Embeddable
	@DiscriminatorValue( "sub_child_one" )
	static class SubChildEmbeddableOne extends ChildEmbeddableOne {
		private Double subChildOneProp;

		public SubChildEmbeddableOne() {
		}

		public SubChildEmbeddableOne(String parentProp, Integer childOneProp, Double subChildOneProp) {
			super( parentProp, childOneProp );
			this.subChildOneProp = subChildOneProp;
		}

		public Double getSubChildOneProp() {
			return subChildOneProp;
		}
	}

	@Embeddable
	@DiscriminatorValue( "child_two" )
	static class ChildEmbeddableTwo extends ParentEmbeddable {
		private Long childTwoProp;

		public ChildEmbeddableTwo() {
		}

		public ChildEmbeddableTwo(String parentProp, Long childTwoProp) {
			super( parentProp );
			this.childTwoProp = childTwoProp;
		}

		public Long getChildTwoProp() {
			return childTwoProp;
		}
	}
}
