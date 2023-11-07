/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.generated;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;


@DomainModel( annotatedClasses = MarcosGeneratedValueTest.TheEntity.class )
@SessionFactory
public class MarcosGeneratedValueTest {
	@Test
	public void testInsertGeneration(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TheEntity theEntity = new TheEntity( 1 );
			session.persist( theEntity );
			session.flush();
			assertThat( theEntity.getName() ).isEqualTo( "default" );
		} );
	}

	@Entity( name = "TheEntity" )
	public static class TheEntity {
		@Id
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default'" )
		private String name;

		@Generated( event = EventType.UPDATE )
		@ColumnDefault( "CURRENT_TIMESTAMP" )
		private Calendar updateDate;

		public TheEntity() {
		}

		private TheEntity(Integer id) {
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
