/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.MappingException;
import org.hibernate.SharedSessionContract;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.mapping.EmbeddableDiscriminatorConverter;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.DiscriminatorHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.CompositeTypeImplementor;
import org.hibernate.type.spi.TypeConfiguration;


import static org.hibernate.metamodel.RepresentationMode.POJO;

/**
 * Describes a "normal" embeddable.
 *
 * @apiNote At the moment, this class is also used to describe some non-"normal" things:
 *          mainly composite foreign keys.
 */
public class EmbeddableMappingTypeImpl extends AbstractEmbeddableMapping implements SelectableMappings {
	public static EmbeddableMappingTypeImpl from(
			Component bootDescriptor,
			CompositeType compositeType,
			boolean[] insertability,
			boolean[] updateability,
			Function<EmbeddableMappingType, EmbeddableValuedModelPart> embeddedPartBuilder,
			MappingModelCreationProcess creationProcess) {
		return from(
				bootDescriptor,
				compositeType,
				null,
				null,
				null,
				null,
				0,
				insertability,
				updateability,
				embeddedPartBuilder,
				creationProcess
		);
	}

	public static EmbeddableMappingTypeImpl from(
			Component bootDescriptor,
			CompositeType compositeType,
			String rootTableExpression,
			String[] rootTableKeyColumnNames,
			Property componentProperty,
			DependantValue dependantValue,
			int dependantColumnIndex,
			boolean[] insertability,
			boolean[] updateability,
			Function<EmbeddableMappingType,EmbeddableValuedModelPart> embeddedPartBuilder,
			MappingModelCreationProcess creationProcess) {
		final RuntimeModelCreationContext creationContext = creationProcess.getCreationContext();

		final EmbeddableMappingTypeImpl mappingType = new EmbeddableMappingTypeImpl(
				bootDescriptor,
				componentProperty,
				embeddedPartBuilder,
				creationContext
		);

		if ( compositeType instanceof CompositeTypeImplementor ) {
			( (CompositeTypeImplementor) compositeType ).injectMappingModelPart( mappingType.getEmbeddedValueMapping(), creationProcess );
		}

		// todo marco : we definetely need to stop duplicating attribute mappings
		//  e.g. do the same thing as for entities, the attribute is created by
		//  the declaring class' type
		creationProcess.registerInitializationCallback(
				"EmbeddableMappingType(" + mappingType.getNavigableRole().getFullPath() + ")#finishInitialization",
				() -> {
					mappingType.subtypesByClass
							.values()
							.forEach( t -> EmbeddableMappingTypeImpl.finishInitialization(
									(AbstractEmbeddableMapping) t,
									t.getSuperMappingType(),
									t.getEmbeddedValueMapping(),
									t.getRepresentationStrategy(),
									bootDescriptor,
									compositeType,
									rootTableExpression,
									rootTableKeyColumnNames,
									dependantValue,
									dependantColumnIndex,
									insertability,
									updateability,
									creationProcess
							) );
					return true;
				}
		);

		return mappingType;
	}

	private final JavaType<?> embeddableJtd;
	private final EmbeddableRepresentationStrategy representationStrategy;

	private final EmbeddableValuedModelPart valueMapping;
	private final EntityDiscriminatorMapping discriminatorMapping;
	private final Map<Class<?>, EmbeddableMappingType> subtypesByClass;
	private final List<Class<?>> orderedSubclasses;
	private final HashSet<String> declaredAttributes;

	private final boolean createEmptyCompositesEnabled;
	private final SelectableMapping aggregateMapping;
	private final boolean aggregateMappingRequiresColumnWriter;
	private final boolean preferSelectAggregateMapping;
	private final boolean preferBindAggregateMapping;

