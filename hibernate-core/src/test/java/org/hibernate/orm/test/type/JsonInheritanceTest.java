/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.type;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@SessionFactory
@DomainModel( annotatedClasses = {
		JsonInheritanceTest.CommonEntity.class, JsonInheritanceTest.EntityA.class, JsonInheritanceTest.EntityB.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-15929" )
public class JsonInheritanceTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new EntityA( new PropertyTypeA( "property_a" ) ) );
			session.persist( new EntityB( new PropertyTypeB( "property_b" ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from CommonEntity" ).executeUpdate() );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.find( CommonEntity.class, 1L );
			session.find( CommonEntity.class, 2L );
		} );
	}

	@Entity( name = "CommonEntity" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "type", discriminatorType = DiscriminatorType.STRING )
	public abstract static class CommonEntity {
		@Id
		@GeneratedValue
		private Long id;
	}

	public static class PropertyTypeA {
		private String propertyA;

		public PropertyTypeA() {
		}

		public PropertyTypeA(String propertyA) {
			this.propertyA = propertyA;
		}

		public String getPropertyA() {
			return propertyA;
		}

		public void setPropertyA(String propertyA) {
			this.propertyA = propertyA;
		}
	}

	@Entity( name = "EntityA" )
	@DiscriminatorValue( "A" )
	public static class EntityA extends CommonEntity {
		@JdbcTypeCode( SqlTypes.JSON )
		private PropertyTypeA property;

		public EntityA() {
		}

		public EntityA(PropertyTypeA property) {
			this.property = property;
		}
	}

	public static class PropertyTypeB {
		private String propertyB;

		public PropertyTypeB() {
		}

		public PropertyTypeB(String propertyB) {
			this.propertyB = propertyB;
		}

		public String getPropertyB() {
			return propertyB;
		}

		public void setPropertyB(String propertyB) {
			this.propertyB = propertyB;
		}
	}

	@Entity( name = "EntityB" )
	@DiscriminatorValue( "B" )
	public static class EntityB extends CommonEntity {
		@JdbcTypeCode( SqlTypes.JSON )
		private PropertyTypeB property;

		public EntityB() {
		}

		public EntityB(PropertyTypeB property) {
			this.property = property;
		}
	}
}
