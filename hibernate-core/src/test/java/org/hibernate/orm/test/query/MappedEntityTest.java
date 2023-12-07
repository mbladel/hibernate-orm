/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		MappedEntityTest.StandardSalesItem.class,
		MappedEntityTest.ProductSILink.class,
		MappedEntityTest.Selection.class,
		MappedEntityTest.SelectionProductRule.class,
		MappedEntityTest.ProductCollectionNeedAnalysisLink.class

} )
@SessionFactory
public class MappedEntityTest {
	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<String> bars = session.createQuery(
							"select obj1.ident from MappedEntityTest$SelectionProductRule as obj0 join obj0.parent as obj1  " )
					.list();

		} );
	}

	// entities -----------------------------------------------------------

	@MappedSuperclass
	public static abstract class SimpleObject {
		@Id
		protected Long id;
	}

	@MappedSuperclass
	public abstract class CommonObject extends SimpleObject {

	}

	@MappedSuperclass
	public abstract class CommonLinkObject<T extends SimpleObject> extends CommonObject {
		@ManyToOne( fetch = FetchType.EAGER, optional = false )
		@JoinColumn
		private T parent;
	}

	@MappedSuperclass
	public abstract class SeqOrderLinkObject<T extends CommonObject> extends CommonLinkObject<T> {
	}

	@MappedSuperclass
	public abstract class SeqOrderLinkObjectWithUserContext<T extends CommonObject> extends SeqOrderLinkObject<T> {
	}

	@MappedSuperclass
	public abstract class ModelingObject extends CommonObject {
	}

	@MappedSuperclass
	public abstract class GenericLink<T extends ModelingObject> extends SeqOrderLinkObjectWithUserContext<T> {
		private String ident;
	}

	@Entity
	public class StandardSalesItem extends ModelingObject {
	}

	@Entity
	public class ProductSILink extends GenericLink<StandardSalesItem> {
	}


	@Entity
	public class Selection extends ModelingObject {
	}

	@Entity
	public class SelectionProductRule extends SeqOrderLinkObjectWithUserContext<Selection> {
	}


	@Entity
	public class ProductCollectionNeedAnalysisLink extends SeqOrderLinkObjectWithUserContext<Selection> {
		private String ident;
	}
}
