/*-
 * #%L
 * JWT Security
 * %%
 * Copyright (C) 2005 - 2020 Daniel Sagenschneider
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package net.officefloor.web.jwt.jwks;

import java.math.BigInteger;
import java.security.Key;
import java.util.Base64;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Context for the {@link JwksKeyParser}.
 * 
 * @author Daniel Sagenschneider
 */
public interface JwksKeyParserContext {

	/**
	 * Obtains the {@link JsonNode} containing the {@link Key} information.
	 * 
	 * @return {@link JsonNode} containing the {@link Key} information.
	 */
	JsonNode getKeyNode();

	/**
	 * Obtains the key type.
	 * 
	 * @return Key type.
	 */
	default String getKty() {
		return this.getString(this.getKeyNode(), "kty", null);
	}

	/**
	 * Convenience method to obtain long value from key {@link JsonNode}.
	 * 
	 * @param fieldName Field name.
	 * @return Long value from key {@link JsonNode} or <code>null</code>.
	 */
	default Long getLong(String fieldName) {
		return this.getLong(this.getKeyNode(), fieldName, null);
	}

	/**
	 * Obtains the field long value.
	 * 
	 * @param node         {@link JsonNode}.
	 * @param fieldName    Field name.
	 * @param defaultValue Default value.
	 * @return Field long value.
	 */
	default Long getLong(JsonNode node, String fieldName, Long defaultValue) {
		return this.getValue(node, fieldName, defaultValue, (field) -> field.asLong(defaultValue));
	}

	/**
	 * Convenience method to obtain string value from key {@link JsonNode}.
	 * 
	 * @param fieldName Field name.
	 * @return String value from key {@link JsonNode} or <code>null</code>.
	 */
	default String getString(String fieldName) {
		return this.getString(this.getKeyNode(), fieldName, null);
	}

	/**
	 * Obtains the field string value.
	 * 
	 * @param node         {@link JsonNode}.
	 * @param fieldName    Field name.
	 * @param defaultValue Default value.
	 * @return Field string value.
	 */
	default String getString(JsonNode node, String fieldName, String defaultValue) {
		return this.getValue(node, fieldName, defaultValue, (field) -> field.asText(defaultValue));
	}

	/**
	 * Convenience method to obtain {@link BigInteger} value from key
	 * {@link JsonNode}.
	 * 
	 * @param fieldName Field name.
	 * @return {@link BigInteger} value from key {@link JsonNode} or
	 *         <code>null</code>.
	 */
	default BigInteger getBase64BigInteger(String fieldName) {
		return this.getBase64BigInteger(this.getKeyNode(), fieldName, null);
	}

	/**
	 * Obtains the field {@link BigInteger} value.
	 * 
	 * @param node         {@link JsonNode}.
	 * @param fieldName    Field name.
	 * @param defaultValue Default value.
	 * @return Field {@link BigInteger} value.
	 */
	default BigInteger getBase64BigInteger(JsonNode node, String fieldName, BigInteger defaultValue) {
		return this.getValue(node, fieldName, defaultValue, (field) -> {
			String base64Value = field.asText();
			byte[] bytes = Base64.getUrlDecoder().decode(base64Value);
			return new BigInteger(bytes);
		});
	}

	/**
	 * Convenience method to obtain bytes from key {@link JsonNode}.
	 * 
	 * @param fieldName Field name.
	 * @return Bytes from key {@link JsonNode} or <code>null</code>.
	 */
	default byte[] getBase64Bytes(String fieldName) {
		return this.getBase64Bytes(this.getKeyNode(), fieldName, null);
	}

	/**
	 * Obtains the field byes.
	 * 
	 * @param node         {@link JsonNode}.
	 * @param fieldName    Field name.
	 * @param defaultValue Default value.
	 * @return Field {@link BigInteger} value.
	 */
	default byte[] getBase64Bytes(JsonNode node, String fieldName, byte[] defaultValue) {
		return this.getValue(node, fieldName, defaultValue, (field) -> {
			String base64Value = field.asText();
			return Base64.getUrlDecoder().decode(base64Value);
		});
	}

	/**
	 * Obtains the field value from the {@link JsonNode}.
	 * 
	 * @param node         {@link JsonNode}.
	 * @param fieldName    Field name.
	 * @param defaultValue Default value. May be <code>null</code>.
	 * @param getValue     Obtains the value from the {@link JsonNode} field.
	 * @return Field value from the {@link JsonNode}.
	 */
	default <T> T getValue(JsonNode node, String fieldName, T defaultValue, Function<JsonNode, T> getValue) {
		JsonNode field = node.get(fieldName);
		if (field == null) {
			return defaultValue;
		}
		return getValue.apply(field);
	}

}
