/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ParentEmbeddable.class,
		EmbeddableInheritanceAssciationsTest.TestEntity.class,
		EmbeddableInheritanceAssciationsTest.AssociatedEntity.class,
		EmbeddableInheritanceAssciationsTest.AssociationChildOne.class,
		EmbeddableInheritanceAssciationsTest.AssociationSubChildOne.class,
		EmbeddableInheritanceAssciationsTest.AssociationChildTwo.class,
		EmbeddableInheritanceAssciationsTest.AssociationChildThree.class,
} )
@SessionFactory
public class EmbeddableInheritanceAssciationsTest {
	@Test
	public void testToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// insert
			final AssociatedEntity associated = session.find( AssociatedEntity.class, 1L );
			session.persist( new TestEntity( 1L, new AssociationChildOne( "embeddable_1", associated ) ) );
		} );
		scope.inTransaction( session -> {
			// find
			final TestEntity result = session.find( TestEntity.class, 1L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_1" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( AssociationChildOne.class );
			final AssociationChildOne embeddable = (AssociationChildOne) result.getEmbeddable();
			assertThat( embeddable.getManyToOne().getId() ).isEqualTo( 1L );
			assertThat( embeddable.getManyToOne().getName() ).isEqualTo( "associated_1" );
			// update
			final AssociatedEntity newAssociated = new AssociatedEntity( 11L, "associated_1_new" );
			session.persist( newAssociated );
			embeddable.setManyToOne( newAssociated );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 1L );
			final AssociationChildOne embeddable = (AssociationChildOne) result.getEmbeddable();
			assertThat( embeddable.getManyToOne().getId() ).isEqualTo( 11L );
			assertThat( embeddable.getManyToOne().getName() ).isEqualTo( "associated_1_new" );
		} );
	}

	@Test
	public void testToOneSubtype(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// insert
			final AssociatedEntity associated = session.find( AssociatedEntity.class, 2L );
			session.persist( new TestEntity( 2L, new AssociationSubChildOne( "embeddable_2", associated ) ) );
		} );
		scope.inTransaction( session -> {
			// find
			final TestEntity result = session.find( TestEntity.class, 2L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_2" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( AssociationSubChildOne.class );
			final AssociationSubChildOne embeddable = (AssociationSubChildOne) result.getEmbeddable();
			assertThat( embeddable.getManyToOne().getId() ).isEqualTo( 2L );
			assertThat( embeddable.getManyToOne().getName() ).isEqualTo( "associated_2" );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new AssociatedEntity( 1L, "associated_1" ) );
			session.persist( new AssociatedEntity( 2L, "associated_2" ) );
			session.persist( new AssociatedEntity( 3L, "associated_3" ) );
			session.persist( new AssociatedEntity( 4L, "associated_4" ) );
			session.persist( new AssociatedEntity( 5L, "associated_5" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from TestEntity" ).executeUpdate();
			session.createMutationQuery( "delete from AssociatedEntity" ).executeUpdate();
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

		public void setEmbeddable(ParentEmbeddable embeddable) {
			this.embeddable = embeddable;
		}
	}

	@Entity( name = "AssociatedEntity" )
	static class AssociatedEntity {
		@Id
		private Long id;

		private String name;

		public AssociatedEntity() {
		}

		public AssociatedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Embeddable
	@DiscriminatorValue( "child_one" )
	static class AssociationChildOne extends ParentEmbeddable {
		@ManyToOne
		private AssociatedEntity manyToOne;

		public AssociationChildOne() {
		}

		public AssociationChildOne(String parentProp, AssociatedEntity manyToOne) {
			super( parentProp );
			this.manyToOne = manyToOne;
		}

		public AssociatedEntity getManyToOne() {
			return manyToOne;
		}

		public void setManyToOne(AssociatedEntity manyToOne) {
			this.manyToOne = manyToOne;
		}
	}

	@Embeddable
	@DiscriminatorValue( "sub_child_one" )
	static class AssociationSubChildOne extends AssociationChildOne {
		public AssociationSubChildOne() {
		}

		public AssociationSubChildOne(String parentProp, AssociatedEntity manyToOne) {
			super( parentProp, manyToOne );
		}
	}

	@Embeddable
	@DiscriminatorValue( "child_two" )
	static class AssociationChildTwo extends ParentEmbeddable {
		@ManyToMany
		private List<AssociatedEntity> manyToMany = new ArrayList<>();

		public AssociationChildTwo() {
		}

		public AssociationChildTwo(String parentProp) {
			super( parentProp );
		}

		public List<AssociatedEntity> getManyToMany() {
			return manyToMany;
		}
	}

	@Embeddable
	@DiscriminatorValue( "child_three" )
	static class AssociationChildThree extends ParentEmbeddable {
		@OneToMany
		@JoinColumn
		private List<AssociatedEntity> oneToMany = new ArrayList<>();

		public AssociationChildThree() {
		}

		public AssociationChildThree(String parentProp) {
			super( parentProp );
		}

		public List<AssociatedEntity> getOneToMany() {
			return oneToMany;
		}
	}
}
