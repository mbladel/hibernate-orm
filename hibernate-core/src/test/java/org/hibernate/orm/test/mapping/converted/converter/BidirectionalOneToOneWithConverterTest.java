/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.hibernate.cfg.AvailableSettings.MAX_FETCH_DEPTH;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = MAX_FETCH_DEPTH, value = "2"),
})
@DomainModel(annotatedClasses = {
		BidirectionalOneToOneWithConverterTest.FooEntity.class,
		BidirectionalOneToOneWithConverterTest.BarEntity.class,
})
@JiraKey("HHH-15950")
public class BidirectionalOneToOneWithConverterTest {
	@Test
	public void testBidirectionalFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			BarEntity bar = new BarEntity();
			bar.setBusinessId( new BusinessId( UUID.randomUUID().toString() ) );

			session.persist( bar );

			FooEntity foo = new FooEntity();
			foo.setBusinessId( new BusinessId( UUID.randomUUID().toString() ) );
			foo.setBar( bar );

			session.persist( foo );

			session.find( FooEntity.class, 1L );
		} );

		scope.inTransaction( session -> {
			session.find( FooEntity.class, 1L );

			// todo marco : aggiungi assert su modello (magari con nuovi attributi)
			// todo marco : verifica che get su associazione non faccia altra query
			//  foo.getBar() - non deve fare query
			//  bar.getFoo() - non deve fare query + deve essere stessa instance di quello col find
			// todo marco : provare anche contrario (session.find(Bar.class, 1L);

				// todo marco : fare un altro test con associazione EAGER
			//  questo dovrebbe fare il detect della circularity
		} );
	}

//	@MappedSuperclass
//	public static class BaseEntity {
//		@Id
//		@Column(name = "id")
//		@GeneratedValue(strategy = GenerationType.IDENTITY)
//		private Long id;
//
//		@Column(name = "uuid", unique = true, updatable = false, columnDefinition = "varchar")
//		@Convert(converter = BusinessIdConverter.class)
//		private BusinessId businessId;
//
//		public Long getId() {
//			return id;
//		}
//
//		public void setId(Long id) {
//			this.id = id;
//		}
//
//		public BusinessId getBusinessId() {
//			return businessId;
//		}
//
//		public void setBusinessId(BusinessId businessId) {
//			this.businessId = businessId;
//		}
//	}

	public static class BusinessId implements Serializable {
		private String value;

		public BusinessId() {
		}

		public BusinessId(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public static class BusinessIdConverter implements AttributeConverter<BusinessId, String> {
		@Override
		public String convertToDatabaseColumn(BusinessId uuid) {
			return uuid != null ? uuid.getValue() : null;
		}

		@Override
		public BusinessId convertToEntityAttribute(String s) {
			return s == null ? null : new BusinessId( s );
		}
	}

	@Entity
	@Table(name = "foo")
	public static class FooEntity {
		@OneToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "bar_uuid", referencedColumnName = "uuid", nullable = false, updatable = false)
		private BarEntity bar;

		public BarEntity getBar() {
			return bar;
		}

		public void setBar(BarEntity bar) {
			this.bar = bar;
		}

		@Id
		@Column(name = "id")
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Column(name = "uuid", unique = true, updatable = false, columnDefinition = "varchar")
		@Convert(converter = BusinessIdConverter.class)
		private BusinessId businessId;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public BusinessId getBusinessId() {
			return businessId;
		}

		public void setBusinessId(BusinessId businessId) {
			this.businessId = businessId;
		}
	}

	@Entity
	@Table(name = "bar")
	public static class BarEntity {
		@OneToOne(fetch = FetchType.LAZY, mappedBy = "bar")
		private FooEntity foo;

		public FooEntity getFoo() {
			return foo;
		}

		public void setFoo(FooEntity foo) {
			this.foo = foo;
		}

		@Id
		@Column(name = "id")
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Column(name = "uuid", unique = true, updatable = false, columnDefinition = "varchar")
		@Convert(converter = BusinessIdConverter.class)
		private BusinessId businessId;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public BusinessId getBusinessId() {
			return businessId;
		}

		public void setBusinessId(BusinessId businessId) {
			this.businessId = businessId;
		}
	}

}
