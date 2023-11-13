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
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		MarcosGeneratedValueTest.ValuesOnly.class,
		MarcosGeneratedValueTest.ValuesAndRowId.class,
} )
@SessionFactory
public class MarcosGeneratedValueTest {
	@Test
	public void testInsertGenerationValuesOnly(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ValuesOnly entity = new ValuesOnly( 1 );
			session.persist( entity );
			session.flush();
			assertThat( entity.getName() ).isEqualTo( "default" );
		} );
	}

	@Test
	public void testInsertGenerationValuesAndRowId(SessionFactoryScope scope) {
		final EntityPersister entityDescriptor = scope.getSessionFactory().getMappingMetamodel()
				.findEntityDescriptor( ValuesAndRowId.class );
		scope.inTransaction( session -> {
			final ValuesAndRowId entity = new ValuesAndRowId( 1 );
			session.persist( entity );
			session.flush();
			assertThat( entity.getName() ).isEqualTo( "default" );
			if ( ( (EntityMutationTarget) entityDescriptor ).getInsertDelegate()
					.supportsRetrievingRowId() && entityDescriptor.getRowIdMapping() != null ) {
				// assert row-id was populated in entity entry
				final PersistenceContext pc = session.getPersistenceContextInternal();
				final EntityEntry entry = pc.getEntry( entity );
				assertThat( entry.getRowId() ).isNotNull();
			}
		} );
	}

	@Entity( name = "ValuesOnly" )
	public static class ValuesOnly {
		@Id
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default'" )
		private String name;

		@Generated( event = EventType.UPDATE )
		@ColumnDefault( "CURRENT_TIMESTAMP" )
		private Calendar updateDate;

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
	}

	@Entity( name = "ValuesAndRowId" )
	@RowId
	public static class ValuesAndRowId {
		@Id
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default'" )
		private String name;

		@Generated( event = EventType.UPDATE )
		@ColumnDefault( "CURRENT_TIMESTAMP" )
		private Calendar updateDate;

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
	}
}
