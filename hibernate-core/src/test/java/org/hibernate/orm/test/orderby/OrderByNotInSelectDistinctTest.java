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
			// todo marco : this doesn't fail on oracle but does on e.g. PostgreSQL and SQLServer
			session.createQuery( "select distinct p.name from Person p order by upper(p.name)", String.class ).getResultList();
		} );
	}

	@Test
	public void testOrderByNonSelectedSimplePath(SessionFactoryScope scope) {
		scope.inSession( session -> {
			// simple path
			session.createQuery( "select distinct p.name from Person p order by p.age", String.class ).getResultList();
			// function path sort
			session.createQuery( "select distinct p.age from Person p order by upper(p.name)", Integer.class ).getResultList();
			// function path selection
			session.createQuery( "select distinct upper(p.name) from Person p order by p.name", String.class ).getResultList();
			// same function path selection and sort
			session.createQuery( "select distinct upper(p.name) from Person p order by upper(p.name)", String.class ).getResultList();
			// different function path selection and sort
			session.createQuery( "select distinct upper(p.name) from Person p order by lower(p.name)", String.class ).getResultList();
		} );
	}

	@Test
	public void testOrderBySelectedToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// select root and sort by implicit to-one
			session.createQuery( "select distinct p from Person p order by p.consort", Person.class ).getResultList();
			// select root and sort by explicit to-one
			session.createQuery( "select distinct p from Person p join p.consort c order by c", Person.class ).getResultList();
			// select and sort by implicit subpath
			session.createQuery( "select distinct p.consort.name from Person p order by p.consort.name", String.class ).getResultList();
			// select and sort by explicit subpath
			session.createQuery( "select distinct c.name from Person p join p.consort c order by c.name", String.class ).getResultList();
		} );
	}

	@Test
	public void testOrderByNonSelectedToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// select root and sort by path
			session.createQuery( "select distinct p.age from Person p order by p.consort", Integer.class ).getResultList();
			// select and sort by implicit to-one
			session.createQuery( "select distinct p.consort from Person p order by p.consort", Person.class ).getResultList();
			// select and sort by explicit to-one
			session.createQuery( "select distinct c from Person p join p.consort c order by c", Person.class ).getResultList();
			// select and sort by explicit subpath
			session.createQuery( "select distinct c.name from Person p join p.consort c order by c.age", String.class ).getResultList();
		} );
	}

	@Test
	public void testOrderBySelectedToMany(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// select whole element
			session.createQuery( "select element(t) from Person p left join p.travels t order by element(t)", Travel.class ).getResultList();
			// select subpath
			session.createQuery( "select element(t).destination from Person p left join p.travels t order by element(t).destination", Travel.class ).getResultList();
		} );
	}

	@Test
	public void testOrderByNonSelectedToMany(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// order by whole element
			session.createQuery( "select distinct p.age from Person p left join p.travels t order by element(t)", Integer.class ).getResultList();
			// order by subpath
			session.createQuery( "select distinct p.name from Person p left join p.travels t order by element(t).destination", String.class ).getResultList();
		} );
	}

	@Test
	public void testSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// test order by in subquery
			session.createQuery( "select p.name from (select distinct pers.name as name from Person pers order by pers.age limit 1) p", String.class ).getResultList();
			// test order by in main query
			session.createQuery( "select distinct p.name from (select pers.name as name, pers.age as age from Person pers) p order by p.age", String.class ).getResultList();
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
