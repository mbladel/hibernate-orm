/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.milvius;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.neo4j.Neo4jDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;



/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = {})
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.NATIVE_IGNORE_JDBC_PARAMETERS, value = "true")
		}
//		services = {
//				@ServiceRegistry.Service( role = NativeQueryInterpreter.class, impl = MilvusNativeQueryInterpreter.class)
//		}
)
@RequiresDialect(value = Neo4jDialect.class)
public class Neo4jTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

		} );
	}
}
