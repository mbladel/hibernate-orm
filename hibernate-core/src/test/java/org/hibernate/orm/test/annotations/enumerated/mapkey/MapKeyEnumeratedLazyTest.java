package org.hibernate.orm.test.annotations.enumerated.mapkey;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@SessionFactory
@DomainModel( annotatedClasses = {
		MapKeyEnumeratedLazyTest.Car.class,
		MapKeyEnumeratedLazyTest.CarName.class
} )
public class MapKeyEnumeratedLazyTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Car car = new Car( "Audi" );
			session.persist( car );
			final CarName carName = new CarName( 2L, Country.CZECHIA );
			car.getBrandNames().put( Country.CZECHIA, carName );
			session.flush();
			session.refresh( car );
		} );
	}

	@Entity( name = "Car" )
	public static class Car {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.ALL )
		@JoinColumn( name = "car_id", referencedColumnName = "id" )
		@MapKeyColumn( name = "country" )
		@MapKeyEnumerated( EnumType.ORDINAL )
		private Map<Country, CarName> brandNames;

		public Car(String name) {
			this.name = name;
			this.brandNames = new HashMap<>();
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Map<Country, CarName> getBrandNames() {
			return brandNames;
		}
	}

	@Entity
	@Table( name = "carname" )
	public static class CarName {
		@Id
		@Column( name = "id" )
		private Long id;

		private Country country;

		public CarName(Long id, Country country) {
			this.id = id;
			this.country = country;
		}

		public Long getId() {
			return id;
		}

		public Country getCountry() {
			return country;
		}
	}

	public enum Country {
		FRANCE,
		ENGLAND,
		CZECHIA
	}
}
