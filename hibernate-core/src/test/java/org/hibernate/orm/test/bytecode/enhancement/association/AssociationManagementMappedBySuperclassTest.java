/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import org.hibernate.orm.test.bytecode.enhancement.access.BaseEntity;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		BaseEntity.class,
		AssociationManagementMappedBySuperclassTest.TestEntity.class,
		AssociationManagementMappedBySuperclassTest.UserEntity.class,
} )
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( biDirectionalAssociationManagement = true )
public class AssociationManagementMappedBySuperclassTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

		} );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity extends BaseEntity {
		private Integer amount;

		public TestEntity() {
		}

		public TestEntity(Long id, UserEntity createdBy, Integer amount) {
			super( id, createdBy );
			this.amount = amount;
		}
	}

	@Entity(name = "UserEntity")
	public static class UserEntity {
		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "owner")
		private List<TestEntity> items;

		public UserEntity() {
		}

		public UserEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
