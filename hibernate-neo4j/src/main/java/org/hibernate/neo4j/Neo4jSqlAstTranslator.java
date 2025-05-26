package org.hibernate.neo4j;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlParameterInfo;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.internal.TableDeleteStandard;
import org.hibernate.sql.model.internal.TableInsertStandard;
import org.hibernate.sql.model.internal.TableUpdateStandard;

import java.util.List;

public class Neo4jSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	protected Neo4jSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement, SqlParameterInfo parameterInfo) {
		super( sessionFactory, statement, parameterInfo );
	}

	@Override
	public void renderNamedSetReturningFunction(String functionName, List<? extends SqlAstNode> sqlAstArguments, AnonymousTupleTableGroupProducer tupleType, String tableIdentifierVariable, SqlAstNodeRenderingMode argumentRenderingMode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void render(SqlAstNode sqlAstNode, SqlAstNodeRenderingMode renderingMode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitStandardTableInsert(TableInsertStandard tableInsert) {
		getCurrentClauseStack().push( Clause.INSERT );
		appendSql( "create " );
		appendSql( OPEN_PARENTHESIS );

		appendSql( "e:" ); // generic variable name, not needed
		appendSql( tableInsert.getMutatingTable().getTableName() );
		appendSql( ' ' );

		getCurrentClauseStack().push( Clause.VALUES );
		tableInsert.forEachValueBinding( (columnPosition, columnValueBinding) -> {
			if ( columnPosition == 0 ) {
				appendSql( '{' );
			}
			else {
				appendSql( ',' );
			}
			appendSql( columnValueBinding.getColumnReference().getColumnExpression() );
			appendSql( ':' );
			columnValueBinding.getValueExpression().accept( this );
		} );
		appendSql( "})" );

		getCurrentClauseStack().pop();
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		getQueryPartStack().push( querySpec );

		try {
			visitFromClause( querySpec.getFromClause() ); // match
			visitWhereClause( querySpec.getWhereClauseRestrictions() );
			visitSelectClause( querySpec.getSelectClause() ); // return
			visitOrderBy( querySpec.getSortSpecifications() );
			visitOffsetFetchClause( querySpec );
			visitForUpdateClause( querySpec );
		}
		finally {
			getQueryPartStack().pop();
		}
	}

	@Override
	public void visitFromClause(FromClause fromClause) {
		if ( fromClause == null || fromClause.getRoots().isEmpty() ) {
			throw new UnsupportedOperationException( "Neo4j doesn't support an empty from clause" );
		}
		else {
			renderFromClauseSpaces( fromClause );
		}
	}

	@Override
	protected void renderFromClauseSpaces(FromClause fromClause) {
		try {
			getCurrentClauseStack().push( Clause.FROM );
			String separator = NO_SEPARATOR;
			for ( TableGroup root : fromClause.getRoots() ) {
				separator = renderFromClauseRoot( root, separator );
			}
		}
		finally {
			getCurrentClauseStack().pop();
		}
	}

	private String renderFromClauseRoot(TableGroup root, String separator) {
		if ( root.isVirtual() ) {
			for ( TableGroupJoin tableGroupJoin : root.getTableGroupJoins() ) {
				addAdditionalWherePredicate( tableGroupJoin.getPredicate() );
				renderFromClauseRoot( tableGroupJoin.getJoinedGroup(), separator );
			}
			for ( TableGroupJoin tableGroupJoin : root.getNestedTableGroupJoins() ) {
				addAdditionalWherePredicate( tableGroupJoin.getPredicate() );
				renderFromClauseRoot( tableGroupJoin.getJoinedGroup(), separator );
			}
		}
		else if ( root.isInitialized() ) {
			appendSql( separator );
			appendSql( "match(" );
			renderRootTableGroup( root, null );
			appendSql( ')' ); // separate labels with a pipe
		}
		return " ";
	}

	@Override
	protected boolean renderNamedTableReference(NamedTableReference tableReference, LockMode lockMode) {
		appendSql( tableReference.getIdentificationVariable() != null
				? tableReference.getIdentificationVariable()
				: tableReference.getTableId() );
		appendSql( ":" );
		appendSql( tableReference.getTableExpression() );
		registerAffectedTable( tableReference );
		return false;
	}

	@Override
	public void visitSelectClause(SelectClause selectClause) {
		getCurrentClauseStack().push( Clause.SELECT );

		try {
			appendSql( " return " );
			if ( selectClause.isDistinct() ) {
				appendSql( "distinct " );
			}
			visitSqlSelections( selectClause );
			renderVirtualSelections( selectClause );
		}
		finally {
			getCurrentClauseStack().pop();
		}
	}

	@Override
	public void visitStandardTableUpdate(TableUpdateStandard tableUpdate) {
		super.visitStandardTableUpdate( tableUpdate );
	}

	@Override
	public void visitStandardTableDelete(TableDeleteStandard tableDelete) {
		super.visitStandardTableDelete( tableDelete );
	}
}
