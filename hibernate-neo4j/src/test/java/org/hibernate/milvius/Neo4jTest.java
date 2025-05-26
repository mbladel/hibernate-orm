/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.milvius;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.neo4j.Neo4jDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
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
			final TestEntity testEntity = new TestEntity();
			testEntity.id = 1L;
			testEntity.name = "test_1";
			session.persist( testEntity );
		} );

		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1L );
			testEntity.name = "test_1_updated";
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "TestEntity")
	static class TestEntity {
		@Id
		private Long id;

		@Column(name = "name_property", nullable = false)
		private String name;

		private LocalTime localTime;

		private LocalDateTime localDateTime;

		// todo neo4j : test more types (also translate arrays into native lists ?)
	}
}
