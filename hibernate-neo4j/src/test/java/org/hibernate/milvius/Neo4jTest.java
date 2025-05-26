/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.milvius;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.neo4j.Neo4jDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;


/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = {Neo4jTest.TestEntity.class})
@SessionFactory
@ServiceRegistry(
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

	@Entity(name = "TestEntity")
	static class TestEntity {
		@Id
		private Long id;

		@Column(name = "name_property", nullable = false)
		private String name;

		private LocalTime localTime;

		private LocalDateTime localDateTime;

		private String[] array;
	}
}
