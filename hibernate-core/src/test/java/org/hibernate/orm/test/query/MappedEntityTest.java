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
			final List<String> bars = session.createQuery(
					"select obj1.ident from SelectionProductRule as obj0 join obj0.parent as obj1",
					String.class
			).getResultList();

		} );
	}


	// entities -----------------------------------------------------------


	@MappedSuperclass
	public static abstract class SimpleObject {

		@Id
		protected Long id;

	}

	@MappedSuperclass
	public abstract static class CommonObject extends SimpleObject {

	}

	@MappedSuperclass
	public abstract static class CommonLinkObject<T extends SimpleObject> extends CommonObject {
		@ManyToOne( fetch = FetchType.EAGER, optional = false )
		@JoinColumn
		private T parent;
	}

	@MappedSuperclass
	public abstract static class SeqOrderLinkObject<T extends CommonObject> extends CommonLinkObject<T> {

	}

	@MappedSuperclass
	public abstract static class SeqOrderLinkObjectWithUserContext<T extends CommonObject>
			extends SeqOrderLinkObject<T> {

	}


	@MappedSuperclass
	public abstract static class ModelingObject extends CommonObject {

	}


	@MappedSuperclass
	public abstract static class GenericLink<T extends ModelingObject> extends SeqOrderLinkObjectWithUserContext<T> {

		private String ident;

	}


	@Entity( name = "StandardSalesItem" )
	public static class StandardSalesItem extends ModelingObject {

	}

	@Entity( name = "ProductSILink" )
	public static class ProductSILink extends GenericLink<StandardSalesItem> {

	}


	@Entity( name = "Selection" )
	public static class Selection extends ModelingObject {


	}

	@Entity( name = "SelectionProductRule" )
	public static class SelectionProductRule extends SeqOrderLinkObjectWithUserContext<Selection> {

	}


	@Entity( name = "ProductCollectionNeedAnalysisLink" )
	public static class ProductCollectionNeedAnalysisLink extends SeqOrderLinkObjectWithUserContext<Selection> {

		private String ident;
	}


}