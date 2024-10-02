package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.AbstractIdentifiableType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.MappedSuperclassDomainType;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;

import java.util.Collection;

/**
 * @author Marco Belladelli
 */
public class MappedSuperclassEntityTypeImpl<J> extends AbstractIdentifiableType<J>
		implements EntityDomainType<J>, MappedSuperclassDomainType<J> {
	final EntityDomainType<J> delegate;

	public MappedSuperclassEntityTypeImpl(EntityDomainType<J> delegate, JpaMetamodelImplementor metamodel) {
		super(
				delegate.getTypeName(),
				delegate.getExpressibleJavaType(),
				(IdentifiableDomainType<? super J>) delegate.getSuperType(),
				delegate.hasIdClass(),
				delegate.hasSingleIdAttribute(),
				delegate.hasVersionAttribute(),
				metamodel
		);
		this.delegate = delegate;
	}

	@Override
	public MappedSuperclassEntityTypeImpl<J> getSqmType() {
		return this;
	}

	@Override
	public Collection getSubTypes() {
		return delegate.getSubTypes();
	}

	@Override
	public String getHibernateEntityName() {
		return delegate.getHibernateEntityName();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public BindableType getBindableType() {
		return delegate.getBindableType();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return delegate.getPersistenceType();
	}

	@Override
	public String getPathName() {
		return delegate.getPathName();
	}

	@Override
	public DomainType<J> getSqmPathType() {
		return delegate.getSqmPathType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return delegate.findSubPathSource( name );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		return delegate.createSqmPath( lhs, intermediatePathSource );
	}
}
