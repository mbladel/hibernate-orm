/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.generated;

import java.util.Date;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.orm.test.common.JournalingBatchObserver;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		BatchGeneratedValuesTest.IdentityOnly.class,
		BatchGeneratedValuesTest.IdentityAndValues.class,
		BatchGeneratedValuesTest.IdentityAndValuesAndNaturalId.class,
} )
@SessionFactory
@ServiceRegistry( settings = @Setting( name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "2" ) )
public class BatchGeneratedValuesTest {
	@Test
	public void testIdentityOnly(SessionFactoryScope scope) {
		// insert
		scope.inStatelessTransaction( session -> {
			// insert the first entity to initialize batch
			final IdentityOnly first = new IdentityOnly();
			first.setData( "object_1" );
			session.batchInsert( first );

			final Batch batch = ( (SharedSessionContractImplementor) session ).getJdbcCoordinator().getBatch(
					getBatchKey( scope, IdentityOnly.class, MutationType.INSERT ),
					null,
					null
			);
			assertThat( batch ).isNotNull();
			final JournalingBatchObserver observer = new JournalingBatchObserver();
			batch.addObserver( observer );

			for ( int i = 2; i <= 10; i++ ) {
				final IdentityOnly object = new IdentityOnly();
				object.setData( "object_" + i );
				session.batchInsert( object );
				// implicit executions should happen once every 2 inserts
				assertThat( observer.getImplicitExecutionCount() ).isEqualTo( i >> 1 );
				assertThat( observer.getExplicitExecutionCount() ).isEqualTo( 0 );
			}
		} );
		scope.inStatelessSession( session -> assertThat( session.createQuery(
				"select id from IdentityOnly",
				Long.class
		).getResultList() ).hasSize( 10 ).doesNotHaveDuplicates() );
	}

	@Test
	public void testInsertIdentityAndValues(SessionFactoryScope scope) {
		// insert
		scope.inStatelessTransaction( session -> {
			// insert the first entity to initialize batch
			final IdentityAndValues first = new IdentityAndValues();
			first.setData( "object_1" );
			session.batchInsert( first );

			final Batch batch = ( (SharedSessionContractImplementor) session ).getJdbcCoordinator().getBatch(
					getBatchKey( scope, IdentityAndValues.class, MutationType.INSERT ),
					null,
					null
			);
			assertThat( batch ).isNotNull();
			final JournalingBatchObserver observer = new JournalingBatchObserver();
			batch.addObserver( observer );

			for ( int i = 2; i <= 10; i++ ) {
				final IdentityAndValues object = new IdentityAndValues();
				object.setData( "object_" + i );
				session.batchInsert( object );
				// implicit executions should happen once every 2 inserts
				assertThat( observer.getImplicitExecutionCount() ).isEqualTo( i >> 1 );
				assertThat( observer.getExplicitExecutionCount() ).isEqualTo( 0 );
			}
		} );
		scope.inStatelessSession( session -> assertThat( session.createQuery(
				"select id from IdentityAndValues",
				Long.class
		).getResultList() ).hasSize( 10 ).doesNotHaveDuplicates() );
	}

	@Test
	public void testInsertIdentityAndValuesAndNaturalId(SessionFactoryScope scope) {
		// insert
		scope.inStatelessTransaction( session -> {
			// insert the first entity to initialize batch
			final IdentityAndValuesAndNaturalId first = new IdentityAndValuesAndNaturalId();
			first.setData( "object_1" );
			session.batchInsert( first );

			final Batch batch = ( (SharedSessionContractImplementor) session ).getJdbcCoordinator().getBatch(
					getBatchKey( scope, IdentityAndValuesAndNaturalId.class, MutationType.INSERT ),
					null,
					null
			);
			assertThat( batch ).isNotNull();
			final JournalingBatchObserver observer = new JournalingBatchObserver();
			batch.addObserver( observer );

			for ( int i = 2; i <= 10; i++ ) {
				final IdentityAndValuesAndNaturalId object = new IdentityAndValuesAndNaturalId();
				object.setData( "object_" + i );
				session.batchInsert( object );
				// implicit executions should happen once every 2 inserts
				assertThat( observer.getImplicitExecutionCount() ).isEqualTo( i >> 1 );
				assertThat( observer.getExplicitExecutionCount() ).isEqualTo( 0 );
			}
		} );
		scope.inStatelessSession( session -> assertThat( session.createQuery(
				"select id from IdentityAndValuesAndNaturalId",
				Long.class
		).getResultList() ).hasSize( 10 ).doesNotHaveDuplicates() );
	}

	private BatchKey getBatchKey(SessionFactoryScope scope, Class<?> entityClass, MutationType mutationType) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory().getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor( entityClass );
		return new BasicBatchKey( entityDescriptor.getEntityName() + "#" + mutationType.name() );
	}

	@Entity( name = "IdentityOnly" )
	static class IdentityOnly {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Long id;

		private String data;

		public Long getId() {
			return id;
		}

		public String getData() {
			return data;
		}

		public void setData(String name) {
			this.data = name;
		}
	}

	@Entity( name = "IdentityAndValues" )
	@SuppressWarnings( "unused" )
	public static class IdentityAndValues {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Long id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@UpdateTimestamp( source = SourceType.DB )
		private Date updateDate;

		private String data;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Date getUpdateDate() {
			return updateDate;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@Entity( name = "IdentityAndValuesAndNaturalId" )
	@SuppressWarnings( "unused" )
	public static class IdentityAndValuesAndNaturalId {
		@Id
		@Column( name = "id_column" )
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Long id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@NaturalId
		private String data;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setData(String data) {
			this.data = data;
		}
	}
}