	private EmbeddableMappingTypeImpl(
			Component bootDescriptor,
			Property componentProperty,
			Function<EmbeddableMappingType, EmbeddableValuedModelPart> embeddedPartBuilder,
			RuntimeModelCreationContext creationContext) {
		super( new MutableAttributeMappingList( 5 ) );
		this.representationStrategy = creationContext
				.getBootstrapContext()
				.getRepresentationStrategySelector()
				.resolveStrategy( bootDescriptor, bootDescriptor.getComponentClass(), () -> this, creationContext );

		this.embeddableJtd = representationStrategy.getMappedJavaType();
		this.valueMapping = embeddedPartBuilder.apply( this );
		if ( bootDescriptor.getDiscriminator() != null ) {
			this.subtypesByClass = new IdentityHashMap<>( bootDescriptor.getDiscriminatorValues().size() );
			this.orderedSubclasses = new ArrayList<>( bootDescriptor.getDiscriminatorValues().size() );
			this.subtypesByClass.put( bootDescriptor.getComponentClass(), this );
			this.orderedSubclasses.add( bootDescriptor.getComponentClass() );
		}
		else {
			this.subtypesByClass = Map.of( bootDescriptor.getComponentClass(), this );
			this.orderedSubclasses = null;
		}
		this.discriminatorMapping = generateDiscriminatorMapping( bootDescriptor, creationContext );
		this.declaredAttributes = new HashSet<>();

		this.createEmptyCompositesEnabled = ConfigurationHelper.getBoolean(
				Environment.CREATE_EMPTY_COMPOSITES_ENABLED,
				creationContext.getServiceRegistry()
						.requireService( ConfigurationService.class )
						.getSettings()
		);
		final AggregateColumn aggregateColumn = bootDescriptor.getAggregateColumn();
		if ( aggregateColumn != null ) {
			final Dialect dialect = creationContext.getDialect();
			final boolean insertable;
			final boolean updatable;
			if ( componentProperty == null ) {
				insertable = true;
				updatable = true;
			}
			else {
				insertable = componentProperty.isInsertable();
				updatable = componentProperty.isUpdateable();
			}
			this.aggregateMapping = SelectableMappingImpl.from(
					bootDescriptor.getOwner().getTable().getQualifiedName( creationContext.getSqlStringGenerationContext() ),
					aggregateColumn,
					bootDescriptor.getParentAggregateColumn() != null
							? bootDescriptor.getParentAggregateColumn().getSelectablePath()
							: null,
					resolveJdbcMapping( bootDescriptor, creationContext ),
					creationContext.getTypeConfiguration(),
					insertable,
					updatable,
					false,
					dialect,
					null,
					creationContext
			);
			final AggregateSupport aggregateSupport = dialect.getAggregateSupport();
			final int sqlTypeCode = aggregateColumn.getSqlTypeCode();
			this.aggregateMappingRequiresColumnWriter = aggregateSupport
					.requiresAggregateCustomWriteExpressionRenderer( sqlTypeCode );
			this.preferSelectAggregateMapping = aggregateSupport.preferSelectAggregateMapping( sqlTypeCode );
			this.preferBindAggregateMapping = aggregateSupport.preferBindAggregateMapping( sqlTypeCode );
		}
		else {
			this.aggregateMapping = null;
			this.aggregateMappingRequiresColumnWriter = false;
			this.preferSelectAggregateMapping = false;
			this.preferBindAggregateMapping = false;
		}
	}

