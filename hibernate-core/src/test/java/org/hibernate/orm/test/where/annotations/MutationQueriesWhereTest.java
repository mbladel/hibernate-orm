/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.where.annotations;

import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.orm.test.filter.MutationQueriesFilterTest;
import org.hibernate.query.MutationQuery;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/**
 * Same as {@link MutationQueriesFilterTest},
 * but using {@link SQLRestriction @SQLRestriction}
 *
 * @author Marco Belladelli
 */
@SessionFactory(useCollectingStatementInspector = true)
@DomainModel( annotatedClasses = {
		MutationQueriesWhereTest.BaseEntity.class,
		MutationQueriesWhereTest.UserEntity.class,
		MutationQueriesWhereTest.RoleEntity.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16392" )
public class MutationQueriesWhereTest {
	@Test
	public void testDeleteWithCondition(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final MutationQuery mutationQuery = session.createMutationQuery( "DELETE FROM UserEntity WHERE id = :id" );
			mutationQuery.setParameter( "id", 1L );
			mutationQuery.executeUpdate();
			// assert both discriminators and @Where clause were added to 1st query (collection cleanup) and 2nd query (actual delete)
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "deleted", 1 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "disc_col", 1 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "deleted", 1 );
		} );
	}

	@Test
	public void testDeleteAll(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final MutationQuery mutationQuery = session.createMutationQuery( "DELETE FROM UserEntity" );
			mutationQuery.executeUpdate();
			// assert both discriminators and @Where clause were added to 1st query (collection cleanup) and 2nd query (actual delete)
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "deleted", 1 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "disc_col", 1 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "deleted", 1 );
		} );
	}

	@Test
	public void testUpdateWithCondition(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final MutationQuery mutationQuery = session.createMutationQuery( "UPDATE UserEntity SET name = 'Marco' where id = :id" );
			mutationQuery.setParameter( "id", 1L );
			mutationQuery.executeUpdate();
			// assert both discriminators and @Where clause were added to the update query
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "deleted", 1 );
		} );
	}

	@Test
	public void testUpdateAll(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final MutationQuery mutationQuery = session.createMutationQuery( "UPDATE UserEntity SET name = 'Marco'" );
			mutationQuery.executeUpdate();
			// assert both discriminators and @Where clause were added to the update query
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "deleted", 1 );
		} );
	}

	@Entity( name = "BaseEntity" )
	@DiscriminatorColumn( name = "disc_col" )
	@SQLRestriction( "deleted = false" )
	public static class BaseEntity {
		@Id
		private Long id;
		@Column
		private boolean deleted;
	}

	@Entity( name = "UserEntity" )
	@DiscriminatorValue( "user" )
	public static class UserEntity extends BaseEntity {
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
