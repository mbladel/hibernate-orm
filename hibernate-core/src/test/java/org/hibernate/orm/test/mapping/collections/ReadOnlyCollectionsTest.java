/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JoinFormula;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ReadOnlyCollectionsTest.CollectionsContainer.class,
		ReadOnlyCollectionsTest.TargetEntity.class,
		BasicEntity.class
} )
@SessionFactory
public class ReadOnlyCollectionsTest {
	@Test
	public void testInsert(SessionFactoryScope scope) {
		// todo marco : would be nice to test deletes too !

		final Long containerId = scope.fromTransaction( session -> {
			final TargetEntity te1 = new TargetEntity();
			final TargetEntity te2 = new TargetEntity();
			session.persist( te1 );
			session.persist( te2 );
			final BasicEntity be1 = new BasicEntity( 1, "b1" );
			final BasicEntity be2 = new BasicEntity( 2, "b2" );
			session.persist( be1 );
			session.persist( be2 );
			final CollectionsContainer container = new CollectionsContainer();

			container.oneToMany.add( te1 );
			container.oneToMany.add( te2 );

			container.oneToManyJoinTable.add( be1 );
			container.oneToManyJoinTable.add( be2 );

			container.manyToMany.add( be1 );
			container.manyToMany.add( be2 );

			// This leads to an error: value column is not in insert statement, yet we still try to bind it
			container.elementCollection.add( 1L );
			container.elementCollection.add( 2L );

			container.elementCollectionTable.add( 1L );
			container.elementCollectionTable.add( 2L );
			session.persist( container );

			session.flush();
			return container.id;
		} );

		scope.inSession( session -> {
			final CollectionsContainer container = session.find( CollectionsContainer.class, containerId );
			assertThat( container.oneToMany ).hasSize( 0 );
			assertThat( container.oneToManyJoinTable ).hasSize( 0 );
			assertThat( container.manyToMany ).hasSize( 0 );
			assertThat( container.elementCollection ).hasSize( 0 );
			assertThat( container.elementCollectionTable ).hasSize( 0 );
		} );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		final Long containerId = scope.fromTransaction( session -> {
			final CollectionsContainer container = new CollectionsContainer();
			session.persist( container );
			session.flush();
			return container.id;
		} );

		scope.inTransaction( session -> {
			final TargetEntity te1 = new TargetEntity();
			final TargetEntity te2 = new TargetEntity();
			session.persist( te1 );
			session.persist( te2 );
			final BasicEntity be1 = new BasicEntity( 3, "b1" );
			final BasicEntity be2 = new BasicEntity( 4, "b2" );
			session.persist( be1 );
			session.persist( be2 );
			final CollectionsContainer container = session.find( CollectionsContainer.class, containerId );

			container.oneToMany.add( te1 );
			container.oneToMany.add( te2 );

			container.oneToManyJoinTable.add( be1 );
			container.oneToManyJoinTable.add( be2 );

			container.manyToMany.add( be1 );
			container.manyToMany.add( be2 );

			// This leads to an error: value column is not in insert statement, yet we still try to bind it
			container.elementCollection.add( 1L );
			container.elementCollection.add( 2L );

			container.elementCollectionTable.add( 1L );
			container.elementCollectionTable.add( 2L );
			session.persist( container );
		} );
		scope.inSession( session -> {
			final CollectionsContainer container = session.find( CollectionsContainer.class, containerId );
			assertThat( container.oneToMany ).hasSize( 0 );
			assertThat( container.oneToManyJoinTable ).hasSize( 0 );
			assertThat( container.manyToMany ).hasSize( 0 );
			assertThat( container.elementCollection ).hasSize( 0 );
			assertThat( container.elementCollectionTable ).hasSize( 0 );
		} );
	}

	@Entity( name = "CollectionsContainer" )
	public static class CollectionsContainer {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany
		@JoinColumn( name = "container_id", insertable = false, updatable = false )
		private List<TargetEntity> oneToMany = new ArrayList<>();

		@OneToMany
		@JoinTable(
				name = "one_to_many_join_table",
				joinColumns = @JoinColumn( name = "basic_id", insertable = false, updatable = false ),
				inverseJoinColumns = @JoinColumn( name = "container_id", insertable = false, updatable = false )
		)
		private List<BasicEntity> oneToManyJoinTable = new ArrayList<>();

		@ManyToMany
		@JoinTable(
				name = "many_to_many_join_table",
				joinColumns = @JoinColumn( name = "container_id", insertable = false, updatable = false ),
				inverseJoinColumns = @JoinColumn( name = "basic_id", insertable = false, updatable = false )
		)
		private List<BasicEntity> manyToMany = new ArrayList<>();

		@ElementCollection
		@Column( name = "element_collection_col", insertable = false, updatable = false )
		private List<Long> elementCollection = new ArrayList<>();

		@ElementCollection
		@CollectionTable( name = "el_col_table", joinColumns = @JoinColumn( name = "container_id", insertable = false, updatable = false ) )
		private List<Long> elementCollectionTable = new ArrayList<>();
	}

	@Entity( name = "TargetEntity" )
	public static class TargetEntity {
		@Id
		@GeneratedValue
		private Long id;
	}
}
