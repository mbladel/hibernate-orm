/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.util.List;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = EnumInListTest.WithEnum.class )
@SessionFactory
public class EnumInListTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new WithEnum() );
		} );
	}

	@Test
	void testHql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery(
					"from WithEnum e where e.stringEnum in :list",
					WithEnum.class
			).setParameter( "list", List.of( Enum.X, Enum.Y ) ).getResultList();
			session.createQuery(
					"from WithEnum e where e.ordinalEnum in :list",
					WithEnum.class
			).setParameter( "list", List.of( Enum.Y, Enum.Z ) ).getResultList();
		} );
	}

	@Test
	void testCriteria(SessionFactoryScope scope) {
		// string
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<WithEnum> cq = cb.createQuery( WithEnum.class );
			final Root<WithEnum> root = cq.from( WithEnum.class );
			cq.where( root.get( "stringEnum" ).in( List.of(Enum.X, Enum.Y) ) );
			session.createQuery( cq ).getResultList();
		} );
		// ordinal
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<WithEnum> cq = cb.createQuery( WithEnum.class );
			final Root<WithEnum> root = cq.from( WithEnum.class );
			cq.where( root.get( "ordinalEnum" ).in( List.of(Enum.Y, Enum.Z) ) );
			session.createQuery( cq ).getResultList();
		} );
	}

	enum Enum {X, Y, Z}

	@Entity( name = "WithEnum" )
	static class WithEnum {
		@Id
		@GeneratedValue
		long id;

		@Enumerated( EnumType.STRING )
		Enum stringEnum = Enum.Y;

		@Enumerated( EnumType.ORDINAL )
		Enum ordinalEnum = Enum.Z;
	}
}
