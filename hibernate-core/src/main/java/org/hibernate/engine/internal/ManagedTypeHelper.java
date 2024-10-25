/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.collection.spi.LazyInitializable;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.PrimeAmongSecondarySupertypes;
import org.hibernate.engine.spi.Managed;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.ManagedMappedSuperclass;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import java.util.Objects;
import java.util.function.BiConsumer;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This is a helper to encapsulate an optimal strategy to execute type checks
 * for interfaces which attempts to avoid the performance issues tracked
 * as <a href="https://bugs.openjdk.org/browse/JDK-8180450">JDK-8180450</a>;
 * the problem is complex and explained better on the OpenJDK tracker;
 * we'll focus on a possible solution here.
 * <p>
 * To avoid polluting the secondary super-type cache, the important aspect is to
 * not switch types repeatedly for the same concrete object; using a Java
 * agent which was developed for this purpose (https://github.com/franz1981/type-pollution-agent)
 * we identified a strong case with Hibernate ORM is triggered when the entities are
 * using bytecode enhancement, as they are being frequently checked for compatibility with
 * the following interfaces:
 * <ul>
 * 	<li>{@link org.hibernate.engine.spi.PersistentAttributeInterceptable}</li>
 * 	<li>{@link org.hibernate.engine.spi.ManagedEntity}</li>
 * 	<li>{@link org.hibernate.engine.spi.SelfDirtinessTracker}</li>
 * 	<li>{@link org.hibernate.engine.spi.Managed}</li>
 * 	<li>{@link org.hibernate.proxy.HibernateProxy}</li>
 * 	</ul>
 * <p>
 * Some additional interfaces are involved in bytecode enhancement (such as {@link ManagedMappedSuperclass}),
 * but some might not be managed here as there was no evidence of them triggering the problem;
 * this might change after further testing.
 * <p>
 * The approach we pursue is to have all these internal interfaces extend a single
 * interface {@link PrimeAmongSecondarySupertypes} which then exposes a type widening
 * contract; this allows to consistently cast to {@code PrimeAmongSecondarySupertypes} exclusively
 * and avoid any further type checks; since the cast consistently happens on this interface
 * we avoid polluting the secondary super type cache described in JDK-8180450.
 * <p>
 * This presents two known drawbacks:
 * <p>
 * 1# we do assume such user entities aren't being used via interfaces in hot user code;
 * this is typically not the case based on our experience of Hibernate usage, but it
 * can't be ruled out.
 * <p>
 * 2# we're introducing virtual dispatch calls which are likely going to be megamorphic;
 * this is not great but we assume it's far better to avoid the scalability issue.
 *
 * @author Sanne Grinovero
 */
public final class ManagedTypeHelper {

	private static final ClassValue<TypeMeta> typeMetaCache = new ClassValue<>() {
		@Override
		protected TypeMeta computeValue(Class<?> type) {
			return new TypeMeta(type);
		}
	};

	/**
	 * @param type the type to check
	 * @return true if and only if the type is assignable to a {@see Managed} type.
	 */
	public static boolean isManagedType(final Class<?> type) {
		return typeMetaCache.get( type ).isManagedType;
	}

	/**
	 * @param type the type to check
	 * @return true if and only if the type is assignable to a {@see SelfDirtinessTracker} type.
	 */
	public static boolean isSelfDirtinessTrackerType(final Class<?> type) {
		return typeMetaCache.get( type ).isSelfDirtinessTrackerType;
	}

	/**
	 * @param type the type to check
	 * @return true if and only if the type is assignable to a {@see PersistentAttributeInterceptable} type.
	 */
	public static boolean isPersistentAttributeInterceptableType(final Class<?> type) {
		return typeMetaCache.get( type ).isPersistentAttributeInterceptable;
	}

	/**
	 * @param entity the entity to check
	 * @return true if and only if the entity implements {@link ManagedEntity}
	 * @apiNote {@link #isManagedEntity(Object, SessionFactoryImplementor)} should be preferred when
	 * we're not sure if the provided object is of the right type
	 */
	public static boolean isManagedEntity(final @Nullable Object entity) {
		return asManagedEntityOrNull( entity ) != null;
	}

	/**
	 * @param entity the entity to check
	 * @return true if and only if the entity implements {@link ManagedEntity}
	 */
	public static boolean isManagedEntity(final @Nullable Object entity, final SessionFactoryImplementor factory) {
		return asManagedEntityOrNull( entity, factory ) != null;
	}

	/**
	 * @return true if and only if the entity implements {@link HibernateProxy}
	 * @apiNote {@link #isHibernateProxy(Object, SessionFactoryImplementor)} should be preferred when
	 * we're not sure if the provided object is of the right type
	 */
	public static boolean isHibernateProxy(final @Nullable Object entity) {
		return asHibernateProxyOrNull( entity ) != null;
	}

	/**
	 * @return true if and only if the entity implements {@link HibernateProxy}
	 */
	public static boolean isHibernateProxy(final @Nullable Object entity, final SessionFactoryImplementor factory) {
		return asHibernateProxyOrNull( entity, factory ) != null;
	}

	/**
	 * @param entity the entity to check
	 * @return true if and only if the entity implements {@see PersistentAttributeInterceptable}
	 */
	public static boolean isPersistentAttributeInterceptable(final @Nullable Object entity) {
		return asPersistentAttributeInterceptableOrNull( entity ) != null;
	}

	/**
	 * @param entity the entity to check
	 * @return true if and only if the entity implements {@see SelfDirtinessTracker}
	 */
	public static boolean isSelfDirtinessTracker(final @Nullable Object entity) {
		return asSelfDirtinessTrackerOrNull( entity ) != null;
	}

	/**
	 * @param entity the entity to check
	 * @return true if and only if the entity implements {@see CompositeOwner}
	 */
	public static boolean isCompositeOwner(final @Nullable Object entity) {
		final PrimeAmongSecondarySupertypes prime = asPrimeAmongSecondarySupertypesOrNull( entity );
		return prime != null && prime.asCompositeOwner() != null;
	}

	/**
	 * @param entity the entity to check
	 * @return true if and only if the entity implements {@see CompositeTracker}
	 */
	public static boolean isCompositeTracker(final @Nullable Object entity) {
		final PrimeAmongSecondarySupertypes prime = asPrimeAmongSecondarySupertypesOrNull( entity );
		return prime != null && prime.asCompositeTracker() != null;
	}

	/**
	 * This interface has been introduced to mitigate <a href="https://bugs.openjdk.org/browse/JDK-8180450">JDK-8180450</a>.<br>
	 * Sadly, using  {@code BiConsumer} will trigger a type pollution issue because of generics type-erasure:
	 * {@code BiConsumer}'s actual parameters types on the lambda implemention's
	 * {@link BiConsumer#accept} are stealthy enforced via {@code checkcast}, messing up with type check cached data.
	 */
	@FunctionalInterface
	public interface PersistentAttributeInterceptableAction<T> {
		void accept(PersistentAttributeInterceptable interceptable, T optionalParam);
	}

	/**
	 * Helper to execute an action on an entity, but exclusively if it's implementing the {@link PersistentAttributeInterceptable}
	 * interface. Otherwise no action is performed.
	 *
	 * @param entity
	 * @param action The action to be performed; it should take the entity as first parameter, and an additional parameter T as second parameter.
	 * @param optionalParam a parameter which can be passed to the action
	 * @param <T> the type of the additional parameter.
	 * @deprecated In favor of {@link #processIfPersistentAttributeInterceptable(Object, PersistentAttributeInterceptableAction, Object, SessionFactoryImplementor)}
	 */
	@Deprecated(since = "7.1")
	public static <T> void processIfPersistentAttributeInterceptable(
			final @Nullable Object entity,
			final PersistentAttributeInterceptableAction<T> action,
			final T optionalParam) {
		final PersistentAttributeInterceptable e = asPersistentAttributeInterceptableOrNull( entity );
		if ( e != null ) {
			action.accept( e, optionalParam );
		}
	}

	/**
	 * Helper to execute an action on an entity, but exclusively if it's implementing the {@link PersistentAttributeInterceptable}
	 * interface. Otherwise no action is performed.
	 *
	 * @param entity
	 * @param action The action to be performed; it should take the entity as first parameter, and an additional parameter T as second parameter.
	 * @param optionalParam a parameter which can be passed to the action
	 * @param <T> the type of the additional parameter.
	 */
	public static <T> void processIfPersistentAttributeInterceptable(
			final @Nullable Object entity,
			final PersistentAttributeInterceptableAction<T> action,
			final T optionalParam,
			final SessionFactoryImplementor factory) {
		final PersistentAttributeInterceptable e = asPersistentAttributeInterceptableOrNull( entity, factory );
		if ( e != null ) {
			action.accept( e, optionalParam );
		}
	}

	/**
	 * If the entity is implementing SelfDirtinessTracker, apply some action to it.
	 * It is first cast to SelfDirtinessTracker using an optimal strategy.
	 * If the entity does not implement SelfDirtinessTracker, no operation is performed.
	 * @param entity
	 * @param action the action to be performed
	 * @deprecated In favor of {@link #processIfSelfDirtinessTracker(Object, SelfDirtinessTrackerConsumer, SessionFactoryImplementor)}
	 */
	@Deprecated(since = "7.1")
	public static void processIfSelfDirtinessTracker(
			final @Nullable Object entity,
			final SelfDirtinessTrackerConsumer action) {
		final SelfDirtinessTracker e = asSelfDirtinessTrackerOrNull( entity );
		if ( e != null ) {
			action.accept( e );
		}
	}

	/**
	 * If the entity is implementing SelfDirtinessTracker, apply some action to it.
	 * It is first cast to SelfDirtinessTracker using an optimal strategy.
	 * If the entity does not implement SelfDirtinessTracker, no operation is performed.
	 * @param entity
	 * @param action the action to be performed
	 */
	public static void processIfSelfDirtinessTracker(
			final @Nullable Object entity,
			final SelfDirtinessTrackerConsumer action,
			final SessionFactoryImplementor factory) {
		final SelfDirtinessTracker e = asSelfDirtinessTrackerOrNull( entity, factory );
		if ( e != null ) {
			action.accept( e );
		}
	}

	/**
	 * Helper to execute an action on an entity, but exclusively if it's implementing the {@link ManagedEntity}
	 * interface. Otherwise, no action is performed.
	 * @param entity
	 * @param action the action to be performed
	 * @deprecated In favor of {@link #processIfManagedEntity(Object, ManagedEntityConsumer, SessionFactoryImplementor)}
	 */
	@Deprecated(since = "7.1")
	public static void processIfManagedEntity(final @Nullable Object entity, final ManagedEntityConsumer action) {
		final ManagedEntity e = asManagedEntityOrNull( entity );
		if ( e != null ) {
			action.accept( e );
		}
	}

	/**
	 * Helper to execute an action on an entity, but exclusively if it's implementing the {@link ManagedEntity}
	 * interface. Otherwise, no action is performed.
	 * @param entity
	 * @param action the action to be performed
	 */
	public static void processIfManagedEntity(
			final @Nullable Object entity,
			final ManagedEntityConsumer action,
			final SessionFactoryImplementor factory) {
		final ManagedEntity e = asManagedEntityOrNull( entity, factory );
		if ( e != null ) {
			action.accept( e );
		}
	}

	// Not using Consumer<SelfDirtinessTracker> because of JDK-8180450:
	// use a custom functional interface with explicit type.
	@FunctionalInterface
	public interface SelfDirtinessTrackerConsumer {
		void accept(SelfDirtinessTracker tracker);
	}

	@FunctionalInterface
	public interface ManagedEntityConsumer {
		void accept(ManagedEntity entity);
	}

	/**
	 * If the entity is implementing SelfDirtinessTracker, apply some action to it; this action should take
	 * a parameter of type T.
	 * It is first cast to SelfDirtinessTracker using an optimal strategy.
	 * If the entity does not implement SelfDirtinessTracker, no operation is performed.
	 * @param entity
	 * @param action the action to be performed
	 * @param optionalParam a parameter which can be passed to the action
	 * @param <T> the type of the additional parameter.
	 * @deprecated In favor of {@link #processIfSelfDirtinessTracker(Object, SelfDirtinessTrackerAction, Object, SessionFactoryImplementor)}
	 */
	@Deprecated(since = "7.1")
	public static <T> void processIfSelfDirtinessTracker(
			final @Nullable Object entity,
			final SelfDirtinessTrackerAction<T> action,
			final T optionalParam) {
		final SelfDirtinessTracker e = asSelfDirtinessTrackerOrNull( entity );
		if ( e != null ) {
			action.accept( e, optionalParam );
		}
	}

	/**
	 * If the entity is implementing SelfDirtinessTracker, apply some action to it; this action should take
	 * a parameter of type T.
	 * It is first cast to SelfDirtinessTracker using an optimal strategy.
	 * If the entity does not implement SelfDirtinessTracker, no operation is performed.
	 * @param entity
	 * @param action the action to be performed
	 * @param optionalParam a parameter which can be passed to the action
	 * @param <T> the type of the additional parameter.
	 */
	public static <T> void processIfSelfDirtinessTracker(
			final @Nullable Object entity,
			final SelfDirtinessTrackerAction<T> action,
			final T optionalParam,
			final SessionFactoryImplementor factory) {
		final SelfDirtinessTracker e = asSelfDirtinessTrackerOrNull( entity, factory );
		if ( e != null ) {
			action.accept( e, optionalParam );
		}
	}

	// Not using BiConsumer<SelfDirtinessTracker, T> because of JDK-8180450:
	// use a custom functional interface with explicit type.
	@FunctionalInterface
	public interface SelfDirtinessTrackerAction<T> {
		void accept(SelfDirtinessTracker tracker, T optionalParam);
	}

	/**
	 * Cast the object to PrimeAmongSecondarySubpertypes
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static PrimeAmongSecondarySupertypes asPrimeAmongSecondarySupertypes(final Object entity) {
		Objects.requireNonNull( entity );
		final PrimeAmongSecondarySupertypes prime = asPrimeAmongSecondarySupertypesOrNull( entity );
		if ( prime != null ) {
			return prime;
		}
		else {
			throw new ClassCastException( "Object of type '" + entity.getClass() + "' can't be cast to PrimeAmongSecondarySupertypes" );
		}
	}

	/**
	 * Same as {@link #asPrimeAmongSecondarySupertypes} but returns {@code null} instead of throwing
	 * when the provided object is not of the required type
	 * @param entity the entity to cast
	 * @return the same instance after casting or null
	 */
	public static @Nullable PrimeAmongSecondarySupertypes asPrimeAmongSecondarySupertypesOrNull(final @Nullable Object entity) {
		if ( entity instanceof PrimeAmongSecondarySupertypes prime ) {
			return prime;
		}
		return null;
	}

	/**
	 * Same as {@link #asPrimeAmongSecondarySupertypes} but returns {@code null} instead of throwing
	 * when the provided object is not of the required type
	 * @param entity the entity to cast
	 * @param factory the session factory
	 * @return the same instance after casting or null
	 */
	public static @Nullable PrimeAmongSecondarySupertypes asPrimeAmongSecondarySupertypesOrNull(
			final @Nullable Object entity,
			final SessionFactoryImplementor factory) {
		if ( entity != null ) {
			// Cache classes implementing PrimeAmongSecondarySupertypes to avoid repeated instanceof checks
			final PrimeMeta meta = factory.getClassMetadata( PrimeMeta.class, PrimeMeta::new ).get( entity.getClass() );
			if ( meta.isPrime ) {
				return (PrimeAmongSecondarySupertypes) entity;
			}
		}
		return null;
	}

	/**
	 * Cast the object to PersistentAttributeInterceptable
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static PersistentAttributeInterceptable asPersistentAttributeInterceptable(final Object entity) {
		Objects.requireNonNull( entity );
		final PersistentAttributeInterceptable interceptable = asPersistentAttributeInterceptableOrNull( entity );
		if ( interceptable != null ) {
			return interceptable;
		}
		else {
			throw new ClassCastException( "Object of type '" + entity.getClass() + "' can't be cast to PersistentAttributeInterceptable" );
		}
	}

	/**
	 * Same as {@link #asPersistentAttributeInterceptable} but returns {@code null} instead of throwing
	 * when the provided object is not of the required type
	 * @param entity the entity to cast
	 * @return the same instance after casting or null
	 * @apiNote {@link #asPersistentAttributeInterceptableOrNull(Object, SessionFactoryImplementor)} should be preferred when
	 * we're not sure if the provided object is of the right type
	 */
	public static @Nullable PersistentAttributeInterceptable asPersistentAttributeInterceptableOrNull(final @Nullable Object entity) {
		final PrimeAmongSecondarySupertypes prime = asPrimeAmongSecondarySupertypesOrNull( entity );
		return prime != null ? prime.asPersistentAttributeInterceptable() : null;
	}

	/**
	 * Same as {@link #asPersistentAttributeInterceptable} but returns {@code null} instead of throwing
	 * when the provided object is not of the required type
	 * @param entity the entity to cast
	 * @param factory the session factory
	 * @return the same instance after casting or null
	 */
	public static @Nullable PersistentAttributeInterceptable asPersistentAttributeInterceptableOrNull(
			final @Nullable Object entity,
			final SessionFactoryImplementor factory) {
		final PrimeAmongSecondarySupertypes prime = asPrimeAmongSecondarySupertypesOrNull( entity, factory );
		return prime != null ? prime.asPersistentAttributeInterceptable() : null;
	}

	/**
	 * Cast the object to HibernateProxy
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static HibernateProxy asHibernateProxy(final Object entity) {
		Objects.requireNonNull( entity );
		final HibernateProxy e = asHibernateProxyOrNull( entity );
		if ( e != null ) {
			return e;
		}
		throw new ClassCastException( "Object of type '" + entity.getClass() + "' can't be cast to HibernateProxy" );
	}

	/**
	 * Same as {@link #asHibernateProxy} but returns {@code null} instead of throwing
	 * when the provided object is not of the required type
	 * @param entity the entity to cast
	 * @return the same instance after casting or null if it is not an instance of HibernateProxy
	 * @apiNote {@link #asHibernateProxyOrNull(Object, SessionFactoryImplementor)} should be preferred when
	 * we're not sure if the provided object is of the right type
	 */
	public static @Nullable HibernateProxy asHibernateProxyOrNull(final @Nullable Object entity) {
		final PrimeAmongSecondarySupertypes prime = asPrimeAmongSecondarySupertypesOrNull( entity );
		return prime != null ? prime.asHibernateProxy() : null;
	}

	/**
	 * Same as {@link #asHibernateProxy} but returns {@code null} instead of throwing
	 * when the provided object is not of the required type
	 * @param entity the entity to cast
	 * @return the same instance after casting or null if it is not an instance of HibernateProxy
	 */
	public static @Nullable HibernateProxy asHibernateProxyOrNull(
			final @Nullable Object entity,
			final SessionFactoryImplementor factory) {
		final PrimeAmongSecondarySupertypes prime = asPrimeAmongSecondarySupertypesOrNull( entity, factory );
		return prime != null ? prime.asHibernateProxy() : null;
	}

	/**
	 * Extract the {@link LazyInitializer} from the given object,
	 * if and only if the object is actually a proxy. Otherwise,
	 * return a null value.
	 *
	 * @param object any reference to an entity
	 * @param factory the session factory
	 * @return the associated {@link LazyInitializer} if the given
	 *         object is a proxy, or {@code null} otherwise.
	 * @apiNote Should be preferred over {@link HibernateProxy#asHibernateProxy()} when possible
	 */
	public static @Nullable LazyInitializer extractLazyInitializer(
			final @Nullable Object object,
			final SessionFactoryImplementor factory) {
		final HibernateProxy hibernateProxy = asHibernateProxyOrNull( object, factory );
		return hibernateProxy != null ? hibernateProxy.getHibernateLazyInitializer() : null;
	}

	/**
	 * Force initialization of a proxy or persistent collection. In the case of a
	 * many-valued association, only the collection itself is initialized. It is not
	 * guaranteed that the associated entities held within the collection will be
	 * initialized.
	 *
	 * @param proxy a persistable object, proxy, persistent collection or {@code null}
	 * @throws HibernateException if the proxy cannot be initialized at this time,
	 * for example, if the {@code Session} was closed
	 * @apiNote Should be preferred over {@link org.hibernate.Hibernate#initialize(Object)} when possible
	 */
	public static void initialize(@Nullable Object proxy, SessionFactoryImplementor factory) throws HibernateException {
		final boolean initialized = initializeProxy( asPrimeAmongSecondarySupertypesOrNull( proxy, factory ) );
		if ( !initialized && proxy instanceof LazyInitializable lazyInitializable ) {
			lazyInitializable.forceInitialization();
		}
	}

	/**
	 * Utility method that checks if a {@link PrimeAmongSecondarySupertypes} instance is either an
	 * {@link HibernateProxy} or a {@link PersistentAttributeInterceptable} and initializes it.
	 *
	 * @param prime the instance to check
	 *
	 * @return {@code true}, if the provided object was a proxy and was successfully initialized, {@code false} otherwise
	 */
	public static boolean initializeProxy(final @Nullable PrimeAmongSecondarySupertypes prime) {
		if ( prime != null ) {
			final LazyInitializer lazyInitializer = getLazyInitializer( prime );
			if ( lazyInitializer != null ) {
				lazyInitializer.initialize();
				return true;
			}
			else if ( getAttributeInterceptor( prime ) instanceof EnhancementAsProxyLazinessInterceptor enhancementInterceptor ) {
				enhancementInterceptor.forceInitialize( prime, null );
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines if the given proxy or persistent collection is initialized.
	 * <p>
	 * This operation is equivalent to {@link jakarta.persistence.PersistenceUtil#isLoaded(Object)}.
	 *
	 * @param proxy a persistable object, proxy, persistent collection or {@code null}
	 * @return true if the argument is already initialized, or is not a proxy or collection
	 * @apiNote Should be preferred over {@link org.hibernate.Hibernate#isInitialized(Object)} when possible
	 */
	public static boolean isInitialized(@Nullable Object proxy, SessionFactoryImplementor factory) {
		final Boolean initialized = isInitializedProxy( asPrimeAmongSecondarySupertypesOrNull( proxy, factory ) );
		if ( initialized != null ) {
			return Boolean.TRUE.equals( initialized );
		}
		else if ( proxy instanceof LazyInitializable lazyInitializable ) {
			return lazyInitializable.wasInitialized();
		}
		else {
			return true;
		}
	}

	/**
	 * Utility method that checks if a {@link PrimeAmongSecondarySupertypes} instance is either an
	 * {@link HibernateProxy} or a {@link PersistentAttributeInterceptable} and returns {@code true}
	 * if they are initialized, {@code false} if they're not initialized, or {@code null} if the
	 * instance was not a proxy at all.
	 *
	 * @param prime the instance to check
	 *
	 * @return true, if the provided instance is an initialized proxy, false if the instance is a
	 * non-initialized proxy, and {@code null} if the instance was not a proxy at all
	 */
	public static @Nullable Boolean isInitializedProxy(final @Nullable PrimeAmongSecondarySupertypes prime) {
		if ( prime != null ) {
			final LazyInitializer lazyInitializer = getLazyInitializer( prime );
			if ( lazyInitializer != null ) {
				return !lazyInitializer.isUninitialized();
			}
			else {
				final PersistentAttributeInterceptable interceptable = prime.asPersistentAttributeInterceptable();
				if ( interceptable != null ) {
					final boolean uninitialized =
							interceptable.$$_hibernate_getInterceptor()
									instanceof EnhancementAsProxyLazinessInterceptor enhancementInterceptor
									&& !enhancementInterceptor.isInitialized();
					return !uninitialized;
				}
			}
		}
		return null;
	}

	public static PersistentAttributeInterceptor getAttributeInterceptor(PrimeAmongSecondarySupertypes prime) {
		final PersistentAttributeInterceptable interceptable = prime.asPersistentAttributeInterceptable();
		return interceptable != null ? interceptable.$$_hibernate_getInterceptor() : null;
	}

	public static LazyInitializer getLazyInitializer(PrimeAmongSecondarySupertypes prime) {
		final HibernateProxy hibernateProxy = prime.asHibernateProxy();
		return hibernateProxy != null ? hibernateProxy.getHibernateLazyInitializer() : null;
	}

	/**
	 * Cast the object to ManagedEntity
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static ManagedEntity asManagedEntity(final Object entity) {
		Objects.requireNonNull( entity );
		final ManagedEntity managedEntity = asManagedEntityOrNull( entity );
		if ( managedEntity != null ) {
			return managedEntity;
		}
		else {
			throw new ClassCastException( "Object of type '" + entity.getClass() + "' can't be cast to ManagedEntity" );
		}
	}

	/**
	 * Same as {@link #asManagedEntity} but returns {@code null} instead of throwing
	 * when the provided object is not of the required type
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @apiNote {@link #asManagedEntityOrNull(Object, SessionFactoryImplementor)} should be preferred when
	 * we're not sure if the provided object is of the right type
	 */
	public static @Nullable ManagedEntity asManagedEntityOrNull(final @Nullable Object entity) {
		final PrimeAmongSecondarySupertypes prime = asPrimeAmongSecondarySupertypesOrNull( entity );
		return prime != null ? prime.asManagedEntity() : null;
	}

	/**
	 * Same as {@link #asManagedEntity} but returns {@code null} instead of throwing
	 * when the provided object is not of the required type
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 */
	public static @Nullable ManagedEntity asManagedEntityOrNull(
			final @Nullable Object entity,
			final SessionFactoryImplementor factory) {
		final PrimeAmongSecondarySupertypes prime = asPrimeAmongSecondarySupertypesOrNull( entity, factory );
		return prime != null ? prime.asManagedEntity() : null;
	}

	/**
	 * Cast the object to CompositeTracker
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static CompositeTracker asCompositeTracker(final Object entity) {
		Objects.requireNonNull( entity );
		final PrimeAmongSecondarySupertypes prime = asPrimeAmongSecondarySupertypesOrNull( entity );
		if ( prime != null ) {
			final CompositeTracker e = prime.asCompositeTracker();
			if ( e != null ) {
				return e;
			}
		}
		throw new ClassCastException( "Object of type '" + entity.getClass() + "' can't be cast to CompositeTracker" );
	}

	/**
	 * Cast the object to CompositeOwner
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static CompositeOwner asCompositeOwner(final Object entity) {
		Objects.requireNonNull( entity );
		final PrimeAmongSecondarySupertypes prime = asPrimeAmongSecondarySupertypesOrNull( entity );
		if ( prime != null ) {
			final CompositeOwner e = prime.asCompositeOwner();
			if ( e != null ) {
				return e;
			}
		}
		throw new ClassCastException( "Object of type '" + entity.getClass() + "' can't be cast to CompositeOwner" );
	}

	/**
	 * Cast the object to SelfDirtinessTracker
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static SelfDirtinessTracker asSelfDirtinessTracker(final Object entity) {
		Objects.requireNonNull( entity );
		final SelfDirtinessTracker tracker = asSelfDirtinessTrackerOrNull( entity );
		if ( tracker != null ) {
			return tracker;
		}
		else {
			throw new ClassCastException( "Object of type '" + entity.getClass() + "' can't be cast to SelfDirtinessTracker" );
		}
	}

	/**
	 * Same as {@link #asSelfDirtinessTracker} but returns {@code null} instead of throwing
	 * when the provided object is not of the required type
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @apiNote {@link #asSelfDirtinessTrackerOrNull(Object, SessionFactoryImplementor)} should be preferred when
	 * we're not sure if the provided object is of the right type
	 */
	public static @Nullable SelfDirtinessTracker asSelfDirtinessTrackerOrNull(final @Nullable Object entity) {
		final PrimeAmongSecondarySupertypes prime = asPrimeAmongSecondarySupertypesOrNull( entity );
		return prime != null ? prime.asSelfDirtinessTracker() : null;
	}

	/**
	 * Same as {@link #asSelfDirtinessTracker} but returns {@code null} instead of throwing
	 * when the provided object is not of the required type
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 */
	public static @Nullable SelfDirtinessTracker asSelfDirtinessTrackerOrNull(
			final @Nullable Object entity,
			final SessionFactoryImplementor factory) {
		final PrimeAmongSecondarySupertypes prime = asPrimeAmongSecondarySupertypesOrNull( entity, factory );
		return prime != null ? prime.asSelfDirtinessTracker() : null;
	}

	private static final class TypeMeta {
		final boolean isManagedType;
		final boolean isSelfDirtinessTrackerType;
		final boolean isPersistentAttributeInterceptable;

		TypeMeta(final Class<?> type) {
			Objects.requireNonNull( type );
			this.isManagedType = Managed.class.isAssignableFrom( type );
			this.isSelfDirtinessTrackerType = SelfDirtinessTracker.class.isAssignableFrom( type );
			this.isPersistentAttributeInterceptable = PersistentAttributeInterceptable.class.isAssignableFrom( type );
		}
	}

	private static final class PrimeMeta {
		final boolean isPrime;

		PrimeMeta(final Class<?> type) {
			Objects.requireNonNull( type );
			this.isPrime = PrimeAmongSecondarySupertypes.class.isAssignableFrom( type );
		}
	}
}
