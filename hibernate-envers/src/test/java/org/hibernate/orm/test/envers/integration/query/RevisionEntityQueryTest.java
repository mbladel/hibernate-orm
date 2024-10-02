/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
import org.hibernate.orm.test.envers.entities.ids.EmbIdTestEntity;
import org.hibernate.orm.test.envers.entities.ids.MulId;
import org.hibernate.orm.test.envers.entities.ids.MulIdTestEntity;
import org.junit.Test;

import java.util.List;

/**
 * @author Marco Belladelli
 */
public class RevisionEntityQueryTest extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private MulId mulId1;
	private EmbId embId1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrIntTestEntity.class, MulIdTestEntity.class, EmbIdTestEntity.class};
	}

	@Test
	@Priority( 10 )
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
		StrIntTestEntity site2 = new StrIntTestEntity( "a", 10 );
		StrIntTestEntity site3 = new StrIntTestEntity( "b", 5 );

		em.persist( site1 );
		em.persist( site2 );
		em.persist( site3 );

		id1 = site1.getId();
		id2 = site2.getId();
		id3 = site3.getId();

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		mulId1 = new MulId( 1, 2 );
		em.persist( new MulIdTestEntity( mulId1.getId1(), mulId1.getId2(), "data" ) );

		embId1 = new EmbId( 3, 4 );
		em.persist( new EmbIdTestEntity( embId1, "something" ) );

		site1 = em.find( StrIntTestEntity.class, id1 );
		site2 = em.find( StrIntTestEntity.class, id2 );

		site1.setStr1( "aBc" );
		site2.setNumber( 20 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		site3 = em.find( StrIntTestEntity.class, id3 );

		site3.setStr1( "a" );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		site1 = em.find( StrIntTestEntity.class, id1 );

		em.remove( site1 );

		em.getTransaction().commit();
	}

	@Test
	public void testEntitiesIdQuery() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Object> query = criteriaBuilder.createQuery();

		Root<?> from = query.from( SequenceIdRevisionEntity.class );
		query.select( from.get( "id" ) );

		List<Object> resultList = em.createQuery( query ).getResultList();

		List resultList2 = em.createQuery( "select e from " + SequenceIdRevisionEntity.class.getName() + " e" )
				.getResultList();

//		List resultList3 = getAuditReader().createQuery().forRevisionsOfEntity( SequenceIdRevisionEntity.class, true )
//				.add( AuditEntity.revisionNumber().between( 1, 25 ) ).getResultList();

		em.getTransaction().commit();
	}
}
