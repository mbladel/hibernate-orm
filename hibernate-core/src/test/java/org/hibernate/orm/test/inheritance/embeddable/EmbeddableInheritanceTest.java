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
		EmbeddableInheritanceTest.TestEntity.class,
		EmbeddableInheritanceTest.ParentEmbeddable.class,
		EmbeddableInheritanceTest.ChildEmbeddableOne.class,
		EmbeddableInheritanceTest.ChildEmbeddableTwo.class,
} )
@SessionFactory
public class EmbeddableInheritanceTest {
	// todo marco : test same embeddable (or maybe even subtype) used in different entities
	// todo marco : test nested embeddable inheritance (?)
	// todo marco : also test aggregate embeddables (in another test class)

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, new ChildEmbeddableOne( "embeddable_1", 1 ) ) );
			session.persist( new TestEntity( 2L, new ChildEmbeddableTwo( "embeddable_2", 2L ) ) );
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 1L );
			assertThat( result.embeddable.parentProp ).isEqualTo( "embeddable_1" );
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.createQuery( "from TestEntity where id = 2", TestEntity.class )
					.getSingleResult();
			assertThat( result.embeddable.parentProp ).isEqualTo( "embeddable_2" );
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
	}
}
