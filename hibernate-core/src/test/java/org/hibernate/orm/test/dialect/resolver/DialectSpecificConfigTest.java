/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.dialect.resolver;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.MySQLServerConfiguration;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.DatabaseVersion.NO_VERSION;

/**
 * @author Marco Belladelli
 */
public class DialectSpecificConfigTest {
	@Test
	public void testOracleMaxStringSize() {
		final Dialect dialect = resolveDialect(
				"Oracle",
				values -> values.put( OracleDialect.EXTENDED_STRING_SIZE, "true" )
		);

		assertThat( dialect ).isInstanceOf( OracleDialect.class );
		assertThat( dialect.getMaxVarcharLength() ).isEqualTo( 32_767 );
		assertThat( dialect.getMaxVarbinaryLength() ).isEqualTo( 32_767 );
	}

	@Test
	public void testOracleIsAutonomous() {
		final Dialect dialect = resolveDialect(
				"Oracle",
				values -> values.put( OracleDialect.IS_AUTONOMOUS_DATABASE, "true" )
		);

		assertThat( dialect ).isInstanceOf( OracleDialect.class );
		assertThat( ( (OracleDialect) dialect ).isAutonomous() ).isTrue();
	}

	@Test
	public void testSybaseASEIsAnsiNull() {
		final Dialect dialect = resolveDialect(
				"ASE",
				values -> values.put( SybaseASEDialect.IS_ANSI_NULL, "true" )
		);

		assertThat( dialect ).isInstanceOf( SybaseASEDialect.class );
		assertThat( ( (SybaseASEDialect) dialect ).isAnsiNullOn() ).isTrue();
	}

	@Test
	public void testMySQLBytesPerCharacter() {
		final Dialect dialect = resolveDialect(
				"MySQL",
				values -> values.put( MySQLServerConfiguration.BYTES_PER_CHARACTER, "1" )
		);

		assertThat( dialect ).isInstanceOf( MySQLDialect.class );
		assertThat( dialect.getMaxVarcharLength() ).isEqualTo( 65_535 );
	}

	@Test
	public void testMySQLNoBackslashEscape() {
		final Dialect dialect = resolveDialect(
				"MySQL",
				values -> values.put( MySQLServerConfiguration.NO_BACKSLASH_ESCAPES, "true" )
		);

		assertThat( dialect ).isInstanceOf( MySQLDialect.class );
		assertThat( ( (MySQLDialect) dialect ).isNoBackslashEscapesEnabled() ).isTrue();
	}

	@Test
	public void testCockroachDBVersion() {
		final Dialect dialect = resolveDialect( "PostgreSQL", values -> values.put(
				CockroachDialect.VERSION_STRING,
				"CockroachDB CCL v23.1.8 (x86_64-pc-linux-gnu, built 2023/08/04 18:11:44, go1.19.10)"
		) );

		assertThat( dialect ).isInstanceOf( CockroachDialect.class );
		assertThat( dialect.getVersion().getMajor() ).isEqualTo( 23 );
		assertThat( dialect.getVersion().getMinor() ).isEqualTo( 1 );
		assertThat( dialect.getVersion().getMicro() ).isEqualTo( 8 );
	}

	private static Dialect resolveDialect(String productName, Consumer<Map<String, Object>> configurationProvider) {
		final Map<String, Object> configurationValues = new HashMap<>();
		configurationProvider.accept( configurationValues );
		final TestingDialectResolutionInfo info = TestingDialectResolutionInfo.forDatabaseInfo(
				productName,
				null,
				NO_VERSION,
				NO_VERSION,
				configurationValues
		);

		assertThat( info.getDatabaseMetadata() ).isNull();

		return new StandardDialectResolver().resolveDialect( info );
	}
}
