package org.hibernate.orm.test.jpa.query;

import java.time.LocalDateTime;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = EntityOfBasics.class)
public class DateTimeParameterArithmeticTest {
	@Test
	public void testSumAttributeHql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "select eob.theLocalDateTime + 1 day from EntityOfBasics eob" )
					.getResultList();
		} );
	}

	@Test
	public void testSumLiteralHql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
//			final java.util.Date javaUtilDate = new java.util.Date();
//			session.createQuery( "select :dt + 1 day" )
//					.setParameter( "dt", javaUtilDate )
//					.getSingleResult();
//			session.createQuery( "select :dt + 1 day" )
//					.setParameter( "dt", new java.sql.Date( javaUtilDate.getTime() ) )
//					.getSingleResult();
			session.createQuery( "select :dt + 1 day" )
					.setParameter( "dt", LocalDateTime.now() )
					.getSingleResult();
		} );
	}
}
