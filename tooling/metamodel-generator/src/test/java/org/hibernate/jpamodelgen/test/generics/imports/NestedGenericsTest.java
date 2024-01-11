/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.generics.imports;

import org.hibernate.jpamodelgen.test.generics.imports.a.ChildA;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;

import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Marco Belladelli
 */
public class NestedGenericsTest extends CompilationTest {
	@Test
	@WithClasses( { Parent.class, ChildA.class, ChildB.class } )
	public void testGenerics() {
		assertMetamodelClassGeneratedFor( Parent.class );
		assertMetamodelClassGeneratedFor( ChildA.class );
		assertMetamodelClassGeneratedFor( ChildB.class );
	}
}
