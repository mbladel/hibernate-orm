/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

/**
 * @author Marco Belladelli
 */
public class StructAttributeValues {
	private final Object[] attributeValues;
	private final int numberOfAttributeMappings;
	private Object discriminatorValue;

	public StructAttributeValues(int numberOfAttributeMappings, Object[] rawJdbcValues) {
		if ( rawJdbcValues == null || numberOfAttributeMappings != rawJdbcValues.length) {
			attributeValues = new Object[numberOfAttributeMappings];
		}
		else {
			attributeValues = rawJdbcValues;
		}
		this.numberOfAttributeMappings = numberOfAttributeMappings;
	}

	public void setAttributeValue(int index, Object value) {
		if ( index == numberOfAttributeMappings ) {
			discriminatorValue = value;
		}
		else {
			attributeValues[index] = value;
		}
	}

	public Object[] getAttributeValues() {
		return attributeValues;
	}

	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}
}
