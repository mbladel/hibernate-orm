/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.RecordComponentDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.Convert;
import jakarta.persistence.JoinTable;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * @author Emmanuel Bernard

 */
public class ClassPropertyHolder extends AbstractPropertyHolder {
	private final PersistentClass persistentClass;
	private Map<String, Join> joins;
	private transient Map<String, Join> joinsPerRealTableName;
	private EntityBinder entityBinder;
	private final Map<ClassDetails, InheritanceState> inheritanceStatePerClass;

	private final Map<String,AttributeConversionInfo> attributeConversionInfoMap;

	public ClassPropertyHolder(
			PersistentClass persistentClass,
			ClassDetails entityXClass,
			Map<String, Join> joins,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass) {
		super( persistentClass.getEntityName(), null, entityXClass, context );
		this.persistentClass = persistentClass;
		this.joins = joins;
		this.inheritanceStatePerClass = inheritanceStatePerClass;

		this.attributeConversionInfoMap = buildAttributeConversionInfoMap( entityXClass );
	}

	public ClassPropertyHolder(
			PersistentClass persistentClass,
			ClassDetails entityXClass,
			EntityBinder entityBinder,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass) {
		this( persistentClass, entityXClass, entityBinder.getSecondaryTables(), context, inheritanceStatePerClass );
		this.entityBinder = entityBinder;
	}

	@Override
	protected String normalizeCompositePath(String attributeName) {
		return attributeName;
	}

	@Override
	protected String normalizeCompositePathForLogging(String attributeName) {
		return getEntityName() + '.' + attributeName;
	}

	protected Map<String, AttributeConversionInfo> buildAttributeConversionInfoMap(ClassDetails entityClassDetails) {
		final HashMap<String, AttributeConversionInfo> map = new HashMap<>();
		collectAttributeConversionInfo( map, entityClassDetails );
		return map;
	}

	private void collectAttributeConversionInfo(Map<String, AttributeConversionInfo> infoMap, ClassDetails entityClassDetails) {
		if ( entityClassDetails == null ) {
			// typically indicates we have reached the end of the inheritance hierarchy
			return;
		}

		// collect superclass info first
		collectAttributeConversionInfo( infoMap, entityClassDetails.getSuperClass() );

		final boolean canContainConvert = entityClassDetails.hasAnnotationUsage( jakarta.persistence.Entity.class )
				|| entityClassDetails.hasAnnotationUsage( jakarta.persistence.MappedSuperclass.class )
				|| entityClassDetails.hasAnnotationUsage( jakarta.persistence.Embeddable.class );
		if ( ! canContainConvert ) {
			return;
		}

		entityClassDetails.forEachAnnotationUsage( Convert.class, (usage) -> {
			final AttributeConversionInfo info = new AttributeConversionInfo( usage, entityClassDetails );
			if ( isEmpty( info.getAttributeName() ) ) {
				throw new IllegalStateException( "@Convert placed on @Entity/@MappedSuperclass must define attributeName" );
			}
			infoMap.put( info.getAttributeName(), info );
		} );
	}

