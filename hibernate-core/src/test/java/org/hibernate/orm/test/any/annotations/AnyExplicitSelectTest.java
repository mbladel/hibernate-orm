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
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		AnyExplicitSelectTest.DocumentEntity.class,
		AnyExplicitSelectTest.DocClientEntity.class,
} )
@SessionFactory
public class AnyExplicitSelectTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final DocClientEntity docClientEntity = new DocClientEntity( 1L, "test_client" );
			session.persist( docClientEntity );
			final DocumentEntity documentEntity = new DocumentEntity( 1L, "test_document" );
			documentEntity.setParent( docClientEntity );
			session.persist( documentEntity );
		} );
	}

	@Test
	public void testSelectAny(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final IDocumentEntity result = session.createQuery(
					"select parent from DocumentEntity",
					IDocumentEntity.class
			).getSingleResult();
			assertThat( result ).isInstanceOf( DocClientEntity.class );
			assertThat( result.getName() ).isEqualTo( "test_client" );
		} );
	}

	@Test
	public void testSelectTuple(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Tuple result = session.createQuery(
					"select id, name, parent from DocumentEntity",
					Tuple.class
			).getSingleResult();
			assertThat( result.get( 0, Long.class ) ).isEqualTo( 1L );
			assertThat( result.get( 1, String.class ) ).isEqualTo( "test_document" );
			assertThat( result.get( 2, IDocumentEntity.class ).getName() ).isEqualTo( "test_client" );
		} );
	}

	public interface IDocumentEntity {
		Long getId();

		String getName();
	}

	@Entity( name = "DocumentEntity" )
	public static class DocumentEntity implements IDocumentEntity {
		@Id
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

		public DocumentEntity(Long id, String name) {
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
