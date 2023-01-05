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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
@DomainModel(standardModels = StandardDomainModel.ANIMAL)
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(CockroachDialect.class)
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

	@Test
	public void testTruncFunction(SessionFactoryScope scope) {
		testFunction( scope, "trunc" );
	}

	@Test
	public void testRoundFunction(SessionFactoryScope scope) {
		testFunction( scope, "round" );
	}

	private void testFunction(SessionFactoryScope scope, String functionName) {
		final SQLStatementInspector sqlStatementInspector = (SQLStatementInspector) scope.getStatementInspector();
		scope.inTransaction( session -> {
			// float / double types should use floor() workaround
			sqlStatementInspector.clear();
			assertEquals( 1.78d, session.createQuery( "select " + functionName + "(h.heightInches, 2) from Human h", Double.class ).getSingleResult() );
			assertTrue( sqlStatementInspector.getSqlQueries().get( 0 ).contains( "floor" ) );
			sqlStatementInspector.clear();
			assertEquals( 1.78f, session.createQuery( "select " + functionName + "(h.floatValue, 2) from Human h", Float.class ).getSingleResult() );
			assertTrue( sqlStatementInspector.getSqlQueries().get( 0 ).contains( "floor" ) );
			// numeric / decimal types should use nativa trunc() function
			sqlStatementInspector.clear();
			assertEquals( new BigDecimal( "1.78" ), session.createQuery( "select " + functionName + "(h.bigDecimalValue, 2) from Human h", BigDecimal.class ).getSingleResult() );
			assertTrue( sqlStatementInspector.getSqlQueries().get( 0 ).contains( functionName ) );
		} );
	}
}
