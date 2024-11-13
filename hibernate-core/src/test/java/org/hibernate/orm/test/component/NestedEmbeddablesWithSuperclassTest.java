/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		NestedEmbeddablesWithSuperclassTest.TestEntity.class,
		NestedEmbeddablesWithSuperclassTest.VersionBase.class,
		NestedEmbeddablesWithSuperclassTest.VersionInfo.class,
		NestedEmbeddablesWithSuperclassTest.AuditBase.class,
		NestedEmbeddablesWithSuperclassTest.AuditInfo.class,
})
@SessionFactory
public class NestedEmbeddablesWithSuperclassTest {
	@Test
	public void testMappingWorks(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity testEntity = session.find( TestEntity.class, 1L );
			final AuditInfo auditInfo = testEntity.getAuditInfo();
			assertThat( auditInfo ).extracting( AuditBase::getUserID, AuditInfo::getUser ).containsExactly(
					2,
					"user_2"
			);
			assertThat( auditInfo ).extracting( AuditInfo::getVersion ).extracting(
					VersionInfo::getVersion,
					VersionInfo::getVersionName
			).containsExactly( 1L, "version_1" );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final VersionInfo versionInfo = new VersionInfo();
			versionInfo.version = 1L;
			versionInfo.versionName = "version_1";
			final AuditInfo auditInfo = new AuditInfo();
			auditInfo.user = "user_2";
			auditInfo.userID = 2;
			auditInfo.version = versionInfo;
			final TestEntity testEntity = new TestEntity();
			testEntity.id = 1L;
			testEntity.auditInfo = auditInfo;
			session.persist( testEntity );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "TestEntity")
	static class TestEntity {
		@Id
		private Long id;

		@Embedded
		private AuditInfo auditInfo;

		public AuditInfo getAuditInfo() {
			return auditInfo;
		}
	}

	@MappedSuperclass
	static class VersionBase {
		@Column(name = "version_col")
		Long version;

		public Long getVersion() {
			return version;
		}
	}

	@Embeddable
	static class VersionInfo extends VersionBase {
		String versionName;

		public String getVersionName() {
			return versionName;
		}
	}

	@MappedSuperclass
	static class AuditBase {
		int userID;
		VersionInfo version;

		public int getUserID() {
			return userID;
		}

		public VersionInfo getVersion() {
			return version;
		}
	}

	@Embeddable
	static class AuditInfo extends AuditBase {
		@Column(name = "user_col")
		String user;

		public String getUser() {
			return user;
		}
	}
}
