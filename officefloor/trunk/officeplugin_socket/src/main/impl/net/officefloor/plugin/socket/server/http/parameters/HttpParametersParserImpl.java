/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2009 Daniel Sagenschneider
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.officefloor.plugin.socket.server.http.parameters;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

import net.officefloor.plugin.socket.server.http.HttpRequest;
import net.officefloor.plugin.stream.BufferStream;
import net.officefloor.plugin.stream.InputBufferStream;

/**
 * {@link HttpParametersParser} implementation.
 * 
 * @author Daniel Sagenschneider
 */
public class HttpParametersParserImpl implements HttpParametersParser {

	/*
	 * ====================== HttpParametersParser ============================
	 */

	@Override
	public void parseHttpParameters(HttpRequest request,
			HttpParametersParseHandler handler) throws HttpParametersException {

		// Create the temporary buffer (aids reducing object creation)
		TempBuffer tempBuffer = new TempBuffer();

		// Always load the parameters from the request URI
		String requestUri = request.getRequestURI();
		this.loadParameters(requestUri, handler, tempBuffer);

		// Only load parameters of body if a POST
		if ("POST".equalsIgnoreCase(request.getMethod())) {

			// Obtain the content encoding of the body
			// TODO handle content encoding

			// Obtain the content type of the body
			// TODO handle content type
			Charset charset = Charset.forName("UTF-8"); // default for now

			// Obtain the body data
			InputBufferStream body = request.getBody();
			int bodySize = (int) body.available();
			byte[] data = new byte[bodySize < 0 ? 0 : bodySize];
			try {
				InputStream browseStream = body.getBrowseStream();
				int index = 0;
				for (int value = browseStream.read(); value != BufferStream.END_OF_STREAM; value = browseStream
						.read()) {
					data[index++] = (byte) value;
				}
			} catch (IOException ex) {
				// Propagate failure
				throw new HttpParametersParseException(ex);
			}

			// Obtain the body data as string
			String bodyText = new String(data, charset);

			// Load the parameters from the body
			this.loadParameters(bodyText, handler, tempBuffer);
		}
	}

	/**
	 * Loads the parameters to the Object.
	 * 
	 * @param contents
	 *            Contents containing the parameter name/values to be parsed.
	 * @param handler
	 *            {@link HttpParametersParseHandler}.
	 * @param tempBuffer
	 *            {@link TempBuffer}.
	 * @throws HttpParametersException
	 *             If fails to parse the parameters.
	 */
	private void loadParameters(String contents,
			HttpParametersParseHandler handler, TempBuffer tempBuffer)
			throws HttpParametersException {

		// The implementation of this method reduces character array creations
		// and copying by using sub strings. This should both improve parsing
		// performance and reduce memory.

		// Values to aid in parsing
		boolean isPath = false;
		int nameBegin = 0; // start of contents
		int nameEnd = -1;
		int valueBegin = -1;
		int valueEnd = -1;
		boolean isRequireTranslate = false;

		// Iterate over the contents, loading the parameters
		for (int i = 0; i < contents.length(); i++) {
			char character = contents.charAt(i);

			// Handle based on character
			switch (character) {

			case '?':
				// If not processing path then just include
				if (!isPath) {
					// No longer processing path
					isPath = true;
					nameBegin = i + 1; // after '?'
				}
				break;

			case '=':
				// Flag to now obtain value
				nameEnd = i; // before '='
				valueBegin = i + 1; // after '='
				break;

			case '+': // space
			case '%': // escaping
				// Requires translating
				isRequireTranslate = true;
				break;

			case '&':
			case ';':
				// Have parameter name/value, so load
				valueEnd = i; // before terminator
				this.loadParameter(contents, nameBegin, nameEnd, valueBegin,
						valueEnd, isRequireTranslate, handler, tempBuffer);

				// Reset for next parameter name/value
				nameBegin = i + 1; // after terminator
				nameEnd = -1;
				valueBegin = -1;
				valueEnd = -1;
				isRequireTranslate = false;
				break;

			case '#':
				// At end of parameters as have fragment
				valueEnd = i; // before terminator
				if (valueBegin > 0) {
					// Have name/value before fragment so load
					this.loadParameter(contents, nameBegin, nameEnd,
							valueBegin, valueEnd, isRequireTranslate, handler,
							tempBuffer);
				}
				return; // stop parsing
			}
		}

		// Determine if final parameter to load (not terminated)
		if (valueBegin > 0) {
			// Load the final parameter
			valueEnd = contents.length();
			this.loadParameter(contents, nameBegin, nameEnd, valueBegin,
					valueEnd, isRequireTranslate, handler, tempBuffer);
		}
	}

