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

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		// EmbeddableInheritanceTest.AnotherEntity.class,
		EmbeddableInheritanceTest.TestEntity.class,
		EmbeddableInheritanceTest.ParentEmbeddable.class,
		EmbeddableInheritanceTest.ChildEmbeddable.class,
} )
@SessionFactory
public class EmbeddableInheritanceTest {
	// todo marco : test same embeddable (or maybe even subtype) used in different entities
	// todo marco : test nested embeddable inheritance (?)
	// todo marco : also test aggregate embeddables (in another test class)

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, new ChildEmbeddable() ) );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

		} );
	}

	@Entity( name = "AnotherEntity" )
	static class AnotherEntity {
		@Id
		private Long id;

		@Embedded
		private ParentEmbeddable embeddable;

		public AnotherEntity() {
		}

		public AnotherEntity(Long id, ParentEmbeddable embeddable) {
			this.id = id;
			this.embeddable = embeddable;
		}
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
	static class ParentEmbeddable {
		private String parentProp;
	}

	@Embeddable
	static class ChildEmbeddable extends ParentEmbeddable {
		private Integer childProp;
	}
}
