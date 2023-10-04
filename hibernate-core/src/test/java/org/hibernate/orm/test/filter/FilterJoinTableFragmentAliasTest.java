/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.filter;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.SqlFragmentAlias;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		FilterJoinTableFragmentAliasTest.BarEntity.class,
		FilterJoinTableFragmentAliasTest.FooEntity.class,
} )
@SessionFactory
public class FilterJoinTableFragmentAliasTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final BarEntity bar1 = new BarEntity( 1L, "bar_1" );
			final BarEntity bar2 = new BarEntity( 2L, "bar_2" );
			session.persist( bar1 );
			session.persist( bar2 );
			final FooEntity foo1 = new FooEntity( "foo_1", "initial" );
			final FooEntity foo2 = new FooEntity( "foo_2", "migrated" );
			foo1.getBars().addAll( Set.of( bar1, bar2 ) );
			foo2.getBars().addAll( Set.of( bar1, bar2 ) );
			session.persist( foo1 );
			session.persist( foo2 );
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "filterMigrated" );
			final BarEntity bar = session.find( BarEntity.class, 1L );
			assertThat( bar.getFoos().size() ).isEqualTo( 1 );
		} );
	}

	@Entity( name = "FooEntity" )
	public static class FooEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private String status;

		@ManyToMany
		@JoinTable(
				name = "foo_bar",
				joinColumns = { @JoinColumn( name = "foo_id", referencedColumnName = "id" ) },
				inverseJoinColumns = { @JoinColumn( name = "bar_id", referencedColumnName = "id" ) }
		)
		private Set<BarEntity> bars = new HashSet<>();

		public FooEntity() {
		}

		public FooEntity(String name, String status) {
			this.name = name;
			this.status = status;
		}

		public Set<BarEntity> getBars() {
			return bars;
		}
	}

	@Entity( name = "BarEntity" )
	@FilterDef( name = "filterMigrated" )
	public static class BarEntity {
		@Id
		private Long id;

		private String name;

		@ManyToMany( mappedBy = "bars" )
		@FilterJoinTable(
				name = "filterMigrated",
				condition = "{jt}.foo_id in(select id from FooEntity where status <> 'migrated')",
				aliases = { @SqlFragmentAlias( alias = "jt", table = "foo_bar" ) }
		)
		private Set<FooEntity> foos = new HashSet<>();

		public BarEntity() {
		}

		public BarEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Set<FooEntity> getFoos() {
			return foos;
		}
	}
}
