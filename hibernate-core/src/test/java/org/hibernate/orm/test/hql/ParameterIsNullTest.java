/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.hql;

import java.util.Collections;

import org.hibernate.query.SemanticException;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = BasicEntity.class )
@SessionFactory
public class ParameterIsNullTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new BasicEntity( 1, "data_1" ) );
			session.persist( new BasicEntity( 2, "data_2" ) );
			session.persist( new BasicEntity( 3, null ) );
		} );
	}

	@Test
	public void testNonNullBasicParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"where :param is null or data = :param",
				BasicEntity.class
		).setParameter( "param", "data_1" ).getResultList() ).hasSize( 1 ) );
	}

	@Test
	public void testNullBasicParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"where :param is null or data = :param",
				BasicEntity.class
		).setParameter( "param", null ).getResultList() ).hasSize( 3 ) );
	}

	@Test
	public void testNullCollectionParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				session.createQuery(
						"where :param is null or data in :param",
						BasicEntity.class
				).setParameter( "param", null ).getResultList();
				fail( "Nullness predicate should not be allowed on multi-valued parameter" );
			}
			catch (Exception e) {
				assertThat( e.getCause() ).isInstanceOf( SemanticException.class );
				assertThat( e.getCause().getMessage() ).isEqualTo(
						"Nullness predicate not allowed on multi-valued parameter 'param'."
				);
			}
		} );
	}

	@Test
	public void testEmptyCollectionParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				session.createQuery(
						"where :param is null or data in :param",
						BasicEntity.class
				).setParameter( "param", Collections.emptyList() ).getResultList();
				fail( "Nullness predicate should not be allowed on multi-valued parameter" );
			}
			catch (Exception e) {
				assertThat( e.getCause() ).isInstanceOf( SemanticException.class );
				assertThat( e.getCause().getMessage() ).isEqualTo(
						"Nullness predicate not allowed on multi-valued parameter 'param'."
				);
			}
		} );
	}

	@Test
	public void testEmptyCollectionListParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				session.createQuery(
						"where :param is null or data in :param",
						BasicEntity.class
				).setParameterList( "param", Collections.emptyList() ).getResultList();
				fail( "Nullness predicate should not be allowed on multi-valued parameter" );
			}
			catch (Exception e) {
				assertThat( e.getCause() ).isInstanceOf( SemanticException.class );
				assertThat( e.getCause().getMessage() ).isEqualTo(
						"Nullness predicate not allowed on multi-valued parameter 'param'."
				);
			}
		} );
	}
}
