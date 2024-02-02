/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		MaxResultsAndCollectionFetchTest.Author.class,
		MaxResultsAndCollectionFetchTest.Book.class,
} )
@SessionFactory
public class MaxResultsAndCollectionFetchTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Author author1 = new Author( "Frank Herbert" );
			session.persist( author1 );

			final Book book1 = new Book( "Dune", author1 );
			final Book book2 = new Book( "Dune Messiah", author1 );
			session.persist( book1 );
			session.persist( book2 );

			final Author author2 = new Author( "Henry David Thoreau" );
			session.persist( author2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Book" ).executeUpdate();
			session.createMutationQuery( "delete from Author" ).executeUpdate();
		} );
	}

	@Test
	public void testEntityGraph(SessionFactoryScope scope) {
		// todo marco : test both creteQuery and createSelectionQuery
		// todo marco : test join fetch too

		scope.inTransaction( session -> {
			final EntityGraph<Author> entityGraph = session.createEntityGraph( Author.class );
			entityGraph.addAttributeNodes( "books" );
			final Author author = session
					.createQuery( "from Author a order by a.name", Author.class )
					.setMaxResults( 1 )
					.setEntityGraph( entityGraph, GraphSemantic.LOAD )
					.getSingleResult();
			assertThat( Hibernate.isInitialized( author.getBooks() ) ).isTrue();
			assertThat( author.getBooks() ).hasSize( 2 );
		} );
	}

	@Test
	public void testFetchProfile(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Author author = session
					.createQuery( "from Author a order by a.name", Author.class )
					.setMaxResults( 1 )
					.enableFetchProfile( "all-books-profile" )
					.getSingleResult();
			assertThat( Hibernate.isInitialized( author.getBooks() ) ).isTrue();
			assertThat( author.getBooks() ).hasSize( 2 );
		} );
	}

	@Entity( name = "Author" )
	@FetchProfile(
			name = "all-books-profile",
			fetchOverrides = @FetchProfile.FetchOverride( association = "books", entity = Author.class, fetch = FetchType.EAGER )
	)
	public static class Author {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany( cascade = CascadeType.ALL, mappedBy = "author" )
		private Set<Book> books = new HashSet<>();

		public Author() {
		}

		public Author(String name) {
			this.name = name;
		}

		public Set<Book> getBooks() {
			return books;
		}
	}

	@Entity( name = "Book" )
	public static class Book {
		@Id
		@GeneratedValue
		private Long id;

		private String title;

		@ManyToOne
		private Author author;

		public Book() {
		}

		public Book(String title, Author author) {
			this.title = title;
			this.author = author;
		}
	}
}
