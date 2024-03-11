/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Christian Beikov
 */
public class CustomMutabilityConvertedBasicTypeImpl<J> extends ConvertedBasicTypeImpl<J> {
	private final MutabilityPlan<J> mutabilityPlan;
	private final Class<J> primitiveClass;

	public CustomMutabilityConvertedBasicTypeImpl(
			String name,
			JdbcType jdbcType,
			BasicValueConverter<J, ?> converter,
			MutabilityPlan<J> mutabilityPlan) {
		super( name, jdbcType, converter );
		this.mutabilityPlan = mutabilityPlan;
		this.primitiveClass = null;
	}

	public CustomMutabilityConvertedBasicTypeImpl(
			String name,
			String description,
			JdbcType jdbcType,
			BasicValueConverter<J, ?> converter,
			Class<J> primitiveClass,
			MutabilityPlan<J> mutabilityPlan) {
		super( name, description, jdbcType, converter );
		this.mutabilityPlan = mutabilityPlan;
		assert primitiveClass == null || primitiveClass.isPrimitive();
		this.primitiveClass = primitiveClass;
	}

	@Override
	protected MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public Class<J> getJavaType() {
		return primitiveClass != null ? primitiveClass : super.getJavaType();
	}
}
