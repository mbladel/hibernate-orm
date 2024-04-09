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
@DiscriminatorValue( "child_one" )
class ChildEmbeddableOne extends ParentEmbeddable {
	private Integer childOneProp;

	public ChildEmbeddableOne() {
	}

	public ChildEmbeddableOne(String parentProp, Integer childOneProp) {
		super( parentProp );
		this.childOneProp = childOneProp;
	}

	public Integer getChildOneProp() {
		return childOneProp;
	}

	public void setChildOneProp(Integer childOneProp) {
		this.childOneProp = childOneProp;
	}
}
