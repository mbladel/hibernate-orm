/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.generated;

import java.util.Date;
import java.util.List;

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
		StatelessSessionBatchGeneratedValuesTest.IdentityOnly.class,
		StatelessSessionBatchGeneratedValuesTest.IdentityAndValues.class,
		StatelessSessionBatchGeneratedValuesTest.IdentityAndValuesAndNaturalId.class,
} )
@SessionFactory
@ServiceRegistry( settings = @Setting( name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "2" ) )
public class StatelessSessionBatchGeneratedValuesTest {
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

			assertThat( session.createQuery( "from IdentityOnly", IdentityOnly.class ).getResultList() ).hasSize( 10 )
					.extracting( IdentityOnly::getId )
					.doesNotHaveDuplicates();
		} );
	}

	@Test
	public void testIdentityAndValues(SessionFactoryScope scope) {
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
		// update
		scope.inStatelessTransaction( session -> {
			final List<IdentityAndValues> entityList = session.createQuery(
					"from IdentityAndValues order by id",
					IdentityAndValues.class
			).getResultList();
			assertThat( entityList ).hasSize( 10 )
					.allMatch( e -> e.getId() != null && e.getName() != null && e.getUpdateDate() != null );
			final Date max = entityList.stream()
					.map( IdentityAndValues::getUpdateDate )
					.max( Date::compareTo )
					.orElseThrow();

			// update the first entity to initialize batch
			final IdentityAndValues first = entityList.get( 0 );
			first.setData( "updated_1" );
			session.batchUpdate( first );

			final Batch batch = ( (SharedSessionContractImplementor) session ).getJdbcCoordinator().getBatch(
					getBatchKey( scope, IdentityAndValues.class, MutationType.UPDATE ),
					null,
					null
			);
			final JournalingBatchObserver observer = new JournalingBatchObserver();
			batch.addObserver( observer );

			for ( int i = 2; i <= 10; i++ ) {
				final IdentityAndValues object = entityList.get( i - 1 );
				object.setData( "updated_" + i );
				session.batchUpdate( object );
				// implicit executions should happen once every 2 inserts
				assertThat( observer.getImplicitExecutionCount() ).isEqualTo( i >> 1 );
				assertThat( observer.getExplicitExecutionCount() ).isEqualTo( 0 );
			}

			// assert that all update timestamps were increased
			assertThat( session.createQuery( "from IdentityAndValues order by id", IdentityAndValues.class )
								.getResultList() ).hasSize( 10 )
					.extracting( IdentityAndValues::getUpdateDate )
					.allMatch( d -> d.compareTo( max ) > 0 );
		} );
		// deletes are always batched, no need to test
	}

	@Test
	public void testInsertIdentityAndValuesAndNaturalId(SessionFactoryScope scope) {
		// insert
		scope.inStatelessTransaction( session -> {
			// insert the first entity to initialize batch
			final IdentityAndValuesAndNaturalId first = new IdentityAndValuesAndNaturalId();
			first.setData( "object_1" );
			first.setNaturalId( 1 );
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
				object.setNaturalId( i );
				session.batchInsert( object );
				// implicit executions should happen once every 2 inserts
				assertThat( observer.getImplicitExecutionCount() ).isEqualTo( i >> 1 );
				assertThat( observer.getExplicitExecutionCount() ).isEqualTo( 0 );
			}
		} );
		// update
		scope.inStatelessTransaction( session -> {
			final List<IdentityAndValuesAndNaturalId> entityList = session.createQuery(
					"from IdentityAndValuesAndNaturalId order by id",
					IdentityAndValuesAndNaturalId.class
			).getResultList();
			assertThat( entityList ).hasSize( 10 )
					.allMatch( e -> e.getId() != null && e.getName() != null && e.getUpdateDate() != null );
			final Date max = entityList.stream()
					.map( IdentityAndValuesAndNaturalId::getUpdateDate )
					.max( Date::compareTo )
					.orElseThrow();

			// update the first entity to initialize batch
			final IdentityAndValuesAndNaturalId first = entityList.get( 0 );
			first.setData( "updated_1" );
			session.batchUpdate( first );

			final Batch batch = ( (SharedSessionContractImplementor) session ).getJdbcCoordinator().getBatch(
					getBatchKey( scope, IdentityAndValuesAndNaturalId.class, MutationType.UPDATE ),
					null,
					null
			);
			final JournalingBatchObserver observer = new JournalingBatchObserver();
			batch.addObserver( observer );

			for ( int i = 2; i <= 10; i++ ) {
				final IdentityAndValuesAndNaturalId object = entityList.get( i - 1 );
				object.setData( "updated_" + i );
				session.batchUpdate( object );
				// implicit executions should happen once every 2 inserts
				assertThat( observer.getImplicitExecutionCount() ).isEqualTo( i >> 1 );
				assertThat( observer.getExplicitExecutionCount() ).isEqualTo( 0 );
			}

			// assert that all update timestamps were increased
			assertThat( session.createQuery(
					"from IdentityAndValuesAndNaturalId order by id",
					IdentityAndValuesAndNaturalId.class
			).getResultList() ).hasSize( 10 )
					.extracting( IdentityAndValuesAndNaturalId::getUpdateDate )
					.allMatch( d -> d.compareTo( max ) > 0 );
		} );
		// deletes are always batched, no need to test
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

		@UpdateTimestamp( source = SourceType.DB )
		private Date updateDate;

		@NaturalId
		private Integer naturalId;

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

		public void setNaturalId(Integer naturalId) {
			this.naturalId = naturalId;
		}
	}
}
