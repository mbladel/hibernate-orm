/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
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
public class SqmTreatedSingularEmbeddedJoin<O, T, S extends T> extends AbstractSqmTreatedSingularJoin<O, T, S> {
	private final EmbeddableDomainType<S> treatTarget;

	public SqmTreatedSingularEmbeddedJoin(
			SqmSingularJoin<O,T> wrappedPath,
			EmbeddableDomainType<S> treatTarget,
			String alias) {
		super(
				wrappedPath,
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getTypeName(),
						alias
				),
				alias,
				false
		);
		this.treatTarget = treatTarget;
	}

	private SqmTreatedSingularEmbeddedJoin(
			NavigablePath navigablePath,
			SqmSingularJoin<O,T> wrappedPath,
			EmbeddableDomainType<S> treatTarget,
			String alias) {
		//noinspection unchecked
		super(
				wrappedPath,
				navigablePath,
				alias,
				false
		);
		this.treatTarget = treatTarget;
	}

	@Override
	public SqmTreatedSingularEmbeddedJoin<O, T, S> copy(SqmCopyContext context) {
		final SqmTreatedSingularEmbeddedJoin<O, T, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedSingularEmbeddedJoin<O, T, S> path = context.registerCopy(
				this,
				new SqmTreatedSingularEmbeddedJoin<>(
						getNavigablePath(),
						getWrappedPath().copy( context ),
						treatTarget,
						getExplicitAlias()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public EmbeddableDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public EmbeddableDomainType<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public SqmAttributeJoin<O, S> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmTreatedSingularEmbeddedJoin<>( getWrappedPath(), treatTarget, getAlias() );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "treat(" );
		getWrappedPath().appendHqlString( sb );
		sb.append( " as " );
		sb.append( treatTarget.getTypeName() );
		sb.append( ')' );
	}
}
