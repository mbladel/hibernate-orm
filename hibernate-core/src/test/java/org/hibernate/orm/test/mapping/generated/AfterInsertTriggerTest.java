/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Generated;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.generator.EventType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		AfterInsertTriggerTest.TestEntity.class
})
@SessionFactory
@RequiresDialect(OracleDialect.class)
public class AfterInsertTriggerTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity testEntity = new TestEntity( 1 );
			session.persist( testEntity );
			session.flush();
			assertThat( testEntity.getAfterInsert() ).isEqualTo( "after_insert" );
		} );
		scope.inSession( session -> assertThat( session.find( TestEntity.class, 1 ).getAfterInsert() ).isEqualTo(
				"after_insert" ) );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( """
					CREATE OR REPLACE EDITIONABLE TRIGGER AFTER_INSERT_TRIGGER
						FOR INSERT OR UPDATE OR DELETE
						ON TestEntity
						COMPOUND TRIGGER
						TYPE id_t IS TABLE OF NUMBER;
						g_ids id_t \\:= id_t();
					BEFORE EACH ROW IS
					BEGIN
						IF INSERTING THEN
							\\:NEW.afterInsert \\:= 'inserted';
							g_ids.EXTEND;
							g_ids(g_ids.COUNT) \\:= \\:NEW.id;
						END IF;
					END BEFORE EACH ROW;
						AFTER STATEMENT IS
						BEGIN
							FORALL i IN 1 .. g_ids.COUNT
								UPDATE TestEntity
								SET afterInsert = 'after_insert'
								WHERE id = g_ids(i);
						END AFTER STATEMENT;
						END after_insert_trigger;
					""" ).executeUpdate();
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}


	@Entity(name = "TestEntity")
	static class TestEntity {
		@Id
		private Integer id;

		@Generated(event = {EventType.INSERT, EventType.UPDATE})
		@Column(updatable = false)
		private String afterInsert;

		public TestEntity() {
		}

		public TestEntity(Integer id) {
			this.id = id;
		}

		public String getAfterInsert() {
			return afterInsert;
		}
	}
}
