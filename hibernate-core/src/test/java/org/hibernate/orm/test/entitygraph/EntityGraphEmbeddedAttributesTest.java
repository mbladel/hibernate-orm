package org.hibernate.orm.test.entitygraph;

import java.util.Map;
import java.util.UUID;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOAD_GRAPH;

@Jpa( annotatedClasses = {
		EntityGraphEmbeddedAttributesTest.TrackedProduct.class,
		EntityGraphEmbeddedAttributesTest.Tracking.class,
		EntityGraphEmbeddedAttributesTest.UserForTracking.class,
} )
@JiraKey( "HHH-18391" )
public class EntityGraphEmbeddedAttributesTest {
	private static final int TRACKED_PRODUCT_ID = 1;

	@Test
	void testFetchGraph(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final EntityGraph<TrackedProduct> trackedProductGraph = entityManager.createEntityGraph( TrackedProduct.class );
			trackedProductGraph.addSubgraph( "tracking" ).addAttributeNodes( "creator" );
			final TrackedProduct product = entityManager.find(
					TrackedProduct.class,
					TRACKED_PRODUCT_ID,
					Map.of( "javax.persistence.fetchgraph", trackedProductGraph )
			);
			assertThat( Hibernate.isInitialized( product.tracking.creator ) ).isTrue();
			assertThat( Hibernate.isInitialized( product.tracking.modifier ) ).isFalse();
		} );
	}

	@Test
	void testLoadGraph(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final EntityGraph<TrackedProduct> trackedProductGraph = entityManager.createEntityGraph( TrackedProduct.class );
			trackedProductGraph.addSubgraph( "tracking" ).addAttributeNodes( "creator" );
			final TrackedProduct product = entityManager.createQuery(
					"from TrackedProduct",
					TrackedProduct.class
			).setHint( HINT_SPEC_LOAD_GRAPH, trackedProductGraph ).getSingleResult();
			assertThat( Hibernate.isInitialized( product.tracking.creator ) ).isTrue();
			assertThat( Hibernate.isInitialized( product.tracking.modifier ) ).isFalse();
		} );
	}

	@BeforeAll
	void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final UserForTracking creator = new UserForTracking( 1, "foo" );
			entityManager.persist( creator );

			final UserForTracking modifier = new UserForTracking( 2, "bar" );
			entityManager.persist( modifier );

			final Tracking tracking = new Tracking( creator, modifier );

			final TrackedProduct product = new TrackedProduct( TRACKED_PRODUCT_ID, UUID.randomUUID().toString(), tracking );
			entityManager.persist( product );
		} );
	}

	@Entity( name = "TrackedProduct" )
	static class TrackedProduct {
		@Id
		private Integer id;

		@Embedded
		private Tracking tracking;

		private String barcode;

		public TrackedProduct() {
		}

		public TrackedProduct(Integer id, String barcode, Tracking tracking) {
			this.id = id;
			this.barcode = barcode;
			this.tracking = tracking;
		}
	}

	@Embeddable
	static class Tracking {
		@ManyToOne( fetch = FetchType.LAZY )
		private UserForTracking creator;

		@ManyToOne( fetch = FetchType.LAZY )
		private UserForTracking modifier;

		public Tracking() {
		}

		public Tracking(UserForTracking creator, UserForTracking modifier) {
			this.creator = creator;
			this.modifier = modifier;
		}
	}

	@Entity( name = "UserForTracking" )
	static class UserForTracking {
		@Id
		private Integer id;

		private String login;

		public UserForTracking() {
		}

		public UserForTracking(Integer id, String login) {
			this.id = id;
			this.login = login;
		}
	}
}
