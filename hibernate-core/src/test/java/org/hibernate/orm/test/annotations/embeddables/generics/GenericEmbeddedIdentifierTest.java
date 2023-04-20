/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.embeddables.generics;

import java.io.Serializable;

import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.sqm.SqmPathSource;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		GenericEmbeddedIdentifierTest.EmbeddableKey.class,
		GenericEmbeddedIdentifierTest.AccessReport.class,
		GenericEmbeddedIdentifierTest.Group.class,
		GenericEmbeddedIdentifierTest.User.class,
		GenericEmbeddedIdentifierTest.Report.class,
		GenericEmbeddedIdentifierTest.GroupReport.class,
		GenericEmbeddedIdentifierTest.UserReport.class
} )
public class GenericEmbeddedIdentifierTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// user report
			final Report report1 = new Report( "report_1" );
			session.persist( report1 );
			final User user = new User( "user" );
			session.persist( user );
			final UserReport userReport = new UserReport();
			userReport.setId( new EmbeddableKey<>( user, report1, 1 ) );
			session.persist( userReport );
			// group report
			final Report report2 = new Report( "report_2" );
			session.persist( report2 );
			final Group group = new Group( "group" );
			session.persist( group );
			final GroupReport groupReport = new GroupReport();
			groupReport.setId( new EmbeddableKey<>( group, report2, 2 ) );
			session.persist( groupReport );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from GroupReport" ).executeUpdate();
			session.createMutationQuery( "delete from UserReport" ).executeUpdate();
			session.createMutationQuery( "delete from Group" ).executeUpdate();
			session.createMutationQuery( "delete from User" ).executeUpdate();
			session.createMutationQuery( "delete from Report" ).executeUpdate();
		} );
	}

	@Test
	public void testUserReport(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final UserReport result = session.createQuery(
					"select ur from UserReport ur where ur.id.owner.login = 'user'",
					UserReport.class
			).getSingleResult();
			assertThat( result.getId().getOwner().getLogin() ).isEqualTo( "user" );
			assertThat( result.getId().getEntity().getCode() ).isEqualTo( "report_1" );
		} );

		// todo marco : test with generic embeddable property (non-id)
	}

	@Test
	public void testUserReportCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<UserReport> query = cb.createQuery( UserReport.class );
			final Root<UserReport> root = query.from( UserReport.class );
			final Path<Object> id = root.get( "id" );
			assertThat( id.getJavaType() ).isEqualTo( EmbeddableKey.class );
			// assert that the generic attributes inside the component's model are reported as Object type
			final SqmPathSource<?> modelPathSource = (SqmPathSource<?>) id.getModel();
			assertThat( modelPathSource.findSubPathSource( "entity" ).getBindableJavaType() ).isEqualTo( Object.class );
			assertThat( modelPathSource.findSubPathSource( "owner" ).getBindableJavaType() ).isEqualTo( Object.class );
			// the serial property is not generic, so it should have the correct type even in the generic component's model
			assertThat( modelPathSource.findSubPathSource( "serial" )
								.getBindableJavaType() ).isEqualTo( Integer.class );
			// assert that the concrete attributes inside the component's resolved model are reported correctly
			final SqmPathSource<?> resolvedPathSource = ( (JpaPath<Object>) id ).getResolvedModel();
			assertThat( resolvedPathSource.findSubPathSource( "entity" )
								.getBindableJavaType() ).isEqualTo( Report.class );
			assertThat( resolvedPathSource.findSubPathSource( "owner" )
								.getBindableJavaType() ).isEqualTo( User.class );
			assertThat( resolvedPathSource.findSubPathSource( "serial" )
								.getBindableJavaType() ).isEqualTo( Integer.class );
			// test same query as HQL
			query.select( root ).where( cb.equal( id.get( "owner" ).get( "login" ), "user" ) );
			final UserReport result = session.createQuery( query ).getSingleResult();
			assertThat( result.getId().getOwner().getLogin() ).isEqualTo( "user" );
			assertThat( result.getId().getEntity().getCode() ).isEqualTo( "report_1" );
		} );
	}

	@Test
	public void testGroupReport(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final GroupReport result = session.createQuery(
					"select gr from GroupReport gr where gr.id.owner.name = 'group'",
					GroupReport.class
			).getSingleResult();
			assertThat( result.getId().getOwner().getName() ).isEqualTo( "group" );
			assertThat( result.getId().getEntity().getCode() ).isEqualTo( "report_2" );
		} );
	}

	@Test
	public void testGroupReportCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<GroupReport> query = cb.createQuery( GroupReport.class );
			final Root<GroupReport> root = query.from( GroupReport.class );
			final Path<Object> id = root.get( "id" );
			assertThat( id.getJavaType() ).isEqualTo( EmbeddableKey.class );
			// assert that the generic attributes inside the component's model are reported as Object type
			final SqmPathSource<?> modelPathSource = (SqmPathSource<?>) id.getModel();
			assertThat( modelPathSource.findSubPathSource( "entity" ).getBindableJavaType() ).isEqualTo( Object.class );
			assertThat( modelPathSource.findSubPathSource( "owner" ).getBindableJavaType() ).isEqualTo( Object.class );
			// the serial property is not generic, so it should have the correct type even in the generic component's model
			assertThat( modelPathSource.findSubPathSource( "serial" )
								.getBindableJavaType() ).isEqualTo( Integer.class );
			// assert that the concrete attributes inside the component's resolved model are reported correctly
			final SqmPathSource<?> resolvedPathSource = ( (JpaPath<Object>) id ).getResolvedModel();
			assertThat( resolvedPathSource.findSubPathSource( "entity" )
								.getBindableJavaType() ).isEqualTo( Report.class );
			assertThat( resolvedPathSource.findSubPathSource( "owner" )
								.getBindableJavaType() ).isEqualTo( Group.class );
			assertThat( resolvedPathSource.findSubPathSource( "serial" )
								.getBindableJavaType() ).isEqualTo( Integer.class );
			// test same query as HQL
			query.select( root ).where( cb.equal( id.get( "owner" ).get( "name" ), "group" ) );
			final GroupReport result = session.createQuery( query ).getSingleResult();
			assertThat( result.getId().getOwner().getName() ).isEqualTo( "group" );
			assertThat( result.getId().getEntity().getCode() ).isEqualTo( "report_2" );
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
		private String code;

		public Report() {
		}

		public Report(String code) {
			this.code = code;
		}

		@Override
		@Id
		@GeneratedValue
		public Long getId() {
			return super.getId();
		}

		public void setId(Long id) {
			super.setId( id );
		}

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
		private String login;

		public User() {
		}

		public User(String login) {
			this.login = login;
		}

		@Override
		@Id
		@SequenceGenerator( name = "CS_SEQ", sequenceName = "CS_SEQ" )
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "CS_SEQ" )
		public Long getId() {
			return super.getId();
		}

		public void setId(Long id) {
			super.setId( id );
		}

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
		private String name;

		public Group() {
		}

		public Group(String name) {
			this.name = name;
		}

		@Override
		@Id
		@SequenceGenerator( name = "CS_SEQ", sequenceName = "CS_SEQ" )
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "CS_SEQ" )
		public Long getId() {
			return super.getId();
		}

		public void setId(Long id) {
			super.setId( id );
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class EmbeddableKey<O, E> implements Serializable {
		private O owner;
		private E entity;
		private Integer serial;

		public EmbeddableKey() {
		}

		public EmbeddableKey(O owner, E entity, Integer serial) {
			this.owner = owner;
			this.entity = entity;
			this.serial = serial;
		}

		@ManyToOne
		public O getOwner() {
			return owner;
		}

		public void setOwner(O owner) {
			this.owner = owner;
		}

		@ManyToOne
		public E getEntity() {
			return entity;
		}

		public void setEntity(E entity) {
			this.entity = entity;
		}

		public Integer getSerial() {
			return serial;
		}

		public void setSerial(Integer serial) {
			this.serial = serial;
		}
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
						@JoinColumn( name = "group_id" )
				} ),
				@AssociationOverride( name = "entity", joinColumns = {
						@JoinColumn( name = "report_id" )
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
