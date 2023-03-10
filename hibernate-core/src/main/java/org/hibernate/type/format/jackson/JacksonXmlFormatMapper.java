/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.format.jackson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * @author Christian Beikov
 */
public final class JacksonXmlFormatMapper implements FormatMapper {

	public static final String SHORT_NAME = "jackson-xml";

	private final ObjectMapper objectMapper;

	public JacksonXmlFormatMapper() {
		this( new XmlMapper() );
	}

	public JacksonXmlFormatMapper(ObjectMapper objectMapper) {
		// needed to automatically find and register Jackson's jsr310 module for java.time support
		objectMapper.findAndRegisterModules();
		objectMapper.configure( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false );
		this.objectMapper = objectMapper;
	}

	@Override
	public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
		if ( javaType.getJavaType() == String.class || javaType.getJavaType() == Object.class ) {
			return (T) charSequence.toString();
		}
		if ( javaType.getJavaTypeClass().isArray() && javaType.getJavaTypeClass().getComponentType() == String.class ) {
			final StringWrapper[] array = (StringWrapper[]) readValueFromString(
					charSequence,
					javaType,
					StringWrapper[].class
			);
			final List<String> list = new ArrayList<>( array.length );
			for ( StringWrapper sw : array ) {
				list.add( sw.getValue() );
			}
			//noinspection unchecked
			return (T) list.toArray( String[]::new );
		}
		return readValueFromString( charSequence, javaType, javaType.getJavaType() );
	}

	private <T> T readValueFromString(CharSequence charSequence, JavaType<T> javaType, Type type) {
		try {
			return objectMapper.readValue( charSequence.toString(), objectMapper.constructType( type ) );
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException( "Could not deserialize string to java type: " + javaType, e );
		}
	}

	@Override
	public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
		if ( javaType.getJavaType() == String.class || javaType.getJavaType() == Object.class ) {
			return (String) value;
		}
		else if ( javaType.getJavaTypeClass().isArray() ) {
			if ( javaType.getJavaTypeClass().getComponentType() == String.class ) {
				final String[] array = (String[]) value;
				final List<StringWrapper> list = new ArrayList<>( array.length );
				for ( String s : array ) {
					list.add( new StringWrapper( s ) );
				}
				return writeValueAsString( list.toArray( StringWrapper[]::new ), javaType, StringWrapper[].class );
			}
			else if ( javaType.getJavaTypeClass().getComponentType().isEnum() ) {
				// for enum arrays we need to explicitly pass Byte[] as the writer type
				return writeValueAsString( value, javaType, Byte[].class );
			}
		}
		return writeValueAsString( value, javaType, javaType.getJavaType() );
	}

	private <T> String writeValueAsString(Object value, JavaType<T> javaType, Type type) {
		try {
			return objectMapper.writerFor( objectMapper.constructType( type ) ).writeValueAsString( value );
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException( "Could not serialize object of java type: " + javaType, e );
		}
	}

	@JsonInclude( JsonInclude.Include.NON_NULL )
	private static class StringWrapper {
		private final String value;

		@JsonCreator
		public StringWrapper(@JsonProperty( "value" ) String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}
}
