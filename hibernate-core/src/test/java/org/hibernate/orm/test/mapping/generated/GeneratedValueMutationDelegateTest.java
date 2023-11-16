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
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.insert.UniqueKeySelectingDelegate;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.MutationType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		GeneratedValueMutationDelegateTest.ValuesOnly.class,
		GeneratedValueMutationDelegateTest.ValuesAndRowId.class,
		GeneratedValueMutationDelegateTest.ValuesAndNaturalId.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
public class GeneratedValueMutationDelegateTest {
	@Test
	public void testInsertGenerationValuesOnly(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate( scope, ValuesOnly.class, MutationType.INSERT );
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		// todo marco : add tests for multi-table situations ?
		//  - joined inheritance
		//  - secondary table???

		scope.inTransaction( session -> {
			final ValuesOnly entity = new ValuesOnly( 1 );
			session.persist( entity );
			session.flush();

			assertThat( entity.getName() ).isEqualTo( "default_name" );

			inspector.assertIsInsert( 0 );
			inspector.assertExecutedCount(
					delegate != null && delegate.supportsArbitraryValues() ? 1 : 2
			);
		} );
	}

	@Test
	public void testUpdateGenerationValuesOnly(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate( scope, ValuesOnly.class, MutationType.UPDATE );
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final ValuesOnly entity = new ValuesOnly( 2 );
			session.persist( entity );
		} );

		inspector.clear();

		scope.inTransaction( session -> {
			final ValuesOnly entity = session.find( ValuesOnly.class, 2 );
			entity.setData( "changed" );
			session.flush();

			assertThat( entity.getUpdateDate() ).isNotNull();

			inspector.assertIsSelect( 0 );
			inspector.assertIsUpdate( 1 );
			inspector.assertExecutedCount(
					delegate != null && delegate.supportsArbitraryValues() ? 2 : 3
			);
		} );
	}

	@Test
	public void testGeneratedValuesAndRowId(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate(
				scope,
				ValuesAndRowId.class,
				MutationType.INSERT
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final ValuesAndRowId entity = new ValuesAndRowId( 1 );
			session.persist( entity );
			session.flush();

			assertThat( entity.getName() ).isEqualTo( "default_name" );

			inspector.assertIsInsert( 0 );
			inspector.assertExecutedCount(
					delegate != null && delegate.supportsArbitraryValues() ? 1 : 2
			);

			final boolean shouldHaveRowId = delegate != null && delegate.supportsRowId();
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
		scope.inSession( session -> assertThat( session.find( ValuesAndRowId.class, 1 ).getUpdateDate() ).isNotNull() );
	}

	@Test
	public void testInsertGenerationValuesAndNaturalId(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate(
				scope,
				ValuesAndNaturalId.class,
				MutationType.INSERT
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final ValuesAndNaturalId entity = new ValuesAndNaturalId( 1, "natural_1" );
			session.persist( entity );
			session.flush();

			assertThat( entity.getName() ).isEqualTo( "default_name" );

			inspector.assertIsInsert( 0 );
			final boolean isUniqueKeyDelegate = delegate instanceof UniqueKeySelectingDelegate;
			inspector.assertExecutedCount(
					delegate == null || isUniqueKeyDelegate ? 2 : 1
			);
			if ( isUniqueKeyDelegate ) {
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "data", 1 );
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "id_column", 0 );
			}
		} );
	}

	private static GeneratedValuesMutationDelegate getDelegate(
			SessionFactoryScope scope,
			Class<?> entityClass,
			MutationType mutationType) {
		final EntityMutationTarget entityDescriptor = (EntityMutationTarget) scope.getSessionFactory()
				.getMappingMetamodel().findEntityDescriptor( entityClass );
		return entityDescriptor.getMutationDelegate( mutationType );
	}

	@Entity( name = "ValuesOnly" )
	@SuppressWarnings( "unused" )
	public static class ValuesOnly {
		@Id
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@UpdateTimestamp( source = SourceType.DB )
		private Calendar updateDate;

		@SuppressWarnings( "FieldCanBeLocal" )
		private String data;

		public ValuesOnly() {
		}

		private ValuesOnly(Integer id) {
			this.id = id;
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
	@Entity( name = "ValuesAndRowId" )
	@SuppressWarnings( "unused" )
	public static class ValuesAndRowId {
		@Id
		@Column( name = "id_column" )
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@UpdateTimestamp( source = SourceType.DB )
		private Calendar updateDate;

		@SuppressWarnings( "FieldCanBeLocal" )
		private String data;

		public ValuesAndRowId() {
		}

		private ValuesAndRowId(Integer id) {
			this.id = id;
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

	@Entity( name = "ValuesAndNaturalId" )
	@SuppressWarnings( "unused" )
	public static class ValuesAndNaturalId {
		@Id
		@Column( name = "id_column" )
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@NaturalId
		private String data;

		public ValuesAndNaturalId() {
		}

		private ValuesAndNaturalId(Integer id, String data) {
			this.id = id;
			this.data = data;
		}

		public String getName() {
			return name;
		}
	}
}
