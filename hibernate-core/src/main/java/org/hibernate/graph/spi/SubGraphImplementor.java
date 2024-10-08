/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import org.hibernate.graph.CannotBecomeEntityGraphException;
import org.hibernate.graph.CannotContainSubGraphException;
import org.hibernate.graph.SubGraph;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

/**
 * Integration version of the {@link SubGraph} contract.
 *
 * @author Steve Ebersole
 *
 * @see RootGraphImplementor
 */
public interface SubGraphImplementor<J> extends SubGraph<J>, GraphImplementor<J> {

	@Override
	SubGraphImplementor<J> makeCopy(boolean mutable);

	@Override
	default SubGraphImplementor<J> makeSubGraph(boolean mutable) {
		return !mutable && !isMutable() ? this : makeCopy( mutable );
	}

	@Override
	RootGraphImplementor<J> makeRootGraph(String name, boolean mutable)
			throws CannotBecomeEntityGraphException;

	@Override
	<AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName);

	@Override
	<AJ> AttributeNodeImplementor<AJ> addAttributeNode(PersistentAttribute<? super J, AJ> attribute);

	@Override
	default <AJ> SubGraphImplementor<? extends AJ> addKeySubGraph(PersistentAttribute<? super J, AJ> attribute, Class<? extends AJ> subType)
			throws CannotContainSubGraphException {
		return null;
	}
}
