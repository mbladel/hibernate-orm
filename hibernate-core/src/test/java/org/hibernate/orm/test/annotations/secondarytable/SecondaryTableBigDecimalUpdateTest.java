/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.secondarytable;

import java.math.BigDecimal;

import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
public class SecondaryTableBigDecimalUpdateTest {
	private static final BigDecimal PRIMARY_A = new BigDecimal( "3.088361" );
	private static final BigDecimal PRIMARY_B = new BigDecimal( "3.088362" );
	private static final BigDecimal SECONDARY_A = new BigDecimal( "4.088361" );
	private static final BigDecimal SECONDARY_B = new BigDecimal( "4.088362" );

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SecondaryTableCls secondaryTable = new SecondaryTableCls();
			secondaryTable.setId( 1L );
			secondaryTable.setPrimaryBigDecimal( PRIMARY_A );
			secondaryTable.setSecondaryBigDecimal( SECONDARY_A );
			session.persist( secondaryTable );
		} );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SecondaryTableCls entity = session.find( SecondaryTableCls.class, 1L );
			assertValues( entity, PRIMARY_A, SECONDARY_A );
			// update values
			entity.setPrimaryBigDecimal( PRIMARY_B );
			entity.setSecondaryBigDecimal( SECONDARY_B );
		} );
		scope.inSession( session -> {
			final SecondaryTableCls entity = session.find( SecondaryTableCls.class, 1L );
			assertValues( entity, PRIMARY_B, SECONDARY_B );
		} );
	}

	private static void assertValues(SecondaryTableCls entity, BigDecimal primary, BigDecimal secondary) {
		assertThat( entity.getPrimaryBigDecimal() ).isEqualByComparingTo( primary );
		assertThat( entity.getSecondaryBigDecimal() ).isEqualByComparingTo( secondary );
	}

	@Entity(name = "PrimaryTable")
	static abstract class PrimaryTable {
		@Id
		private Long id;

		@Column(name = "primary_numeric", precision = 31, scale = 16)
		private BigDecimal primaryBigDecimal;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public BigDecimal getPrimaryBigDecimal() {
			return primaryBigDecimal;
		}

		public void setPrimaryBigDecimal(BigDecimal primaryBigDecimal) {
			this.primaryBigDecimal = primaryBigDecimal;
		}
	}

	@Entity(name = "SecondaryTableCls")
	@SecondaryTable(name = "secondary_table", pkJoinColumns = @PrimaryKeyJoinColumn(name = "ID", referencedColumnName = "ID"))
	static class SecondaryTableCls extends PrimaryTable {

		@Column(name = "secondary_numeric", table = "secondary_table", precision = 31, scale = 16)
		private BigDecimal secondaryBigDecimal;

		public BigDecimal getSecondaryBigDecimal() {
			return secondaryBigDecimal;
		}

		public void setSecondaryBigDecimal(BigDecimal secondaryBigDecimal) {
			this.secondaryBigDecimal = secondaryBigDecimal;
		}
	}
}
