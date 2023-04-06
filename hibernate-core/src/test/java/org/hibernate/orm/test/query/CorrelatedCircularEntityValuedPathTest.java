/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = CorrelatedCircularEntityValuedPathTest.EntityA.class )
public class CorrelatedCircularEntityValuedPathTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new EntityA( "entity_a", null ) ) );
		scope.inTransaction( session -> {
			final Query<EntityA> query = session.createQuery(
					"select c from EntityA c where c.name = " +
					"(select e.name from EntityA e where (c.reference is null and e.reference is null))",
					EntityA.class
			);
			final List<EntityA> actual = query.list();
			assertThat( actual ).hasSize( 1 );
		} );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn( name = "reference" )
		private EntityA reference;

		public EntityA() {
		}

		public EntityA(String name, EntityA reference) {
			this.name = name;
			this.reference = reference;
		}
	}
}
