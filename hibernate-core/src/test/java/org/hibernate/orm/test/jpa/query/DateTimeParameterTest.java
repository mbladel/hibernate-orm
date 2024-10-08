/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class DateTimeParameterTest extends BaseEntityManagerFunctionalTestCase {
	private static GregorianCalendar nowCal = new GregorianCalendar();
	private static Date now = new Date( nowCal.getTime().getTime() );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Thing.class };
	}

	@Test
	public void testBindingCalendarAsDate() {
		createTestData();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		try {
			Query query = em.createQuery( "from Thing t where t.someDate = :aDate" );
			query.setParameter( "aDate", nowCal, TemporalType.DATE );
			List list = query.getResultList();
			assertEquals( 1, list.size() );
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}

		deleteTestData();
	}

	@Test
	public void testBindingNulls() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		try {
			Query query = em.createQuery( "from Thing t where t.someDate = :aDate or t.someTime = :aTime or t.someTimestamp = :aTimestamp" );
			query.setParameter( "aDate", (Date) null, TemporalType.DATE );
			query.setParameter( "aTime", (Date) null, TemporalType.DATE );
			query.setParameter( "aTimestamp", (Date) null, TemporalType.DATE );
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	private void createTestData() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( new Thing( 1, "test", now, now, now ) );
		em.getTransaction().commit();
		em.close();
	}

	private void deleteTestData() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Thing" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	@Entity( name="Thing" )
	@Table( name = "THING" )
	public static class Thing {
		@Id
		public Integer id;
		public String someString;
		@Temporal( TemporalType.DATE )
		public Date someDate;
		@Temporal( TemporalType.TIME )
		public Date someTime;
		@Temporal( TemporalType.TIMESTAMP )
		public Date someTimestamp;

		public Thing() {
		}

		public Thing(Integer id, String someString, Date someDate, Date someTime, Date someTimestamp) {
			this.id = id;
			this.someString = someString;
			this.someDate = someDate;
			this.someTime = someTime;
			this.someTimestamp = someTimestamp;
		}
	}
}
