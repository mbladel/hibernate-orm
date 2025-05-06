/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetch;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		CollectionInSubselectFetchTest.ParentEntity.class,
		CollectionInSubselectFetchTest.IntermediateEntity.class,
		CollectionInSubselectFetchTest.ChildEntity.class,
} )
@SessionFactory
public class CollectionInSubselectFetchTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final IntermediateEntity intermediateEntity = new IntermediateEntity();
			intermediateEntity.setId( 99L );
			intermediateEntity.setName( "IntermediateEntity" );
			session.persist( intermediateEntity );

			final ChildEntity child1 = new ChildEntity();
			child1.setId( 1L );
			child1.setName( "child_1" );
			child1.setIntermediateEntity( intermediateEntity );
			session.persist( child1 );

			final ChildEntity child2 = new ChildEntity();
			child2.setId( 2L );
			child2.setName( "child_2" );
			child2.setIntermediateEntity( intermediateEntity );
			session.persist( child2 );

			final ParentEntity parentEntity = new ParentEntity();
			parentEntity.setId( 3L );
			parentEntity.setName( "ParentEntity" );
			parentEntity.setIntermediateEntity( intermediateEntity );
			session.persist( parentEntity );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		final List<ChildEntity> children = scope.fromSession( session -> {
			// populates 2LC
			final ParentEntity parentEntity = session.find( ParentEntity.class, 3L );
			assertThat( Hibernate.isInitialized( parentEntity.getIntermediateEntity().getChildEntities() ) ).isFalse();

			// executes nested join fetch query
			final ParentEntity result = session.createNamedQuery( "ParentEntity.select", ParentEntity.class )
					.getSingleResult();
			assertThat( Hibernate.isInitialized( result.getIntermediateEntity().getChildEntities() ) ).isTrue();
			return result.getIntermediateEntity().getChildEntities();
		} );
		assertThat( children ).hasSize( 2 );
		assertThat( children ).extracting( "id" ).containsExactlyInAnyOrder( 1L, 2L );
	}

	@Entity(name = "ParentEntity")
	@NamedQuery(name = "ParentEntity.select",
			query = "SELECT pe" +
					"  FROM ParentEntity pe" +
					" INNER JOIN FETCH pe.intermediateEntity ie" +
					"  LEFT JOIN FETCH ie.childEntities")
	static class ParentEntity {
		@Id
		private Long id;

		private String name;

		@ManyToOne
		@Fetch(value = FetchMode.SELECT)
		private IntermediateEntity intermediateEntity;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public IntermediateEntity getIntermediateEntity() {
			return intermediateEntity;
		}

		public void setIntermediateEntity(IntermediateEntity intermediateEntity) {
			this.intermediateEntity = intermediateEntity;
		}
	}

	@Entity(name = "IntermediateEntity")
	static class IntermediateEntity {
		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "intermediateEntity", cascade = CascadeType.ALL)
		private List<ChildEntity> childEntities = new ArrayList<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<ChildEntity> getChildEntities() {
			return childEntities;
		}

		public void setChildEntities(List<ChildEntity> childEntities) {
			this.childEntities = childEntities;
		}
	}

	@Entity(name = "ChildEntity")
	static class ChildEntity {
		@Id
		private Long id;

		private String name;

		@ManyToOne
		@Fetch(value = FetchMode.SELECT)
		private IntermediateEntity intermediateEntity;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public IntermediateEntity getIntermediateEntity() {
			return intermediateEntity;
		}

		public void setIntermediateEntity(IntermediateEntity intermediateEntity) {
			this.intermediateEntity = intermediateEntity;
		}
	}
}
