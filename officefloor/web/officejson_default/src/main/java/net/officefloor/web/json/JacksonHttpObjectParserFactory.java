package net.officefloor.web.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.web.build.HttpObjectParser;
import net.officefloor.web.build.HttpObjectParserFactory;

/**
 * Jackson {@link HttpObjectParserFactory}.
 * 
 * @author Daniel Sagenschneider
 */
public class JacksonHttpObjectParserFactory implements HttpObjectParserFactory {

	/**
	 * {@link ObjectMapper}.
	 */
	private final ObjectMapper mapper;

	/**
	 * Instantiate.
	 * 
	 * @param mapper {@link ObjectMapper}.
	 */
	public JacksonHttpObjectParserFactory(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	/*
	 * ================= HttpObjectParserFactory ===============
	 */

	@Override
	public String getContentType() {
		return "application/json";
	}

	@Override
	public <T> HttpObjectParser<T> createHttpObjectParser(Class<T> objectClass) throws Exception {

		// Create the type for efficient execution
		JavaType javaType = this.mapper.constructType(objectClass);

		// Determine if can deserialise type
		if (!this.mapper.canDeserialize(javaType)) {
			return null;
		}

		// Can deserialise, so provide parser
		return new HttpObjectParser<T>() {

			@Override
			public String getContentType() {
				return JacksonHttpObjectParserFactory.this.getContentType();
			}

			@Override
			public Class<T> getObjectType() {
				return objectClass;
			}

			@Override
			@SuppressWarnings("unchecked")
			public T parse(ServerHttpConnection connection) throws HttpException {
				try {
					return (T) JacksonHttpObjectParserFactory.this.mapper.readValue(connection.getRequest().getEntity(),
							javaType);
				} catch (IOException ex) {
					throw new HttpException(ex);
				}
			}
		};
	}

}