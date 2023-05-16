/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.cache.polymorphism;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Exact same test as {@link PolymorphicCacheTest} but with batching enabled.
 *
 * @author Marco Belladelli
 */
@ServiceRegistry( settings = @Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "10" ) )
public class PolymorphicCacheAndBatchingTest extends PolymorphicCacheTest {
}
