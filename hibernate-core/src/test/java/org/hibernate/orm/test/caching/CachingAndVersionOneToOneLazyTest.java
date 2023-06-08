package org.hibernate.orm.test.caching;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel( annotatedClasses = {
		CachingAndVersionOneToOneLazyTest.Domain.class,
		CachingAndVersionOneToOneLazyTest.DomainID.class
} )
@SessionFactory( generateStatistics = true )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ) )
public class CachingAndVersionOneToOneLazyTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Domain domain = new Domain( 1L, "domain" );
			final DomainID domainID = new DomainID( 2L, "domain_id" );
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
		scope.getSessionFactory().getCache().evictAllRegions();
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

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
		scope.getSessionFactory().getCache().evictAllRegions();
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// ensure that Domain is put into cache
		scope.inTransaction( session -> {
			session.find( Domain.class, 1L );
			assertThat( statistics.getSecondLevelCachePutCount() ).isEqualTo( 2 );
		} );

		scope.inTransaction( session -> {
			final DomainID domainId = session.createQuery(
					"select id from DomainID id",
					DomainID.class
			).getSingleResult();
			assertThat( domainId.getData() ).isEqualTo( "domain_id" );
			// Since the Domain was found in cache we expect it to be initialized
			assertThat( Hibernate.isInitialized( domainId.getDomain() ) ).isTrue();
			assertThat( domainId.getDomain().getData() ).isEqualTo( "domain" );
			assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 1 ); // found Domain in cache
			assertThat( statistics.getSecondLevelCachePutCount() ).isEqualTo( 2 ); // unchanged
		} );
	}

	@Entity( name = "Domain" )
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	public static class Domain {
		@Id
		private Long id;

		@Version
		private Integer rowVersion;

		@OneToOne( mappedBy = "domain" )
		private DomainID domainID;

		private String data;

		public Domain() {
		}

		public Domain(Long id, String data) {
			this.id = id;
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
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	public static class DomainID {
		@Id
		private Long id;

		@Version
		private Integer rowVersion;

		private String data;

		@OneToOne( fetch = FetchType.LAZY )
		private Domain domain;

		public DomainID() {
		}

		public DomainID(Long id, String data) {
			this.id = id;
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
