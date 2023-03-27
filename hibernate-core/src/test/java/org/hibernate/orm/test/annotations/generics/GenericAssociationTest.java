/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.generics;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.query.criteria.JpaPath;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Yoann Rodière
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		GenericAssociationTest.AbstractParent.class,
		GenericAssociationTest.Parent.class,
		GenericAssociationTest.AbstractChild.class,
		GenericAssociationTest.Child.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16378" )
public class GenericAssociationTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent = new Parent( 1L );
			parent.setBasicProp( "basic_prop" );
			final Child child = new Child( 2L );
			child.setParent( parent );
			parent.getChildren().add( child );
			session.persist( parent );
			session.persist( child );
		} );
	}

	@Test
	public void testParentQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select parent.id from Child",
				Long.class
		).getSingleResult() ).isEqualTo( 1L ) );
	}

	@Test
	public void testParentCriteriaQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Long> query = cb.createQuery( Long.class );
			final Root<Child> root = query.from( Child.class );
			final Path<Parent> parent = root.get( "parent" );
			// generic attributes are always reported as Object java type
			assertThat( parent.getJavaType() ).isEqualTo( Object.class );
			assertThat( parent.getModel() ).isSameAs( root.getModel().getAttribute( "parent" ) );
			assertThat( ( (JpaPath<?>) parent ).getResolvedModel().getBindableJavaType() ).isEqualTo( Parent.class );
			final Long result = session.createQuery( query.select( parent.get( "id" ) ) ).getSingleResult();
			assertThat( result ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testChildQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select c.id from Parent p join p.children c",
				Long.class
		).getSingleResult() ).isEqualTo( 2L ) );
	}

	@Test
	public void testChildCriteriaQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Long> query = cb.createQuery( Long.class );
			final Root<Parent> root = query.from( Parent.class );
			final Join<Parent, Child> join = root.join( "children" );
			// generic attributes are always reported as Object java type
			assertThat( join.getJavaType() ).isEqualTo( Object.class );
			assertThat( join.getModel() ).isSameAs( root.getModel().getAttribute( "children" ) );
			assertThat( ( (JpaPath<?>) join ).getResolvedModel().getBindableJavaType() ).isEqualTo( Child.class );
			final Long result = session.createQuery( query.select( join.get( "id" ) ) ).getSingleResult();
			assertThat( result ).isEqualTo( 2L );
		} );
	}

	@Test
	public void testElementQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select element(c).id from Parent p join p.children c",
				Long.class
		).getSingleResult() ).isEqualTo( 2L ) );
	}

	@Test
	public void testBasicValueCriteriaQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Object> query = cb.createQuery();
			final Root<Parent> root = query.from( Parent.class );
			final Path<String> basicProp = root.get( "basicProp" );
			// generic attributes are always reported as Object java type
			assertThat( basicProp.getJavaType() ).isEqualTo( Object.class );
			assertThat( basicProp.getModel() ).isSameAs( root.getModel().getAttribute( "basicProp" ) );
			assertThat( ( (JpaPath<?>) basicProp ).getResolvedModel().getBindableJavaType() ).isEqualTo( String.class );
			final Object result = session.createQuery( query.select( basicProp ) ).getSingleResult();
			assertThat( result ).isEqualTo( "basic_prop" );
		} );
	}

	@MappedSuperclass
	public abstract static class AbstractParent<E, T> {
		@OneToMany
		private Set<E> children;

		private T basicProp;

		public AbstractParent() {
			this.children = new HashSet<>();
		}

		public Set<E> getChildren() {
			return children;
		}

		public T getBasicProp() {
			return basicProp;
		}

		public void setBasicProp(T basicProp) {
			this.basicProp = basicProp;
		}
	}

	@Entity( name = "Parent" )
	public static class Parent extends AbstractParent<Child, String> {
		@Id
		private Long id;

		public Parent() {
		}

		public Parent(Long id) {
			this.id = id;
		}

		public Long getId() {
			return this.id;
		}
	}

	@MappedSuperclass
	public abstract static class AbstractChild<T> {
		@ManyToOne
		private T parent;

		public AbstractChild() {
		}

		public abstract Long getId();

		public T getParent() {
			return this.parent;
		}

		public void setParent(T parent) {
			this.parent = parent;
		}
	}

	@Entity( name = "Child" )
	public static class Child extends AbstractChild<Parent> {
		@Id
		protected Long id;

		public Child() {
		}

		public Child(Long id) {
			this.id = id;
		}

		@Override
		public Long getId() {
			return this.id;
		}
	}
}
