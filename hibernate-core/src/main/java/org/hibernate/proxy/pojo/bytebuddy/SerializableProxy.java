/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy.pojo.bytebuddy;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.proxy.AbstractSerializableProxy;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.CompositeType;

public final class SerializableProxy extends AbstractSerializableProxy {
	private final Class<?> persistentClass;
	private final Class<?>[] interfaces;

	private final String identifierGetterMethodName;
	private final Class<?> identifierGetterMethodClass;

	private final String identifierSetterMethodName;
	private final Class<?> identifierSetterMethodClass;
	private final Class<?>[] identifierSetterMethodParams;

	private final CompositeType componentIdType;

	private static volatile BytecodeProviderImpl defaultProvider;

	public SerializableProxy(
			String entityName,
			Class<?> persistentClass,
			Class<?>[] interfaces,
			Object id,
			Boolean readOnly,
			String sessionFactoryUuid,
			String sessionFactoryName,
			boolean allowLoadOutsideTransaction,
			Method getIdentifierMethod,
			Method setIdentifierMethod,
			CompositeType componentIdType) {
		super( entityName, id, readOnly, sessionFactoryUuid, sessionFactoryName, allowLoadOutsideTransaction );
		this.persistentClass = persistentClass;
		this.interfaces = interfaces;
		if ( getIdentifierMethod != null ) {
			identifierGetterMethodName = getIdentifierMethod.getName();
			identifierGetterMethodClass = getIdentifierMethod.getDeclaringClass();
		}
		else {
			identifierGetterMethodName = null;
			identifierGetterMethodClass = null;
		}

		if ( setIdentifierMethod != null ) {
			identifierSetterMethodName = setIdentifierMethod.getName();
			identifierSetterMethodClass = setIdentifierMethod.getDeclaringClass();
			identifierSetterMethodParams = setIdentifierMethod.getParameterTypes();
		}
		else {
			identifierSetterMethodName = null;
			identifierSetterMethodClass = null;
			identifierSetterMethodParams = null;
		}

		this.componentIdType = componentIdType;
	}

	@Override
	protected String getEntityName() {
		return super.getEntityName();
	}

	@Override
	protected Object getId() {
		return super.getId();
	}

	Class<?> getPersistentClass() {
		return persistentClass;
	}

	Class<?>[] getInterfaces() {
		return interfaces;
	}

	String getIdentifierGetterMethodName() {
		return identifierGetterMethodName;
	}

	Class<?> getIdentifierGetterMethodClass() {
		return identifierGetterMethodClass;
	}

	String getIdentifierSetterMethodName() {
		return identifierSetterMethodName;
	}

	Class<?> getIdentifierSetterMethodClass() {
		return identifierSetterMethodClass;
	}

	Class<?>[] getIdentifierSetterMethodParams() {
		return identifierSetterMethodParams;
	}

	CompositeType getComponentIdType() {
		return componentIdType;
	}

	private Object readResolve() {
		final SessionFactoryImplementor sessionFactory = retrieveMatchingSessionFactory( this.sessionFactoryUuid, this.sessionFactoryName );
		BytecodeProviderImpl byteBuddyBytecodeProvider = retrieveByteBuddyBytecodeProvider( sessionFactory );
		HibernateProxy proxy = byteBuddyBytecodeProvider.getByteBuddyProxyHelper().deserializeProxy( this );
		afterDeserialization( (ByteBuddyInterceptor) proxy.getHibernateLazyInitializer() );
		return proxy;
	}

	private static SessionFactoryImplementor retrieveMatchingSessionFactory(final String sessionFactoryUuid, final String sessionFactoryName) {
		Objects.requireNonNull( sessionFactoryUuid );
		return SessionFactoryRegistry.INSTANCE.findSessionFactory( sessionFactoryUuid, sessionFactoryName );
	}

	private static BytecodeProviderImpl retrieveByteBuddyBytecodeProvider(final SessionFactoryImplementor sessionFactory) {
		if ( sessionFactory == null ) {
			// When the session factory is not available fallback to local bytecode provider
			return getDefaultProvider();
		}

		return castBytecodeProvider( sessionFactory.getServiceRegistry().getService( BytecodeProvider.class ) );
	}

	private static BytecodeProviderImpl getDefaultProvider() {
		BytecodeProviderImpl provider = defaultProvider;
		if ( provider == null ) {
			// todo marco : we probably need to use ServiceRegistry here, but how ???
			final ServiceLoader<BytecodeProvider> loader = ServiceLoader.load( BytecodeProvider.class );
			final Set<BytecodeProvider> bytecodeProviders = new HashSet<>( 1 );
			loader.stream().forEach( p -> bytecodeProviders.add( p.get() ) );
			if ( bytecodeProviders.isEmpty() ) {
				// Default to no-op provider
				throw new IllegalStateException( "Unable to deserialize a SerializableProxy proxy: no bytecode provider service available." );
			}
			else if ( bytecodeProviders.size() > 1 ) {
				throw new IllegalStateException( "Only one BytecodeProvider service must be available at the time" );
			}
			else {
				provider = defaultProvider = castBytecodeProvider( bytecodeProviders.iterator().next() );
			}
		}
		return provider;
	}

	private static BytecodeProviderImpl castBytecodeProvider(BytecodeProvider bytecodeProvider) {
		if ( bytecodeProvider instanceof BytecodeProviderImpl ) {
			return (BytecodeProviderImpl) bytecodeProvider;
		}
		else {
			throw new IllegalStateException( "Unable to deserialize a SerializableProxy proxy: the bytecode provider is not ByteBuddy." );
		}
	}
}
