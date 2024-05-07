/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.orderby;

import java.util.List;

import org.hibernate.orm.test.limit.Oracle12LimitTest;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		OrderByNotInSelectDistinctTest.Person.class,
		OrderByNotInSelectDistinctTest.Travel.class,
} )
@SessionFactory
public class OrderByNotInSelectDistinctTest {
	@Test
	public void testOrderBySelectedSimplePath(SessionFactoryScope scope) {
		scope.inSession( session -> {
			// simple path
			session.createQuery( "select distinct p.age from Person p order by p.age", Integer.class ).getResultList();
			// select root and sort by path
			session.createQuery( "select distinct p from Person p order by p.age", Person.class ).getResultList();
			// function path sort
			session.createQuery( "select distinct p.name from Person p order by upper(p.name)", String.class ).getResultList();
			// function path selection
			session.createQuery( "select distinct upper(p.name) from Person p order by p.name", String.class ).getResultList();
			// same function path selection and sort
			session.createQuery( "select distinct upper(p.name) from Person p order by upper(p.name)", String.class ).getResultList();
			// different function path selection and sort
			session.createQuery( "select distinct upper(p.name) from Person p order by lower(p.name)", String.class ).getResultList();
		} );
	}

	@Test
	public void testOrderByNonSelectedSimplePath(SessionFactoryScope scope) {
		scope.inSession( session -> {
			// simple path
			session.createQuery( "select distinct p.age from Person p order by p.name", Integer.class ).getResultList();
			// function path sort
			session.createQuery( "select distinct p.age from Person p order by upper(p.name)", Integer.class ).getResultList();
		} );
	}

	@Entity( name = "Person" )
	static class Person {
		@Id
		private Long id;

		private Integer age;

		private String name;

		@ManyToOne
		private Person consort;

		@OneToMany
		private List<Travel> travels;
	}

	@Entity( name = "Travel" )
	static class Travel {
		@Id
		private Integer id;

		private String destination;
	}
}
