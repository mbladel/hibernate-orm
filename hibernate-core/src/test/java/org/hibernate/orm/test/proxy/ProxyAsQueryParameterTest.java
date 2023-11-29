/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.proxy;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		ProxyAsQueryParameterTest.Product.class,
		ProxyAsQueryParameterTest.Vendor.class,
		ProxyAsQueryParameterTest.CarVendor.class,
		ProxyAsQueryParameterTest.Producer.class,
} )
@SessionFactory
public class ProxyAsQueryParameterTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CarVendor vendor = new CarVendor( 1, "vendor_1", "dealership_1" );
			session.persist( vendor );
			final Producer producer = new Producer( 1, "producer_1" );
			session.persist( producer );
			final Product product = new Product( 1, vendor, producer );
			session.persist( product );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Product" ).executeUpdate();
			session.createMutationQuery( "delete from Vendor" ).executeUpdate();
		} );
	}

	@Test
	public void testNormalProxyParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Product product = session.createQuery( "from Product p", Product.class ).getSingleResult();
			assertThat( Hibernate.isInitialized( product.getVendor() ) ).isFalse();
			final Product result = session.createQuery(
					"from Product p where p.vendor = :vendor",
					Product.class
			).setParameter( "vendor", product.getVendor() ).getSingleResult();
			// The proxy will have been initialized since Vendor has subclasses
			assertThat( Hibernate.isInitialized( product.getVendor() ) ).isTrue();
			assertThat( result.getVendor().getId() ).isEqualTo( product.getVendor().getId() );
		} );
	}

	@Test
	public void testInterfaceProxyParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Product product = session.createQuery( "from Product p", Product.class ).getSingleResult();
			assertThat( Hibernate.isInitialized( product.getProducerAPI() ) ).isFalse();
			final Product result = session.createQuery(
					"from Product p where p.producer = :producer",
					Product.class
			).setParameter( "producer", product.getProducerAPI() ).getSingleResult();
			// The proxy should not have been initialized since Vendor doesn't have subclasses
			assertThat( Hibernate.isInitialized( product.getProducerAPI() ) ).isFalse();
			assertThat( result.getProducerAPI().getId() ).isEqualTo( product.getProducerAPI().getId() );
		} );
	}

	@Test
	public void testSubclassProxyParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Product product = session.createQuery( "from Product p", Product.class ).getSingleResult();
			assertThat( Hibernate.isInitialized( product.getVendor() ) ).isFalse();
			final CarVendor result = session.createQuery(
					"from CarVendor v where v = :vendor",
					CarVendor.class
			).setParameter( "vendor", product.getVendor() ).getSingleResult();
			// The proxy should have been initialized since Vendor has subclasses
			assertThat( Hibernate.isInitialized( product.getVendor() ) ).isTrue();
			assertThat( result.getId() ).isEqualTo( product.getVendor().getId() );
		} );
	}

	public interface ProducerAPI {
		Integer getId();

		String getName();
	}

	@Entity( name = "Producer" )
	public static class Producer implements ProducerAPI {
		@Id
		private Integer id;
		private String name;

		public Producer() {
		}

		public Producer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "Vendor" )
	public static class Vendor {
		@Id
		private Integer id;
		private String name;

		public Vendor() {
		}

		public Vendor(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "CarVendor" )
	public static class CarVendor extends Vendor {
		private String dealership;

		public CarVendor() {
		}

		public CarVendor(Integer id, String name, String dealership) {
			super( id, name );
			this.dealership = dealership;
		}

		public String getDealership() {
			return dealership;
		}
	}

	@Entity( name = "Product" )
	public static final class Product {
		private Integer id;
		private Vendor vendor;
		private ProducerAPI producer;

		public Product() {
		}

		public Product(Integer id, Vendor vendor, ProducerAPI producer) {
			this.id = id;
			this.vendor = vendor;
			this.producer = producer;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ManyToOne( fetch = FetchType.LAZY )
		public Vendor getVendor() {
			return vendor;
		}

		public void setVendor(Vendor vendor) {
			this.vendor = vendor;
		}

		@ManyToOne( fetch = FetchType.LAZY )
		// this is needed to simulate hbm.xml mapping with interfaces
		public Producer getProducer() {
			return new Producer( producer.getId(), null );
		}

		@Transient
		public ProducerAPI getProducerAPI() {
			return producer;
		}

		public void setProducer(ProducerAPI producer) {
			this.producer = producer;
		}
	}
}
