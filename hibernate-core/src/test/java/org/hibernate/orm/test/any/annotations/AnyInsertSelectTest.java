/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.any.annotations;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.type.descriptor.java.LongJavaType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		AnyInsertSelectTest.DocumentEntity.class,
		AnyInsertSelectTest.DocClientEntity.class,
} )
@SessionFactory
public class AnyInsertSelectTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final DocClientEntity docClientEntity = new DocClientEntity( 1L, "test_client" );
			session.persist( docClientEntity );
			final DocumentEntity documentEntity = new DocumentEntity( "test_doc" );
			documentEntity.setParent( docClientEntity );
			session.persist( documentEntity );

		} );
	}

	@Test
	public void testInsertSelect(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final int count = session.createMutationQuery(
					"insert into DocumentEntity(name, parent) select name, parent from DocumentEntity"
			).executeUpdate();
			assertThat( count ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testTypeSelect(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Object result = session.createQuery(
					"select type(d.parent) from DocumentEntity d",
					Object.class
			).getSingleResult();
			assertThat( result ).isNotNull();
		} );
	}

	public interface IDocumentEntity {
		Long getId();

		String getName();
	}

	@Entity( name = "DocumentEntity" )
	public static class DocumentEntity implements IDocumentEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Any( fetch = FetchType.LAZY )
		@JoinColumn( name = "parent_id" )
		@Column( name = "parent_type" )
		@AnyKeyJavaType( value = LongJavaType.class )
		@AnyDiscriminatorValue( discriminator = "document", entity = DocumentEntity.class )
		@AnyDiscriminatorValue( discriminator = "doc_client", entity = DocClientEntity.class )
		private IDocumentEntity parent;

		public DocumentEntity() {
		}

		public DocumentEntity(String name) {
			this.name = name;
		}

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public String getName() {
			return name;
		}

		public IDocumentEntity getParent() {
			return parent;
		}

		public void setParent(IDocumentEntity parent) {
			this.parent = parent;
		}
	}

	@Entity( name = "DocClientEntity" )
	public static class DocClientEntity implements IDocumentEntity {
		@Id
		private Long id;

		private String name;

		public DocClientEntity() {
		}

		public DocClientEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public String getName() {
			return name;
		}
	}
}