	/**
	 * Loads the parameter to the Object.
	 * 
	 * @param contents
	 *            Contents being parsed that contains the parameter name/values.
	 * @param nameBegin
	 *            Beginning index of name in contents.
	 * @param nameEnd
	 *            Ending index of name in contents.
	 * @param valueBegin
	 *            Beginning index of value in contents.
	 * @param valueEnd
	 *            Ending index of value in contents.
	 * @param isRequireTranslate
	 *            Indicates if a translation is required. {@link Method} array
	 *            to load the parameters to the Object.
	 * @param handler
	 *            {@link HttpParametersParseHandler}.
	 * @param tempBuffer
	 *            {@link TempBuffer}.
	 * @throws HttpParametersException
	 *             If fails to parse the parameters.
	 */
	private void loadParameter(String contents, int nameBegin, int nameEnd,
			int valueBegin, int valueEnd, boolean isRequireTranslate,
			HttpParametersParseHandler handler, TempBuffer tempBuffer)
			throws HttpParametersException {

		// Ensure valid
		if ((nameEnd < 0) || (valueBegin < 0) || (valueEnd < 0)) {
			throw new HttpParametersParseException(
					"Invalid HTTP contents (name " + nameBegin + "," + nameEnd
							+ "  value " + valueBegin + "," + valueEnd + "): "
							+ contents);
		}

		// Obtain the raw name and value
		String rawName = contents.substring(nameBegin, nameEnd);
		String rawValue = contents.substring(valueBegin, valueEnd);

		// Obtain the name and value
		String name = (isRequireTranslate ? this.translate(rawName, tempBuffer)
				: rawName);
		String value = (isRequireTranslate ? this.translate(rawValue,
				tempBuffer) : rawValue);

		// Handle the parameter
		handler.handleHttpParameter(name, value);
	}

	/**
	 * Enum providing the escape state for translating.
	 */
	private static enum EscapeState {
		NONE, HIGH, LOW
	}

	/**
	 * Translates the parameter text.
	 * 
	 * @param parameterText
	 *            Text to be translated.
	 * @param tempBuffer
	 *            {@link TempBuffer}.
	 * @return Translated text.
	 * @throws HttpParametersParseException
	 *             If fails to translate.
	 */
	private String translate(String parameterText, TempBuffer tempBuffer)
			throws HttpParametersParseException {

		// Obtain the temporary buffer
		char[] buffer = tempBuffer.buffer;

		// Ensure temporary buffer large enough
		if ((buffer == null) || (buffer.length < parameterText.length())) {
			// Increase buffer size (translation should not be bigger)
			buffer = new char[parameterText.length()];

			// Make available for further translations
			tempBuffer.buffer = buffer;
		}

		// Iterate over parameter text translating
		int charIndex = 0;
		EscapeState escape = EscapeState.NONE;
		byte highBits = 0;
		for (int i = 0; i < parameterText.length(); i++) {
			char character = parameterText.charAt(i);

			// Handle on whether escaping
			switch (escape) {
			case NONE:
				// Not escaped so handle character
				switch (character) {
				case '+':
					// Translate to space
					buffer[charIndex++] = ' ';
					break;

				case '%':
					// Escaping
					escape = EscapeState.HIGH;
					break;

				default:
					// No translation needed of character
					buffer[charIndex++] = character;
					break;
				}
				break;

			case HIGH:
				// Obtain the high bits for escaping
				highBits = this.translateEscapedCharToBits(character);
				escape = EscapeState.LOW;
				break;

			case LOW:
				// Have low bits, so obtain escaped character
				byte lowBits = this.translateEscapedCharToBits(character);
				character = (char) ((highBits << 4) | lowBits);

				// Load the character and no longer escaped
				buffer[charIndex++] = character;
				escape = EscapeState.NONE;
				break;
			}
		}

		// Should always be in non-escape state after translating
		if (escape != EscapeState.NONE) {
			throw new HttpParametersParseException(
					"Invalid parameter text as escaping not complete: '"
							+ parameterText + "'");
		}

		// Return the translated text
		return new String(buffer, 0, charIndex);
	}

	/**
	 * Translates the character to the 4 bits as per escaping of HTTP.
	 * 
	 * @param character
	 *            Character to translate.
	 * @return Corresponding 4 bits for character.
	 * @throws HttpParametersParseException
	 *             If invalid character for escaping.
	 */
	private byte translateEscapedCharToBits(char character)
			throws HttpParametersParseException {

		// Obtain the bits for the character
		int bits;
		if (('0' <= character) && (character <= '9')) {
			bits = character - '0';
		} else if (('A' <= character) && (character <= 'F')) {
			bits = (character - 'A') + 0xA;
		} else if (('a' <= character) && (character <= 'f')) {
			bits = (character - 'a') + 0xA;
		} else {
			// Invalid character for escaping
			throw new HttpParametersParseException(
					"Invalid character for escaping: " + character);
		}

		// Return the bits
		return (byte) bits;
	}

	/**
	 * Temporary buffer.
	 */
	private static class TempBuffer {

		/**
		 * Buffer.
		 */
		public char[] buffer = null;
	}

}