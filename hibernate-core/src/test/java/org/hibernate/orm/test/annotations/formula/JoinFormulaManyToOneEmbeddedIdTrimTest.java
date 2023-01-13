/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.formula;

import java.io.Serializable;
import java.util.List;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@Jpa(annotatedClasses = {
		JoinFormulaManyToOneEmbeddedIdTrimTest.Vehicle.class,
		JoinFormulaManyToOneEmbeddedIdTrimTest.VehicleInvoice.class,
})
public class JoinFormulaManyToOneEmbeddedIdTrimTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Vehicle vehicle = new Vehicle();
			vehicle.setField1( "VOLVO" );
			vehicle.setField2( "2020" );

			VehicleInvoice invoice = new VehicleInvoice();
			invoice.setId( new VehicleInvoiceId( "VO".toCharArray(), "2020".toCharArray() ) );
			invoice.setVehicle( vehicle );

			entityManager.persist( vehicle );
			entityManager.persist( invoice );
		} );
	}

	@Test
	public void testJoinFormulaTrimQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<VehicleInvoice> resultList = entityManager.createQuery(
					"from VehicleInvoice",
					VehicleInvoice.class
			).getResultList();
			assertEquals( 1, resultList.size() );
			assertEquals( 1L, resultList.get( 0 ).getVehicle().getId() );
		} );
	}

	@Entity(name = "Vehicle")
	@Table(name = "VEHICLE")
	public static class Vehicle implements Serializable {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "FIELD_1", nullable = false)
		private String field1;

		@Column(name = "FIELD_2", nullable = false, length = 4)
		private String field2;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getField1() {
			return field1;
		}

		public void setField1(String field1) {
			this.field1 = field1;
		}

		public String getField2() {
			return field2;
		}

		public void setField2(String field2) {
			this.field2 = field2;
		}
	}

	@Embeddable
	public static class VehicleInvoiceId implements Serializable {
		@Column(name = "INVOICE_FIELD1", length = 2, nullable = false)
		private char[] field1;

		@Column(name = "INVOICE_FIELD2", length = 4, nullable = false)
		private char[] field2;

		public VehicleInvoiceId() {
		}

		public VehicleInvoiceId(char[] field1, char[] field2) {
			this.field1 = field1;
			this.field2 = field2;
		}

		public char[] getField1() {
			return field1;
		}

		public void setField1(char[] field1) {
			this.field1 = field1;
		}

		public char[] getField2() {
			return field2;
		}

		public void setField2(char[] field2) {
			this.field2 = field2;
		}
	}

	@Entity(name = "VehicleInvoice")
	@Table(name = "INVOICE")
	public static class VehicleInvoice {
		@EmbeddedId
		private VehicleInvoiceId id;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumnsOrFormulas({
				@JoinColumnOrFormula(formula = @JoinFormula(value = "trim(INVOICE_FIELD1)", referencedColumnName = "FIELD_1"))				,
				@JoinColumnOrFormula(column = @JoinColumn(name = "INVOICE_FIELD2", referencedColumnName = "FIELD_2", insertable = false, updatable = false))
		})
		private Vehicle vehicle;

		public VehicleInvoiceId getId() {
			return id;
		}

		public void setId(VehicleInvoiceId id) {
			this.id = id;
		}

		public Vehicle getVehicle() {
			return vehicle;
		}

		public void setVehicle(Vehicle vehicle) {
			this.vehicle = vehicle;
		}
	}
}
