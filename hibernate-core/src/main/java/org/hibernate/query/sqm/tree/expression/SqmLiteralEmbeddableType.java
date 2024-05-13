/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.type.StandardBasicTypes;

/**
 * Represents a reference to an embeddable type as a literal.
 *
 * @author Marco Belladelli
 */
public class SqmLiteralEmbeddableType
		extends AbstractSqmExpression<String>
		implements SqmSelectableNode<String>, SemanticPathPart {
	private final String embeddableClassName;

	public SqmLiteralEmbeddableType(
			String embeddableClassName,
			NodeBuilder nodeBuilder) {
		super(
				nodeBuilder.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ),
				nodeBuilder
		);
		this.embeddableClassName = embeddableClassName;
	}

	public String getEmbeddableClassName() {
		return embeddableClassName;
	}

	@Override
	public SqmLiteralEmbeddableType copy(SqmCopyContext context) {
		final SqmLiteralEmbeddableType existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmLiteralEmbeddableType expression = context.registerCopy(
				this,
				new SqmLiteralEmbeddableType(
						embeddableClassName,
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public void internalApplyInferableType(SqmExpressible<?> type) {
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String asLoggableText() {
		return "TYPE(" + embeddableClassName + ")";
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an embeddable name" );
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an embeddable name" );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( embeddableClassName );
	}
}
