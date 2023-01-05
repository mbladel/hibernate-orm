/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.dialect.function;

import java.math.BigDecimal;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.animal.Human;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
@DomainModel(standardModels = StandardDomainModel.ANIMAL)
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
public class PostgreSQLTruncRoundFunctionTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Human human = new Human();
			human.setId( 1L );
			human.setHeightInches( 1.78253d );
			human.setFloatValue( 1.78253f );
			human.setBigDecimalValue( new BigDecimal( "1.78253" ) );
			session.persist( human );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Human" ).executeUpdate() );
	}

	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialect(value = CockroachDialect.class, majorVersion = 22, minorVersion = 2, comment = "CockroachDB didn't support the two-argument trunc before version 22.2")
	public void testTrunc(SessionFactoryScope scope) {
		testFunction( scope, "trunc", "floor" );
	}

	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	public void testRound(SessionFactoryScope scope) {
		testFunction( scope, "round", "floor" );
	}

	@Test
	@RequiresDialect(value = CockroachDialect.class, comment = "CockroachDB natively supports round with two args for both deciaml and float types")
	public void testRoundWithoutWorkaround(SessionFactoryScope scope) {
		testFunction( scope, "round", "round" );
	}

	private void testFunction(SessionFactoryScope scope, String function, String workaround) {
		final SQLStatementInspector sqlStatementInspector = (SQLStatementInspector) scope.getStatementInspector();
		scope.inTransaction( session -> {
			// float / double types should use floor() workaround
			sqlStatementInspector.clear();
			assertEquals(
					1.78d,
					session.createQuery(
							"select " + function + "(h.heightInches, 2) from Human h",
							Double.class
					).getSingleResult()
			);
			assertTrue( sqlStatementInspector.getSqlQueries().get( 0 ).contains( workaround ) );
			sqlStatementInspector.clear();
			assertEquals(
					1.78f,
					session.createQuery(
							"select " + function + "(h.floatValue, 2) from Human h",
							Float.class
					).getSingleResult()
			);
			assertTrue( sqlStatementInspector.getSqlQueries().get( 0 ).contains( workaround ) );
			// numeric / decimal types should use nativa trunc() function
			sqlStatementInspector.clear();
			assertEquals(
					0,
					session.createQuery(
							"select " + function + "(h.bigDecimalValue, 2) from Human h",
							BigDecimal.class
					).getSingleResult().compareTo( new BigDecimal( "1.78" ) )
			);
			assertTrue( sqlStatementInspector.getSqlQueries().get( 0 ).contains( function ) );
		} );
	}
}
