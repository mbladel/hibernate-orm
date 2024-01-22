/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpamodelgen.test.embeddable.collections;

import java.util.Set;

import org.hibernate.jpamodelgen.test.embeddable.SimpleEntity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToMany;

/**
 * @author Marco Belladelli
 */
@Embeddable
//@Access( AccessType.FIELD )
public class CollectionsEmbeddable {
	@ManyToMany
	private Set<SimpleEntity> simpleEntities;

	public Set<SimpleEntity> getSimpleEntities() {
		return simpleEntities;
	}

	public void setSimpleEntities(Set<SimpleEntity> simpleEntities) {
		this.simpleEntities = simpleEntities;
	}
}
