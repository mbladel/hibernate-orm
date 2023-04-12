/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.util.List;
import java.util.Optional;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel( annotatedClasses = BooleanToStringConverterTest.BooleanTestPOJO.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16401" )
public class BooleanToStringConverterTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new BooleanTestPOJO( 1L, true, "CODE123" ) );
			session.persist( new BooleanTestPOJO( 2L, true, "CODE456" ) );
			session.persist( new BooleanTestPOJO( 3L, true, "CODE789" ) );
			session.persist( new BooleanTestPOJO( 4L, false, "CODE987" ) );
			session.persist( new BooleanTestPOJO( 5L, false, "CODE654" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BooleanTestPOJO " ).executeUpdate() );
	}

	@Test
	public void testEqualsTrueFalse(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<BooleanTestPOJO> trueResults = session.createQuery(
					"from BooleanTestPOJO pojo where pojo.test = true ",
					BooleanTestPOJO.class
			).getResultList();
			assertEquals( 3, trueResults.size() );
			final List<BooleanTestPOJO> falseResults = session.createQuery(
					"from BooleanTestPOJO pojo where pojo.test = false ",
					BooleanTestPOJO.class
			).getResultList();
			assertEquals( 2, falseResults.size() );
		} );
	}

	@Entity( name = "BooleanTestPOJO" )
	public static class BooleanTestPOJO {
		@Id
		private Long id;
		@Convert( converter = BooleanJNConverter.class )
		private Boolean test;
		private String code;

		public BooleanTestPOJO() {
		}

		public BooleanTestPOJO(Long id, Boolean test, String code) {
			this.id = id;
			this.test = test;
			this.code = code;
		}
	}

	@Converter
	public static class BooleanJNConverter implements AttributeConverter<Boolean, String> {
		@Override
		public String convertToDatabaseColumn(Boolean attribute) {
			return Optional.ofNullable( attribute ).map( b -> b ? "J" : "N" ).orElse( null );
		}

		@Override
		public Boolean convertToEntityAttribute(String dbData) {
			return Optional.ofNullable( dbData ).map( "J"::equals ).orElse( null );
		}
	}
}