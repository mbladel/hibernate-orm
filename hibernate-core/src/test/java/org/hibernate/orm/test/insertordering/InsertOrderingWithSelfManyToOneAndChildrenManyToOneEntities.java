/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.insertordering;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

public class InsertOrderingWithSelfManyToOneAndChildrenManyToOneEntities extends BaseInsertOrderingTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PostComment.class, Post.class };
	}

	@Test
	public void testBatching() {
		sessionFactoryScope().inTransaction( session -> {
			Post parentPost = new Post( "parent_post" );
			PostComment parentPostCommentA = new PostComment( "parent_post_comment_a" );
			parentPostCommentA.post = parentPost;
			PostComment parentPostCommentB = new PostComment( "parent_post_comment_b" );
			parentPostCommentB.post = parentPost;
			Post childPostA = new Post( "child_post_a" );
			childPostA.parent = parentPost;
			Post childPostB = new Post( "child_post_b" );
			childPostB.parent = parentPost;
			PostComment childApostComment = new PostComment( "child_post_a_comment" );
			childApostComment.post = childPostA;
			PostComment childBpostComment = new PostComment( "child_post_b_comment" );
			childBpostComment.post = childPostB;

			session.persist( parentPost );
			session.persist( parentPostCommentA );
			session.persist( parentPostCommentB );

			session.persist( childPostA );
			session.persist( childPostB );

			session.persist( childApostComment );
			session.persist( childBpostComment );

			clearBatches();
		} );
		verifyContainsBatches(
				new Batch( "insert into PostComment (post_id,text,id) values (?,?,?)", 4 ),
				new Batch( "insert into Post (name,parent_id,id) values (?,?,?)", 3 )
		);
	}

	@Entity( name = "PostComment" )
	public static class PostComment {
		@Id
		private String id;
		private String text;
		@ManyToOne
		private Post post;

		public PostComment() {
		}

		public PostComment(String id) {
			this.id = id;
		}
	}

	@Entity( name = "Post" )
	public static class Post {
		@Id
		private String id;
		private String name;
		@ManyToOne
		private Post parent;

		public Post() {
		}

		public Post(String id) {
			this.id = id;
		}
	}
}