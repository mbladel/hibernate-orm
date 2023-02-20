/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Custom {@link DateTruncEmulation} that handles rendering when using the convert function to parse datetime strings
 *
 * @author Marco Belladelli
 */
public class DateTruncConvertEmulation extends DateTruncEmulation {
	public DateTruncConvertEmulation(TypeConfiguration typeConfiguration) {
		super( "convert", typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( toDateFunction );
		sqlAppender.append( '(' );
		sqlAppender.append( "datetime," );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( ')' );
	}
}
