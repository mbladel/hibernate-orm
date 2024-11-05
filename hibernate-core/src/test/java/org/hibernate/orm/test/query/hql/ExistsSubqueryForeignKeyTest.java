/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.List;

import org.hibernate.dialect.HSQLDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		ExistsSubqueryForeignKeyTest.Person.class,
		ExistsSubqueryForeignKeyTest.Document.class,
})
@SessionFactory
@SkipForDialect(dialectClass = HSQLDialect.class, reason = "HSQLDB doesn't like the case-when selection not being in the group-by")
public class ExistsSubqueryForeignKeyTest {
	@Test
	public void testWhereClause(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Tuple> resultList = session.createQuery(
					"select count(*) from Document d join d.owner o "
							+ "where exists(select p.id from Person p where p.id = o.id) group by o.id",
					Tuple.class
			).getResultList();
			assertThat( resultList ).isNotNull();
		} );
	}

	@Test
	public void testSelectCaseWhen(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Tuple> resultList = session.createQuery(
					"select case when exists(select p.id from Person p where p.id = o.id) then 1 else 0 end,"
							+ "count(*) from Document d join d.owner o group by o.id",
					Tuple.class
			).getResultList();
			assertThat( resultList ).isNotNull();
		} );
	}

	@Entity(name = "Person")
	static class Person {
		@Id
		private Long id;

		private String name;

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Document")
	static class Document {
		@Id
		private Long id;

		private String title;

		@ManyToOne
		private Person owner;

		public Document() {
		}

		public Document(Long id, String title, Person owner) {
			this.id = id;
			this.title = title;
			this.owner = owner;
		}
	}
}
