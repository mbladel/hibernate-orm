/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.where.annotations;

import java.util.List;

import org.hibernate.annotations.Where;
import org.hibernate.query.MutationQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		ManyToManySubqueryDeleteWhereTest.WhereEntity.class,
		ManyToManySubqueryDeleteWhereTest.WhereUser.class,
		ManyToManySubqueryDeleteWhereTest.RoleEntity.class
} )
public class ManyToManySubqueryDeleteWhereTest {
	@Test
	public void testWhereDeleteWithCondition(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MutationQuery deleteUser = session.createMutationQuery( "DELETE FROM WhereUser WHERE name = :name" );
			deleteUser.setParameter( "name", "Marco" );
			deleteUser.executeUpdate();
		} );
	}

	@Test
	public void testWhereDeleteAll(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MutationQuery deleteUser = session.createMutationQuery( "DELETE FROM WhereEntity" );
			deleteUser.executeUpdate();
		} );

		// todo marco : DO NOT add data, use sql statement assertions
		// todo marco : add @Filter tests in dedicated file
		// todo marco : add to 6.0 migration guide as well?
	}

	@Entity( name = "WhereEntity" )
	@Where( clause = "deleted = false" )
	public static class WhereEntity {
		@Id
		private Long id;
		@Column
		private boolean deleted;
	}

	@Entity( name = "WhereUser" )
	@DiscriminatorValue( "user" )
	public static class WhereUser extends WhereEntity {
		private String name;
		@ManyToMany
		@JoinTable(
				name = "users_roles",
				joinColumns = @JoinColumn( name = "user_id" ),
				inverseJoinColumns = @JoinColumn( name = "role_id" )
		)
		private List<RoleEntity> roles;
	}

	@Entity( name = "RoleEntity" )
	public static class RoleEntity {
		@Id
		@GeneratedValue
		private Long id;
		private String name;
	}
}
