/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.idgen.userdefined;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Member;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = SequenceOrAssignedGeneratorTest.MyEntity.class )
@SessionFactory
public class SequenceOrAssignedGeneratorTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MyEntity e = new MyEntity( 123L, "Hello World" );
			session.persist( e );
			session.flush();
			assertThat( e.getId() ).isEqualTo( 123L );
		} );
	}

	@IdGeneratorType( SequenceOrAssignedGenerator.class )
	@Retention( RUNTIME )
	@Target( { METHOD, FIELD } )
	public @interface SequenceOrAssigned {
		String name();
	}

	@Entity
	@GenericGenerator( type = SequenceOrAssignedGenerator.class, name = MyEntity.SEQUENCE )
	public static class MyEntity {
		protected static final String SEQUENCE = "SEQ_MyEntity";

		@Id
//		@GeneratedValue( generator = SEQUENCE )
		@GeneratedValue( strategy = GenerationType.SEQUENCE )
//		@SequenceOrAssigned( name = SEQUENCE )
		private Long id;

		//	@Version
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
	}

	public static class SequenceOrAssignedGenerator extends SequenceStyleGenerator
			implements AnnotationBasedGenerator<SequenceOrAssigned> {
//		@Override
//		protected DatabaseStructure buildSequenceStructure(
//				Type type, Properties params, JdbcEnvironment jdbcEnvironment,
//				QualifiedName sequenceName, int initialValue, int incrementSize) {
//			final String contributor = determineContributor( params );
//			final Class<?> numberType = getNumberType( type );
//			return new SequenceStructure( jdbcEnvironment, contributor, sequenceName, initialValue, incrementSize, numberType );
//		}

//		@Override
//		protected DatabaseStructure buildTableStructure(Type type, Properties params, JdbcEnvironment jdbcEnvironment,
//														QualifiedName sequenceName, int initialValue, int incrementSize) {
//			Identifier valueColumnName = determineValueColumnName( params, jdbcEnvironment );
//			String contributor = determineContributor( params );
//			Class<?> numberType = getNumberType( type );
//			return new TableStructure( jdbcEnvironment, contributor, sequenceName, valueColumnName, initialValue, incrementSize,
//									   numberType );
//		}

//		private String determineContributor(Properties params) {
//			final String contributor = params.getProperty( IdentifierGenerator.CONTRIBUTOR_NAME );
//			return contributor == null ? "orm" : contributor;
//		}

//		private Class<?> getNumberType(Type type) {
//			if ( type instanceof BasicType<?> ) {
//				BasicType<?> basicType = (BasicType<?>) type;
//				JdbcType jdbcType = basicType.getJdbcType();
//				int jdbcTypeCode = jdbcType.getJdbcTypeCode();
//				return JdbcTypeJavaClassMappings.INSTANCE.determineJavaClassForJdbcTypeCode( jdbcTypeCode );
//			}
//			return type.getReturnedClass();
//		}

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
		public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry)
				throws MappingException {
			super.configure( type, parameters, serviceRegistry );
		}

		@Override
		public void initialize(SequenceOrAssigned annotation, Member member, GeneratorCreationContext context) {
			// todo marco : what do here ?
			annotation.name();
		}
	}

}
