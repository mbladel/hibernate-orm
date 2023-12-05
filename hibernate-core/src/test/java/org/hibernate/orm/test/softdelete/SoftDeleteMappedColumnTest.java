/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.softdelete;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
public class SoftDeleteMappedColumnTest {
	@Test
	public void testValid() {
		try (SessionFactory sf = buildSessionFactory( ValidEntity.class )) {
			sf.inTransaction( session -> {
				final ValidEntity validEntity = new ValidEntity( 1L, "valid1" );
				session.persist( validEntity );
				session.flush();
				assertThat( validEntity.isDeleted() ).isFalse();
				session.remove( validEntity );
			} );
			sf.inSession( session -> {
				assertThat( session.find( ValidEntity.class, 1L ) ).isNull();
			} );
		}
	}

	@Test
	public void testInvalid() {
		try (SessionFactory sf = buildSessionFactory( InvalidEntity.class )) {
			// todo : we should fail() here, validation should prevent the sf being build
			sf.inTransaction( session -> {
				final InvalidEntity entity = new InvalidEntity( 2L, "invalid2" );
				session.persist( entity );
			} );
		}
		// todo : we should catch the validation error and assert
	}

	private SessionFactory buildSessionFactory(Class<?> entityClass) {
		final StandardServiceRegistryBuilder ssrb = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.CREATE_DROP );
		return new MetadataSources( ssrb.build() ).addAnnotatedClasses( entityClass )
				.buildMetadata()
				.buildSessionFactory();
	}

	@Entity( name = "ValidEntity" )
	@SoftDelete( columnName = "is_deleted" )
	public static class ValidEntity {
		@Id
		private Long id;

		private String name;

		@Column( name = "is_deleted", insertable = false, updatable = false )
		private boolean deleted;

		public ValidEntity() {
		}

		public ValidEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public boolean isDeleted() {
			return deleted;
		}
	}

	@Entity( name = "InvalidEntity" )
	@SoftDelete( columnName = "is_deleted" )
	public static class InvalidEntity {
		@Id
		private Long id;

		private String name;

		@Column( name = "is_deleted" )
		private boolean deleted;

		public InvalidEntity() {
		}

		public InvalidEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
