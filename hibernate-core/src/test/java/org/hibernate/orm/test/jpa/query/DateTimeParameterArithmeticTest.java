package org.hibernate.orm.test.jpa.query;

import java.time.LocalDateTime;
import java.util.Calendar;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel
public class DateTimeParameterArithmeticTest {
	@Test
	public void testSum(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			java.util.Date utilDate = new java.util.Date();
			session.createQuery( "select :d1 + 1 day" )
					.setParameter( "d1", utilDate )
					.getSingleResult();
			session.createQuery( "select :d2 + 1 day" )
					.setParameter( "d2", new java.sql.Date( utilDate.getTime() ) )
					.getSingleResult();
			session.createQuery( "select :d3 + 1 day" )
					.setParameter( "d3", Calendar.getInstance() )
					.getSingleResult();
			session.createQuery( "select :d4 + 1 day" )
					.setParameter( "d4", LocalDateTime.now() )
					.getSingleResult();
		} );
	}

	@Test
	public void testDiff(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			java.util.Date utilDate = new java.util.Date();
			session.createQuery( "select :d1 - 1 day" )
					.setParameter( "d1", utilDate )
					.getSingleResult();
			session.createQuery( "select :d2 - 1 day" )
					.setParameter( "d2", new java.sql.Date( utilDate.getTime() ) )
					.getSingleResult();
			session.createQuery( "select :d3 - 1 day" )
					.setParameter( "d3", Calendar.getInstance() )
					.getSingleResult();
			session.createQuery( "select :d4 - 1 day" )
					.setParameter( "d4", LocalDateTime.now() )
					.getSingleResult();
		} );
	}
}
