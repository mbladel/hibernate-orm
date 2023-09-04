/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = CriteriaConvertedAttributeParameterTest.DateDriven.class )
public class CriteriaConvertedAttributeParameterTest {
	private static final String NOW = "now";
	private static final String ACTIVE_START_DATE = "activeStartDate";
	private static final String ACTIVE_END_DATE = "activeEndDate";

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Instant now = Instant.now();
			final DateDriven entity = new DateDriven();
			entity.setActiveEndDate( now.minus( 5, ChronoUnit.DAYS ) );
			entity.setActiveStartDate( now.minus( 5, ChronoUnit.DAYS ) );
			entity.setId( "test" );
			entityManager.persist( entity );
		} );
	}

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<DateDriven> criteria = builder.createQuery( DateDriven.class );
			final Root<DateDriven> root = criteria.from( DateDriven.class );
			criteria.select( root );
			final List<Predicate> predicates = new ArrayList<>();
			final Map<String, Object> params = new HashMap<>();
			addInactiveDateRangePredicate( builder, root, predicates, params );
			criteria.where( predicates.toArray( new Predicate[0] ) );
			final TypedQuery<DateDriven> query = entityManager.createQuery( criteria );
			params.forEach( query::setParameter );
			final List<DateDriven> resultList = query.getResultList();
		} );
	}

	private void addInactiveDateRangePredicate(
			CriteriaBuilder builder,
			Root<DateDriven> root,
			List<Predicate> predicates,
			Map<String, Object> params) {
		params.put( NOW, Instant.now() );
		predicates.add( builder.or(
				builder.greaterThanOrEqualTo(
						root.get( ACTIVE_START_DATE ),
						builder.parameter( Instant.class, NOW )
				),
				builder.and(
						builder.isNotNull( root.get( ACTIVE_END_DATE ) ),
						builder.lessThanOrEqualTo(
								root.get( ACTIVE_END_DATE ),
								builder.parameter( Instant.class, NOW )
						)
				)
		) );
	}

	@Entity( name = "DateDriven" )
//	@Inheritance( strategy = InheritanceType.JOINED )
	public static class DateDriven {
		@Id
		private String id;

		private Instant activeStartDate;

		private Instant activeEndDate;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Instant getActiveStartDate() {
			return activeStartDate;
		}

		public void setActiveStartDate(Instant activeStartDate) {
			this.activeStartDate = activeStartDate;
		}

		public Instant getActiveEndDate() {
			return activeEndDate;
		}

		public void setActiveEndDate(Instant activeEndDate) {
			this.activeEndDate = activeEndDate;
		}
	}

	@Converter( autoApply = true )
	public static class InstantConverter implements AttributeConverter<Instant, Timestamp> {
		@Override
		public Timestamp convertToDatabaseColumn(Instant instant) {
			return instant == null ? null : Timestamp.from( instant );
		}

		@Override
		public Instant convertToEntityAttribute(Timestamp timestamp) {
			return timestamp == null ? null : timestamp.toInstant();
		}

	}
}