	private JdbcMapping resolveJdbcMapping(Component bootDescriptor, RuntimeModelCreationContext creationContext) {
		// The following is a bit "hacky" because ideally, this should happen in InferredBasicValueResolver#from,
		// but since we don't have access to the EmbeddableMappingType there yet, we do it here.
		// A possible alternative design would be to change AggregateJdbcType#resolveAggregateDescriptor
		// to accept a CompositeType instead of EmbeddableMappingType, and I even tried that,
		// but it doesn't work out unfortunately, because the type would have to be created too early,
		// when the values of the component properties aren't fully initialized yet.
		// Both designs would do this as part of the "finishInitialization" phase,
		// so there is IMO no real win to do it differently
		final TypeConfiguration typeConfiguration = creationContext.getTypeConfiguration();
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final Column aggregateColumn = bootDescriptor.getAggregateColumn();
		final BasicType<?> basicType = basicTypeRegistry.resolve(
				getMappedJavaType(),
				typeConfiguration.getJdbcTypeRegistry().resolveAggregateDescriptor(
						aggregateColumn.getSqlTypeCode(),
						aggregateColumn.getSqlTypeCode() == SqlTypes.STRUCT
								? aggregateColumn.getSqlType( creationContext.getMetadata() )
								: null,
						this,
						creationContext
				)
		);
		// Register the resolved type under its struct name and java class name
		if ( bootDescriptor.getStructName() != null ) {
			basicTypeRegistry.register( basicType, bootDescriptor.getStructName() );
			basicTypeRegistry.register( basicType, getMappedJavaType().getJavaTypeClass().getName() );
		}
		return basicType;
	}

	public EmbeddableMappingTypeImpl(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			EmbeddableMappingType inverseMappingType,
			MappingModelCreationProcess creationProcess) {
		super( new MutableAttributeMappingList( 5 ) );

		this.embeddableJtd = inverseMappingType.getJavaType();
		this.representationStrategy = inverseMappingType.getRepresentationStrategy();
		this.valueMapping = valueMapping;
		// todo marco : do we need to handle inheritance for inverse mappings ?
		this.discriminatorMapping = null;
		this.declaredAttributes = null;
		this.subtypesByClass = null;
		this.orderedSubclasses = null;
		this.createEmptyCompositesEnabled = inverseMappingType.isCreateEmptyCompositesEnabled();
		this.aggregateMapping = null;
		this.aggregateMappingRequiresColumnWriter = false;
		this.preferSelectAggregateMapping = false;
		this.preferBindAggregateMapping = false;
		this.selectableMappings = selectableMappings;
		creationProcess.registerInitializationCallback(
				"EmbeddableMappingType(" + inverseMappingType.getNavigableRole().getFullPath() + ".{inverse})#finishInitialization",
				() -> inverseInitializeCallback(
						declaringTableGroupProducer,
						selectableMappings,
						inverseMappingType,
						creationProcess,
						this,
						attributeMappings
				)
		);
	}

	public EmbeddableMappingType createInverseMappingType(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			MappingModelCreationProcess creationProcess) {
		return new EmbeddableMappingTypeImpl(
				valueMapping,
				declaringTableGroupProducer,
				selectableMappings,
				this,
				creationProcess
		);
	}

