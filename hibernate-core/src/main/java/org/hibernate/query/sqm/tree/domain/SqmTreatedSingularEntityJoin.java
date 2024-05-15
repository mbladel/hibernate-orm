/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedSingularEntityJoin<O, T, S extends T> extends AbstractSqmTreatedSingularJoin<O, T, S>
		implements SqmTreatedEntityPath<T, S> {
	private final EntityDomainType<S> treatTarget;

	public SqmTreatedSingularEntityJoin(
			SqmSingularJoin<O,T> wrappedPath,
			EntityDomainType<S> treatTarget,
			String alias) {
		this( wrappedPath, treatTarget, alias, false );
	}

	public SqmTreatedSingularEntityJoin(
			SqmSingularJoin<O,T> wrappedPath,
			EntityDomainType<S> treatTarget,
			String alias,
			boolean fetched) {
		super(
				wrappedPath,
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getHibernateEntityName(),
						alias
				),
				alias,
				fetched
		);
		this.treatTarget = treatTarget;
	}

	private SqmTreatedSingularEntityJoin(
			NavigablePath navigablePath,
			SqmSingularJoin<O,T> wrappedPath,
			EntityDomainType<S> treatTarget,
			String alias,
			boolean fetched) {
		super(
				wrappedPath,
				navigablePath,
				alias,
				fetched
		);
		this.treatTarget = treatTarget;
	}

	@Override
	public SqmTreatedSingularEntityJoin<O, T, S> copy(SqmCopyContext context) {
		final SqmTreatedSingularEntityJoin<O, T, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedSingularEntityJoin<O, T, S> path = context.registerCopy(
				this,
				new SqmTreatedSingularEntityJoin<>(
						getNavigablePath(),
						getWrappedPath().copy( context ),
						treatTarget,
						getExplicitAlias(),
						isFetched()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public EntityDomainType<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public SqmAttributeJoin<O, S> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmTreatedSingularEntityJoin<>( getWrappedPath(), treatTarget, getAlias() );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "treat(" );
		getWrappedPath().appendHqlString( sb );
		sb.append( " as " );
		sb.append( treatTarget.getName() );
		sb.append( ')' );
	}
}
