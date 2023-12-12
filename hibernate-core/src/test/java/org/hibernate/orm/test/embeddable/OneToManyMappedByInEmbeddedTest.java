package org.hibernate.orm.test.embeddable;

import java.io.Serializable;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel( annotatedClasses = {
		OneToManyMappedByInEmbeddedTest.EntityA.class,
		OneToManyMappedByInEmbeddedTest.EntityC.class,
} )
@SessionFactory
public class OneToManyMappedByInEmbeddedTest {
	@BeforeAll
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = new EntityA( 1 );

			final EmbeddedValueInA embeddedValueInA = new EmbeddedValueInA();

			final EntityC entityC = new EntityC();
			entityC.setId( 1 );
			entityC.setName( "testName" );

			final EntityC entityC1 = new EntityC();
			entityC1.setName( "testName1" );
			entityC1.setId( 2 );

			embeddedValueInA.setEntityCList( List.of( entityC, entityC1 ) );
			entityA.setEmbedded( embeddedValueInA );

			session.persist( entityA );
		} );
	}

	@Test
	public void testEmbeddableWithOneToManyLoadBefore(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = session.get( EntityA.class, 1 );
			assertThat( entityA ).isNotNull();
			final Object entityA1 = session.createQuery( "select a.embedded from EntityA a where a.id = 1" )
					.getSingleResult();
			assertThat( entityA ).isNotNull();
		} );
	}

	@Test
	public void testEmbeddableWithoutOneToMany(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Object entityB = session.createQuery( "select b.embedded from EntityB b where b.id = 1" )
					.getSingleResult();
			assertThat( entityB ).isNotNull();
		} );
	}

	@Test
	public void testEmbeddableWithOneToMany(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Object entityA = session.createQuery( "select a.embedded from EntityA a where a.id = 1" )
					.getSingleResult();
			assertThat( entityA ).isNotNull();
		} );
	}


	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		private Integer id;

		@Embedded
		private EmbeddedValueInA embedded = new EmbeddedValueInA();

		public EntityA() {
		}

		private EntityA(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public EmbeddedValueInA getEmbedded() {
			return embedded;
		}

		public void setEmbedded(EmbeddedValueInA embedded) {
			this.embedded = embedded;
		}
	}

	@Embeddable
	public static class EmbeddedValueInA implements Serializable {
		private String testString;

		@OneToMany( cascade = CascadeType.ALL, orphanRemoval = true )
		@JoinColumn( name = "entityA_id" )
		@OrderBy( "id" )
		private List<EntityC> entityCList;

		public List<EntityC> getEntityCList() {
			return entityCList;
		}

		public void setEntityCList(List<EntityC> entityCList) {
			this.entityCList = entityCList;
		}

		public String getTestString() {
			return testString;
		}

		public void setTestString(String testString) {
			this.testString = testString;
		}
	}

	@Entity( name = "EntityC" )
	public static class EntityC {
		@Id
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
