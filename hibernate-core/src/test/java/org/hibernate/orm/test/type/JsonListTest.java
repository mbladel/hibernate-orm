/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.type;

import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel( annotatedClasses = JsonListTest.Path.class )
public class JsonListTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Path( List.of(
				UUID.randomUUID(),
				UUID.randomUUID()
		) ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Path" ).executeUpdate() );
	}

	@Test
	public void testJsonRetrieval(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Path path = session.createQuery( "from Path", Path.class ).getSingleResult();
			assertThat( path ).isNotNull();
			assertThat( path.getRelativePaths() ).hasSize( 2 );
		} );
	}

	@Entity( name = "Path" )
	@Table( name = "paths" )
	public static class Path {
		@Id
		@GeneratedValue
		public Long id;

		@JdbcTypeCode( SqlTypes.JSON )
		@Column( columnDefinition = "json", nullable = false, updatable = false )
		public List<UUID> relativePaths;

		public Path() {
		}

		public Path(List<UUID> relativePaths) {
			this.relativePaths = relativePaths;
		}

		public List<UUID> getRelativePaths() {
			return relativePaths;
		}
	}

}
