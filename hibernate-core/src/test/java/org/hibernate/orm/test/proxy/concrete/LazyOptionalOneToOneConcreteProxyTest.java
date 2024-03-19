/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.proxy.concrete;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.ConcreteProxy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-16960" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17818" )
@DomainModel( annotatedClasses = {
		LazyOptionalOneToOneConcreteProxyTest.Person.class,
		LazyOptionalOneToOneConcreteProxyTest.PersonContact.class,
		LazyOptionalOneToOneConcreteProxyTest.BusinessContact.class,
} )
@SessionFactory
public class LazyOptionalOneToOneConcreteProxyTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Person person = new Person( 1L, "test" );
			final Person child1 = new Person( 2L, "child1" );
			final Person child2 = new Person( 2L, "child2" );
			child1.addParent( person );
			child2.addParent( person );
			entityManager.persist( person );
		} );
	}

	@Test
	public void testFindNull(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final EntityGraph<?> personGraph = entityManager.createEntityGraph( Person.class );
			personGraph.addAttributeNodes( "children" );

			final Person loadedPerson = entityManager.find(
					Person.class,
					1L,
					Map.of( "javax.persistence.fetchgraph", personGraph )
			);

			final PersonContact personContact = loadedPerson.getPersonContact();
			assertThat( personContact ).isNull();
		} );
	}

	@Entity( name = "Person" )
	public static class Person {

		@Id
		private Long id;

		private String name;

		@ManyToOne
		private Person parent;

		@OneToOne( mappedBy = "person", orphanRemoval = true, cascade = CascadeType.ALL )
		private PersonContact personContact;

		@OneToMany( mappedBy = "parent" )
		private Set<Person> children = new HashSet<>( 0 );

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public PersonContact getPersonContact() {
			return personContact;
		}

		public void setPersonContact(PersonContact personContact) {
			this.personContact = personContact;
		}

		public Person getParent() {
			return parent;
		}

		public void setParent(Person parent) {
			this.parent = parent;
		}

		public void addParent(Person parent) {
			this.parent = parent;
			parent.getChildren().add( this );
		}

		public Set<Person> getChildren() {
			return children;
		}

		public void setChildren(Set<Person> children) {
			this.children = children;
		}
	}

	@Entity( name = "PersonContact" )
	@ConcreteProxy
	public static class PersonContact {
		@Id
		private Long id;

		@OneToOne( optional = false, fetch = FetchType.LAZY )
		@MapsId
		private Person person;
	}

	@Entity(name="BusinessContact")
	public static class BusinessContact extends PersonContact {
		private String business;
	}
}
