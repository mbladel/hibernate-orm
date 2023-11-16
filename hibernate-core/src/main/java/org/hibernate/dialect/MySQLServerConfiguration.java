/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.internal.util.config.ConfigurationHelper;

public class MySQLServerConfiguration {
	private final int bytesPerCharacter;
	private final boolean noBackslashEscapesEnabled;

	/**
	 * Fallback setting when a connection to the database is not available at boot time.
	 * Specifies the bytes per character to use based on the database's configured
	 * <a href="https://dev.mysql.com/doc/refman/8.0/en/charset-charsets.html">charset</a>.
	 *
	 * @settingDefault {@code false}
	 */
	private static final String BYTES_PER_CHARACTER = "hibernate.dialect.mysql.bytes_per_character";

	/**
	 * Fallback setting when a connection to the database is not available at boot time.
	 * Specifies whether the {@code NO_BACKSLASH_ESCAPES} sql mode is enabled.
	 *
	 * @settingDefault {@code false}
	 */
	private static final String NO_BACKSLASH_ESCAPES = "hibernate.dialect.mysql.no_backslash_escapes";

	public MySQLServerConfiguration(int bytesPerCharacter, boolean noBackslashEscapesEnabled) {
		this.bytesPerCharacter = bytesPerCharacter;
		this.noBackslashEscapesEnabled = noBackslashEscapesEnabled;
	}

	public int getBytesPerCharacter() {
		return bytesPerCharacter;
	}

	public boolean isNoBackslashEscapesEnabled() {
		return noBackslashEscapesEnabled;
	}

	public static MySQLServerConfiguration fromDialectResolutionInfo(DialectResolutionInfo info) {
		Integer bytesPerCharacter = null;
		Boolean noBackslashEscapes = null;
		final DatabaseMetaData databaseMetaData = info.getDatabaseMetadata();
		if ( databaseMetaData != null ) {
			try (java.sql.Statement s = databaseMetaData.getConnection().createStatement()) {
				final ResultSet rs = s.executeQuery( "SELECT @@character_set_database, @@sql_mode" );
				if ( rs.next() ) {
					final String characterSet = rs.getString( 1 );
					final int collationIndex = characterSet.indexOf( '_' );
					// According to https://dev.mysql.com/doc/refman/8.0/en/charset-charsets.html
					switch ( collationIndex == -1 ? characterSet : characterSet.substring( 0, collationIndex ) ) {
						case "utf16":
						case "utf16le":
						case "utf32":
						case "utf8mb4":
						case "gb18030":
							break;
						case "utf8":
						case "utf8mb3":
						case "eucjpms":
						case "ujis":
							bytesPerCharacter = 3;
							break;
						case "ucs2":
						case "cp932":
						case "big5":
						case "euckr":
						case "gb2312":
						case "gbk":
						case "sjis":
							bytesPerCharacter = 2;
							break;
						default:
							bytesPerCharacter = 1;
					}
					// NO_BACKSLASH_ESCAPES
					final String sqlMode = rs.getString( 2 );
					noBackslashEscapes = sqlMode.toLowerCase().contains( "no_backslash_escapes" );
				}
			}
			catch (SQLException ex) {
				// Ignore
			}
		}
		// default to the dialect-specific configuration settings
		if ( bytesPerCharacter == null ) {
			bytesPerCharacter = ConfigurationHelper.getInt( BYTES_PER_CHARACTER, info.getConfigurationValues(), 4 );
		}
		if ( noBackslashEscapes == null ) {
			noBackslashEscapes = ConfigurationHelper.getBoolean(
					NO_BACKSLASH_ESCAPES,
					info.getConfigurationValues(),
					false
			);
		}
		return new MySQLServerConfiguration( bytesPerCharacter, noBackslashEscapes );
	}

	/**
	 * @deprecated Use {@link #fromDialectResolutionInfo} instead.
	 */
	@Deprecated( since = "6.4" )
	public static MySQLServerConfiguration fromDatabaseMetadata(DatabaseMetaData databaseMetaData) {
		int bytesPerCharacter = 4;
		boolean noBackslashEscapes = false;
		if ( databaseMetaData != null ) {
			try (java.sql.Statement s = databaseMetaData.getConnection().createStatement()) {
				final ResultSet rs = s.executeQuery( "SELECT @@character_set_database, @@sql_mode" );
				if ( rs.next() ) {
					final String characterSet = rs.getString( 1 );
					final int collationIndex = characterSet.indexOf( '_' );
					// According to https://dev.mysql.com/doc/refman/8.0/en/charset-charsets.html
					switch ( collationIndex == -1 ? characterSet : characterSet.substring( 0, collationIndex ) ) {
						case "utf16":
						case "utf16le":
						case "utf32":
						case "utf8mb4":
						case "gb18030":
							break;
						case "utf8":
						case "utf8mb3":
						case "eucjpms":
						case "ujis":
							bytesPerCharacter = 3;
							break;
						case "ucs2":
						case "cp932":
						case "big5":
						case "euckr":
						case "gb2312":
						case "gbk":
						case "sjis":
							bytesPerCharacter = 2;
							break;
						default:
							bytesPerCharacter = 1;
					}
					// NO_BACKSLASH_ESCAPES
					final String sqlMode = rs.getString( 2 );
					if ( sqlMode.toLowerCase().contains( "no_backslash_escapes" ) ) {
						noBackslashEscapes = true;
					}
				}
			}
			catch (SQLException ex) {
				// Ignore
			}
		}
		return new MySQLServerConfiguration( bytesPerCharacter, noBackslashEscapes );
	}
}
