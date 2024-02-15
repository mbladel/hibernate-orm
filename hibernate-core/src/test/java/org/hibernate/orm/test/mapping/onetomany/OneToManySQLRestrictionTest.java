/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.onetomany;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		OneToManySQLRestrictionTest.EntityA.class,
		OneToManySQLRestrictionTest.EntityB.class,
} )
@SessionFactory
public class OneToManySQLRestrictionTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery(
					"select b from EntityB b " +
							"left join b.entityAList1 l1 " +
							"left join b.entityAList2 l2 " +
							"where l1.externalId = 1 or l2.externalId = 1",
					EntityB.class
			).getResultList();
		} );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {

		@Id
		private long id;

		@Column( name = "TO_DATE" )
		private LocalDateTime toDate;

		@Column( name = "FROM_DATE" )
		private LocalDateTime fromDate;

		@Column( name = "EXTERNAL_ID" )
		private int externalId;

		@Column( name = "TYPE" )
		private int type;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "JOIN_COLUMN_ID" )
		public EntityB entityB;
	}

	@Entity( name = "EntityB" )
	public static class EntityB {
		@Id
		private long id;

		@Column( name = "TITLE" )
		private String title;

		@OneToMany( mappedBy = "entityB", fetch = FetchType.LAZY, cascade = CascadeType.ALL )
		@SQLRestriction( "TYPE = 1 AND TO_DATE IS NULL" )
		public List<EntityA> entityAList1;

		@OneToMany( mappedBy = "entityB", fetch = FetchType.LAZY, cascade = CascadeType.ALL )
		@SQLRestriction( "TYPE = 1 AND TO_DATE IS NULL" )
		public List<EntityA> entityAList2;
	}
}
