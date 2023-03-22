/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Davide D'Alto
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		MergeWithReferenceTest.Foo.class,
		MergeWithReferenceTest.Bar.class
} )
@SessionFactory
public class MergeWithReferenceTest {
	@Test
	public void testMergeReference(SessionFactoryScope scope) {
		final Bar bar = new Bar( "unique_key" );
		scope.inTransaction( session -> {
			session.persist( bar );
		} );
		scope.inTransaction( session -> {
			final Bar reference = session.getReference( Bar.class, bar.getId() );
			final Foo merged = session.merge( new Foo( reference ) );
			assertThat( merged.getBar().getKey() ).isEqualTo( bar.getKey() );
		} );

		// todo marco : add test with initialized reference ?
	}

	@Entity( name = "Foo" )
	public static class Foo {
		@Id
		@GeneratedValue
		private long id;

		@ManyToOne( cascade = CascadeType.PERSIST, fetch = FetchType.EAGER )
		@Fetch( FetchMode.JOIN )
		@JoinColumn( name = "bar_key", referencedColumnName = "nat_key" )
		private Bar bar;

		public Foo() {
		}

		public Foo(Bar bar) {
			this.bar = bar;
		}

		public long getId() {
			return id;
		}

		public Bar getBar() {
			return bar;
		}

		public void setBar(Bar bar) {
			this.bar = bar;
		}
	}

	@Entity( name = "Bar" )
	public static class Bar {
		@Id
		@GeneratedValue
		private long id;

		@Column( name = "nat_key", unique = true )
		private String key;

		public Bar() {
		}

		public Bar(String key) {
			this.key = key;
		}

		public long getId() {
			return id;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}
	}
}