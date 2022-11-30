/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.criteria;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaExpression;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@ServiceRegistry
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
public class CriteriaOrderedSetAggregateTest {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			Date now = new Date();

			EntityOfBasics entity1 = new EntityOfBasics();
			entity1.setId( 1 );
			entity1.setTheString( "5" );
			entity1.setTheInt( 5 );
			entity1.setTheInteger( -1 );
			entity1.setTheDouble( 5.0 );
			entity1.setTheDate( now );
			entity1.setTheBoolean( true );
			em.persist( entity1 );

			EntityOfBasics entity2 = new EntityOfBasics();
			entity2.setId( 2 );
			entity2.setTheString( "6" );
			entity2.setTheInt( 6 );
			entity2.setTheInteger( -2 );
			entity2.setTheDouble( 6.0 );
			entity2.setTheBoolean( true );
			em.persist( entity2 );

			EntityOfBasics entity3 = new EntityOfBasics();
			entity3.setId( 3 );
			entity3.setTheString( "7" );
			entity3.setTheInt( 7 );
			entity3.setTheInteger( 3 );
			entity3.setTheDouble( 7.0 );
			entity3.setTheBoolean( false );
			entity3.setTheDate( new Date( now.getTime() + 200000L ) );
			em.persist( entity3 );

			EntityOfBasics entity4 = new EntityOfBasics();
			entity4.setId( 4 );
			entity4.setTheString( "13" );
			entity4.setTheInt( 13 );
			entity4.setTheInteger( 4 );
			entity4.setTheDouble( 13.0 );
			entity4.setTheBoolean( false );
			entity4.setTheDate( new Date( now.getTime() + 300000L ) );
			em.persist( entity4 );

			EntityOfBasics entity5 = new EntityOfBasics();
			entity5.setId( 5 );
			entity5.setTheString( "5" );
			entity5.setTheInt( 5 );
			entity5.setTheInteger( 5 );
			entity5.setTheDouble( 9.0 );
			entity5.setTheBoolean( false );
			em.persist( entity5 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createQuery( "delete from EntityOfBasics" ).executeUpdate() );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	public void testListaggWithoutOrder(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<String> cr = cb.createQuery( String.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<String> function = cb.listagg( null, null, root.get( "theString" ), cb.literal( "," ) );

			cr.select( function );
			List<String> elements = Arrays.asList( session.createQuery( cr ).getSingleResult().split( "," ) );
			List<String> expectedElements = List.of( "13", "5", "5", "6", "7" );
			elements.sort( String.CASE_INSENSITIVE_ORDER );

			assertEquals( expectedElements, elements );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	public void testListagg(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<String> cr = cb.createQuery( String.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<String> function = cb.listagg(
					cb.desc( root.get( "id" ) ),
					null,
					root.get( "theString" ),
					cb.literal( "," )
			);

			cr.select( function );
			String result = session.createQuery( cr ).getSingleResult();
			assertEquals( "5,13,7,6,5", result );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	public void testListaggWithFilter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<String> cr = cb.createQuery( String.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<String> function = cb.listagg(
					cb.desc( root.get( "id" ) ),
					cb.lt( root.get( "theInt" ), cb.literal( 10 ) ),
					root.get( "theString" ),
					cb.literal( "," )
			);

			cr.select( function );
			String result = session.createQuery( cr ).getSingleResult();
			assertEquals( "5,7,6,5", result );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	public void testListaggWithNullsClause(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<String> cr = cb.createQuery( String.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<String> function = cb.listagg(
					cb.desc( root.get( "id" ), true ),
					null,
					root.get( "theString" ),
					cb.literal( "," )
			);

			cr.select( function );
			String result = session.createQuery( cr ).getSingleResult();
			assertEquals( "5,13,7,6,5", result );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsInverseDistributionFunctions.class)
	public void testInverseDistribution(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Integer> cr = cb.createQuery( Integer.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<Integer> function = cb.percentileDisc(
					cb.asc( root.get( "theInt" ) ),
					null,
					cb.literal( 0.5 )
			);

			cr.select( function );
			Integer result = session.createQuery( cr ).getSingleResult();
			assertEquals( 6, result );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsHypotheticalSetFunctions.class)
	public void testHypotheticalSetPercentRank(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Double> cr = cb.createQuery( Double.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<Double> function = cb.percentRank( cb.asc( root.get( "theInt" ) ), null, cb.literal( 5 ) );

			cr.select( function );
			Double result = session.createQuery( cr ).getSingleResult();
			assertEquals( 0.0D, result );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsHypotheticalSetFunctions.class)
	public void testHypotheticalSetRank(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Long> cr = cb.createQuery( Long.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<Long> function = cb.rank( cb.asc( root.get( "theInt" ) ), null, cb.literal( 5 ) );

			cr.select( function );
			Long result = session.createQuery( cr ).getSingleResult();
			assertEquals( 1L, result );
		} );
	}

	// @Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsHypotheticalSetFunctions.class)
	public void testHypotheticalSetRankWithGroupByHavingOrderByLimit(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> cr = cb.createQuery( Tuple.class );
			Root<EntityOfBasics> root1 = cr.from( EntityOfBasics.class );
			Root<EntityOfBasics> root2 = cr.from( EntityOfBasics.class );

			JpaExpression<Long> function = cb.rank( cb.asc( root1.get( "theInt" ) ), null, cb.literal( 5 ) );

			cr.multiselect( root2.get( "id" ), function )
					.groupBy( root2.get( "id" ) ).having( cb.gt( root2.get( "id" ), cb.literal( 1 ) ) )
					.orderBy( cb.asc( cb.literal( 1 ) ), cb.asc( cb.literal( 2 ) ) );

			// todo marco : this test causes problems but only with mssql and db2, the sql obtained is not correct.
			//  we are trying to get something like this query:
			//  select eob2.id, rank(5) within group (order by eob.theInt asc) from EntityOfBasics eob
			//  cross join EntityOfBasics eob2 group by eob2.id having eob2.id > 1 order by 1,2 offset 1

			List<Tuple> resultList = session.createQuery( cr ).setFirstResult( 1 ).getResultList();
			assertEquals( 3, resultList.size() );
			assertEquals( 1L, resultList.get( 0 ).get( 1, Long.class ) );
			assertEquals( 1L, resultList.get( 1 ).get( 1, Long.class ) );
			assertEquals( 1L, resultList.get( 2 ).get( 1, Long.class ) );
		} );
	}

}
