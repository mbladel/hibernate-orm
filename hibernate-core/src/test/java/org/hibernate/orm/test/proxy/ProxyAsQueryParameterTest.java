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

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = { ProxyAsQueryParameterTest.Product.class, ProxyAsQueryParameterTest.Vendor.class } )
@SessionFactory
public class ProxyAsQueryParameterTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Vendor vendor = new Vendor( 1, "vendor_1" );
			session.persist( vendor );
			final Product product = new Product( 1, vendor );
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
	public void testProxyParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Product product = session.createQuery( "from Product p", Product.class ).getSingleResult();
			assertThat( Hibernate.isInitialized( product.getVendor() ) ).isFalse();
			final Product result = session.createQuery(
					"select p from Product p where p.vendor = :vendor",
					Product.class
			).setParameter( "vendor", product.getVendor() ).getSingleResult();
			// The proxy should not have been initialized since Vendor doesn't have subclasses
			assertThat( Hibernate.isInitialized( product.getVendor() ) ).isFalse();
			assertThat( result.getVendor().getId() ).isEqualTo( product.getVendor().getId() );
		} );
	}

	@Entity( name = "Vendor" )
	public static class Vendor implements VendorAPI {
		@Id
		private Integer id;
		private String name;

		public Vendor() {
		}

		public Vendor(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public Integer getId() {
			return id;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public interface VendorAPI {
		Integer getId();

		String getName();
	}

	@Entity( name = "Product" )
	public static final class Product {
		@Id
		private Integer id;

		@ManyToOne( fetch = FetchType.LAZY )
		private VendorAPI vendor;

		public Product() {
		}

		public Product(Integer id, Vendor vendor) {
			this.id = id;
			this.vendor = vendor;
		}

		public Integer getId() {
			return id;
		}

		public VendorAPI getVendor() {
			return vendor;
		}
	}
}
