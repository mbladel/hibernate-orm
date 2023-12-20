/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.foreignkeys.disabled;

import java.util.stream.StreamSupport;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Table;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Yanming Zhou
 */
public class DefaultConstraintModeTest extends BaseUnitTestCase {
	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-14253" )
	public void testForeignKeyShouldNotBeCreated() {
		testForeignKeyCreation( false );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-14253" )
	public void testForeignKeyShouldBeCreated() {
		testForeignKeyCreation( true );
	}

	private void testForeignKeyCreation(boolean created) {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_DEFAULT_CONSTRAINT_MODE, created ? "CONSTRAINT" : "NO_CONSTRAINT" )
				.build()) {
			Metadata metadata = new MetadataSources( ssr ).addAnnotatedClasses( TestEntity.class, ChildEntity.class ).buildMetadata();
			assertThat( findTable( metadata, "TestEntity" ).getForeignKeys().isEmpty(), is( !created ) );
			assertThat( findTable( metadata, "ChildEntity" ).getForeignKeys().isEmpty(), is( !created ) );
		}
	}

	private static Table findTable(Metadata metadata, String tableName) {
		return StreamSupport.stream( metadata.getDatabase().getNamespaces().spliterator(), false )
				.flatMap( namespace -> namespace.getTables().stream() ).filter( t -> t.getName().equals( tableName ) )
				.findFirst().orElse( null );
	}

	@Entity( name = "TestEntity" )
	@jakarta.persistence.Table( name = "TestEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class TestEntity {

		@Id
		private Long id;

		@ManyToOne
		@JoinColumn
		private TestEntity mate;

	}

	@Entity( name = "ChildEntity" )
	@jakarta.persistence.Table( name = "ChildEntity" )
	public static class ChildEntity extends TestEntity {
		private String childName;
	}
}
