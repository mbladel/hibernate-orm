/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EnumTupleComparisonTest.WithEnum.class,
		EnumTupleComparisonTest.TestEntity.class,
} )
@SessionFactory
public class EnumTupleComparisonTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new WithEnum( Enum.X, Enum.Y ) );
			session.persist( new TestEntity( "X", 1 ) );
		} );
	}

	@Test
	public void testTupleComparisonMixed(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final WithEnum result = session.createQuery(
					"from WithEnum e where (e.stringEnum, e.ordinalEnum) in " +
							"(select t.stringField, t.intField from TestEntity t)",
					WithEnum.class
			).getSingleResult();
			assertThat( result.getStringEnum() ).isEqualTo( Enum.X );
			assertThat( result.getOrdinalEnum() ).isEqualTo( Enum.Y );
		} );
	}

	@Test
	public void testStringTupleComparison(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final WithEnum result = session.createQuery(
					"from WithEnum e where (e.stringEnum, e.stringEnum) in " +
							"(select t.stringField, t.stringField from TestEntity t)",
					WithEnum.class
			).getSingleResult();
			assertThat( result.getStringEnum() ).isEqualTo( Enum.X );
			assertThat( result.getOrdinalEnum() ).isEqualTo( Enum.Y );
		} );
	}

	@Test
	public void testOrdinalComparisonMixed(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final WithEnum result = session.createQuery(
					"from WithEnum e where (e.ordinalEnum, e.ordinalEnum) in " +
							"(select t.intField, t.intField from TestEntity t)",
					WithEnum.class
			).getSingleResult();
			assertThat( result.getStringEnum() ).isEqualTo( Enum.X );
			assertThat( result.getOrdinalEnum() ).isEqualTo( Enum.Y );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from WithEnum" ).executeUpdate();
			session.createMutationQuery( "delete from TestEntity" ).executeUpdate();
		} );
	}

	public enum Enum {X, Y}

	@Entity( name = "WithEnum" )
	public static class WithEnum {
		@Id
		@GeneratedValue
		private Long id;

		@Enumerated( EnumType.STRING )
		private Enum stringEnum;

		@Enumerated( EnumType.ORDINAL )
		private Enum ordinalEnum;

		public WithEnum() {
		}

		public WithEnum(Enum stringEnum, Enum ordinalEnum) {
			this.stringEnum = stringEnum;
			this.ordinalEnum = ordinalEnum;
		}

		public Enum getStringEnum() {
			return stringEnum;
		}

		public Enum getOrdinalEnum() {
			return ordinalEnum;
		}
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String stringField;

		private Integer intField;


		public TestEntity() {
		}

		public TestEntity(String stringField, Integer intField) {
			this.stringField = stringField;
			this.intField = intField;
		}

		public String getStringField() {
			return stringField;
		}

		public Integer getIntField() {
			return intField;
		}
	}
}
