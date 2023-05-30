/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.hql;

import org.hibernate.query.MutationQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		NestedManyToOneMutationQueryTest.Person.class,
		NestedManyToOneMutationQueryTest.PrivateData.class,
		NestedManyToOneMutationQueryTest.PrivateSubData.class,
} )
public class NestedManyToOneMutationQueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person person = new Person( 1L, "Some person" );
			session.persist( person );
			for ( int i = 0; i < 5; i++ ) {
				final PrivateData privateData = new PrivateData( person, "Some data " + i );
				session.persist( privateData );
				for ( int j = 0; j < 5; j++ ) {
					session.persist( new PrivateSubData( privateData, "Some subdata " + i ) );
				}
			}
		} );
	}

	@Test
	public void testUpdateSimple(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MutationQuery query = session.createMutationQuery(
					"update PrivateData pd set pd.dataName = '...' where pd.person.id = ?1"
			);
			query.setParameter( 1, 1L );
			query.executeUpdate();
		} );
	}

	@Test
	public void testUpdateNested(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MutationQuery query = session.createMutationQuery(
					"update PrivateSubData psd set psd.dataName = '...' where psd.data.person.id = ?1"
			);
			query.setParameter( 1, 1L );
			query.executeUpdate();
		} );
	}

	@Test
	public void testDeleteSimple(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MutationQuery query = session.createMutationQuery(
					"delete from PrivateData pd where pd.person.id = ?1"
			);
			query.setParameter( 1, 1L );
			query.executeUpdate();
		} );
	}

	@Test
	public void testDeleteNested(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MutationQuery query = session.createMutationQuery(
					"delete from PrivateSubData psd where psd.data.person.id = ?1"
			);
			query.setParameter( 1, 1L );
			query.executeUpdate();
		} );
	}

	@Entity( name = "Person" )
	public static class Person {
		@Id
		private Long id;

		@Basic
		private String userName;

		public Person() {
		}

		public Person(Long id, String userName) {
			this.id = id;
			this.userName = userName;
		}
	}

	@Entity( name = "PrivateData" )
	public static class PrivateData {
		@Id
		@GeneratedValue
		private Long id;

		@Basic
		private String dataName;

		@ManyToOne
		private Person person;

		public PrivateData() {
		}

		public PrivateData(Person person, String dataName) {
			this.person = person;
			this.dataName = dataName;
		}
	}

	@Entity( name = "PrivateSubData" )
	public static class PrivateSubData {
		@Id
		@GeneratedValue
		private Long id;

		@Basic
		private String dataName;

		@ManyToOne
		private PrivateData data;

		public PrivateSubData() {
		}

		public PrivateSubData(PrivateData data, String dataName) {
			this.data = data;
			this.dataName = dataName;
		}
	}
}
