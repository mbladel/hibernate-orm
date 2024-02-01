/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.collections;

import java.sql.Types;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
public class BasicTypeCollectionTest {
	@Test
	public void test() {
		try (final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( EntityA.class )
					.addAnnotatedClass( EntityB.class )
					.getMetadataBuilder()
					.build();
			final PersistentClass entityABinding = metadata.getEntityBinding( EntityA.class.getName() );
			final Property entityAData = entityABinding.getProperty( "data" );
			assertThat( ( (JdbcMapping) entityAData.getType() ).getJdbcType()
								.getDdlTypeCode() ).isEqualTo( Types.ARRAY );

			final PersistentClass entityBBinding = metadata.getEntityBinding( EntityB.class.getName() );
			final Property entityBData = entityBBinding.getProperty( "data" );
			assertThat( ( (JdbcMapping) entityBData.getType() ).getJdbcType()
								.getDdlTypeCode() ).isEqualTo( SqlTypes.JSON );
		}
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		@GeneratedValue
		private Long id;

		//		@Basic
		private List<String> data;
	}

	@Entity( name = "EntityB" )
	public static class EntityB {
		@Id
		@GeneratedValue
		private Long id;

		@JdbcTypeCode( SqlTypes.JSON )
		private List<String> data;
	}
}
