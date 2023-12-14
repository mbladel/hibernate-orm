/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.idgen.userdefined;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.HibernateException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = SequenceOrAssignedGeneratorTest.MyEntity.class )
@SessionFactory( useCollectingStatementInspector = true )
public class SequenceOrAssignedGeneratorTest {
	@Test
	public void testPersistExistingId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MyEntity e = new MyEntity( "Hello World" );
			session.persist( e );
			session.flush();
			assertThat( e.getId() ).isNotNull();
		} );
	}

	@Test
	public void testPersistNullId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MyEntity e = new MyEntity( 123L, "Hello World" );
			session.persist( e );
			session.flush();
			assertThat( e.getId() ).isEqualTo( 123L );
		} );
	}

	@Test
	public void testMergeExistingId(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		final MyEntity myEntity = scope.fromTransaction( session -> {
			final MyEntity e = new MyEntity( 124L, "entity_1" );
			session.persist( e );
			session.flush();
			return e;
		} );
		scope.inTransaction( session -> {
			myEntity.setName( "merged_entity_1" );
			inspector.clear();
			session.merge( myEntity );
			session.flush();
			assertThat( myEntity.getName() ).isEqualTo( "merged_entity_1" );
			inspector.assertExecutedCount( 2 );
			inspector.assertIsSelect( 0 );
			inspector.assertIsUpdate( 1 );
		} );
	}

	@Test
	public void testMergeNullId(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		final MyEntity myEntity = scope.fromTransaction( session -> {
			final MyEntity e = new MyEntity( "entity_2" );
			session.persist( e );
			session.flush();
			assertThat( e.getId() ).isNotNull();
			return e;
		} );
		scope.inTransaction( session -> {
			myEntity.setName( "merged_entity_2" );
			inspector.clear();
			session.merge( myEntity );
			session.flush();
			assertThat( myEntity.getName() ).isEqualTo( "merged_entity_2" );
			inspector.assertExecutedCount( 2 );
			inspector.assertIsSelect( 0 );
			inspector.assertIsUpdate( 1 );
		} );
	}

	@IdGeneratorType( SequenceOrAssignedGenerator.class )
	@Retention( RUNTIME )
	@Target( { METHOD, FIELD } )
	public @interface SequenceOrAssigned {
		String name();
	}

	@Entity( name = "MyEntity" )
	@GenericGenerator( type = SequenceOrAssignedGenerator.class, name = MyEntity.SEQUENCE )
	public static class MyEntity {
		protected static final String SEQUENCE = "SEQ_MyEntity";

		@Id
		@GeneratedValue( generator = SEQUENCE )
		private Long id;

		@Version
		private Long version;

		private String name;

		public MyEntity() {
		}

		public MyEntity(String name) {
			this.name = name;
		}

		public MyEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class SequenceOrAssignedGenerator extends SequenceStyleGenerator {
		@Override
		public Object generate(SharedSessionContractImplementor session, Object owner) throws HibernateException {
			if ( owner instanceof MyEntity ) {
				final MyEntity entity = (MyEntity) owner;
				final Long id = entity.getId();
				if ( id != null ) {
					// Use existing ID if one is present
					return id;
				}
			}
			return super.generate( session, owner );
		}

		@Override
		public boolean allowAssignedIdentifiers() {
			return true;
		}
	}
}
