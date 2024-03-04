/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.merge;

import org.hibernate.annotations.PartitionKey;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = PartitionKeyStatelessSessionTest.PartitionedEntity.class )
@SessionFactory
public class PartitionKeyStatelessSessionTest {
	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from PartitionedEntity" ).executeUpdate() );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		final PartitionedEntity entity = new PartitionedEntity( 1, 1, "entity_1" );
		scope.inStatelessTransaction( session -> {
			session.insert( entity );
		} );

		scope.inStatelessTransaction( session -> {
			entity.setName( "updated_1" );
			session.update( entity );
		} );

		scope.inStatelessSession( session -> assertThat(
				session.get( PartitionedEntity.class, 1 ).getName()
		).isEqualTo( "updated_1" ) );
	}


	@Test
	public void testUpsertTransient(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.upsert( new PartitionedEntity( 2, 2, "entity_2" ) );
		} );

		scope.inStatelessSession( session -> assertThat(
				session.get( PartitionedEntity.class, 2 ).getName()
		).isEqualTo( "entity_2" ) );
	}

	@Test
	public void testUpsertDetached(SessionFactoryScope scope) {
		final PartitionedEntity entity = new PartitionedEntity( 3, 3, "entity_3" );
		scope.inStatelessTransaction( session -> {
			session.insert( entity );
		} );

		scope.inStatelessTransaction( session -> {
			entity.setName( "updated_3" );
			session.upsert( entity );
		} );

		scope.inStatelessSession( session -> assertThat(
				session.get( PartitionedEntity.class, 3 ).getName()
		).isEqualTo( "updated_3" ) );
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		final PartitionedEntity entity = new PartitionedEntity( 4, 4, "entity_4" );
		scope.inStatelessTransaction( session -> {
			session.insert( entity );
		} );

		scope.inStatelessTransaction( session -> {
			session.delete( entity );
		} );

		scope.inStatelessSession( session -> assertThat( session.get( PartitionedEntity.class, 4 ) ).isNull() );
	}

	@Entity( name = "PartitionedEntity" )
	public static class PartitionedEntity {
		@Id
		private Integer id;

		@PartitionKey
		@Column( name = "partition_key", updatable = false )
		private Integer partitionKey;

		private String name;

		protected PartitionedEntity() {
		}

		public PartitionedEntity(Integer id, Integer partitionKey, String name) {
			this.id = id;
			this.partitionKey = partitionKey;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getPartitionKey() {
			return partitionKey;
		}
	}
}
