/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = BasicEntity.class )
public class NegationTests {
	@Test
	@ServiceRegistry( settings = @Setting( name = AvailableSettings.JPA_QUERY_COMPLIANCE, value = "false" ) )
	@SessionFactory
	public void testComplianceDisabled(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new BasicEntity( 1, "entity_1" ) ) );
		testPredicateNegation( scope, false,false );
		testPredicateNegation( scope, false, true );
	}

	@Test
	@ServiceRegistry( settings = @Setting( name = AvailableSettings.JPA_QUERY_COMPLIANCE, value = "true" ) )
	@SessionFactory
	public void testComplianceEnabled(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new BasicEntity( 1, "entity_1" ) ) );
		testPredicateNegation( scope, true,false );
		testPredicateNegation( scope, true, true );
	}

	private void testPredicateNegation(SessionFactoryScope scope, boolean compliance, boolean doubleNegation) {
		// single comparison
		executeQuery( scope, doubleNegation, (cb, cq, root) -> {
			final JpaPredicate comparison = cb.equal( root.get( "id" ), 1 );
			final JpaPredicate negated = doubleNegation ? comparison.not().not() : comparison.not();
			assertThat( negated.isNegated() ).isEqualTo( compliance && !doubleNegation );
			if ( compliance ) {
				assertThat( negated ).isNotSameAs( comparison );
			}
			else {
				assertThat( negated ).isSameAs( comparison );
			}
			// return original predicate when compliance is disabled
			return compliance ? negated : comparison;
		} );
		// junction predicate
		executeQuery( scope, doubleNegation, (cb, cq, root) -> {
			final JpaPredicate junction = cb.or(
					cb.equal( root.get( "id" ), 1 ),
					cb.equal( root.get( "data" ), "entity_1" )
			);
			// always need to use the returned predicate
			final JpaPredicate negated = doubleNegation ? junction.not().not() : junction.not();
			assertThat( negated ).isNotSameAs( junction );
			assertThat( negated.isNegated() ).isEqualTo( doubleNegation );
			return negated;
		} );
	}

	public interface PredicateProducer {
		Predicate accept(HibernateCriteriaBuilder cb, JpaCriteriaQuery<BasicEntity> cq, JpaRoot<BasicEntity> root);
	}

	private void executeQuery(SessionFactoryScope scope, boolean hasResult, PredicateProducer predicateProducer) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<BasicEntity> cq = cb.createQuery( BasicEntity.class );
			final JpaRoot<BasicEntity> root = cq.from( BasicEntity.class );
			assertThat( session.createQuery(
					cq.where( predicateProducer.accept( cb, cq, root ) )
			).getResultList() ).hasSize( hasResult ? 1 : 0 );
		} );
	}
}
