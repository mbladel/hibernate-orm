/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier.uuid.time2;

import org.hibernate.id.uuid.CustomVersionOneStrategy;
import org.hibernate.orm.test.mapping.identifier.uuid.Helper;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class TimeBasedUuidGenerationTests {
	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = Book.class)
	void verifyModel(ServiceRegistryScope registryScope, DomainModelScope domainModelScope) {
		domainModelScope.withHierarchy( Book.class, (descriptor) -> {
			Helper.verifyAlgorithm( registryScope, domainModelScope, descriptor, CustomVersionOneStrategy.class );
		} );
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = Book.class)
	@SessionFactory
	void testUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new Book() );
		} );
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Book" ).executeUpdate();
		} );
	}

}
