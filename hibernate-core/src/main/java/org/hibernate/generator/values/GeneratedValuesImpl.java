/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ValuedModelPart;

/**
 * @author Marco Belladelli
 */
public class GeneratedValuesImpl {
	private final Map<ModelPart, Object> generatedValuesMap;

	public GeneratedValuesImpl(List<? extends ModelPart> generatedProperties) {
		this.generatedValuesMap = new IdentityHashMap<>( generatedProperties.size() );
	}

	public void addGeneratedValue(ModelPart modelPart, Object value) {
		generatedValuesMap.put( modelPart, value );
	}

	public Object getGeneratedValue(ModelPart modelPart) {
		return generatedValuesMap.get( modelPart );
	}

	public List<Object> getGeneratedValues(List<? extends ModelPart> modelParts) {
		if ( CollectionHelper.isEmpty( modelParts ) ) {
			return Collections.emptyList();
		}

		final List<Object> generatedValues = new ArrayList<>( modelParts.size() );
		for ( ModelPart modelPart : modelParts ) {
			assert generatedValuesMap.containsKey( modelPart );
			generatedValues.add( generatedValuesMap.get( modelPart ) );
		}

		return generatedValues;
	}
}
