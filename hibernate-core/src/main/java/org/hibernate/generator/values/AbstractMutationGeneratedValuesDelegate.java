/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.values;

import org.hibernate.generator.EventType;

/**
 * @author Marco Belladelli
 */
public abstract class AbstractMutationGeneratedValuesDelegate implements MutationGeneratedValuesDelegate {
	private final EventType timing;

	public AbstractMutationGeneratedValuesDelegate(EventType timing) {
		this.timing = timing;
	}

	@Override
	public EventType getTiming() {
		return timing;
	}
}
