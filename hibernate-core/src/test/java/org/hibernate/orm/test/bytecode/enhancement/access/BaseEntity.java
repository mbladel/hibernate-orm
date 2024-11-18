/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.access;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.orm.test.bytecode.enhancement.association.AssociationManagementMappedBySuperclassTest;

/**
 * @author Marco Belladelli
 */
@MappedSuperclass
public class BaseEntity {
	@Id
	private Long id;

	@ManyToOne
	private AssociationManagementMappedBySuperclassTest.UserEntity owner;

	public BaseEntity() {
	}

	public BaseEntity(Long id, AssociationManagementMappedBySuperclassTest.UserEntity owner) {
		this.id = id;
		this.owner = owner;
	}
}
