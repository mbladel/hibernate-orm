/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.userdefined;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.UUID;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = {
		BeforeExecutionAssignedValuesTest.EntityWithGeneratedId.class,
		BeforeExecutionAssignedValuesTest.EntityWithGeneratedProperty.class,
})
class BeforeExecutionAssignedValuesTest {
	@Test
	void testAssignedId(SessionFactoryScope scope) {
		final String assigned = "assigned-id";
		final EntityWithGeneratedId entity = new EntityWithGeneratedId( assigned, "assigned-entity" );
		scope.inTransaction( session -> session.persist( entity ) );
		assertThat( entity.getGeneratedId() ).isEqualTo( assigned );
	}

	@Test
	void testGeneratedId(SessionFactoryScope scope) {
		final EntityWithGeneratedId entity = new EntityWithGeneratedId( null, "assigned-entity" );
		scope.inTransaction( session -> session.persist( entity ) );
		assertThat( entity.getGeneratedId() ).isNotNull();
	}

	@Test
	void testInsertAssignedProperty(SessionFactoryScope scope) {
		final String assigned = "assigned-property";
		final EntityWithGeneratedProperty entity = new EntityWithGeneratedProperty( 1L, assigned );
		scope.inTransaction( session -> session.persist( entity ) );
		assertThat( entity.getGeneratedProperty() ).isEqualTo( assigned );
	}

	@Test
	void testGeneratedPropertyAndUpdate(SessionFactoryScope scope) {
		final EntityWithGeneratedProperty entity = new EntityWithGeneratedProperty( 2L, null );
		scope.inTransaction( session -> {
			session.persist( entity );
			session.flush();

			assertThat( entity.getGeneratedProperty() ).isNotNull();

			// test update
			entity.setGeneratedProperty( "new-assigned-property" );
		} );

		assertThat( entity.getGeneratedProperty() ).isEqualTo( "new-assigned-property" );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "EntityWithGeneratedId")
	static class EntityWithGeneratedId {
		@Id
		@GeneratedValue
		@AssignableGenerator
		private String generatedId;

		private String name;

		public EntityWithGeneratedId() {
		}

		public EntityWithGeneratedId(String generatedId, String name) {
			this.generatedId = generatedId;
			this.name = name;
		}

		public String getGeneratedId() {
			return generatedId;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "EntityWithGeneratedProperty")
	static class EntityWithGeneratedProperty {
		@Id
		private Long id;

		@AssignableGenerator
		private String generatedProperty;

		public EntityWithGeneratedProperty() {
		}

		public EntityWithGeneratedProperty(Long id, String generatedProperty) {
			this.id = id;
			this.generatedProperty = generatedProperty;
		}

		public Long getId() {
			return id;
		}

		public String getGeneratedProperty() {
			return generatedProperty;
		}

		public void setGeneratedProperty(String generatedProperty) {
			this.generatedProperty = generatedProperty;
		}
	}

	@IdGeneratorType(AssignedIdGenerator.class)
	@ValueGenerationType(generatedBy = AssignedGenerator.class)
	@Retention(RUNTIME)
	@Target({FIELD, METHOD})
	@interface AssignableGenerator {
	}

	public static class AssignedGenerator implements BeforeExecutionGenerator {
		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			if ( currentValue != null ) {
				return currentValue;
			}
			return UUID.randomUUID().toString();
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_AND_UPDATE;
		}
	}

	public static class AssignedIdGenerator extends AssignedGenerator {
		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}

		@Override
		public boolean allowAssignedIdentifiers() {
			return true;
		}
	}
}
