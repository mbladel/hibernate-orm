/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.embeddable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * This test verifies that when a map-based {@link ElementCollection} of {@link Embeddable} objects
 * are audited that the same number of audit rows are generated regardless whether the embeddable
 * implements proper {@code equals} and {@code hashCode} methods.
 *
 * The {@link ValidityAuditStrategy} with equals/hashcode.
 *
 * +-----+---------+---------------+-----------+--------+--------+
 * | REV | REVTYPE | TESTENTITY_ID | EMBS1_KEY | REVEND | VALUE  |
 * +-----+---------+---------------+-----------+--------+--------+
 * | 1   | 0       | 1             | a         | 2      | value1 |
 * | 1   | 0       | 1             | b         | null   | value2 |
 * | 2   | 0       | 1             | a         | null   | value3 |
 * | 2   | 2       | 1             | a         | null   | value1 |
 * +-----+---------+---------------+-----------+--------+--------+
 *
 * The {@link org.hibernate.envers.strategy.DefaultAuditStrategy} with equals/hashcode.
 *
 * +-----+---------+---------------+-----------+--------+
 * | REV | REVTYPE | TESTENTITY_ID | EMBS1_KEY | VALUE  |
 * +-----+---------+---------------+-----------+--------+
 * | 1   | 0       | 1             | a         | value1 |
 * | 1   | 0       | 1             | b         | value2 |
 * | 2   | 0       | 1             | a         | value3 |
 * | 2   | 2       | 1             | a         | value1 |
 * +-----+---------+---------------+-----------+--------+
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12607")
public class MapNoEqualsHashCodeTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			TestEntity e = new TestEntity( 1 );
			e.setEmbs1( new HashMap<>() );
			e.getEmbs1().put( "a", new Emb( "value1" ) );
			e.getEmbs1().put( "b", new Emb( "value2" ) );
			entityManager.persist( e );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			TestEntity e = entityManager.find( TestEntity.class, 1 );
			e.getEmbs1().put( "a", new Emb( "value3" ) );
		} );
	}

	@Test
	public void testAuditRowsForValidityAuditStrategy() {
		if ( ValidityAuditStrategy.class.getName().equals( getAuditStrategy() ) ) {
			doInJPA( this::entityManagerFactory, entityManager -> {
				Long results = entityManager
						.createQuery(
								"SELECT COUNT(1) FROM TestEntity_embs1_AUD WHERE REVEND IS NULL",
								Long.class
						)
						.getSingleResult();

				assertNotNull( results );
				assertEquals( Long.valueOf( 3 ), results );
			} );

			doInJPA( this::entityManagerFactory, entityManager -> {
				Long results = entityManager
						.createQuery(
								"SELECT COUNT(1) FROM TestEntity_embs1_AUD",
								Long.class
						)
						.getSingleResult();

				assertNotNull( results );
				assertEquals( Long.valueOf( 4 ), results );
			} );
		}
	}

	@Test
	public void testAuditRowsForDefaultAuditStrategy() {
		if ( !ValidityAuditStrategy.class.getName().equals( getAuditStrategy() ) ) {
			doInJPA( this::entityManagerFactory, entityManager -> {
				Long results = entityManager
						.createQuery(
								"SELECT COUNT(1) FROM TestEntity_embs1_AUD",
								Long.class
						)
						.getSingleResult();

				assertNotNull( results );
				assertEquals( Long.valueOf( 4 ), results );
			} );
		}
	}

	@Test
	public void testRevisionHistory1() {
		TestEntity e = getAuditReader().find( TestEntity.class, 1, 1 );
		assertEquals( 2, e.getEmbs1().size() );
		assertEquals( "value1", e.getEmbs1().get( "a" ).getValue() );
		assertEquals( "value2", e.getEmbs1().get( "b" ).getValue() );
	}

	@Test
	public void testRevisionHistory2() {
		TestEntity e = getAuditReader().find( TestEntity.class, 1, 2 );
		assertEquals( 2, e.getEmbs1().size() );
		assertEquals( "value3", e.getEmbs1().get( "a" ).getValue() );
		assertEquals( "value2", e.getEmbs1().get( "b" ).getValue() );
	}


	@Entity(name = "TestEntity")
	@Audited
	public static class TestEntity {
		@Id
		private Integer id;

		@ElementCollection
		private Map<String, Emb> embs1;

		public TestEntity() {

		}

		public TestEntity(Integer id) {
			this.id = id;
		}

		public Map<String, Emb> getEmbs1() {
			return embs1;
		}

		public void setEmbs1(Map<String, Emb> embs1) {
			this.embs1 = embs1;
		}
	}

	@Embeddable
	public static class Emb implements Serializable {
		@Column(name = "val")
		private String value;

		public Emb() {

		}

		public Emb(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}