	private static void finishInitialization(
			AbstractEmbeddableMapping container,
			EmbeddableMappingType superMappingType,
			EmbeddableValuedModelPart valueMapping,
			EmbeddableRepresentationStrategy representationStrategy,
			Component bootDescriptor,
			CompositeType compositeType,
			String rootTableExpression,
			String[] rootTableKeyColumnNames,
			DependantValue dependantValue,
			int dependantColumnIndex,
			boolean[] insertability,
			boolean[] updateability,
			MappingModelCreationProcess creationProcess) {
// for some reason I cannot get this to work, though only a single test fails - `CompositeElementTest`
//		return finishInitialization(
//				getNavigableRole(),
//				bootDescriptor,
//				compositeType,
//				rootTableExpression,
//				rootTableKeyColumnNames,
//				this,
//				representationStrategy,
//				(name, type) -> {},
//				(column, jdbcEnvironment) -> getTableIdentifierExpression(
//						column.getValue().getTable(),
//						jdbcEnvironment
//				),
//				this::addAttribute,
//				() -> {
//					// We need the attribute mapping types to finish initialization first before we can build the column mappings
//					creationProcess.registerInitializationCallback(
//							"EmbeddableMappingType(" + getEmbeddedValueMapping().getNavigableRole().getFullPath() + ")#initColumnMappings",
//							this::initColumnMappings
//					);
//				},
//				creationProcess
//		);
// todo (6.0) - get this ^^ to work, or drop the comment

		final TypeConfiguration typeConfiguration = creationProcess.getCreationContext().getTypeConfiguration();
		final JdbcServices jdbcServices = creationProcess.getCreationContext().getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		final String baseTableExpression = valueMapping.getContainingTableExpression();
		final Type[] subtypes = compositeType.getSubtypes();

		int attributeIndex = 0;
		int columnPosition = 0;

		// Reset the attribute mappings that were added in previous attempts
		container.attributeMappings.clear();


		if ( superMappingType != null ) {
			for ( ; attributeIndex < superMappingType.getAttributeMappings().size(); attributeIndex++ ) {
				final AttributeMapping attribute = superMappingType.getAttributeMapping( attributeIndex );
				container.addAttribute( attribute );
				container.markDeclaredAttribute( attribute.getAttributeName() );
			}
		}

		for ( ; attributeIndex < bootDescriptor.getProperties().size(); attributeIndex++ ) {
			final Property bootPropertyDescriptor = bootDescriptor.getProperty( attributeIndex );
			if ( !bootDescriptor.getPropertyDeclaringClass( bootPropertyDescriptor )
					.isAssignableFrom( container.getJavaType().getJavaTypeClass() ) ) {
				// All subsequent properties should be defined in subtypes, we can skip them
				break;
			}
			final AttributeMapping attributeMapping;

			final Type subtype = subtypes[attributeIndex];
			final Value value = bootPropertyDescriptor.getValue();
			if ( subtype instanceof BasicType ) {
				final BasicValue basicValue = (BasicValue) value;
				final Selectable selectable = dependantValue != null ?
						dependantValue.getColumns().get( dependantColumnIndex + columnPosition ) :
						basicValue.getColumn();
				final String containingTableExpression;
				final String columnExpression;
				if ( rootTableKeyColumnNames == null ) {
					if ( selectable.isFormula() ) {
						columnExpression = selectable.getTemplate(
								dialect,
								creationProcess.getCreationContext().getTypeConfiguration(),
								creationProcess.getSqmFunctionRegistry()
						);
					}
					else {
						columnExpression = selectable.getText( dialect );
					}
					if ( selectable instanceof Column ) {
						final Column column = (Column) selectable;
						containingTableExpression = MappingModelCreationHelper.getTableIdentifierExpression(
								column.getValue().getTable(),
								creationProcess
						);
					}
					else {
						containingTableExpression = baseTableExpression;
					}
				}
				else {
					containingTableExpression = rootTableExpression;
					columnExpression = rootTableKeyColumnNames[columnPosition];
				}
				final SelectablePath selectablePath;
				final String columnDefinition;
				final Long length;
				final Integer precision;
				final Integer scale;
				final Integer temporalPrecision;
				final boolean isLob;
				final boolean nullable;
				if ( selectable instanceof Column ) {
					final Column column = (Column) selectable;
					columnDefinition = column.getSqlType();
					length = column.getLength();
					precision = column.getPrecision();
					scale = column.getScale();
					temporalPrecision = column.getTemporalPrecision();
					isLob = column.isSqlTypeLob( creationProcess.getCreationContext().getMetadata() );
					nullable = bootPropertyDescriptor.isOptional() && column.isNullable() ;
					selectablePath = basicValue.createSelectablePath( column.getQuotedName( dialect ) );
				}
				else {
					columnDefinition = null;
					length = null;
					precision = null;
					scale = null;
					temporalPrecision = null;
					isLob = false;
					nullable = bootPropertyDescriptor.isOptional();
					selectablePath = new SelectablePath( determineEmbeddablePrefix( container ) + bootPropertyDescriptor.getName() );
				}
				attributeMapping = MappingModelCreationHelper.buildBasicAttributeMapping(
						bootPropertyDescriptor.getName(),
						valueMapping.getNavigableRole().append( bootPropertyDescriptor.getName() ),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						container,
						(BasicType<?>) subtype,
						containingTableExpression,
						columnExpression,
						selectablePath,
						selectable.isFormula(),
						selectable.getCustomReadExpression(),
						selectable.getWriteExpr( ( (BasicType<?>) subtype ).getJdbcMapping(), dialect ),
						columnDefinition,
						length,
						precision,
						scale,
						temporalPrecision,
						isLob,
						nullable,
						insertability[columnPosition],
						updateability[columnPosition],
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
						compositeType.getCascadeStyle( attributeIndex ),
						creationProcess
				);

				columnPosition++;
			}
			else if ( subtype instanceof AnyType ) {
				final Any bootValueMapping = (Any) value;
				final AnyType anyType = (AnyType) subtype;

				final PropertyAccess propertyAccess = representationStrategy.resolvePropertyAccess( bootPropertyDescriptor );
				final boolean nullable = bootValueMapping.isNullable();
				final boolean insertable = insertability[columnPosition];
				final boolean updateable = updateability[columnPosition];
				final boolean includeInOptimisticLocking = bootPropertyDescriptor.isOptimisticLocked();
				final CascadeStyle cascadeStyle = compositeType.getCascadeStyle( attributeIndex );

				SimpleAttributeMetadata attributeMetadataAccess = new SimpleAttributeMetadata(
						propertyAccess,
						getMutabilityPlan( updateable ),
						nullable,
						insertable,
						updateable,
						includeInOptimisticLocking,
						true,
						cascadeStyle
				);

				attributeMapping = new DiscriminatedAssociationAttributeMapping(
						valueMapping.getNavigableRole().append( bootPropertyDescriptor.getName() ),
						typeConfiguration.getJavaTypeRegistry().getDescriptor( Object.class ),
						container,
						attributeIndex,
						attributeIndex,
						attributeMetadataAccess,
						bootPropertyDescriptor.isLazy() ? FetchTiming.DELAYED : FetchTiming.IMMEDIATE,
						propertyAccess,
						bootPropertyDescriptor,
						anyType,
						bootValueMapping,
						creationProcess
				);
			}
			else if ( subtype instanceof CompositeType ) {
				final CompositeType subCompositeType = (CompositeType) subtype;
				final int columnSpan = subCompositeType.getColumnSpan( creationProcess.getCreationContext().getMetadata() );
				final String subTableExpression;
				final String[] subRootTableKeyColumnNames;
				if ( rootTableKeyColumnNames == null ) {
					subTableExpression = baseTableExpression;
					subRootTableKeyColumnNames = null;
				}
				else {
					subTableExpression = rootTableExpression;
					subRootTableKeyColumnNames = new String[columnSpan];
					System.arraycopy( rootTableKeyColumnNames, columnPosition, subRootTableKeyColumnNames, 0, columnSpan );
				}

				attributeMapping = MappingModelCreationHelper.buildEmbeddedAttributeMapping(
						bootPropertyDescriptor.getName(),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						dependantValue,
						dependantColumnIndex + columnPosition,
						container,
						subCompositeType,
						subTableExpression,
						subRootTableKeyColumnNames,
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
						compositeType.getCascadeStyle( attributeIndex ),
						creationProcess
				);

				columnPosition += columnSpan;
			}
			else if ( subtype instanceof CollectionType ) {
				attributeMapping = MappingModelCreationHelper.buildPluralAttributeMapping(
						bootPropertyDescriptor.getName(),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						container,
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
						compositeType.getCascadeStyle( attributeIndex),
						compositeType.getFetchMode( attributeIndex ),
						creationProcess
				);
			}
			else if ( subtype instanceof EntityType ) {
				final EntityPersister entityPersister = creationProcess.getEntityPersister( bootDescriptor.getOwner().getEntityName() );

				attributeMapping = MappingModelCreationHelper.buildSingularAssociationAttributeMapping(
						bootPropertyDescriptor.getName(),
						valueMapping.getNavigableRole().append( bootPropertyDescriptor.getName() ),
						attributeIndex,
						attributeIndex,
						bootPropertyDescriptor,
						container,
						entityPersister,
						(EntityType) subtype,
						representationStrategy.resolvePropertyAccess( bootPropertyDescriptor ),
						compositeType.getCascadeStyle( attributeIndex ),
						creationProcess
				);
				columnPosition += bootPropertyDescriptor.getColumnSpan();
			}
			else {
				throw new MappingException(
						String.format(
								Locale.ROOT,
								"Unable to determine attribute nature : %s#%s",
								bootDescriptor.getOwner().getEntityName(),
								bootPropertyDescriptor.getName()
						)
				);
			}

			container.addAttribute( attributeMapping );
			container.markDeclaredAttribute( attributeMapping.getAttributeName() );
		}

		// We need the attribute mapping types to finish initialization first before we can build the column mappings
		creationProcess.registerInitializationCallback(
				"EmbeddableMappingType(" + valueMapping.getNavigableRole().getFullPath() + ")#initColumnMappings",
				container::initColumnMappings
		);
	}

