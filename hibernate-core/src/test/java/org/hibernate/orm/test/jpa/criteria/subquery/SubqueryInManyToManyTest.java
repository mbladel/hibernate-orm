/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.criteria.subquery;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		SubqueryInManyToManyTest.Participant.class,
		SubqueryInManyToManyTest.Submission.class
})
@SessionFactory
@JiraKey("HHH-15802")
public class SubqueryInManyToManyTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Participant p1 = new Participant();
			Participant p2 = new Participant();

			Submission s1 = new Submission();
			s1.getSubmitters().add( p1 );
			s1.getSubmitters().add( p2 );

			Submission s2 = new Submission();
			Participant p3 = new Participant();
			p3.getSubmissions().add( s2 );

			session.persist( p1 );
			session.persist( p2 );
			session.persist( p3 );
			session.persist( s1 );
			session.persist( s2 );
		} );
	}

	@Test
	public void testSubqueryIn(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<Participant> criteria = builder.createQuery( Participant.class );
			Root<Participant> root = criteria.from( Participant.class );
			criteria.select( root );

			Subquery<Participant> subQuery = criteria.subquery( Participant.class );
			Root<Submission> rootSubQuery = subQuery.from( Submission.class );
			subQuery.select( rootSubQuery.join( "submitters" ) );

			criteria.where( root.get( "id" ).in( subQuery ) );
			List<Participant> resultList = session.createQuery( criteria ).getResultList();

			assertEquals( 2, resultList.size() );
		} );
	}

	@Entity(name = "Participant")
	@Table(name = "Participant")
	public static class Participant {
		private int id;
		private Set<Submission> submissions = new HashSet<>();

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@ManyToMany
		public Set<Submission> getSubmissions() {
			return submissions;
		}

		public void setSubmissions(Set<Submission> submissions) {
			this.submissions = submissions;
		}
	}

	@Entity(name = "Submission")
	@Table(name = "Submission")
	public static class Submission {
		private int submissionid;
		private Set<Participant> submitters = new HashSet<>();

		@ManyToMany
		public Set<Participant> getSubmitters() {
			return submitters;
		}

		public void setSubmitters(Set<Participant> submitters) {
			this.submitters = submitters;
		}

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public int getSubmissionid() {
			return submissionid;
		}

		public void setSubmissionid(int submissionid) {
			this.submissionid = submissionid;
		}
	}
}
