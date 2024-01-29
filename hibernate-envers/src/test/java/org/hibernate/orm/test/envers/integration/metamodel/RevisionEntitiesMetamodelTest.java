/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.envers.integration.metamodel;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.DefaultRevisionEntity_;
import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity_;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity_;
import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity_;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.metamodel.EntityType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;

/**
 * @author Marco Belladelli
 */
public class RevisionEntitiesMetamodelTest {
	// todo marco : add log listener and assertions for it

	@Test
	public void testDefaultRevisionEntity() {
		try (final SessionFactoryImplementor sf = buildSessionFactory( false, true )) {
			inTransaction( sf, session -> {
				assertThat( DefaultRevisionEntity_.class_ ).isNotNull();
				assertThat( DefaultRevisionEntity_.class_ ).isInstanceOf( EntityType.class );
			} );
		}
	}

	@Test
	public void testSequenceIdRevisionEntity() {
		try (final SessionFactoryImplementor sf = buildSessionFactory( false, false )) {
			inTransaction( sf, session -> {
				assertThat( SequenceIdRevisionEntity_.class_ ).isNotNull();
				assertThat( SequenceIdRevisionEntity_.class_ ).isInstanceOf( EntityType.class );
			} );
		}
	}

	@Test
	public void testDefaultTrackingModifiedEntitiesRevisionEntity() {
		try (final SessionFactoryImplementor sf = buildSessionFactory( true, true )) {
			inTransaction( sf, session -> {
				assertThat( DefaultTrackingModifiedEntitiesRevisionEntity_.class_ ).isNotNull();
				assertThat( DefaultTrackingModifiedEntitiesRevisionEntity_.class_ ).isInstanceOf( EntityType.class );
			} );
		}
	}

	@Test
	public void testSequenceIdTrackingModifiedEntitiesRevisionEntity() {
		try (final SessionFactoryImplementor sf = buildSessionFactory( true, false )) {
			inTransaction( sf, session -> {
				assertThat( SequenceIdTrackingModifiedEntitiesRevisionEntity_.class_ ).isNotNull();
				assertThat( SequenceIdTrackingModifiedEntitiesRevisionEntity_.class_ ).isInstanceOf( EntityType.class );
			} );
		}
	}

	@SuppressWarnings( "resource" )
	private static SessionFactoryImplementor buildSessionFactory(boolean trackEntities, boolean nativeId) {
		final StandardServiceRegistryBuilder registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, trackEntities );
		registryBuilder.applySetting( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, nativeId );
		return new MetadataSources( registryBuilder.build() )
				.addAnnotatedClasses( Customer.class )
				.buildMetadata()
				.buildSessionFactory()
				.unwrap( SessionFactoryImplementor.class );
	}

	@Audited
	@Entity( name = "Customer" )
	@SuppressWarnings( "unused" )
	public static class Customer {
		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Temporal( TemporalType.TIMESTAMP )
		@Column( name = "created_on" )
		@CreationTimestamp
		private Date createdOn;
	}
}
