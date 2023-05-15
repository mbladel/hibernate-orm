/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.embeddable;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		NestedEmbeddedIdQueryTest.InnerId.class,
		NestedEmbeddedIdQueryTest.OuterId.class,
		NestedEmbeddedIdQueryTest.DomainEntity.class
} )
public class NestedEmbeddedIdQueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new DomainEntity( new OuterId(
				new InnerId( 1L ),
				"id_type_1"
		), "entity_1" ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from DomainEntity" ).executeUpdate() );
	}

	@Test
	public void testIdQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final OuterId result = session.createQuery(
					"select id from DomainEntity",
					OuterId.class
			).getSingleResult();
			assertThat( result.getIdType() ).isEqualTo( "id_type_1" );
			assertThat( result.getInnerId().getId() ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testIdQueryCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<OuterId> cq = cb.createQuery( OuterId.class );
			final Root<DomainEntity> domainEntity = cq.from( DomainEntity.class );
			final OuterId result = session.createQuery(
					cq.select( domainEntity.get( "id" ) )
			).getSingleResult();
			assertThat( result.getIdType() ).isEqualTo( "id_type_1" );
			assertThat( result.getInnerId().getId() ).isEqualTo( 1L );
		} );
	}

	@Embeddable
	public static class InnerId implements Serializable {
		@Basic
		private Long id;

		public InnerId() {
		}

		public InnerId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@Embeddable
	public static class OuterId implements Serializable {
		@Embedded
		private InnerId innerId;

		private String idType;

		public OuterId() {
		}

		public OuterId(InnerId innerId, String idType) {
			this.innerId = innerId;
			this.idType = idType;
		}

		public InnerId getInnerId() {
			return innerId;
		}

		public String getIdType() {
			return idType;
		}
	}

	@Entity( name = "DomainEntity" )
	public static class DomainEntity {
		@EmbeddedId
		private OuterId id;

		private String name;

		public DomainEntity() {
		}

		public DomainEntity(OuterId id, String name) {
			this.id = id;
			this.name = name;
		}

		public OuterId getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
