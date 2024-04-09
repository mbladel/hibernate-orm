/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;

/**
 * @author Marco Belladelli
 */
@Embeddable
@DiscriminatorValue( "sub_child_one" )
class SubChildEmbeddableOne extends ChildEmbeddableOne {
	private Double subChildOneProp;

	public SubChildEmbeddableOne() {
	}

	public SubChildEmbeddableOne(String parentProp, Integer childOneProp, Double subChildOneProp) {
		super( parentProp, childOneProp );
		this.subChildOneProp = subChildOneProp;
	}

	public Double getSubChildOneProp() {
		return subChildOneProp;
	}

	public void setSubChildOneProp(Double subChildOneProp) {
		this.subChildOneProp = subChildOneProp;
	}
}
