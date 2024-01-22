/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sql;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel
@SessionFactory
public class NativeQueryListParameterTest {
	@Test
	public void testInPredicate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery( "select 1 where 1 in (:values)", Integer.class )
								.setParameter( "values", List.of( 1, 2, 3 ) )
								.getSingleResult() ).isEqualTo( 1 );
			assertThat( session.createNativeQuery( "select 1 where 1 in :values", Integer.class )
								.setParameter( "values", List.of( 1, 2, 3 ) )
								.getSingleResult() ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testCoalesceLiteralBefore(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery( "select coalesce(1, :values)", Integer.class )
								.setParameter( "values", List.of( 2, 3 ) )
								.getSingleResult() ).isEqualTo( 1 );
			assertThat( session.createNativeQuery( "select coalesce(1, 2 , :values)", Integer.class )
								.setParameter( "values", List.of( 3, 4 ) )
								.getSingleResult() ).isEqualTo( 1 );
			assertThat( session.createNativeQuery( "select coalesce( null , 2 , :values)", Integer.class )
								.setParameter( "values", List.of( 3, 4 ) )
								.getSingleResult() ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testCoalesceLiteralAfter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery( "select coalesce(:values, '1')", String.class )
								.setParameter( "values", List.of( "2", "3" ) )
								.getSingleResult() ).isEqualTo( "2" );
			assertThat( session.createNativeQuery( "select coalesce(:values , '1', '2')", String.class )
								.setParameter( "values", List.of( "3", "4" ) )
								.getSingleResult() ).isEqualTo( "3" );
			assertThat( session.createNativeQuery( "select coalesce( :values , null, '2' )", String.class )
								.setParameter( "values", List.of( "3", "4" ) )
								.getSingleResult() ).isEqualTo( "3" );
		} );
	}

	@Test
	public void testCoalesceLiteralBeforeAndAfter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery( "select coalesce( '1' , :values , '4' )", String.class )
								.setParameter( "values", List.of( "2", "3" ) )
								.getSingleResult() ).isEqualTo( "1" );
			assertThat( session.createNativeQuery( "select coalesce('1', :values , null, '2')", String.class )
								.setParameter( "values", List.of( "2", "3" ) )
								.getSingleResult() ).isEqualTo( "1" );
			assertThat( session.createNativeQuery( "select coalesce(null, '1', :values , null, '4' )", String.class )
								.setParameter( "values", List.of( "2", "3" ) )
								.getSingleResult() ).isEqualTo( "1" );
		} );
	}
}
