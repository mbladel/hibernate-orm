/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = {FilterDefLookupTest.AMyEntity.class, FilterDefLookupTest.XEntity.class,})
public class FilterDefLookupTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "x_filter" );
			final List<AMyEntity> resultList = session.createQuery( "from AMyEntity", AMyEntity.class ).getResultList();
			assertThat( resultList ).hasSize( 1 ).element( 0 ).extracting( AMyEntity::getField ).isEqualTo( "Hello" );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new AMyEntity( "test_1" ) );
			session.persist( new AMyEntity( "Hello" ) );
			session.persist( new AMyEntity( "test_2" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "AMyEntity")
	@Filter(name = "x_filter")
	static class AMyEntity {
		@Id
		@GeneratedValue
		private Long id;

		public String field;

		public AMyEntity() {
		}

		public AMyEntity(String field) {
			this.field = field;
		}

		public String getField() {
			return field;
		}
	}

	@Entity(name = "XEntity")
	@FilterDef(name = "x_filter", defaultCondition = "field = 'Hello'")
	static class XEntity {
		@Id
		@GeneratedValue
		private Long id;

		public String field;
	}
}
