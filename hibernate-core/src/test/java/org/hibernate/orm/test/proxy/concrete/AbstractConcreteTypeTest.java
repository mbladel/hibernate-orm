/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.proxy.concrete;

import org.hibernate.Hibernate;
import org.hibernate.annotations.ConcreteType;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * @author Marco Belladelli
 */
public abstract class AbstractConcreteTypeTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		sources.addAnnotatedClass( ParentEntity.class );
		sources.addAnnotatedClass( SingleBase.class );
		sources.addAnnotatedClass( SingleChild1.class );
		sources.addAnnotatedClass( SingleSubChild1.class );
		sources.addAnnotatedClass( SingleChild2.class );
	}


	@Test
	public void testSingleTable() {
		inSession( session -> {
			final ParentEntity parent1 = session.find( ParentEntity.class, 1L );
			assertThat( Hibernate.isInitialized( parent1.getSingle() ), is( false ) );
			assertThat( parent1.getSingle(), instanceOf( SingleSubChild1.class ) );
			final SingleSubChild1 proxy = (SingleSubChild1) parent1.getSingle();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
		} );
		inSession( session -> {
			final ParentEntity parent2 = session.createQuery(
					"from ParentEntity where id = 2",
					ParentEntity.class
			).getSingleResult();
			assertThat( Hibernate.isInitialized( parent2.getSingle() ), is( false ) );
			assertThat( parent2.getSingle(), instanceOf( SingleChild2.class ) );
			final SingleChild2 proxy = (SingleChild2) parent2.getSingle();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
		} );
	}

	@Before
	public void setUp() {
		inTransaction( session -> {
			session.persist( new ParentEntity( 1L, new SingleSubChild1( 1L, "1", "1" ) ) );
			session.persist( new ParentEntity( 2L, new SingleChild2( 2L, 2 ) ) );
		} );
	}

	@Entity( name = "ParentEntity" )
	public static class ParentEntity {
		@Id
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		private SingleBase single;

		public ParentEntity() {
		}

		public ParentEntity(Long id, SingleBase single) {
			this.id = id;
			this.single = single;
		}

		public SingleBase getSingle() {
			return single;
		}
	}


	@Entity( name = "SingleBase" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@ConcreteType
	public static class SingleBase {
		@Id
		private Long id;

		public SingleBase() {
		}

		public SingleBase(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "SingleChild1" )
	public static class SingleChild1 extends SingleBase {
		private String child1Prop;

		public SingleChild1() {
		}

		public SingleChild1(Long id, String child1Prop) {
			super( id );
			this.child1Prop = child1Prop;
		}
	}

	@Entity( name = "SingleSubChild1" )
	public static class SingleSubChild1 extends SingleChild1 {
		private String subChild1Prop;

		public SingleSubChild1() {
		}

		public SingleSubChild1(Long id, String child1Prop, String subChild1Prop) {
			super( id, child1Prop );
			this.subChild1Prop = subChild1Prop;
		}
	}

	@Entity( name = "SingleChild2" )
	public static class SingleChild2 extends SingleBase {
		private Integer child2Prop;

		public SingleChild2() {
		}

		public SingleChild2(Long id, Integer child2Prop) {
			super( id );
			this.child2Prop = child2Prop;
		}
	}
}
