/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.idgen.userdefined;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.Random;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.OnExecutionGenerator;
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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		MixedTimingGeneratorsTest.AssignedEntity.class,
		MixedTimingGeneratorsTest.RandomEntity.class,
		MixedTimingGeneratorsTest.StringGeneratedEntity.class,
} )
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsIdentityColumns.class )
public class MixedTimingGeneratorsTest {
	@Test
	public void testIdentityOrAssignedId(SessionFactoryScope scope) {
		// on execution generation
		scope.inTransaction( session -> session.persist( new AssignedEntity( "identity" ) ) );
		scope.inSession( session -> assertThat( session.createQuery(
				"from AssignedEntity where name = :name",
				AssignedEntity.class
		).setParameter( "name", "identity" ).getSingleResult().getId() ).isEqualTo( 1L ) );
		// before execution generation
		scope.inTransaction( session -> session.persist( new AssignedEntity( 42L, "assigned" ) ) );
		scope.inSession( session -> assertThat( session.createQuery(
				"from AssignedEntity where name = :name",
				AssignedEntity.class
		).setParameter( "name", "assigned" ).getSingleResult().getId() ).isEqualTo( 42L ) );
	}

	@Test
	public void testIdentityOrRandomId(SessionFactoryScope scope) {
		// on execution generation
		scope.inTransaction( session -> session.persist( new RandomEntity( "identity" ) ) );
		scope.inSession( session -> assertThat( session.createQuery(
				"from RandomEntity where name = :name",
				RandomEntity.class
		).setParameter( "name", "identity" ).getSingleResult().getId() ).isEqualTo( 1L ) );
		// before execution generation
		scope.inTransaction( session -> session.persist( new RandomEntity( "random" ) ) );
		scope.inSession( session -> assertThat( session.createQuery(
				"from RandomEntity where name = :name",
				RandomEntity.class
		).setParameter( "name", "random" ).getSingleResult().getId() ).isNotEqualTo( 1L ) );
	}

	@Test
	public void testGeneratedPropInsert(SessionFactoryScope scope) {
		// on execution generation
		scope.inTransaction( session -> session.persist( new StringGeneratedEntity( 1L, "literal" ) ) );
		// todo marco : assert also value setting after persist+flush (see GeneratedValuesProcessor)
		scope.inSession( session -> assertThat(
				session.find( StringGeneratedEntity.class, 1L ).getGeneratedProp()
		).startsWith( "literal" ) );
		// todo marco : GeneratedValuesProcessor can be optimized to not select back the value in this cases
		//  (we need to call generatedOnExecution(session, owner) again though which might be expensive?
		// before execution generation
		scope.inTransaction( session -> session.persist( new StringGeneratedEntity( 2L, "generated" ) ) );
		scope.inSession( session -> assertThat(
				session.find( StringGeneratedEntity.class, 2L ).getGeneratedProp()
		).startsWith( "generated" ) );
	}

	@Entity( name = "AssignedEntity" )
	public static class AssignedEntity {
		@Id
		@GeneratedValue( generator = "identity_or_assigned" )
		@GenericGenerator( name = "identity_or_assigned", type = IdentityOrAssignedGenerator.class )
		private Long id;

		private String name;

		public AssignedEntity() {
		}

		public AssignedEntity(String name) {
			this.name = name;
		}

		public AssignedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity( name = "RandomEntity" )
	public static class RandomEntity {
		@Id
		@GeneratedValue( generator = "identity_or_random" )
		@GenericGenerator( name = "identity_or_random", type = IdentityOrRandomGenerator.class )
		private Long id;

		private String name;

		public RandomEntity() {
		}

		public RandomEntity(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@ValueGenerationType(generatedBy = LiteralOrGeneratedStringGenerator.class)
	@Retention(RUNTIME)
	@Target({ FIELD, METHOD })
	public @interface GeneratedString {
		/**
		 * Specifies how the timestamp is generated. By default, it is generated
		 * in memory, which saves a round trip to the database.
		 */
		SourceType source() default SourceType.VM;
	}


	@Entity( name = "StringGeneratedEntity" )
	public static class StringGeneratedEntity {
		@Id
		private Long id;

		private String name;

		@GeneratedString
		private String generatedProp;

		public StringGeneratedEntity() {
		}

		public StringGeneratedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public String getGeneratedProp() {
			return generatedProp;
		}
	}

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

	public static class IdentityOrRandomGenerator extends IdentityGenerator implements BeforeExecutionGenerator {
		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			return new Random().nextLong( 1_000 ) + 100;
		}

		@Override
		public boolean generatedOnExecution(SharedSessionContractImplementor session, Object owner) {
			return !( (RandomEntity) owner ).getName().equals( "random" );
		}

		@Override
		public boolean generatedOnExecution() {
			return true;
		}
	}

	public static class LiteralOrGeneratedStringGenerator implements OnExecutionGenerator, BeforeExecutionGenerator {
		private int count;

		public LiteralOrGeneratedStringGenerator() {
			count = 0;
		}

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			return "generated_" + count++;
		}

		@Override
		public boolean generatedOnExecution(SharedSessionContractImplementor session, Object owner) {
			return !( (StringGeneratedEntity) owner ).getName().equals( "generated" );
		}

		@Override
		public boolean generatedOnExecution() {
			return true;
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			// todo marco : updates are still not handled
			return EventTypeSets.ALL;
		}

		@Override
		public boolean referenceColumnsInSql(Dialect dialect) {
			return true;
		}

		@Override
		public boolean writePropertyValue() {
			return false;
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect) {
			return new String[] { "'literal_" + count++ + "'" };
		}
	}
}
