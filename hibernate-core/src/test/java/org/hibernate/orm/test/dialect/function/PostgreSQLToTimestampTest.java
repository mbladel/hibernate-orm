/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.dialect.function;

import java.sql.Timestamp;

import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel
@RequiresDialect( PostgreSQLDialect.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16351" )
public class PostgreSQLToTimestampTest {
	@Test
	public void testString(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select to_timestamp('1974-10-15 9:30:42', 'yyyy-mm-dd hh:mi:ss')",
				Timestamp.class
		).getSingleResult() ).isEqualTo( Timestamp.valueOf( "1974-10-15 9:30:42" ) ) );
	}

	@Test
	public void testNumber(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select to_timestamp(151057842)",
				Timestamp.class
		).getSingleResult() ).isEqualTo( Timestamp.valueOf( "1974-10-15 9:30:42" ) ) );
	}
}
