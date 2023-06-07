package org.hibernate.orm.test.caching;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel( annotatedClasses = {
		CachingAndVersionOneToOneLazyTest.Domain.class,
		CachingAndVersionOneToOneLazyTest.DomainID.class
} )
@SessionFactory
public class CachingAndVersionOneToOneLazyTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Domain domain = new Domain( "domain" );
			final DomainID domainID = new DomainID( "domain_id" );
			domain.setDomainID( domainID );
			session.persist( domain );
			session.persist( domainID );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from DomainID" ).executeUpdate();
			session.createMutationQuery( "delete from Domain" ).executeUpdate();
		} );
	}

	@Test
	public void testSelectDomain(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Domain domain = session.createQuery(
					"select d from Domain d",
					Domain.class
			).getSingleResult();
			assertThat( domain.getData() ).isEqualTo( "domain" );
			assertThat( domain.getDomainID().getData() ).isEqualTo( "domain_id" );
			assertThat( Hibernate.isInitialized( domain.getDomainID().getDomain() ) ).isTrue();
		} );
	}

	@Test
	public void testSelectDomainID(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final DomainID domainId = session.createQuery(
					"select id from DomainID id",
					DomainID.class
			).getSingleResult();
			assertThat( domainId.getData() ).isEqualTo( "domain_id" );
			assertThat( Hibernate.isInitialized( domainId.getDomain() ) ).isFalse();
			assertThat( domainId.getDomain().getData() ).isEqualTo( "domain" );
		} );
	}

	@Entity( name = "Domain" )
	public static class Domain {
		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Integer rowVersion;

		@OneToOne( mappedBy = "domain" )
		private DomainID domainID;

		private String data;

		public Domain() {
		}

		public Domain(String data) {
			this.data = data;
		}

		public Long getId() {
			return id;
		}

		public Integer getRowVersion() {
			return rowVersion;
		}

		public DomainID getDomainID() {
			return domainID;
		}

		public void setDomainID(DomainID domainID) {
			this.domainID = domainID;
			domainID.setDomain( this );
		}

		public String getData() {
			return data;
		}
	}

	@Entity( name = "DomainID" )
	@Cacheable
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	public static class DomainID {
		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Integer rowVersion;

		private String data;

		@OneToOne( fetch = FetchType.LAZY )
		private Domain domain;

		public DomainID() {
		}

		public DomainID(String data) {
			this.data = data;
		}

		public Long getId() {
			return id;
		}

		public Integer getRowVersion() {
			return rowVersion;
		}

		public Domain getDomain() {
			return domain;
		}

		public void setDomain(Domain domain) {
			this.domain = domain;
		}

		public String getData() {
			return data;
		}
	}
}
