/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.identifier;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EmbeddedIdGeneratedValueTest.SystemUser.class,
		EmbeddedIdGeneratedValueTest.SystemUserIdClass.class,
		EmbeddedIdGeneratedValueTest.PK.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-15074" )
public class EmbeddedIdGeneratedValueTest {
	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from SystemUser" ).executeUpdate() );
		scope.inTransaction( session -> session.createMutationQuery( "delete from SystemUserIdClass" )
				.executeUpdate() );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		final SystemUser _systemUser = scope.fromTransaction( session -> {
			final SystemUser systemUser = new SystemUser();
			systemUser.setUsername( "mbladel" );
			systemUser.setName( "Marco Belladelli" );
			session.persist( systemUser );
			return systemUser;
		} );

		scope.inSession( session -> {
			final SystemUser systemUser = session.find( SystemUser.class, new PK(
					_systemUser.getUsername(),
					_systemUser.getRegistrationId()
			) );
			assertThat( systemUser.getName() ).isEqualTo( "Marco Belladelli" );
			assertThat( systemUser.getUsername() ).isEqualTo( "mbladel" );
			assertThat( systemUser.getRegistrationId() ).isNotNull();
		} );
	}

	@Test
	public void testIdClass(SessionFactoryScope scope) {
		final SystemUserIdClass _systemUser = scope.fromTransaction( session -> {
			final SystemUserIdClass systemUser = new SystemUserIdClass();
			systemUser.setUsername( "mbladel" );
			systemUser.setName( "Marco Belladelli" );
			session.persist( systemUser );
			return systemUser;
		} );

		scope.inSession( session -> {
			final SystemUserIdClass systemUser = session.find( SystemUserIdClass.class, new PK(
					_systemUser.getUsername(),
					_systemUser.getRegistrationId()
			) );
			assertThat( systemUser.getName() ).isEqualTo( "Marco Belladelli" );
			assertThat( systemUser.getUsername() ).isEqualTo( "mbladel" );
			assertThat( systemUser.getRegistrationId() ).isNotNull();
		} );
	}


	@Entity( name = "SystemUser" )
	public static class SystemUser {
		@EmbeddedId
		private PK id;

		private String name;

		public SystemUser() {
			this.id = new PK();
		}

		public String getUsername() {
			return id.getUsername();
		}

		public void setUsername(String username) {
			this.id.setUsername( username );
		}

		public Integer getRegistrationId() {
			return id.getRegistrationId();
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	@Entity( name = "SystemUserIdClass" )
	@IdClass( PK.class )
	public static class SystemUserIdClass {
		@Id
		private String username;

		@Id
		@GeneratedValue
		private Integer registrationId;

		private String name;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public Integer getRegistrationId() {
			return registrationId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class PK implements Serializable {
		private String username;

		@GeneratedValue
		private Integer registrationId;

		public PK() {
		}

		public PK(String username, Integer registrationId) {
			this.username = username;
			this.registrationId = registrationId;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public Integer getRegistrationId() {
			return registrationId;
		}
	}
}
