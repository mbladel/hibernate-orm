/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.neo4j;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.Exporter;

public class Neo4jTableExporter implements Exporter<Table> {
	public static final Neo4jTableExporter INSTANCE = new Neo4jTableExporter();

	@Override
	public String[] getSqlCreateStrings(Table exportable, Metadata metadata, SqlStringGenerationContext context) {
		// todo neo4j

		return new String[0];
	}

	@Override
	public String[] getSqlDropStrings(Table exportable, Metadata metadata, SqlStringGenerationContext context) {
		// todo neo4j

		return new String[0];
	}
}
