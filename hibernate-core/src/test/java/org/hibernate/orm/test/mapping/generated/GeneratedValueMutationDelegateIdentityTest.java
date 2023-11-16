/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.generated;

import java.util.Calendar;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.MutationGeneratedValuesDelegate;
import org.hibernate.id.insert.UniqueKeySelectingDelegate;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.MutationType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		GeneratedValueMutationDelegateIdentityTest.IdentityOnly.class,
		GeneratedValueMutationDelegateIdentityTest.IdentityAndValues.class,
		GeneratedValueMutationDelegateIdentityTest.IdentityAndValuesAndRowId.class,
		GeneratedValueMutationDelegateIdentityTest.IdentityAndValuesAndRowIdAndNaturalId.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsIdentityColumns.class )
public class GeneratedValueMutationDelegateIdentityTest {
	@Test
	public void testInsertGenerationIdentityOnly(SessionFactoryScope scope) {
		final MutationGeneratedValuesDelegate delegate = getDelegate( scope, IdentityOnly.class, MutationType.INSERT );
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final IdentityOnly entity = new IdentityOnly();
			session.persist( entity );
			session.flush();

			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isNull();

			inspector.assertIsInsert( 0 );
			inspector.assertExecutedCount( delegate != null ? 1 : 2 );
		} );
	}

	@Test
	public void testInsertGenerationValuesAndIdentity(SessionFactoryScope scope) {
		final MutationGeneratedValuesDelegate delegate = getDelegate(
				scope,
				IdentityAndValues.class,
				MutationType.INSERT
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final IdentityAndValues entity = new IdentityAndValues();
			session.persist( entity );
			session.flush();

			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "default_name" );

			inspector.assertIsInsert( 0 );
			inspector.assertExecutedCount(
					delegate != null && delegate.supportsRetrievingGeneratedValues() ? 1 : 2
			);
		} );
	}

	@Test
	public void testUpdateGenerationAndIdentity(SessionFactoryScope scope) {
		final MutationGeneratedValuesDelegate delegate = getDelegate(
				scope,
				IdentityAndValues.class,
				MutationType.UPDATE
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		final Integer id = scope.fromTransaction( session -> {
			final IdentityAndValues entity = new IdentityAndValues();
			session.persist( entity );
			session.flush();
			return entity.getId();
		} );

		inspector.clear();

		scope.inTransaction( session -> {
			final IdentityAndValues entity = session.find( IdentityAndValues.class, id );
			entity.setData( "changed" );
			session.flush();

			assertThat( entity.getUpdateDate() ).isNotNull();

			inspector.assertIsSelect( 0 );
			inspector.assertIsUpdate( 1 );
			inspector.assertExecutedCount(
					delegate != null && delegate.supportsRetrievingGeneratedValues() ? 2 : 3
			);
		} );
	}

	@Test
	public void testInsertGenerationValuesAndIdentityAndRowId(SessionFactoryScope scope) {
		final MutationGeneratedValuesDelegate delegate = getDelegate(
				scope,
				IdentityAndValuesAndRowId.class,
				MutationType.INSERT
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final IdentityAndValuesAndRowId entity = new IdentityAndValuesAndRowId();
			session.persist( entity );
			session.flush();

			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "default_name" );

			inspector.assertIsInsert( 0 );
			inspector.assertExecutedCount(
					delegate != null && delegate.supportsRetrievingGeneratedValues() ? 1 : 2
			);

			final boolean shouldHaveRowId = delegate != null && delegate.supportsRetrievingRowId();
			if ( shouldHaveRowId ) {
				// assert row-id was populated in entity entry
				final PersistenceContext pc = session.getPersistenceContextInternal();
				final EntityEntry entry = pc.getEntry( entity );
				assertThat( entry.getRowId() ).isNotNull();
			}

			// test update in same transaction
			inspector.clear();

			entity.setData( "changed" );
			session.flush();

			assertThat( entity.getUpdateDate() ).isNotNull();

			inspector.assertIsUpdate( 0 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "id_column", shouldHaveRowId ? 0 : 1 );
		} );
		scope.inSession( session -> assertThat( session.find(
				GeneratedValueMutationDelegateTest.ValuesAndRowId.class,
				1
		).getUpdateDate() ).isNotNull() );
	}

	@Test
	public void testInsertGenerationValuesAndIdentityAndRowIdAndNaturalId(SessionFactoryScope scope) {
		final MutationGeneratedValuesDelegate delegate = getDelegate(
				scope,
				IdentityAndValuesAndRowIdAndNaturalId.class,
				MutationType.INSERT
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final IdentityAndValuesAndRowIdAndNaturalId entity = new IdentityAndValuesAndRowIdAndNaturalId();
			session.persist( entity );
			session.flush();

			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "default_name" );

			inspector.assertIsInsert( 0 );
			final boolean isUniqueKeyDelegate = delegate instanceof UniqueKeySelectingDelegate;
			inspector.assertExecutedCount(
					delegate == null || !delegate.supportsRetrievingGeneratedValues() || isUniqueKeyDelegate ? 2 : 1
			);
			if ( isUniqueKeyDelegate ) {
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "data", 1 );
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "id_column", 0 );
			}

			final boolean shouldHaveRowId = delegate != null && delegate.supportsRetrievingRowId();
			if ( shouldHaveRowId ) {
				// assert row-id was populated in entity entry
				final PersistenceContext pc = session.getPersistenceContextInternal();
				final EntityEntry entry = pc.getEntry( entity );
				assertThat( entry.getRowId() ).isNotNull();
			}
		} );
	}
	private static MutationGeneratedValuesDelegate getDelegate(
			SessionFactoryScope scope,
			Class<?> entityClass,
			MutationType mutationType) {
		final EntityMutationTarget entityDescriptor = (EntityMutationTarget) scope.getSessionFactory()
				.getMappingMetamodel().findEntityDescriptor( entityClass );
		return entityDescriptor.getMutationDelegate( mutationType );
	}

	@Entity( name = "IdentityOnly" )
	public static class IdentityOnly {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "IdentityAndValues" )
	@SuppressWarnings( "unused" )
	public static class IdentityAndValues {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@UpdateTimestamp( source = SourceType.DB )
		private Calendar updateDate;

		private String data;

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Calendar getUpdateDate() {
			return updateDate;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@RowId
	@Entity( name = "IdentityAndValuesAndRowId" )
	@SuppressWarnings( "unused" )
	public static class IdentityAndValuesAndRowId {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@UpdateTimestamp( source = SourceType.DB )
		private Calendar updateDate;

		private String data;

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Calendar getUpdateDate() {
			return updateDate;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@RowId
	@Entity( name = "IdentityAndValuesAndRowIdAndNaturalId" )
	@SuppressWarnings( "unused" )
	public static class IdentityAndValuesAndRowIdAndNaturalId {
		@Id
		@Column( name = "id_column" )
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@NaturalId
		private String data;

		public IdentityAndValuesAndRowIdAndNaturalId() {
		}

		private IdentityAndValuesAndRowIdAndNaturalId(Integer id, String data) {
			this.id = id;
			this.data = data;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
