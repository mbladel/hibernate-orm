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
package org.hibernate.orm.test.idclass;

import java.io.Serializable;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
public class IdClassInheritanceTest {
	@Test
	public void testRight() {
		try (StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build()) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( RightEntity.class );
			assertDoesNotThrow( () -> metadataSources.buildMetadata() );
		}
	}

	@Test
	public void testWrong() {
		try (StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build()) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( WrongEntity.class );
			final AnnotationException thrown = assertThrows( AnnotationException.class, metadataSources::buildMetadata );
			assertTrue( thrown.getMessage().contains( "'anotherId' which do not match properties of the specified '@IdClass'" ) );
		}
	}

	@Embeddable
	@MappedSuperclass
	public static class ParentPK implements Serializable {
		private Long parentId;
	}

	@Embeddable
	public static class ChildPK extends ParentPK {
		private Long childId;
	}

	@Entity( name = "RightEntity" )
	@IdClass( ChildPK.class )
	public static class RightEntity {
		@Id
		private Long parentId;
		@Id
		private Long childId;
	}

	@Entity( name = "WrongEntity" )
	@IdClass( ChildPK.class )
	public static class WrongEntity {
		@Id
		private Long parentId;
		@Id
		private Long childId;
		@Id
		private Long anotherId;
	}
}
