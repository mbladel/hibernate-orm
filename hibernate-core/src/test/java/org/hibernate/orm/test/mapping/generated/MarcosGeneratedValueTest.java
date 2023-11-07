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
import org.hibernate.generator.EventType;

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
		MarcosGeneratedValueTest.ValuesOnly.class,
		MarcosGeneratedValueTest.IdentityOnly.class,
		MarcosGeneratedValueTest.IdentityAndValues.class,
} )
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsIdentityColumns.class )
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
}
