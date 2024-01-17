/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.naturalid;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		BigDecimalNaturalIdTest.SimpleEntity.class,
		BigDecimalNaturalIdTest.CompoundEntity.class,
} )
@SessionFactory
public class BigDecimalNaturalIdTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new SimpleEntity( 1L, BigDecimal.ONE, "test" ) );
			session.persist( new CompoundEntity( 2L, BigDecimal.TEN, BigInteger.TEN, "test" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from SimpleEntity" ).executeUpdate();
			session.createMutationQuery( "delete from CompoundEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testSimpleNaturalId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SimpleEntity entity = session.find( SimpleEntity.class, 1L );
			// Set the natural id to the same value
			final BigDecimal old = entity.getNaturalId();
			entity.setNaturalId( new BigDecimal( "1" ) );
			assertThat( old ).isEqualByComparingTo( entity.getNaturalId() );
			// test update
			entity.setData( "updated" );
		} );
		scope.inSession( session -> assertThat(
				session.find( SimpleEntity.class, 1L ).getData()
		).isEqualTo( "updated" ) );
	}

	@Test
	public void testCompoundNaturalId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CompoundEntity entity = session.find( CompoundEntity.class, 2L );
			// Set the natural id to the same value
			final BigDecimal oldDecimal = entity.getBigDecimal();
			entity.setBigDecimal( new BigDecimal( "10" ) );
			assertThat( oldDecimal ).isEqualByComparingTo( entity.getBigDecimal() );
			final BigInteger oldInteger = entity.getBigInteger();
			entity.setBigInteger( new BigInteger( "10" ) );
			assertThat( oldInteger ).isEqualByComparingTo( entity.getBigInteger() );
			// test update
			entity.setData( "updated" );
		} );
		scope.inSession( session -> assertThat(
				session.find( CompoundEntity.class, 2L ).getData()
		).isEqualTo( "updated" ) );
	}

	@Entity( name = "SimpleEntity" )
	public static class SimpleEntity {
		@Id
		private Long id;

		@NaturalId
		private BigDecimal naturalId;

		private String data;

		public SimpleEntity() {
		}

		public SimpleEntity(Long id, BigDecimal naturalId, String data) {
			this.id = id;
			this.naturalId = naturalId;
			this.data = data;
		}

		public BigDecimal getNaturalId() {
			return naturalId;
		}

		public void setNaturalId(BigDecimal naturalId) {
			this.naturalId = naturalId;
		}

		public void setData(String data) {
			this.data = data;
		}

		public String getData() {
			return data;
		}
	}

	@Entity( name = "CompoundEntity" )
	public static class CompoundEntity {
		@Id
		private Long id;

		@NaturalId
		private BigDecimal bigDecimal;

		@NaturalId
		private BigInteger bigInteger;

		private String data;

		public CompoundEntity() {
		}

		public CompoundEntity(Long id, BigDecimal bigDecimal, BigInteger bigInteger, String data) {
			this.id = id;
			this.bigDecimal = bigDecimal;
			this.bigInteger = bigInteger;
			this.data = data;
		}

		public BigDecimal getBigDecimal() {
			return bigDecimal;
		}

		public void setBigDecimal(BigDecimal bigDecimal) {
			this.bigDecimal = bigDecimal;
		}

		public BigInteger getBigInteger() {
			return bigInteger;
		}

		public void setBigInteger(BigInteger bigInteger) {
			this.bigInteger = bigInteger;
		}

		public void setData(String data) {
			this.data = data;
		}

		public String getData() {
			return data;
		}
	}
}
