/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaDerivedRoot;
import org.hibernate.query.criteria.JpaSubQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		CriteriaMultiselectGroupByAndOrderByTest.Primary.class,
		CriteriaMultiselectGroupByAndOrderByTest.Secondary.class,
} )
@SessionFactory
public class CriteriaMultiselectGroupByAndOrderByTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Secondary secondaryA = new Secondary( 1, "a" );
			final Secondary secondaryB = new Secondary( 2, "b" );
			final Secondary secondaryC = new Secondary( 3, "c" );
			final ArrayList<Object> entities = new ArrayList<>( List.of(
					new Primary( 1, new BigDecimal( "10" ), secondaryA ),
					new Primary( 2, new BigDecimal( "20" ), secondaryA ),
					new Primary( 3, new BigDecimal( "30" ), secondaryA ),
					new Primary( 4, new BigDecimal( "100" ), secondaryB ),
					new Primary( 5, new BigDecimal( "200" ), secondaryB ),
					new Primary( 6, new BigDecimal( "300" ), secondaryB ),
					new Primary( 7, new BigDecimal( "1000" ), secondaryC ),
					new Primary( 8, new BigDecimal( "2000" ), secondaryC ),
					new Primary( 9, new BigDecimal( "3000" ), secondaryC )
			) );
			entities.addAll( List.of( secondaryA, secondaryB, secondaryC ) );
			entities.forEach( session::persist );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Primary" ).executeUpdate();
			session.createMutationQuery( "delete from Secondary" ).executeUpdate();
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17085" )
	public void testCriteriaGroupBy(SessionFactoryScope scope) {
		executeQuery( scope, false );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17085" )
	public void testCriteriaGroupByAndOrderBy(SessionFactoryScope scope) {
		executeQuery( scope, true );
	}

	private void executeQuery(SessionFactoryScope scope, boolean order) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Tuple> query = cb.createQuery( Tuple.class );
			final Root<Primary> root = query.from( Primary.class );
			final Join<Primary, Secondary> join = root.join( "secondary" );
			query.multiselect(
					join.get( "entityName" ).alias( "secondary_name" ),
					cb.sum( root.get( "amount" ) ).alias( "amount_sum" )
			).groupBy( join );
			if ( order ) {
				query.orderBy( cb.desc( join.get( "entityName" ) ) );
			}
			final List<Tuple> resultList = session.createQuery( query ).getResultList();
			assertThat( resultList ).hasSize( 3 );
			assertThat( resultList.stream().map( tuple -> tuple.get( 0, String.class ) ) ).contains( "a", "b", "c" );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17231" )
	public void testSubqueryGroupBy(SessionFactoryScope scope) {
		executeSubquery( scope, false );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17231" )
	@SkipForDialect( dialectClass = SQLServerDialect.class, reason = "The ORDER BY clause is invalid derived tables, used for tuple in-subquery emulation")
	@SkipForDialect( dialectClass = SybaseDialect.class, reason = "An ORDER BY clause is not allowed in a derived table, used for tuple in-subquery emulation")
	public void testSubqueryGroupByAndOrderBy(SessionFactoryScope scope) {
		executeSubquery( scope, true );
	}

	private void executeSubquery(SessionFactoryScope scope, boolean order) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> query = cb.createTupleQuery();
			final JpaSubQuery<Tuple> subquery = query.subquery( Tuple.class );
			final Root<Primary> sqRoot = subquery.from( Primary.class );
			final Join<Object, Object> secondaryJoin = sqRoot.join( "secondary" );
			subquery.multiselect(
					secondaryJoin.get( "entityName" ).alias( "secondary_name" ),
					cb.sum( sqRoot.get( "amount" ) ).alias( "amount_sum" )
			).groupBy(
					secondaryJoin
			);
			if ( order ) {
				subquery.orderBy(
						cb.desc( secondaryJoin.get( "entityName" ) )
				);
			}
			final JpaDerivedRoot<Tuple> root = query.from( subquery );
			query.multiselect(
					root.get( "secondary_name" ),
					root.get( "amount_sum" )
			);
			final List<Tuple> resultList = session.createQuery( query ).getResultList();
			assertThat( resultList ).hasSize( 3 );
			assertThat( resultList.stream().map( tuple -> tuple.get( 0, String.class ) ) ).contains( "a", "b", "c" );
		} );
	}

	@Entity( name = "Primary" )
	@Table( name = "t_primary" )
	public static class Primary {
		@Id
		private int id;

		private BigDecimal amount;

		@ManyToOne( fetch = FetchType.LAZY )
		private Secondary secondary;

		public Primary() {
		}

		public Primary(int id, BigDecimal amount, Secondary secondary) {
			this.id = id;
			this.amount = amount;
			this.secondary = secondary;
		}

		public Secondary getSecondary() {
			return secondary;
		}

		public BigDecimal getAmount() {
			return amount;
		}
	}

	@Entity( name = "Secondary" )
	@Table( name = "t_secondary" )
	public static class Secondary {
		@Id
		private int id;

		private String entityName;

		public Secondary() {
		}

		public Secondary(int id, String entityName) {
			this.id = id;
			this.entityName = entityName;
		}

		public String getEntityName() {
			return entityName;
		}
	}
}
