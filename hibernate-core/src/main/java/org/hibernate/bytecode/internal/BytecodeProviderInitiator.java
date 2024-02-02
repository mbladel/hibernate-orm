/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.hibernate.Internal;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import static org.hibernate.cfg.BytecodeSettings.BYTECODE_PROVIDER;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;

public final class BytecodeProviderInitiator implements StandardServiceInitiator<BytecodeProvider> {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( BytecodeProviderInitiator.class );

	/**
	 * @deprecated Register a {@link BytecodeProvider} through Java {@linkplain java.util.ServiceLoader services}.
	 */
	@Deprecated( forRemoval = true )
	public static final String BYTECODE_PROVIDER_NAME_BYTEBUDDY = "bytebuddy";

	/**
	 * @deprecated Register a {@link BytecodeProvider} through Java {@linkplain java.util.ServiceLoader services}.
	 */
	@Deprecated( forRemoval = true )
	public static final String BYTECODE_PROVIDER_NAME_NONE = "none";

	/**
	 * @deprecated Deprecated with no replacement
	 */
	@Deprecated( forRemoval = true )
	public static final String BYTECODE_PROVIDER_NAME_DEFAULT = BYTECODE_PROVIDER_NAME_BYTEBUDDY;

	/**
	 * Singleton access
	 */
	public static final StandardServiceInitiator<BytecodeProvider> INSTANCE = new BytecodeProviderInitiator();

	@Override
	public BytecodeProvider initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final String provider = ConfigurationHelper.getString( BYTECODE_PROVIDER, configurationValues );
		log.error( "Old provider configuration: " + provider );

		final ClassLoaderService classLoaderService = castNonNull( registry.getService( ClassLoaderService.class ) );
		final Collection<BytecodeProvider> bytecodeProviders = classLoaderService.loadJavaServices( BytecodeProvider.class );
		return getBytecodeProvider( bytecodeProviders );
	}

	@Override
	public Class<BytecodeProvider> getServiceInitiated() {
		return BytecodeProvider.class;
	}

	@Internal
	public static BytecodeProvider buildDefaultBytecodeProvider() {
		return getBytecodeProvider( ServiceLoader.load( BytecodeProvider.class ) );
	}

	@Internal
	public static BytecodeProvider buildDefaultByteBuddyProvider() {
		final Iterator<BytecodeProvider> iterator = ServiceLoader.load( BytecodeProvider.class ).iterator();
		if ( iterator.hasNext() ) {
			log.error( "Found something!!!" );
			Thread.dumpStack();
			return iterator.next();
		}

		log.error( "Defaulting to ByteBuddy implementation" );
		Thread.dumpStack();
		return new org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl();
	}

	@Internal
	public static BytecodeProvider buildDefaultBytecodeProvider(ClassLoader classLoader) {
		final Iterator<BytecodeProvider> iterator = ServiceLoader.load( BytecodeProvider.class ).iterator();
		if ( iterator.hasNext()) {
			final BytecodeProvider next = iterator.next();
			log.errorf( "Found BytecodeProvider with default ClassLoder: %s", next.getClass() );
			Thread.dumpStack();
			return next;
		}

		log.errorf( "Getting BytecodeProvider from provided classloader: %s", classLoader.getClass() );
		Thread.dumpStack();
		// this is not correct, we get a strange error that the bytebuddy impl is not an implementation of BytecodeProvider?
		return getBytecodeProvider( ServiceLoader.load( BytecodeProvider.class, classLoader ) );
	}

	@Internal
	public static BytecodeProvider getBytecodeProvider(Iterable<BytecodeProvider> bytecodeProviders) {
		final Iterator<BytecodeProvider> iterator = bytecodeProviders.iterator();
		if ( !iterator.hasNext() ) {
			// If no BytecodeProvider service is available, default to the "no-op" enhancer
			log.error( "No BytecodeProvider service found" );
			Thread.dumpStack();
			return new org.hibernate.bytecode.internal.none.BytecodeProviderImpl();
		}

		final BytecodeProvider provider = iterator.next();

		log.error( "Found BytecodeProvider: " + provider.getClass() );
		Thread.dumpStack();

		if ( iterator.hasNext() ) {
			throw new IllegalStateException( "Found multiple BytecodeProvider service registrations, cannot determine which one to use" );
		}
		return provider;
	}
}
