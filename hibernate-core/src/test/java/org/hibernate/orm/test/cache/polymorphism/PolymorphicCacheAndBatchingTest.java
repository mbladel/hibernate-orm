/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.cache.polymorphism;

import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact same test as {@link PolymorphicCacheTest} but with batching enabled.
 *
 * @author Marco Belladelli
 */
@ServiceRegistry( settings = @Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "10" ) )
public class PolymorphicCacheAndBatchingTest extends PolymorphicCacheTest {
	@Test
	public void testMultiLoad(SessionFactoryScope scope) {
		final CacheImplementor cache = scope.getSessionFactory().getCache();

		assertThat( cache.containsEntity( CachedItem1.class, 1 ) ).isTrue();
		assertThat( cache.containsEntity( CachedItem2.class, 2 ) ).isTrue();

		// test accessing the wrong class by id with a cache-hit
		scope.inTransaction( session -> {
			final List<CachedItem2> resultList = session.byMultipleIds( CachedItem2.class )
					.with( CacheMode.NORMAL )
					.enableSessionCheck( true )
					.multiLoad( 1, 2 );
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.get( 0 ) ).isNull();
			assertThat( resultList.get( 1 ).getName() ).isEqualTo( "name 2" );
		} );

		// test accessing the wrong class by id with no cache-hit
		cache.evictEntityData();
		scope.inTransaction( (session) -> {
			final List<CachedItem2> resultList = session.byMultipleIds( CachedItem2.class )
					.with( CacheMode.NORMAL )
					.enableSessionCheck( true )
					.multiLoad( 1, 2 );
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.get( 0 ) ).isNull();
			assertThat( resultList.get( 1 ).getName() ).isEqualTo( "name 2" );
		} );
	}
}
