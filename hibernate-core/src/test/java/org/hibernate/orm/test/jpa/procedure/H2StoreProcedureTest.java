/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.procedure;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 * @author Andrea Boriero
 */
@Jpa(annotatedClasses = H2StoreProcedureTest.MyEntity.class)
@RequiresDialect(H2Dialect.class)
public class H2StoreProcedureTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createNativeQuery(
									"CREATE ALIAS get_all_entities FOR \"" + H2StoreProcedureTest.class.getCanonicalName() + ".getAllEntities\";" )
							.executeUpdate();
					entityManager.createNativeQuery(
									"CREATE ALIAS by_id FOR \"" + H2StoreProcedureTest.class.getCanonicalName() + ".entityById\";" )
							.executeUpdate();
					entityManager.createNativeQuery(
									"CREATE ALIAS invalid_proc FOR \"" + H2StoreProcedureTest.class.getCanonicalName() + ".invalidProcedure\";" )
							.executeUpdate();
					MyEntity entity = new MyEntity();
					entity.id = 1;
					entity.name = "entity1";
					entityManager.persist( entity );
				}
		);
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createNativeQuery( "DROP ALIAS IF EXISTS get_all_entities" ).executeUpdate();
					entityManager.createNativeQuery( "DROP ALIAS IF EXISTS by_id" ).executeUpdate();
				}
		);
	}

	public static ResultSet getAllEntities(Connection conn) throws SQLException {
		return conn.createStatement().executeQuery( "select * from MY_ENTITY" );
	}

	public static ResultSet entityById(Connection conn, long id) throws SQLException {
		return conn.createStatement().executeQuery( "select * from MY_ENTITY where id = " + Long.toString( id ) );
	}

	public static void invalidProcedure() {
		throw new RuntimeException( "This is a error from h2 stored procedure" );
	}

	@Test
	public void testStoreProcedureGetParameters(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery(
							"get_all_entities",
							MyEntity.class
					);
					final Set<Parameter<?>> parameters = query.getParameters();
					assertThat( parameters ).hasSize( 0 );

					final List resultList = query.getResultList();
					assertThat( resultList ).hasSize( 1 );
				}
		);
	}

	@Test
	public void testStoreProcedureGetParameterByPosition(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "by_Id", MyEntity.class );
					query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );

					query.setParameter( 1, 1L );

					final List resultList = query.getResultList();
					assertThat( resultList ).hasSize( 1 );

					final Set<Parameter<?>> parameters = query.getParameters();
					assertThat( parameters ).hasSize( 1 );

					final Parameter<?> parameter = query.getParameter( 1 );
					assertThat( parameter ).isNotNull();

					try {
						query.getParameter( 2 );
						fail( "IllegalArgumentException expected, parameter at position 2 does not exist" );
					}
					catch (IllegalArgumentException iae) {
						//expected
					}
				}
		);
	}

	@Test
	public void testInvalidStoredProcedure(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			try (final ProcedureCall query = entityManager.unwrap( Session.class )
					.createStoredProcedureQuery( "invalid_proc" )) {
				query.execute();
				fail( "Failure expected" );
			}
			catch (Exception e) {
				assertThat( e ).hasMessageContaining( "This is a error from h2 stored procedure" );
			}
		} );
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity {
		@Id
		long id;

		String name;
	}
}
