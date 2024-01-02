/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.generics;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		GenericMappedSuperclassNestedJoinTest.Selection.class,
		GenericMappedSuperclassNestedJoinTest.SelectionProductRule.class,
		GenericMappedSuperclassNestedJoinTest.SelectionProductRuleProductLink.class,

} )
@SessionFactory
public class GenericMappedSuperclassNestedJoinTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Selection s1 = new Selection();
			s1.id = 1L;
			s1.ident = "s1";
			session.persist( s1 );
			final SelectionProductRule s2 = new SelectionProductRule();
			s2.id = 2L;
			s2.ident = 2;
			s2.parent = s1;
			session.persist( s2 );
			final SelectionProductRuleProductLink s3 = new SelectionProductRuleProductLink();
			s3.id = 3L;
			s3.parent = s2;
			session.persist( s3 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from SelectionProductRuleProductLink" ).executeUpdate();
			session.createMutationQuery( "delete from SelectionProductRule" ).executeUpdate();
			session.createMutationQuery( "delete from Selection" ).executeUpdate();
		} );
	}

	@Test
	public void testSimpleGenericJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Integer> resultList = session.createQuery(
					"select p1.ident from SelectionProductRuleProductLink s join s.parent p1",
					Integer.class
			).getResultList();
			assertThat( resultList ).containsOnly( 2 );
		} );
	}

	@Test
	public void testNestedGenericJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<String> resultList = session.createQuery(
					"select p2.ident from SelectionProductRuleProductLink s join s.parent p1 join p1.parent p2",
					String.class
			).getResultList();
			assertThat( resultList ).containsOnly( "s1" );
		} );
	}

	@MappedSuperclass
	public static abstract class SimpleObject {
		@Id
		protected Long id;
	}

	@MappedSuperclass
	public abstract static class CommonLinkObject<T extends SimpleObject> extends SimpleObject {
		@ManyToOne
		protected T parent;
	}

	@MappedSuperclass
	public abstract static class SeqOrderLinkObject<T extends SimpleObject> extends CommonLinkObject<T> {
	}

	@MappedSuperclass
	public abstract static class SeqOrderLinkObjectWithUserContext<T extends SimpleObject>
			extends SeqOrderLinkObject<T> {
	}

	@Entity( name = "Selection" )
	public static class Selection extends SimpleObject {
		private String ident;
	}

	@Entity( name = "SelectionProductRule" )
	public static class SelectionProductRule extends SeqOrderLinkObjectWithUserContext<Selection> {
		private Integer ident;
	}

	@MappedSuperclass
	public abstract static class AbsProductRuleProductLink<T extends SeqOrderLinkObjectWithUserContext<?>>
			extends SimpleObject {
		@ManyToOne( fetch = FetchType.EAGER, optional = false )
		protected T parent;
	}

	@Entity( name = "SelectionProductRuleProductLink" )
	public static class SelectionProductRuleProductLink extends AbsProductRuleProductLink<SelectionProductRule> {
	}
}