/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.batch;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@SessionFactory
@DomainModel( annotatedClasses = {
		BatchFetchAndPreLoadEventTest.EntityA.class,
		BatchFetchAndPreLoadEventTest.EntityB.class
} )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "10" ) )
public class BatchFetchAndPreLoadEventTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.getSessionFactory().getEventEngine().getListenerRegistry().appendListeners(
				EventType.PRE_LOAD,
				new MyPreLoadEventListener()
		);
		scope.inTransaction( session -> {
			session.addEventListeners();
			final EntityA entityA = new EntityA();
			final EntityB entityB = new EntityB();
			entityB.name = "entity_b";
			entityA.child = entityB;
			session.persist( entityB );
			session.persist( entityA );
		} );
		scope.inTransaction( session -> {
			session.createQuery( "from EntityA", EntityB.class ).getSingleResult();
		} );
	}

	public static class MyPreLoadEventListener implements PreLoadEventListener {
		@Override
		public void onPreLoad(PreLoadEvent event) {
			// todo marco : can we allow this ?
			//  in some cases, there will just be a proxy in the Persistence Context
			//  and this will always cause an infinite initialization recursion
			//  so eiter a. we disallow it or b. we find a way to "stop" the recursion
			Hibernate.getClass( event.getEntity() );
		}
	}

	@Entity( name = "EntityA" )
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	public static class EntityA {
		@Id
		@GeneratedValue
		private Long id;

		@OneToOne
		@Fetch( FetchMode.SELECT )
		@JoinColumn( name = "child_id" )
		private EntityB child;
	}

	@Entity( name = "EntityB" )
	public static class EntityB {
		@Id
		@GeneratedValue
		private Long id;

		private String name;
	}
}
