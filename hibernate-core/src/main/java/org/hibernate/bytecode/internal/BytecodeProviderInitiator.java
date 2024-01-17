/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal;

import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;

import org.hibernate.Internal;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.BytecodeSettings.BYTECODE_PROVIDER;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;

public final class BytecodeProviderInitiator implements StandardServiceInitiator<BytecodeProvider> {

	public static final String BYTECODE_PROVIDER_NAME_BYTEBUDDY = "bytebuddy";
	public static final String BYTECODE_PROVIDER_NAME_NONE = "none";
	public static final String BYTECODE_PROVIDER_NAME_DEFAULT = BYTECODE_PROVIDER_NAME_BYTEBUDDY;

	/**
	 * Singleton access
	 */
	public static final StandardServiceInitiator<BytecodeProvider> INSTANCE = new BytecodeProviderInitiator();

	@Override
	public BytecodeProvider initiateService(
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor registry) {
		final ClassLoaderService classLoaderService = castNonNull( registry.getService( ClassLoaderService.class ) );
		final Collection<BytecodeProvider> bytecodeProviders = classLoaderService.loadJavaServices( BytecodeProvider.class );
		if ( bytecodeProviders.isEmpty() ) {
			// Default to the configuration property for backwards compatibility
			// todo marco : should we default to the no-op impl, and maybe log a warning if using the deprecated config property?
			// todo marco : should we deprecate the configuration property? Note that it will be removed from auto-generated docs that way
			final String provider = ConfigurationHelper.getString(
					BYTECODE_PROVIDER,
					configurationValues,
					BYTECODE_PROVIDER_NAME_DEFAULT
			);
			return buildBytecodeProvider( provider );
		}
		else if ( bytecodeProviders.size() > 1 ) {
			throw new IllegalStateException( "Only one BytecodeProvider service must be available at the time" );
		}
		else {
			return bytecodeProviders.iterator().next();
		}
	}

	@Override
	public Class<BytecodeProvider> getServiceInitiated() {
		return BytecodeProvider.class;
	}

	@Internal
	public static BytecodeProvider buildDefaultBytecodeProvider() {
		// todo marco : deprecate this ? Probably not, change it to just instantiate ByteBuddy impl
		return buildBytecodeProvider( BYTECODE_PROVIDER_NAME_BYTEBUDDY );
	}

	@Internal
	public static BytecodeProvider buildBytecodeProvider(String providerName) {
		// todo marco : deprecate this ?
		CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, BytecodeProviderInitiator.class.getName() );
		LOG.bytecodeProvider( providerName );

		if ( BYTECODE_PROVIDER_NAME_NONE.equals( providerName ) ) {
			return new org.hibernate.bytecode.internal.none.BytecodeProviderImpl();
		}
		if ( BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals( providerName ) ) {
			return new org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl();
		}

		// There is no need to support plugging in a custom BytecodeProvider via FQCN
		// as it's possible to plug a custom BytecodeProviderInitiator into the bootstrap.
		//
		// This also allows integrators to inject a BytecodeProvider instance which has some
		// state: particularly useful to inject proxy definitions which have been prepared in
		// advance.
		// See also https://hibernate.atlassian.net/browse/HHH-13804 and how this was solved in
		// Quarkus.

		LOG.unknownBytecodeProvider( providerName, BYTECODE_PROVIDER_NAME_DEFAULT );
		return new org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl();
	}

}