	private static MutabilityPlan<?> getMutabilityPlan(boolean updateable) {
		if ( updateable ) {
			return new MutabilityPlan<>() {
				@Override
				public boolean isMutable() {
					return true;
				}

				@Override
				public Object deepCopy(Object value) {
					return value;
				}

				@Override
				public Serializable disassemble(Object value, SharedSessionContract session) {
					throw new UnsupportedOperationException();
				}

				@Override
				public Object assemble(Serializable cached, SharedSessionContract session) {
					throw new UnsupportedOperationException();
				}
			};
		}
		else {
			return ImmutableMutabilityPlan.INSTANCE;
		}
	}

	private EntityDiscriminatorMapping generateDiscriminatorMapping(
			Component bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		// todo marco : add discriminator value to boot descriptor (and remove cast)
		final Value discriminator = bootDescriptor.getDiscriminator();
		if ( discriminator == null ) {
			return null;
		}

		final Selectable selectable = discriminator.getSelectables().get( 0 );
		final String discriminatorColumnExpression;
		final String columnDefinition;
		final Long length;
		final Integer precision;
		final Integer scale;
		final boolean isFormula;
		if ( discriminator.hasFormula() ) {
			final Formula formula = (Formula) selectable;
			discriminatorColumnExpression = formula.getTemplate(
					creationContext.getDialect(),
					creationContext.getTypeConfiguration(),
					creationContext.getFunctionRegistry()
			);
			columnDefinition = null;
			length = null;
			precision = null;
			scale = null;
			isFormula = true;
		}
		else {
			final Column column = discriminator.getColumns().get( 0 );
			assert column != null : "Embeddable discriminators require a column";
			discriminatorColumnExpression = column.getReadExpr( creationContext.getDialect() );
			columnDefinition = column.getSqlType();
			length = column.getLength();
			precision = column.getPrecision();
			scale = column.getScale();
			isFormula = false;
		}

		final DiscriminatorType<?> discriminatorType = buildDiscriminatorType(
				bootDescriptor,
				creationContext
		);

		// todo marco : use proper class here
		return new ExplicitColumnDiscriminatorMappingImpl(
				this,
				bootDescriptor.getTable().getName(),
				discriminatorColumnExpression,
				isFormula,
				true,
				columnDefinition,
				length,
				precision,
				scale,
				discriminatorType,
				null //todo marco : this is not used, remove it in embedded discriminator mapping
		);
	}

