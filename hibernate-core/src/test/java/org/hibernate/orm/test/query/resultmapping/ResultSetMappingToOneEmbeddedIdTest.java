/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.resultmapping;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SqlResultSetMapping;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ResultSetMappingToOneEmbeddedIdTest.Concert.class,
		ResultSetMappingToOneEmbeddedIdTest.Organizer.class,
		ResultSetMappingToOneEmbeddedIdTest.OrganizerPK.class,
} )
@SessionFactory
public class ResultSetMappingToOneEmbeddedIdTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

		} );
	}

	@SqlResultSetMapping( name = "ConcertMapping", entities = {
			@EntityResult( entityClass = Concert.class, fields = {
					@FieldResult( name = "id", column = "id" ),
					@FieldResult( name = "ticketPrice", column = "ticketPrice" ),
					@FieldResult( name = "organizer.{id}.registration_id", column = "organizer_registration_id" ),
					@FieldResult( name = "organizer.{id}.district", column = "organizer_district" )
			} )
	} )
	@Entity( name = "Concert" )
	public static class Concert {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		private Organizer organizer;

		private double ticketPrice;

		public Concert() {
		}

		public Concert(Organizer organizer, double ticketPrice) {
			this.organizer = organizer;
			this.ticketPrice = ticketPrice;
		}
	}

	@Entity( name = "Organizer" )
	public static class Organizer implements Serializable {
		@EmbeddedId
//		@AttributeOverrides( {
//				@AttributeOverride( name = "handelsregisternummer", column = @Column( name = "HR_NR" ) ),
//				@AttributeOverride( name = "amtsgericht", column = @Column( name = "AMTSGERICHT" ) )
//		} )
		private OrganizerPK pk;

		private String name;

		public Organizer() {
		}

		public Organizer(OrganizerPK pk, String name) {
			this.pk = pk;
			this.name = name;
		}
	}

	@Embeddable
	public static class OrganizerPK implements Serializable {
		private Long registration_id;
		private String district;

		public OrganizerPK() {
		}

		public OrganizerPK(Long registration_id, String district) {
			this.registration_id = registration_id;
			this.district = district;
		}
	}
}
