/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.embeddable;

import java.io.Serializable;

import org.hibernate.annotations.Formula;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EmbeddableFormulaOnSameColumnTest.Person.class,
		EmbeddableFormulaOnSameColumnTest.Address.class,
		EmbeddableFormulaOnSameColumnTest.AddressBis.class,
} )
@SessionFactory
public class EmbeddableFormulaOnSameColumnTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person p = new Person();
			p.setId( 1L );
			p.setAddress( new Address( "Via Gustavo Fara", "Milan", "Italy" ) );
			session.persist( p );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person p = session.find( Person.class, 1L );
			assertThat( p.getAddress().getFullAddress() ).isEqualTo( "Via Gustavo Fara, Milan, Italy" );
			assertThat( p.getAddressBis().getFullAddress() ).isEqualTo( "Via Gustavo Fara, Milan, Italy bis" );
		} );
	}

	@Entity( name = "Person" )
	static class Person {
		@Id
		private Long id;

		@Embedded
		private Address address;

		@Embedded
		private AddressBis addressBis;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}

		public AddressBis getAddressBis() {
			return addressBis;
		}

		public void setAddressBis(AddressBis addressBis) {
			this.addressBis = addressBis;
		}
	}

	@Embeddable
	static class Address implements Serializable {
		private String address;
		private String city;
		private String countryName;
		@Formula( "concat(concat(concat(concat(address, ', '), city), ', '), countryName)" )
		private String fullAddress;

		public Address() {
		}

		public Address(String address, String city, String countryName) {
			this.address = address;
			this.city = city;
			this.countryName = countryName;
		}

		public String getFullAddress() {
			return fullAddress;
		}
	}

	@Embeddable
	static class AddressBis {
		@Formula( "concat(concat(concat(concat(concat(address, ', '), city), ', '), countryName), ' bis')" )
		private String fullAddress;

		public String getFullAddress() {
			return fullAddress;
		}
	}
}