	@Override
	public void startingProperty(MemberDetails property) {
		if ( property == null ) {
			return;
		}

		final String propertyName = property.getName();
		if ( attributeConversionInfoMap.containsKey( propertyName ) ) {
			return;
		}

		property.forEachAnnotationUsage( Convert.class, (usage) -> {
			final AttributeConversionInfo info = new AttributeConversionInfo( usage, property );
			if ( isEmpty( info.getAttributeName() ) ) {
				attributeConversionInfoMap.put( propertyName, info );
			}
			else {
				attributeConversionInfoMap.put( propertyName + '.' + info.getAttributeName(), info );
			}
		} );
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(MemberDetails attributeMember) {
		return locateAttributeConversionInfo( attributeMember.getName() );
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(String path) {
		return attributeConversionInfoMap.get( path );
	}

	@Override
	public String getEntityName() {
		return persistentClass.getEntityName();
	}

	@Override
	public void addProperty(Property prop, MemberDetails memberDetails, AnnotatedColumns columns, ClassDetails declaringClass) {
		//AnnotatedColumn.checkPropertyConsistency( ); //already called earlier
		if ( columns != null ) {
			if ( columns.isSecondary() ) {
				addPropertyToJoin( prop, memberDetails, declaringClass, columns.getJoin() );
			}
			else {
				addProperty( prop, memberDetails, declaringClass );
			}
		}
		else {
			addProperty( prop, memberDetails,  declaringClass );
		}
	}

	@Override
	public void addProperty(Property prop, MemberDetails memberDetails, ClassDetails declaringClass) {
		if ( prop.getValue() instanceof Component ) {
			//TODO handle quote and non quote table comparison
			String tableName = prop.getValue().getTable().getName();
			if ( getJoinsPerRealTableName().containsKey( tableName ) ) {
				final Join join = getJoinsPerRealTableName().get( tableName );
				addPropertyToJoin( prop, memberDetails, declaringClass, join );
			}
			else {
				addPropertyToPersistentClass( prop, memberDetails, declaringClass );
			}
		}
		else {
			addPropertyToPersistentClass( prop, memberDetails, declaringClass );
		}
	}

	@Override
	public Join addJoin(AnnotationUsage<JoinTable> joinTableAnn, boolean noDelayInPkColumnCreation) {
		final Join join = entityBinder.addJoinTable( joinTableAnn, this, noDelayInPkColumnCreation );
		joins = entityBinder.getSecondaryTables();
		return join;
	}

	@Override
	public Join addJoin(AnnotationUsage<JoinTable> joinTable, Table table, boolean noDelayInPkColumnCreation) {
		final Join join = entityBinder.createJoin(
				this,
				noDelayInPkColumnCreation,
				false,
				joinTable.getList( "joinColumns" ),
				table.getQualifiedTableName(),
				table
		);
		joins = entityBinder.getSecondaryTables();
		return join;
	}

	/**
	 * Embeddable classes can be defined using generics. For this reason, we must check
	 * every property value and specially handle generic components by setting the property
	 * as generic, to later be able to resolve its concrete type, and creating a new component
	 * with correctly typed sub-properties for the metamodel.
	 */
	public static void handleGenericComponentProperty(Property property, MemberDetails memberDetails, MetadataBuildingContext context) {
		final Value value = property.getValue();
		if ( value instanceof Component ) {
			final Component component = (Component) value;
			if ( component.isGeneric() && context.getMetadataCollector()
					.getGenericComponent( component.getComponentClass() ) == null ) {
				// If we didn't already, register the generic component to use it later
				// as the metamodel type for generic embeddable attributes
				final Component copy = component.copy();
				copy.setGeneric( false );
				copy.getProperties().clear();
				for ( Property prop : component.getProperties() ) {
					prepareActualProperty(
							prop,
							null,
							memberDetails.getType().determineRawClass(),
							true,
							context,
							copy::addProperty
					);
				}
				context.getMetadataCollector().registerGenericComponent( copy );
			}
		}
	}

	private void addPropertyToPersistentClass(Property property, MemberDetails memberDetails, ClassDetails declaringClass) {
		handleGenericComponentProperty( property, memberDetails, getContext() );
		if ( declaringClass != null ) {
			final InheritanceState inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState == null ) {
				throw new AssertionFailure(
						"Declaring class is not found in the inheritance state hierarchy: " + declaringClass
				);
			}
			if ( inheritanceState.isEmbeddableSuperclass() ) {
				persistentClass.addMappedSuperclassProperty( property );
				addPropertyToMappedSuperclass( property, memberDetails, declaringClass );
			}
			else {
				persistentClass.addProperty( property );
			}
		}
		else {
			persistentClass.addProperty( property );
		}
	}

	private void addPropertyToMappedSuperclass(Property prop, MemberDetails memberDetails, ClassDetails declaringClass) {
		final MappedSuperclass superclass = getContext().getMetadataCollector().getMappedSuperclass( declaringClass.toJavaClass() );
		prepareActualProperty( prop, memberDetails, true, getContext(), superclass::addDeclaredProperty );
	}

	static void prepareActualProperty(
			Property prop,
			MemberDetails memberDetails,
			boolean allowCollections,
			MetadataBuildingContext context,
			Consumer<Property> propertyConsumer) {
		prepareActualProperty(
				prop,
				memberDetails,
				memberDetails.getDeclaringType(),
				allowCollections,
				context,
				propertyConsumer
		);
	}

	static void prepareActualProperty(
			Property prop,
			MemberDetails memberDetails,
			ClassDetails declaringClass,
			boolean allowCollections,
			MetadataBuildingContext context,
			Consumer<Property> propertyConsumer) {
		if ( declaringClass.getGenericSuperType() == null ) {
			propertyConsumer.accept( prop );
			return;
		}

		final MemberDetails attributeMember;
		if ( memberDetails == null ) {
			attributeMember = getDeclaredAttributeMember(
					prop.getName(),
					declaringClass,
					prop.getPropertyAccessorName()
			);
			if ( attributeMember == null ) {
				return;
			}
		}
		else {
			attributeMember = memberDetails;
		}

		final TypeDetails.Kind kind = attributeMember.getType().getTypeKind();
		if ( kind != TypeDetails.Kind.TYPE_VARIABLE && kind != TypeDetails.Kind.PARAMETERIZED_TYPE ) {
			// Avoid copying when the property doesn't depend on a type variable
			propertyConsumer.accept( prop );
			return;
		}

		// If the property depends on a type variable, we have to copy it and the Value
		final Property actualProperty = prop.copy();
		actualProperty.setGeneric( true );
		actualProperty.setReturnedClassName( attributeMember.getType().getName() );
		final Value value = actualProperty.getValue().copy();
		if ( value instanceof Collection collection ) {
			if ( !allowCollections ) {
				throw new AssertionFailure( "Collections are not allowed as identifier properties" );
			}
			// The owner is a MappedSuperclass which is not a PersistentClass, so set it to null
//						collection.setOwner( null );
			collection.setRole( declaringClass.getName() + "." + prop.getName() );
			// To copy the element and key values, we need to defer setting the type name until the CollectionBinder ran
			final Value originalValue = prop.getValue();
			context.getMetadataCollector().addSecondPass(
					new SecondPass() {
						@Override
						public void doSecondPass(Map persistentClasses) throws MappingException {
							final Collection initializedCollection = (Collection) originalValue;
							final Value element = initializedCollection.getElement().copy();
							setTypeName( element, attributeMember.getElementType().getName() );
							if ( initializedCollection instanceof IndexedCollection ) {
								final Value index = ( (IndexedCollection) initializedCollection ).getIndex().copy();
								if ( attributeMember.getMapKeyType() != null ) {
									setTypeName( index, attributeMember.getMapKeyType().getName() );
								}
								( (IndexedCollection) collection ).setIndex( index );
							}
							collection.setElement( element );
						}
					}
			);
		}
		else {
			setTypeName( value, attributeMember.getType().getName() );
		}

		if ( value instanceof Component component ) {
			final Class<?> componentClass = component.getComponentClass();
			if ( component.isGeneric() ) {
				actualProperty.setValue( context.getMetadataCollector().getGenericComponent( componentClass ) );
			}
			else {
				if ( componentClass == Object.class ) {
					// Object is not a valid component class, but that is what we get when using a type variable
					component.getProperties().clear();
				}
				else {
					final Iterator<Property> propertyIterator = component.getProperties().iterator();
					while ( propertyIterator.hasNext() ) {
						try {
							propertyIterator.next().getGetter( componentClass );
						}
						catch (PropertyNotFoundException e) {
							propertyIterator.remove();
						}
					}
				}
			}
		}
		actualProperty.setValue( value );
		propertyConsumer.accept( actualProperty );
	}

	private static MemberDetails getDeclaredAttributeMember(
			String name,
			ClassDetails classDetails,
			String accessType) {
		ClassDetails superclass = classDetails;
		MemberDetails memberDetails = null;
		while ( superclass != null && memberDetails == null ) {
			memberDetails = getDeclaredAttributeMemberFromClass( name, classDetails, accessType );
			superclass = superclass.getSuperClass();
		}
		return memberDetails;
	}

	public static final String ACCESS_PROPERTY = "property";
	public static final String ACCESS_FIELD = "field";
	public static final String ACCESS_RECORD = "record";

	private static MemberDetails getDeclaredAttributeMemberFromClass(String name, ClassDetails classDetails, String accessType) {
		switch ( accessType ) {
			case ACCESS_FIELD -> {
				for ( FieldDetails field : classDetails.getFields() ) {
					if ( field.isPersistable() && field.resolveAttributeName().equals( name ) ) {
						return field;
					}
				}
			}
			case ACCESS_PROPERTY -> {
				for ( MethodDetails methodDetails : classDetails.getMethods() ) {
					if ( methodDetails.isPersistable() && methodDetails.resolveAttributeName().equals( name ) ) {
						return methodDetails;
					}
				}
			}
			case ACCESS_RECORD -> {
				for ( RecordComponentDetails recordComponent : classDetails.getRecordComponents() ) {
					if ( recordComponent.resolveAttributeName().equals( name ) ) {
						return recordComponent;
					}
				}
			}
			default -> throw new IllegalArgumentException( "Unknown access type " + accessType );
		}
		return null;
	}

	private static void setTypeName(Value value, String typeName) {
		if ( value instanceof ToOne toOne ) {
			toOne.setReferencedEntityName( typeName );
			toOne.setTypeName( typeName );
		}
		else if ( value instanceof Component component ) {
			// Avoid setting type name for generic components
			if ( !component.isGeneric() ) {
				component.setComponentClassName( typeName );
			}
			if ( component.getTypeName() != null ) {
				component.setTypeName( typeName );
			}
		}
		else if ( value instanceof SimpleValue ) {
			( (SimpleValue) value ).setTypeName( typeName );
		}
	}

	private void addPropertyToJoin(Property property, MemberDetails memberDetails, ClassDetails declaringClass, Join join) {
		if ( declaringClass != null ) {
			final InheritanceState inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState == null ) {
				throw new AssertionFailure(
						"Declaring class is not found in the inheritance state hierarchy: " + declaringClass
				);
			}
			if ( inheritanceState.isEmbeddableSuperclass() ) {
				join.addMappedSuperclassProperty( property );
				addPropertyToMappedSuperclass( property, memberDetails, declaringClass );
			}
			else {
				join.addProperty( property );
			}
		}
		else {
			join.addProperty( property );
		}
	}

	/**
	 * Needed for proper compliance with naming strategy, the property table
	 * can be overridden if the properties are part of secondary tables
	 */
	private Map<String, Join> getJoinsPerRealTableName() {
		if ( joinsPerRealTableName == null ) {
			joinsPerRealTableName = CollectionHelper.mapOfSize( joins.size() );
			for (Join join : joins.values()) {
				joinsPerRealTableName.put( join.getTable().getName(), join );
			}
		}
		return joinsPerRealTableName;
	}

	@Override
	public String getClassName() {
		return persistentClass.getClassName();
	}

	@Override
	public String getEntityOwnerClassName() {
		return getClassName();
	}

	@Override
	public Table getTable() {
		return persistentClass.getTable();
	}

	@Override
	public boolean isComponent() {
		return false;
	}

	@Override
	public boolean isEntity() {
		return true;
	}

	@Override
	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	@Override
	public KeyValue getIdentifier() {
		return persistentClass.getIdentifier();
	}

	@Override
	public boolean isOrWithinEmbeddedId() {
		return false;
	}

	@Override
	public boolean isWithinElementCollection() {
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getEntityName() + ")";
	}
}
