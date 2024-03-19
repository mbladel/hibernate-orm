/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.proxy.concrete;

import org.hibernate.Hibernate;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
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
public abstract class AbstractConcreteProxyTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testSingleTable() {
		// test find and association
		inSession( session -> {
			final ParentEntity parent1 = session.find( ParentEntity.class, 1L );
			assertThat( Hibernate.isInitialized( parent1.getSingle() ), is( false ) );
			assertThat( parent1.getSingle(), instanceOf( SingleSubChild1.class ) );
			final SingleSubChild1 proxy = (SingleSubChild1) parent1.getSingle();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
		} );
		// test query and association
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
		// test get reference
		inSession( session -> {
			final SingleChild1 proxy1 = session.getReference( SingleChild1.class, 1L );
			assertThat( proxy1, instanceOf( SingleSubChild1.class ) );
			final SingleBase proxy2 = session.byId( SingleBase.class ).getReference( 2L );
			assertThat( proxy2, instanceOf( SingleChild2.class ) );
		} );
	}

	@Test
	public void testJoined() {
		// test find and association
		inSession( session -> {
			final ParentEntity parent1 = session.find( ParentEntity.class, 1L );
			assertThat( Hibernate.isInitialized( parent1.getJoined() ), is( false ) );
			assertThat( parent1.getJoined(), instanceOf( JoinedSubChild1.class ) );
			final JoinedSubChild1 proxy = (JoinedSubChild1) parent1.getJoined();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
		} );
		// test query and association
		inSession( session -> {
			final ParentEntity parent2 = session.createQuery(
					"from ParentEntity where id = 2",
					ParentEntity.class
			).getSingleResult();
			assertThat( Hibernate.isInitialized( parent2.getJoined() ), is( false ) );
			assertThat( parent2.getJoined(), instanceOf( JoinedChild2.class ) );
			final JoinedChild2 proxy = (JoinedChild2) parent2.getJoined();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
		} );
		// test get reference
		inSession( session -> {
			final JoinedChild1 proxy1 = session.getReference( JoinedChild1.class, 1L );
			assertThat( proxy1, instanceOf( JoinedSubChild1.class ) );
			final JoinedBase proxy2 = session.byId( JoinedBase.class ).getReference( 2L );
			assertThat( proxy2, instanceOf( JoinedChild2.class ) );
		} );
	}

	@Test
	public void testJoinedDisc() {
		// test find and association
		inSession( session -> {
			final ParentEntity parent1 = session.find( ParentEntity.class, 1L );
			assertThat( Hibernate.isInitialized( parent1.getJoinedDisc() ), is( false ) );
			assertThat( parent1.getJoinedDisc(), instanceOf( JoinedDiscSubChild1.class ) );
			final JoinedDiscSubChild1 proxy = (JoinedDiscSubChild1) parent1.getJoinedDisc();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
		} );
		// test query and association
		inSession( session -> {
			final ParentEntity parent2 = session.createQuery(
					"from ParentEntity where id = 2",
					ParentEntity.class
			).getSingleResult();
			assertThat( Hibernate.isInitialized( parent2.getJoinedDisc() ), is( false ) );
			assertThat( parent2.getJoinedDisc(), instanceOf( JoinedDiscChild2.class ) );
			final JoinedDiscChild2 proxy = (JoinedDiscChild2) parent2.getJoinedDisc();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
		} );
		// test get reference
		inSession( session -> {
			final JoinedDiscChild1 single1 = session.getReference( JoinedDiscChild1.class, 1L );
			assertThat( single1, instanceOf( JoinedDiscSubChild1.class ) );
			final JoinedDiscBase single2 = session.byId( JoinedDiscBase.class ).getReference( 2L );
			assertThat( single2, instanceOf( JoinedDiscChild2.class ) );
		} );
	}

	@Test
	public void testUnion() {
		// test find and association
		inSession( session -> {
			final ParentEntity parent1 = session.find( ParentEntity.class, 1L );
			assertThat( Hibernate.isInitialized( parent1.getUnion() ), is( false ) );
			assertThat( parent1.getUnion(), instanceOf( UnionSubChild1.class ) );
			final UnionSubChild1 proxy = (UnionSubChild1) parent1.getUnion();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
		} );
		// test query and association
		inSession( session -> {
			final ParentEntity parent2 = session.createQuery(
					"from ParentEntity where id = 2",
					ParentEntity.class
			).getSingleResult();
			assertThat( Hibernate.isInitialized( parent2.getUnion() ), is( false ) );
			assertThat( parent2.getUnion(), instanceOf( UnionChild2.class ) );
			final UnionChild2 proxy = (UnionChild2) parent2.getUnion();
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
		} );
		// test get reference
		inSession( session -> {
			final UnionChild1 single1 = session.getReference( UnionChild1.class, 1L );
			assertThat( single1, instanceOf( UnionSubChild1.class ) );
			final UnionBase single2 = session.byId( UnionBase.class ).getReference( 2L );
			assertThat( single2, instanceOf( UnionChild2.class ) );
		} );
	}

	@Before
	public void setUp() {
		inTransaction( session -> {
			session.persist( new ParentEntity(
					1L,
					new SingleSubChild1( 1L, "1", "1" ),
					new JoinedSubChild1( 1L, "1", "1" ),
					new JoinedDiscSubChild1( 1L, "1", "1" ),
					new UnionSubChild1( 1L, "1", "1" )
			) );
			session.persist( new ParentEntity(
					2L,
					new SingleChild2( 2L, 2 ),
					new JoinedChild2( 2L, 2 ),
					new JoinedDiscChild2( 2L, 2 ),
					new UnionChild2( 2L, 2 )
			) );
		} );
	}

	@After
	public void tearDown() {
		inTransaction( session -> {
			session.createMutationQuery( "delete from ParentEntity" ).executeUpdate();
			session.createMutationQuery( "delete from SingleBase" ).executeUpdate();
			session.createMutationQuery( "delete from JoinedBase" ).executeUpdate();
			session.createMutationQuery( "delete from JoinedDiscBase" ).executeUpdate();
			session.createMutationQuery( "delete from UnionBase" ).executeUpdate();
		} );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		sources.addAnnotatedClass( ParentEntity.class );
		sources.addAnnotatedClass( SingleBase.class );
		sources.addAnnotatedClass( SingleChild1.class );
		sources.addAnnotatedClass( SingleSubChild1.class );
		sources.addAnnotatedClass( SingleChild2.class );
		sources.addAnnotatedClass( JoinedBase.class );
		sources.addAnnotatedClass( JoinedChild1.class );
		sources.addAnnotatedClass( JoinedSubChild1.class );
		sources.addAnnotatedClass( JoinedChild2.class );
		sources.addAnnotatedClass( JoinedDiscBase.class );
		sources.addAnnotatedClass( JoinedDiscChild1.class );
		sources.addAnnotatedClass( JoinedDiscSubChild1.class );
		sources.addAnnotatedClass( JoinedDiscChild2.class );
		sources.addAnnotatedClass( UnionBase.class );
		sources.addAnnotatedClass( UnionChild1.class );
		sources.addAnnotatedClass( UnionSubChild1.class );
		sources.addAnnotatedClass( UnionChild2.class );
	}

	@Entity( name = "ParentEntity" )
	public static class ParentEntity {
		@Id
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		private SingleBase single;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		private JoinedBase joined;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		private JoinedDiscBase joinedDisc;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		private UnionBase union;

		public ParentEntity() {
		}

		public ParentEntity(Long id, SingleBase single, JoinedBase joined, JoinedDiscBase joinedDisc, UnionBase union) {
			this.id = id;
			this.single = single;
			this.joined = joined;
			this.joinedDisc = joinedDisc;
			this.union = union;
		}

		public SingleBase getSingle() {
			return single;
		}

		public JoinedBase getJoined() {
			return joined;
		}

		public JoinedDiscBase getJoinedDisc() {
			return joinedDisc;
		}

		public UnionBase getUnion() {
			return union;
		}
	}

	// InheritanceType.SINGLE_TABLE

	@Entity( name = "SingleBase" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "disc_col" )
	@ConcreteProxy
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

	// InheritanceType.JOINED

	@Entity( name = "JoinedBase" )
	@Inheritance( strategy = InheritanceType.JOINED )
	@ConcreteProxy
	public static class JoinedBase {
		@Id
		private Long id;

		public JoinedBase() {
		}

		public JoinedBase(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "JoinedChild1" )
	public static class JoinedChild1 extends JoinedBase {
		private String child1Prop;

		public JoinedChild1() {
		}

		public JoinedChild1(Long id, String child1Prop) {
			super( id );
			this.child1Prop = child1Prop;
		}
	}

	@Entity( name = "JoinedSubChild1" )
	public static class JoinedSubChild1 extends JoinedChild1 {
		private String subChild1Prop;

		public JoinedSubChild1() {
		}

		public JoinedSubChild1(Long id, String child1Prop, String subChild1Prop) {
			super( id, child1Prop );
			this.subChild1Prop = subChild1Prop;
		}
	}

	@Entity( name = "JoinedChild2" )
	public static class JoinedChild2 extends JoinedBase {
		private Integer child2Prop;

		public JoinedChild2() {
		}

		public JoinedChild2(Long id, Integer child2Prop) {
			super( id );
			this.child2Prop = child2Prop;
		}
	}

	// InheritanceType.JOINED + @DiscriminatorColumn

	@Entity( name = "JoinedDiscBase" )
	@Inheritance( strategy = InheritanceType.JOINED )
	@DiscriminatorColumn( name = "disc_col" )
	@ConcreteProxy
	public static class JoinedDiscBase {
		@Id
		private Long id;

		public JoinedDiscBase() {
		}

		public JoinedDiscBase(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "JoinedDiscChild1" )
	public static class JoinedDiscChild1 extends JoinedDiscBase {
		private String child1Prop;

		public JoinedDiscChild1() {
		}

		public JoinedDiscChild1(Long id, String child1Prop) {
			super( id );
			this.child1Prop = child1Prop;
		}
	}

	@Entity( name = "JoinedDiscSubChild1" )
	public static class JoinedDiscSubChild1 extends JoinedDiscChild1 {
		private String subChild1Prop;

		public JoinedDiscSubChild1() {
		}

		public JoinedDiscSubChild1(Long id, String child1Prop, String subChild1Prop) {
			super( id, child1Prop );
			this.subChild1Prop = subChild1Prop;
		}
	}

	@Entity( name = "JoinedDiscChild2" )
	public static class JoinedDiscChild2 extends JoinedDiscBase {
		private Integer child2Prop;

		public JoinedDiscChild2() {
		}

		public JoinedDiscChild2(Long id, Integer child2Prop) {
			super( id );
			this.child2Prop = child2Prop;
		}
	}

	// InheritanceType.TABLE_PER_CLASS

	@Entity( name = "UnionBase" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	@ConcreteProxy
	public static class UnionBase {
		@Id
		private Long id;

		public UnionBase() {
		}

		public UnionBase(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "UnionChild1" )
	public static class UnionChild1 extends UnionBase {
		private String child1Prop;

		public UnionChild1() {
		}

		public UnionChild1(Long id, String child1Prop) {
			super( id );
			this.child1Prop = child1Prop;
		}
	}

	@Entity( name = "UnionSubChild1" )
	public static class UnionSubChild1 extends UnionChild1 {
		private String subChild1Prop;

		public UnionSubChild1() {
		}

		public UnionSubChild1(Long id, String child1Prop, String subChild1Prop) {
			super( id, child1Prop );
			this.subChild1Prop = subChild1Prop;
		}
	}

	@Entity( name = "UnionChild2" )
	public static class UnionChild2 extends UnionBase {
		private Integer child2Prop;

		public UnionChild2() {
		}

		public UnionChild2(Long id, Integer child2Prop) {
			super( id );
			this.child2Prop = child2Prop;
		}
	}
}
