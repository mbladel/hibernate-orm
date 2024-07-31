/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.records;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		RecordEmbeddedPropertyNamesTest.Vacation.class,
		RecordEmbeddedPropertyNamesTest.TestVacation.class
} )
@SessionFactory
public class RecordEmbeddedPropertyNamesTest {
	@Test
	public void testVacation(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TestVacation( 1L, new Vacation( true, 7 ) ) ) );
		scope.inTransaction( session -> {
			final TestVacation result = session.find( TestVacation.class, 1L );
			assertThat( result.getVacation().amount() ).isEqualTo( 7 );
			assertThat( result.getVacation().issued() ).isTrue();
		} );
	}

	@Embeddable
	record Vacation(Boolean issued, Integer amount) {
	}

	@Entity( name = "TestVacation" )
	static class TestVacation {
		@Id
		private Long id;

		@Embedded
		private Vacation vacation;

		public TestVacation() {
		}

		public TestVacation(Long id, Vacation vacation) {
			this.id = id;
			this.vacation = vacation;
		}

		public Vacation getVacation() {
			return vacation;
		}
	}
}
