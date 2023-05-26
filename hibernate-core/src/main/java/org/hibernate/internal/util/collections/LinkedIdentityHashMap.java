/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility {@link IdentityHashMap} implementation that maintains predictable iteration order.
 *
 * @author Marco Belladelli
 */
public class LinkedIdentityHashMap<K, V> implements Map<K, V> {
	private final IdentityHashMap<K, V> identityMap;
	private final LinkedHashSet<K> orderSet;

	public LinkedIdentityHashMap() {
		this.identityMap = new IdentityHashMap<>();
		this.orderSet = new LinkedHashSet<>();
	}

	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		orderSet.add( key );
		return identityMap.computeIfAbsent( key, mappingFunction );
	}

	@Override
	public int size() {
		return identityMap.size();
	}

	@Override
	public boolean isEmpty() {
		return identityMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object o) {
		return identityMap.containsKey( o );
	}

	@Override
	public boolean containsValue(Object o) {
		return identityMap.containsValue( o );
	}

	@Override
	public V get(Object o) {
		return identityMap.get( o );
	}

	@Override
	public V put(K k, V v) {
		orderSet.add( k );
		return identityMap.put( k, v );
	}

	@Override
	public V remove(Object o) {
		orderSet.remove( o );
		return identityMap.remove( o );
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		orderSet.addAll( map.keySet() );
		identityMap.putAll( map );
	}

	@Override
	public void clear() {
		identityMap.clear();
		orderSet.clear();
	}

	@Override
	public Set<K> keySet() {
		return Collections.unmodifiableSet( orderSet );
	}

	@Override
	public Collection<V> values() {
		return Collections.unmodifiableSet(
				(Set<? extends V>) orderSet.stream()
						.map( identityMap::get )
						.collect( Collectors.toCollection( LinkedHashSet::new ) )
		);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return Collections.unmodifiableSet(
				(Set<? extends Entry<K, V>>) orderSet.stream()
						.map( k -> Map.entry( k, identityMap.get( k ) ) )
						.collect( Collectors.toCollection( LinkedHashSet::new ) )
		);
	}
}
