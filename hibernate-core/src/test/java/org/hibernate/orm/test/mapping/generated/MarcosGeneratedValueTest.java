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
import org.hibernate.annotations.RowId;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.MutationGeneratedValuesDelegate;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;

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
		MarcosGeneratedValueTest.ValuesOnly.class,
		MarcosGeneratedValueTest.ValuesAndRowId.class,
} )
@SessionFactory(useCollectingStatementInspector = true)
public class MarcosGeneratedValueTest {
	@Test
	public void testInsertGenerationValuesOnly(SessionFactoryScope scope) {
		final EntityMutationTarget entityDescriptor = (EntityMutationTarget) scope.getSessionFactory()
				.getMappingMetamodel().findEntityDescriptor( ValuesOnly.class );
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		// todo marco : add tests for multi-table situations ?
		//  - joined inheritance
		//  - secondary table???

		// todo marco : add test for unique key delegate (values, rowid and identity should be supported)

		scope.inTransaction( session -> {
			final ValuesOnly entity = new ValuesOnly( 1 );
			session.persist( entity );
			session.flush();

			assertThat( entity.getName() ).isEqualTo( "default" );

			inspector.assertIsInsert( 0 );
			final MutationGeneratedValuesDelegate insertDelegate = entityDescriptor.getInsertDelegate();
			inspector.assertExecutedCount(
					insertDelegate != null && insertDelegate.supportsRetrievingGeneratedValues() ? 1 : 2
			);
		} );
	}

	@Test
	public void testUpdateGenerationValuesOnly(SessionFactoryScope scope) {
		final EntityMutationTarget entityDescriptor = (EntityMutationTarget) scope.getSessionFactory()
				.getMappingMetamodel().findEntityDescriptor( ValuesOnly.class );
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
			final MutationGeneratedValuesDelegate updateDelegate = entityDescriptor.getUpdateDelegate();
			inspector.assertExecutedCount(
					updateDelegate != null && updateDelegate.supportsRetrievingGeneratedValues() ? 2 : 3
			);
		} );
	}

	@Test
	public void testGeneratedValuesAndRowId(SessionFactoryScope scope) {
		final EntityMutationTarget entityDescriptor = (EntityMutationTarget) scope.getSessionFactory()
				.getMappingMetamodel().findEntityDescriptor( ValuesAndRowId.class );
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final ValuesAndRowId entity = new ValuesAndRowId( 1 );
			session.persist( entity );
			session.flush();

			assertThat( entity.getName() ).isEqualTo( "default" );

			inspector.assertIsInsert( 0 );
			final MutationGeneratedValuesDelegate insertDelegate = entityDescriptor.getInsertDelegate();
			inspector.assertExecutedCount(
					insertDelegate != null && insertDelegate.supportsRetrievingGeneratedValues() ? 1 : 2
			);

			final boolean shouldHaveRowId = insertDelegate != null && insertDelegate.supportsRetrievingRowId();
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

			inspector.assertIsUpdate( 0 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "id_column", shouldHaveRowId ? 0 : 1 );
		} );
	}

	@Entity( name = "ValuesOnly" )
	@SuppressWarnings( "unused" )
	public static class ValuesOnly {
		@Id
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default'" )
		private String name;

		@Generated( event = EventType.UPDATE )
		@ColumnDefault( "CURRENT_TIMESTAMP" )
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
		@ColumnDefault( "'default'" )
		private String name;

		@Generated( event = EventType.UPDATE )
		@ColumnDefault( "CURRENT_TIMESTAMP" )
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
}
