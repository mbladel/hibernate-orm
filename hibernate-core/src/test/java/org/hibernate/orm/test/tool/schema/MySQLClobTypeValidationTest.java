/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.orm.test.tool.schema;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaValidator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel
@RequiresDialect(MySQLDialect.class)
@JiraKey("HHH-16094")
public class MySQLClobTypeValidationTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( "create table Foo (id int, name mediumtext)" ).executeUpdate();
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( "drop table Foo" ).executeUpdate();
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		final ServiceRegistry serviceRegistry = scope.getMetadataImplementor().getDatabase().getServiceRegistry();
		final Metadata metadata = new MetadataSources( serviceRegistry ).addAnnotatedClass( Foo.class ).buildMetadata();
		new SchemaValidator().validate( metadata, serviceRegistry );
	}

	@Entity(name = "Foo")
	@Table(name = "Foo")
	public static class Foo {
		@Id
		private int id;

		@Lob
		private String name;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}




