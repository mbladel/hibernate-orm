/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.milvius;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = {
		Neo4jSmokeTest.TestEntity.class,
		Neo4jSmokeTest.ChildEntity.class
})
@SessionFactory
@RequiresDialect(value = Neo4jDialect.class)
public class Neo4jSmokeTest {

	@Test
	public void persistAndUpdateTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ChildEntity child = new ChildEntity();
			child.id = "child_1";
			session.persist( child );
			final TestEntity testEntity = new TestEntity();
			testEntity.id = 1L;
			testEntity.name = "test_1";
			testEntity.child = child;
			session.persist( testEntity );
		} );

		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1L );
			testEntity.name = "test_1_updated";
		} );

		scope.inSession( session -> {
			final String result = session.createQuery( "select t.name from TestEntity t where id = ?1", String.class )
					.setParameter( 1, 1L )
					.getSingleResult();
			assertThat( result ).isEqualTo( "test_1_updated" );
		} );
	}

	@Test
	public void persistNoChildAndRemoveTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity testEntity = new TestEntity();
			testEntity.id = 2L;
			testEntity.name = "test_2";
			session.persist( testEntity );
		} );

		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 2L );
			session.remove( testEntity );
		} );

		scope.inSession( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 2L );
			assertThat( testEntity ).isNull();
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

		@ManyToOne
		private ChildEntity child;
	}

	@Entity(name = "ChildEntity")
	static class ChildEntity {
		@Id
		private String id;
	}
}
