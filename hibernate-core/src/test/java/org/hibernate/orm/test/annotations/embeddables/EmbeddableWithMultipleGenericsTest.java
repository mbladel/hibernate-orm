/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.embeddables;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		EmbeddableWithMultipleGenericsTest.GenericObject.class,
		EmbeddableWithMultipleGenericsTest.EmbeddableKey.class,
		EmbeddableWithMultipleGenericsTest.AccessReport.class,
		EmbeddableWithMultipleGenericsTest.Group.class,
		EmbeddableWithMultipleGenericsTest.User.class,
		EmbeddableWithMultipleGenericsTest.Report.class,
		EmbeddableWithMultipleGenericsTest.GroupReport.class,
		EmbeddableWithMultipleGenericsTest.UserReport.class
} )
public class EmbeddableWithMultipleGenericsTest {
	@Test
	public void testUser(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery(
					"select ur from UserReport ur where ur.id.owner.login = 'user_1'",
					UserReport.class
			).getResultList();
		} );
	}

	@Test
	public void testGroup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery(
					"select gr from GroupReport gr where gr.id.owner.name = 'group_1'",
					GroupReport.class
			).getResultList();
		} );
	}

	public static abstract class GenericObject<ID extends Serializable> {
		private ID id;

		public ID getId() {
			return id;
		}

		public void setId(ID id) {
			this.id = id;
		}
	}

	@Entity( name = "Report" )
	@Table( name = "reports" )
	public static class Report extends GenericObject<Long> {
		private Long id;
		private String code;

		@Override
		@Id
		@GeneratedValue
		@Column( name = "ID" )
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Column( name = "code" )
		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}

	@Entity( name = "User" )
	@Table( name = "users" )
	public static class User extends GenericObject<Long> {
		private Long id;
		private String login;

		@Override
		@Id
		@SequenceGenerator( name = "CS_SEQ", sequenceName = "CS_SEQ" )
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "CS_SEQ" )
		@Column( name = "ID" )
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Column( name = "login" )
		public String getLogin() {
			return login;
		}

		public void setLogin(String login) {
			this.login = login;
		}
	}

	@Entity( name = "Group" )
	@Table( name = "groups" )
	public static class Group extends GenericObject<Long> {
		private Long id;
		private String name;

		@Override
		@Id
		@SequenceGenerator( name = "CS_SEQ", sequenceName = "CS_SEQ" )
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "CS_SEQ" )
		@Column( name = "ID" )
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Column( name = "name" )
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class EmbeddableKey<O, E> implements Serializable {
		protected O owner;
		protected E entity;

		@ManyToOne( fetch = FetchType.LAZY, optional = false )
		public O getOwner() {
			return owner;
		}

		public void setOwner(O owner) {
			this.owner = owner;
		}

		@ManyToOne( fetch = FetchType.LAZY, optional = false )
		public E getEntity() {
			return entity;
		}

		public void setEntity(E entity) {
			this.entity = entity;
		}
	}

	public static class UserEmbeddableKey<E> extends EmbeddableKey<User, E> {
	}

	public static class GroupEmbeddableKey<E> extends EmbeddableKey<Group, E> {
	}

	@MappedSuperclass
	public static abstract class AccessReport<O> extends GenericObject<EmbeddableKey<O, Report>> {
	}

	@Entity( name = "UserReport" )
	@Table( name = "user_reports" )
	public static class UserReport extends AccessReport<User> {
		@Override
		@EmbeddedId
		@AssociationOverrides( {
				@AssociationOverride( name = "owner", joinColumns = {
						@JoinColumn( name = "user_id" )
				} ),
				@AssociationOverride( name = "entity", joinColumns = {
						@JoinColumn( name = "report_id" )
				} )
		} )
		public EmbeddableKey<User, Report> getId() {
			return super.getId();
		}

		@Override
		public void setId(EmbeddableKey<User, Report> key) {
			super.setId( key );
		}
	}

	@Entity( name = "GroupReport" )
	@Table( name = "group_reports" )
	public static class GroupReport extends AccessReport<Group> {
		@Override
		@EmbeddedId
		@AssociationOverrides( {
				@AssociationOverride( name = "owner", joinColumns = {
						@JoinColumn( name = "user_group_id", nullable = false, updatable = false, insertable = false )
				} ),
				@AssociationOverride( name = "entity", joinColumns = {
						@JoinColumn( name = "report_id", nullable = false, updatable = false, insertable = false )
				} )
		} )
		public EmbeddableKey<Group, Report> getId() {
			return super.getId();
		}

		@Override
		public void setId(EmbeddableKey<Group, Report> key) {
			super.setId( key );
		}
	}
}
