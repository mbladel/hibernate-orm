/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing;

import java.util.List;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Ignore;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect( PostgreSQLDialect.class )
@Ignore
public class TempSpatialTest extends BaseEntityManagerFunctionalTestCase {

	private final GeometryFactory geometryFactory = new GeometryFactory();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Event.class,
		};
	}

	@Test
	public void test() {
		final Long addressId = doInJPA( this::entityManagerFactory, entityManager -> {
			final Event event = new Event();
			event.setId( 1L );
			final Point point = geometryFactory.createPoint( new Coordinate( 10, 5 ) );
			event.setLocations( List.of( point ).toArray( new Point[0] ) );

			entityManager.persist( event );
			return event.getId();
		} );
	}

	@Entity( name = "Event" )
	public static class Event {

		@Id
		private Long id;

		private Point aSinglePoint;

		private Point[] locations;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Point[] getLocations() {
			return locations;
		}

		public void setLocations(Point[] locations) {
			this.locations = locations;
		}
	}
}
