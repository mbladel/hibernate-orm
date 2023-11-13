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

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		MarcosGeneratedValueIdentityTest.IdentityOnly.class,
		MarcosGeneratedValueIdentityTest.IdentityAndValues.class,
		MarcosGeneratedValueIdentityTest.IdentityAndValuesAndRowId.class,
} )
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsIdentityColumns.class )
public class MarcosGeneratedValueIdentityTest {

	@Test
	public void testInsertGenerationIdentityOnly(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final IdentityOnly entity = new IdentityOnly();
			session.persist( entity );
			session.flush();
			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isNull();
		} );
	}

	@Test
	public void testInsertGenerationValuesAndIdentity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final IdentityAndValues entity = new IdentityAndValues();
			session.persist( entity );
			session.flush();
			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "default" );
		} );
	}

	@Test
	public void testInsertGenerationValuesAndIdentityAndRowId(SessionFactoryScope scope) {
		final EntityPersister entityDescriptor = scope.getSessionFactory().getMappingMetamodel()
				.findEntityDescriptor( IdentityAndValuesAndRowId.class );
		scope.inTransaction( session -> {
			final IdentityAndValuesAndRowId entity = new IdentityAndValuesAndRowId();
			session.persist( entity );
			session.flush();
			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "default" );
			if ( ( (EntityMutationTarget) entityDescriptor ).getIdentityInsertDelegate()
					.supportsRetrievingRowId() && entityDescriptor.getRowIdMapping() != null ) {
				// assert row-id was populated in entity entry
				final PersistenceContext pc = session.getPersistenceContextInternal();
				final EntityEntry entry = pc.getEntry( entity );
				assertThat( entry.getRowId() ).isNotNull();
			}
		} );
	}


	@Entity( name = "IdentityOnly" )
	public static class IdentityOnly {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Integer id;

		private String name;

		private Calendar updateDate;

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Calendar getUpdateDate() {
			return updateDate;
		}
	}

	@Entity( name = "IdentityAndValues" )
	public static class IdentityAndValues {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default'" )
		private String name;

		@Generated( event = EventType.UPDATE )
		@ColumnDefault( "CURRENT_TIMESTAMP" )
		private Calendar updateDate;

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Calendar getUpdateDate() {
			return updateDate;
		}
	}

	@Entity( name = "IdentityAndValuesAndRowId" )
	@RowId
	public static class IdentityAndValuesAndRowId {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default'" )
		private String name;

		@Generated( event = EventType.UPDATE )
		@ColumnDefault( "CURRENT_TIMESTAMP" )
		private Calendar updateDate;

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Calendar getUpdateDate() {
			return updateDate;
		}
	}
}
