/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.idgen.identity;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = IdentityGetGeneratedDisabledTest.Flour.class )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.USE_GET_GENERATED_KEYS, value = "false" ) )
@RequiresDialectFeature( DialectChecks.SupportsIdentityColumns.class )
public class IdentityGetGeneratedDisabledTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Flour( "00" ) );
			// todo marco : sybase does not support output
			//  nor returning, found no way of making it work!
		} );
	}

	@Entity( name = "Flour" )
	public static class Flour {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Integer id;

		private String name;

		public Flour() {
		}

		public Flour(String name) {
			this.name = name;
		}
	}
}
