/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.values;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.ValuedModelPart;

/**
 * @author Marco Belladelli
 */
public class GeneratedValuesImpl {
	private final Map<ValuedModelPart, Object> generatedValuesMap;

	public GeneratedValuesImpl(List<? extends ValuedModelPart> generatedProperties) {
		this.generatedValuesMap = new IdentityHashMap<>( generatedProperties.size() );
	}

	public void addGeneratedValue(ValuedModelPart modelPart, Object value) {
		generatedValuesMap.put( modelPart, value );
	}

	public Object getGeneratedValue(ValuedModelPart modelPart) {
		return generatedValuesMap.get( modelPart );
	}

	public Object[] getGeneratedValues(List<? extends ValuedModelPart> modelParts) {
		if ( CollectionHelper.isEmpty( modelParts ) ) {
			return new Object[] {};
		}

		final List<Object> generatedValues = new ArrayList<>( modelParts.size() );
		for ( ValuedModelPart modelPart : modelParts ) {
			assert generatedValuesMap.containsKey( modelPart );
			generatedValues.add( generatedValuesMap.get( modelPart ) );
		}

		return generatedValues.toArray( new Object[0] );
	}
}
