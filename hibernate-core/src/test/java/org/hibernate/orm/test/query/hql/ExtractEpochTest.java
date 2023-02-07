/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.io.Serializable;
import java.util.Date;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		ExtractEpochTest.CalendarEvent.class,
		ExtractEpochTest.CalendarEventReminder.class
})
@JiraKey("HHH-16082")
public class ExtractEpochTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			String jpql = "select c from CalendarEventReminder c where" +
					" (c.snoozeDate is null and (EXTRACT(EPOCH FROM c.calendarEvent.startDate) * 1000 - c.delay) <= ?1) or" +
					" (c.snoozeDate is not null and (EXTRACT(EPOCH FROM c.snoozeDate) * 1000 + c.delay) <= ?1)";
			TypedQuery<CalendarEventReminder> query = session.createQuery( jpql, CalendarEventReminder.class );
			query.setParameter( 1, System.nanoTime() );
			assertNotNull( query.getResultList() );
		} );
	}

	@Test
	public void testEpoch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			System.out.println(new Date().getTime());
			session.createQuery( "select extract(epoch from current_date)" ).getResultList();
		} );
	}

	@Test
	public void testNanoSecond(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "select extract(nanosecond from current_date)" ).getResultList();
		} );
	}

	@Entity(name = "CalendarEventReminder")
	public static class CalendarEventReminder implements Serializable {
		@Id
		@GeneratedValue
		private int id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(nullable = false)
		private CalendarEvent calendarEvent;

		private Date snoozeDate;

		private long delay;

		public CalendarEventReminder() {
		}

		public CalendarEventReminder(int id, CalendarEvent calendarEvent, Date snoozeDate, long delay) {
			this.id = id;
			this.calendarEvent = calendarEvent;
			this.snoozeDate = snoozeDate;
			this.delay = delay;
		}
	}

	@Entity(name = "CalendarEvent")
	public static class CalendarEvent implements Serializable {
		@Id
		@GeneratedValue
		private int id;

		@Temporal(TemporalType.TIMESTAMP)
		private Date startDate;

		public CalendarEvent() {
		}

		public CalendarEvent(int id, Date startDate) {
			this.id = id;
			this.startDate = startDate;
		}
	}
}
