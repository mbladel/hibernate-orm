/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.filter;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SQLRestriction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		FilteredCollectionsTest.MyEntity.class,
		FilteredCollectionsTest.ChildEntity.class,
} )
@SessionFactory
public class FilteredCollectionsTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ChildEntity child1 = new ChildEntity( 3 );
			session.persist( child1 );
			final ChildEntity child2 = new ChildEntity( 4 );
			session.persist( child2 );
			final ChildEmbeddable child3 = new ChildEmbeddable( 3 );
			final MyEntity myEntity = new MyEntity();
			myEntity.oneToOne.add( child1 );
			myEntity.manyToMany.add( child2 );
			myEntity.elementCollection.add( child3 );
			session.persist( myEntity );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "to_one_filter" );
			session.enableFilter( "child_id_filter" );
			final MyEntity myEntity = session.find( MyEntity.class, 1L );
			myEntity.oneToOne.size();
			myEntity.manyToMany.size();
			myEntity.elementCollection.size();
		} );
	}

	@Entity( name = "MyEntity" )
	@FilterDef( name = "to_one_filter" )
	@FilterDef( name = "child_id_filter" )
	public static class MyEntity {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany
		@JoinTable( name = "one_to_many_join_table", joinColumns = @JoinColumn( name = "owner_id" ) )
		@SQLRestriction( "age > 0" )
		@SQLJoinTableRestriction( "owner_id > 0" )
		private List<ChildEntity> oneToOne = new ArrayList<>();

		@ManyToMany
		@JoinTable( name = "many_to_many_join_table", joinColumns = @JoinColumn( name = "owner_id" ) )
		@SQLRestriction( "age > 0" )
		@SQLJoinTableRestriction( "owner_id > 0" )
		private List<ChildEntity> manyToMany = new ArrayList<>();

		@ElementCollection
		@CollectionTable( name = "collection_table", joinColumns = @JoinColumn( name = "owner_id" ) )
		@SQLRestriction( "age > 0" )
		private List<ChildEmbeddable> elementCollection = new ArrayList<>();
	}

	@Embeddable
	public static class ChildEmbeddable {
		private int age;

		public ChildEmbeddable() {
		}

		public ChildEmbeddable(int age) {
			this.age = age;
		}
	}

	@Entity( name = "ChildEntity" )
	public static class ChildEntity {
		@Id
		@GeneratedValue
		private Long id;

		private int age;

		public ChildEntity() {
		}

		public ChildEntity(int age) {
			this.age = age;
		}
	}
}