	private DiscriminatorType<?> buildDiscriminatorType(
			Component bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		final JavaTypeRegistry javaTypeRegistry = creationContext.getSessionFactory().getTypeConfiguration().getJavaTypeRegistry();

		final JavaType<Object> domainJavaType;
		if ( representationStrategy.getMode() == POJO ) {
			domainJavaType = javaTypeRegistry.resolveDescriptor( Class.class );
		}
		else {
			domainJavaType = javaTypeRegistry.resolveDescriptor( String.class );
		}

		final Map<Object, Class<?>> valueMappings = new HashMap<>( bootDescriptor.getDiscriminatorValues().size() );
		for ( Map.Entry<Object, Class<?>> entry : bootDescriptor.getDiscriminatorValues().entrySet() ) {
			final Class<?> embeddableSubclass = entry.getValue();
			valueMappings.put( entry.getKey(), embeddableSubclass );
			// determine the direct supertype embeddable mapping
			final EmbeddableMappingType superType;
			final Class<?> superclass = embeddableSubclass.getSuperclass();
			if ( superclass != null && subtypesByClass.containsKey( superclass ) ) {
				superType = subtypesByClass.get( superclass );
			}
			else {
				superType = this;
			}
			assert superType != null;
			subtypesByClass.put(
					embeddableSubclass,
					new EmbeddableMappingSubTypeImpl(
							bootDescriptor,
							embeddableSubclass,
							superType,
							creationContext
					)
			);
			orderedSubclasses.add( embeddableSubclass );
		}

		final BasicType<?> discriminatorType = DiscriminatorHelper.getDiscriminatorType( bootDescriptor );
		final DiscriminatorConverter<Object, ?> converter = EmbeddableDiscriminatorConverter.fromValueMappings(
				getNavigableRole().append( EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME ),
				domainJavaType,
				discriminatorType,
				valueMappings
		);

		return new DiscriminatorTypeImpl<>( discriminatorType, converter );
	}

	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return valueMapping;
	}

	@Override
	public EntityDiscriminatorMapping getDiscriminatorMapping() {
		return discriminatorMapping;
	}

	@Override
	public EmbeddableMappingType getEmbeddableSubtype(Class<?> embeddableClass) {
		return subtypesByClass == null ? this : subtypesByClass.get( embeddableClass );
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return embeddableJtd;
	}

	public EmbeddableRepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Override
	public String getPartName() {
		return getEmbeddedValueMapping().getPartName();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return valueMapping.getNavigableRole();
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new EmbeddableResultImpl<>(
				navigablePath,
				valueMapping,
				resultVariable,
				creationState
		);
	}

	@Override
	protected void markDeclaredAttribute(String attributeName) {
		declaredAttributes.add( attributeName );
	}

	@Override
	protected Object[] getAttributeValues(Object compositeInstance) {
		final Object[] results = new Object[getNumberOfAttributeMappings()];
		for ( int i = 0; i < getNumberOfAttributeMappings(); i++ ) {
			final AttributeMapping attributeMapping = getAttributeMapping( i );
			if ( declaredAttributes.contains( attributeMapping.getAttributeName() ) ) {
				final Getter getter = attributeMapping.getAttributeMetadata()
						.getPropertyAccess()
						.getGetter();
				results[i] = getter.get( compositeInstance );
			}
			else {
				results[i] = null;
			}
		}
		return results;
	}

	@Override
	protected void setAttributeValues(Object component, Object[] values) {
		for ( int i = 0; i < values.length; i++ ) {
			final AttributeMapping attributeMapping = getAttributeMapping( i );
			if ( declaredAttributes.contains( attributeMapping.getAttributeName() ) ) {
				attributeMapping.getPropertyAccess().getSetter().set( component, values[i] );
			}
			else {
				assert values[i] == null : "Unexpected non-null value for embeddable type " + getJavaType().getJavaTypeClass();
			}
		}
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		final int size = attributeMappings.size();
		int span = 0;
		if ( domainValue instanceof Object[] ) {
			final Object[] values = (Object[]) domainValue;
			assert values.length == size;

			for ( int i = 0; i < size; i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				if ( !attributeMapping.isPluralAttributeMapping() ) {
					final Object attributeValue = values[i];
					span += attributeMapping.breakDownJdbcValues(
							attributeValue,
							offset + span,
							x,
							y,
							valueConsumer,
							session
					);
				}
			}
		}
		else {
			for ( int i = 0; i < size; i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				if ( !attributeMapping.isPluralAttributeMapping() ) {
					final Object attributeValue = domainValue == null
							? null
							: attributeMapping.getPropertyAccess().getGetter().get( domainValue );
					span += attributeMapping.breakDownJdbcValues(
							attributeValue,
							offset + span,
							x,
							y,
							valueConsumer,
							session
					);
				}
			}
		}
		return span;
	}

	@Override
	public <X, Y> int decompose(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		if ( shouldBindAggregateMapping() ) {
			valueConsumer.consume( offset, x, y, domainValue, aggregateMapping );
			return 1;
		}
		int span = 0;
		if ( domainValue instanceof Object[] ) {
			final Object[] values = (Object[]) domainValue;
			assert values.length == attributeMappings.size();

			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				final Object attributeValue = values[ i ];
				span += attributeMapping.decompose( attributeValue, offset + span, x, y, valueConsumer, session );
			}
		}
		else {
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				if ( !(attributeMapping instanceof PluralAttributeMapping )) {
					final Object attributeValue = domainValue == null
							? null
							: attributeMapping.getPropertyAccess().getGetter().get( domainValue );
					span += attributeMapping.decompose( attributeValue, offset + span, x, y, valueConsumer, session );
				}
			}
		}
		return span;
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		if ( orderedSubclasses != null ) {
			return getEmbeddableSubtype( orderedSubclasses.get( orderedSubclasses.size() - 1 ) ).forEachSelectable(
					offset,
					consumer
			);
		}
		else {
			return super.forEachSelectable( offset, consumer );
		}
	}

	@Override
	public void forEachInsertable(int offset, SelectableConsumer consumer) {
		if ( shouldMutateAggregateMapping() ) {
			if ( aggregateMapping.isInsertable() ) {
				consumer.accept( offset, aggregateMapping );
			}
		}
		else {
			if ( orderedSubclasses != null ) {
				getEmbeddableSubtype( orderedSubclasses.get( orderedSubclasses.size() - 1 ) ).forEachInsertable(
						offset,
						consumer
				);
			}
			else {
				final int jdbcTypeCount = selectableMappings.getJdbcTypeCount();
				for ( int i = 0; i < jdbcTypeCount; i++ ) {
					final SelectableMapping selectable = selectableMappings.getSelectable( i );
					if ( selectable.isInsertable() ) {
						consumer.accept( offset + i, selectable );
					}
				}
			}
		}
	}

	@Override
	public void forEachUpdatable(int offset, SelectableConsumer consumer) {
		if ( shouldMutateAggregateMapping() ) {
			if ( aggregateMapping.isUpdateable() ) {
				consumer.accept( offset, aggregateMapping );
			}
		}
		else {
			if ( orderedSubclasses != null ) {
				getEmbeddableSubtype( orderedSubclasses.get( orderedSubclasses.size() - 1 ) ).forEachUpdatable(
						offset,
						consumer
				);
			}
			else {
				final int jdbcTypeCount = selectableMappings.getJdbcTypeCount();
				for ( int i = 0; i < jdbcTypeCount; i++ ) {
					final SelectableMapping selectable = selectableMappings.getSelectable( i );
					if ( selectable.isUpdateable() ) {
						consumer.accept( offset + i, selectable );
					}
				}
			}
		}
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return valueMapping.findContainingEntityMapping();
	}


	@Override
	public boolean isCreateEmptyCompositesEnabled() {
		return createEmptyCompositesEnabled;
	}

	@Override
	public SelectableMapping getAggregateMapping() {
		return aggregateMapping;
	}

	@Override
	public boolean requiresAggregateColumnWriter() {
		return aggregateMappingRequiresColumnWriter;
	}

	@Override
	public boolean shouldSelectAggregateMapping() {
		return preferSelectAggregateMapping;
	}

	@Override
	public boolean shouldBindAggregateMapping() {
		return preferBindAggregateMapping;
	}
}
