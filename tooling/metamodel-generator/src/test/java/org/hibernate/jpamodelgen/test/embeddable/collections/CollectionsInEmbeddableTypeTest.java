/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpamodelgen.test.embeddable.collections;

import org.hibernate.jpamodelgen.test.embeddable.SimpleEntity;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;

import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertSetAttributeTypeInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.getMetaModelSourceAsString;

/**
 * @author Marco Belladelli
 */
public class CollectionsInEmbeddableTypeTest extends CompilationTest {
	@Test
	@WithClasses( { CollectionsEmbeddable.class, SimpleEntity.class } )
	public void testAnnotatedEmbeddable() {
		System.out.println( getMetaModelSourceAsString( CollectionsEmbeddable.class ) );
		assertMetamodelClassGeneratedFor( CollectionsEmbeddable.class );
		assertSetAttributeTypeInMetaModelFor(
				CollectionsEmbeddable.class,
				"simpleEntities",
				SimpleEntity.class,
				"Wrong type for set attribute."
		);
	}
}
