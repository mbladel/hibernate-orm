/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.type;

import java.time.LocalTime;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = LocalTimeRoundTripTest.SimpleEntity.class )
public class LocalTimeRoundTripTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SimpleEntity e1 = new SimpleEntity( 1, LocalTime.of( 12, 30, 54 ) );
			session.persist( e1 );
			final SimpleEntity e2 = new SimpleEntity( 2, LocalTime.MAX );
			session.persist( e2 );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SimpleEntity entity = session.find( SimpleEntity.class, 1 );
			assertThat( entity.getTheLocalTime() ).isEqualTo( LocalTime.of( 12, 30, 54 ) );
		} );
	}

	@Test
	public void testMax(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SimpleEntity entity = session.find( SimpleEntity.class, 2 );
			assertThat( entity.getTheLocalTime() ).isEqualToIgnoringNanos( LocalTime.MAX );
		} );
	}

	@Entity( name = "SimpleEntity" )
	public static class SimpleEntity {
		@Id
		private Integer id;

		private LocalTime theLocalTime;

		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, LocalTime theLocalTime) {
			this.id = id;
			this.theLocalTime = theLocalTime;
		}

		public LocalTime getTheLocalTime() {
			return theLocalTime;
		}
	}
}
