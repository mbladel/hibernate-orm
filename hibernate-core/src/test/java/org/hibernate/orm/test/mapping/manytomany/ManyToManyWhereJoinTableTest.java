/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.manytomany;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.WhereJoinTable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ManyToManyWhereJoinTableTest.Project.class,
		ManyToManyWhereJoinTableTest.User.class,
		ManyToManyWhereJoinTableTest.ProjectUsers.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17105" )
public class ManyToManyWhereJoinTableTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final User user = new User( "user" );
			final Project project1 = new Project( "p1" );
			project1.getManagers().add( user );
			project1.getMembers().add( user );
			final Project project2 = new Project( "p2" );
			project2.getMembers().add( user );
			session.persist( user );
			session.persist( project1 );
			session.persist( project2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Project" ).executeUpdate();
			session.createMutationQuery( "delete from User" ).executeUpdate();
		} );
	}

	@Test
	public void testWhereJoinTableRemove(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final User user = session.find( User.class, "user" );
			assertThat( user.getManagedProjects().stream().map( Project::getName ) ).contains( "p1" );
			assertThat( user.getOtherProjects().stream().map( Project::getName ) ).contains( "p1", "p2" );
			final Project p1 = session.find( Project.class, "p1" );
			p1.getManagers().remove( user );
			assertThat( p1.getMembers().stream().map( User::getName ) ).contains( "user" );
			session.persist( user );
		} );
		scope.inTransaction( session -> {
			final User user = session.find( User.class, "user" );
			assertThat( user.getManagedProjects() ).isEmpty();
			assertThat( user.getOtherProjects().stream().map( Project::getName ) ).contains( "p1", "p2" );
		} );

		// todo marco : add another test which verifyis simple delete query plans (i.e. delete from Project where ... ?)
	}

	@Entity( name = "Project" )
	@Table( name = "t_project" )
	public static class Project {
		@Id
		private String name;

		@ManyToMany
		@JoinTable(
				name = "project_users",
				joinColumns = { @JoinColumn( name = "project_id" ) },
				inverseJoinColumns = { @JoinColumn( name = "user_id" ) }
		)
		@SQLJoinTableRestriction( "role = 'MANAGER'" )
		@SQLRestriction( "deleted = false" )
		@SQLInsert( sql = "insert into project_users (project_id, user_id, role) values (?, ?, 'MANAGER')" )
		private Set<User> managers = new HashSet<>();

		@ManyToMany
		@JoinTable(
				name = "project_users",
				joinColumns = { @JoinColumn( name = "project_id" ) },
				inverseJoinColumns = { @JoinColumn( name = "user_id" ) }
		)
		@SQLJoinTableRestriction( "role = 'MEMBER'" )
		@SQLInsert( sql = "insert into project_users (project_id, user_id, role) values (?, ?, 'MEMBER')" )
		private Set<User> members = new HashSet<>();

		public Project() {
		}

		public Project(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Set<User> getManagers() {
			return managers;
		}

		public Set<User> getMembers() {
			return members;
		}
	}

	@Entity( name = "ProjectUsers" )
	@Table( name = "project_users" )
	public static class ProjectUsers {
		@Id
		@Column( name = "project_id" )
		private String projectId;

		@Id
		@Column( name = "user_id" )
		private String userId;

		@Id
		@Column( name = "role" )
		private String role;
	}

	@Entity( name = "User" )
	@Table( name = "t_user" )
	public static class User {
		@Id
		private String name;

		@ManyToMany( mappedBy = "managers" )
		@SQLJoinTableRestriction( "role = 'MANAGER'" )
		private Set<Project> managedProjects = new HashSet<>();

		@ManyToMany( mappedBy = "members" )
		@SQLJoinTableRestriction( "role = 'MEMBER'" )
		private Set<Project> otherProjects = new HashSet<>();

		private boolean deleted;

		public User() {
		}

		public User(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Set<Project> getManagedProjects() {
			return managedProjects;
		}

		public Set<Project> getOtherProjects() {
			return otherProjects;
		}
	}
}
