/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.idgen.userdefined;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		MixedTimingGeneratorsTest.TestEntity.class,
} )
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsIdentityColumns.class )
public class MixedTimingGeneratorsTest {
	@Test
	public void testIdentityGeneration(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TestEntity( "identity" ) ) );
		scope.inSession( session -> {
			assertThat( session.find( TestEntity.class, 1L ).getName() ).isEqualTo( "identity" );
		} );
	}

	@Test
	public void testAssigned(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TestEntity( 42L, "assigned" ) ));
		scope.inSession( session -> {
			assertThat( session.find( TestEntity.class, 42L ).getName() ).isEqualTo( "assigned" );
		} );
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity {
		@Id
		@GeneratedValue( generator = "test" )
		@GenericGenerator( name = "test", type = IdentityOrAssignedGenerator.class )
		private Long id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(String name) {
			this.name = name;
		}

		public TestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	/**
	 * Identifier generator, referenceColumnsInSql is sometimes false
	 */
	public static class IdentityOrAssignedGenerator extends IdentityGenerator implements BeforeExecutionGenerator {
		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			final EntityPersister entityPersister = session.getEntityPersister( null, owner );
			return entityPersister.getIdentifier( owner, session );
		}

		@Override
		public boolean generatedOnExecution(SharedSessionContractImplementor session, Object owner) {
			return generate( session, owner, null, null ) == null;
		}

		@Override
		public boolean generatedOnExecution() {
			return true;
		}
	}
}
