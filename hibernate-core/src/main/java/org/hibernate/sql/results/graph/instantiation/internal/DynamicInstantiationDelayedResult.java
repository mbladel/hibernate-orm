/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import java.util.function.Supplier;

/**
 * @author Marco Belladelli
 */
public class DynamicInstantiationDelayedResult<T> {
	private final Supplier<T> supplier;

	public DynamicInstantiationDelayedResult(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	public T get() {
		return supplier.get();
	}
}
