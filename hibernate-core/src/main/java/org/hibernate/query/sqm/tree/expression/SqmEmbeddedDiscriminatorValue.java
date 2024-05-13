/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.mapping.EmbeddableDiscriminatorMapping;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.type.BasicType;

import org.checkerframework.checker.nullness.qual.Nullable;

public class SqmEmbeddedDiscriminatorValue<T> extends AbstractSqmExpression<T>
		implements SqmSelectableNode<T>, SemanticPathPart {
	private final String pathName;
	private final String embeddableClassName;

	public SqmEmbeddedDiscriminatorValue(
			String pathName,
			BasicType<T> domainType,
			String embeddableClassName,
			NodeBuilder nodeBuilder) {
		super( domainType, nodeBuilder );
		this.embeddableClassName = embeddableClassName;
		this.pathName = pathName;
	}

	public String getEmbeddableClassName() {
		return embeddableClassName;
	}

	@Override
	public @Nullable BasicType<T> getNodeType() {
		return (BasicType<T>) super.getNodeType();
	}

	@Override
	public SqmEmbeddedDiscriminatorValue<T> copy(SqmCopyContext context) {
		final SqmEmbeddedDiscriminatorValue<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmEmbeddedDiscriminatorValue<T> expression = context.registerCopy(
				this,
				new SqmEmbeddedDiscriminatorValue<>(
						pathName,
						getNodeType(),
						embeddableClassName,
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEmbeddedDiscriminatorTypeValueExpression( this );
	}

	public String getPathName() {
		return pathName;
	}

	@Override
	public String asLoggableText() {
		return "SqmEmbeddedDiscriminatorValue(" + embeddableClassName + ")";
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an entity name" );
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an entity name" );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( embeddableClassName );
	}
}
