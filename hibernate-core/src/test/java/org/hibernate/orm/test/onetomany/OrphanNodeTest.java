/*
 *
 *  * Hibernate, Relational Persistence for Idiomatic Java
 *  *
 *  * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 *  * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 *
 */
package org.hibernate.orm.test.onetomany;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Marco Belladelli
 */
@DomainModel(
		annotatedClasses = OrphanNodeTest.Node.class
)
@SessionFactory
@TestForIssue(jiraKey = "HHH-15734")
public class OrphanNodeTest {

	@Test
	public void testAddNewInBetween(SessionFactoryScope scope) {

		// ()
		scope.inTransaction(
				session -> {
					Node a = node( "a" );
					Node b = node( "b" );
					bind( a, b );
					session.persist( a );
				}
		);

		// (a (b))
		scope.inTransaction(
				session -> {
					Node a = session.find( Node.class, "a" );
					Node b = session.find( Node.class, "b" );

					unbind(a, b);
					Node x = node("x");
					bind(a, x);
					bind(x, b);

					// when added triggers an ObjectDeletedException
					// when saving "x" and cascading to save "b"
					session.persist(x);

					// without persisting x the error is not triggered but b is deleted from db
				}
		);

		// (a (x (b)))
		scope.inTransaction(
				session -> {
					Node a = session.find( Node.class, "a" );
					Node b = session.find( Node.class, "b" );
					Node x = session.find( Node.class, "x" );

					assertNotNull( a );
					assertEquals( 1, a.getChildren().size() );
					assertEquals( "x", a.getChildren().iterator().next().getId() );

					assertNotNull( x );
					assertEquals( 1, x.getChildren().size() );
					assertEquals( "b", x.getChildren().iterator().next().getId() );

					assertNotNull( b );
					assertEquals( 0, b.getChildren().size() );
				}
		);
	}

	private Node node(String id) {
		Node node = new Node();
		node.setId( id );
		node.setParent( null );
		node.setChildren( new HashSet<>() );
		return node;
	}

	private void bind(Node parent, Node child) {
		parent.getChildren().add( child );
		child.setParent( parent );
	}

	private void unbind(Node parent, Node child) {
		parent.getChildren().remove( child );
		child.setParent( null );
	}

	@Entity
	@Table(name = "a_node")
	public static class Node {
		@Id
		private String id;

		@ManyToOne
		private Node parent;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		private Set<Node> children;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Node getParent() {
			return parent;
		}

		public void setParent(Node parent) {
			this.parent = parent;
		}

		public Set<Node> getChildren() {
			return children;
		}

		public void setChildren(Set<Node> children) {
			this.children = children;
		}
	}
}
