/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import jakarta.persistence.metamodel.Bindable;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmPathSource<J> implements SqmPathSource<J>, Bindable<J> {
	private final String localPathName;
	protected final SqmPathSource<J> pathModel;
	private final DomainType<J> domainType;
	private final BindableType jpaBindableType;

	public AbstractSqmPathSource(
			String localPathName,
			SqmPathSource<J> pathModel,
			DomainType<J> domainType,
			BindableType jpaBindableType) {
		this.localPathName = localPathName;
		this.pathModel = pathModel == null ? this : pathModel;
		this.domainType = domainType;
		this.jpaBindableType = jpaBindableType;
	}

	@Override
	public Class<J> getQueryJavaType() {
		return domainType.getQueryJavaType();
	}

	@Override
	public String getPathName() {
		return localPathName;
	}

	@Override
	public DomainType<J> getSqmPathType() {
		return domainType;
	}

	@Override
	public Class<J> getBindableJavaType() {
		return getQueryJavaType();
	}

	@Override
	public BindableType getBindableType() {
		return jpaBindableType;
	}

	@Override
	public JavaType<J> getExpressibleJavaType() {
		return domainType.getExpressibleJavaType();
	}
}
