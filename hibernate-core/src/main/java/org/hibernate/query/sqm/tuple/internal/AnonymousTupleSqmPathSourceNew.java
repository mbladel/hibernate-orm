/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tuple.internal;

import jakarta.persistence.metamodel.Bindable;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.internal.PathHelper;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleSqmPathSourceNew<J> implements SqmPathSource<J>, Bindable<J> {
	private final String localPathName;
	private final SqmPathSource<J> pathSource;
	private final DomainType<J> sqmPathType;
	private final BindableType bindableType;

	public AnonymousTupleSqmPathSourceNew(
			String localPathName,
			SqmPathSource<J> pathSource,
			DomainType<J> sqmPathType,
			BindableType bindableType) {
		this.localPathName = localPathName;
		this.pathSource = pathSource;
		this.sqmPathType = sqmPathType;
		this.bindableType = bindableType;
	}

	@Override
	public Class<J> getQueryJavaType() {
		return pathSource.getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public Class<J> getBindableJavaType() {
		return getQueryJavaType();
	}

	@Override
	public String getPathName() {
		return localPathName;
	}

	@Override
	public DomainType<J> getSqmPathType() {
		return sqmPathType;
	}

	@Override
	public BindableType getBindableType() {
		return bindableType;
	}

	@Override
	public JavaType<J> getExpressibleJavaType() {
		return pathSource.getExpressibleJavaType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return pathSource.findSubPathSource( name );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		if ( sqmPathType instanceof BasicDomainType<?> ) {
			return new SqmBasicValuedSimplePath<>(
					PathHelper.append( lhs, this, intermediatePathSource ),
					this,
					lhs,
					lhs.nodeBuilder()
			);
		}
		else if ( sqmPathType instanceof EmbeddableDomainType<?> ) {
			return new SqmEmbeddedValuedSimplePath<>(
					PathHelper.append( lhs, this, intermediatePathSource ),
					this,
					lhs,
					lhs.nodeBuilder()
			);
		}
		else if ( sqmPathType instanceof EntityDomainType<?> ) {
			return new SqmEntityValuedSimplePath<>(
					PathHelper.append( lhs, this, intermediatePathSource ),
					this,
					lhs,
					lhs.nodeBuilder()
			);
		}

		throw new UnsupportedOperationException( "Unsupported path source: " + sqmPathType );
	}
}
