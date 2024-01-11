/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpamodelgen.test.generics.imports;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Marco Belladelli
 */
@MappedSuperclass
public abstract class Parent {
	@Id
	private Long id;
}