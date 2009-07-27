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
package net.officefloor.plugin.socket.server.http.parse.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import net.officefloor.plugin.socket.server.http.HttpHeader;
import net.officefloor.plugin.socket.server.http.HttpRequest;
import net.officefloor.plugin.socket.server.http.parse.HttpRequestParser;
import net.officefloor.plugin.socket.server.http.parse.ParseException;
import net.officefloor.plugin.socket.server.http.source.HttpStatus;
import net.officefloor.plugin.stream.BufferStream;
import net.officefloor.plugin.stream.InputBufferStream;
import net.officefloor.plugin.stream.OutputBufferStream;
import net.officefloor.plugin.stream.impl.BufferStreamImpl;
import net.officefloor.plugin.stream.squirtfactory.NotCreateBufferSquirtFactory;

/**
 * Parser for a HTTP request.
 *
 * @author Daniel Sagenschneider
 */
public class HttpRequestParserImpl implements HttpRequestParser {

	/*
	 * ASCII values.
	 */
	private static final Charset US_ASCII = Charset.forName("US-ASCII");

	private static byte UsAscii(char character) {
		return UsAscii(String.valueOf(character))[0];
	}

	private static byte[] UsAscii(String text) {
		return text.getBytes(US_ASCII);
	}

	private static final byte A = UsAscii('A');

	private static final byte Z = UsAscii('Z');

	private static final byte a = UsAscii('a');

	private static final byte z = UsAscii('z');

	private static final byte SP = UsAscii(' ');

	private static final byte HT = UsAscii('\t');

	private static final byte CR = UsAscii('\r');

	private static final byte LF = UsAscii('\n');

	private static final byte COLON = UsAscii(':');

	/**
	 * Header name for the Content-Length.
	 */
	private static final String HEADER_NAME_CONTENT_LENGTH = "CONTENT-LENGTH";

	/**
	 * Determines if character is a letter of the alphabet.
	 *
	 * @param character
	 *            ASCII character.
	 * @return <code>true</code> if letter of alphabet.
	 */
	private static boolean isAlpha(byte character) {
		return ((character >= A) && (character <= Z))
				|| ((character >= a) && (character <= z));
	}

	/**
	 * Determines if character is a control.
	 *
	 * @param character
	 *            ASCII character.
	 * @return <code>true</code> if letter is control.
	 */
	private static boolean isCtl(byte character) {
		return (character <= 31) || (character == 127);
	}

	/**
	 * Determines if character is a white space (space or tab).
	 *
	 * @param character
	 *            ASCII character.
	 * @return <code>true</code> if {@link #SP} or {@link #HT}.
	 */
	private static boolean isWs(byte character) {
		return (character == SP) || (character == HT);
	}

	/**
	 * Indicates the state of parsing.
	 */
	private static enum ParseState {
		START, METHOD, METHOD_PATH_SEPARATION, PATH, PATH_VERSION_SEPARATION, VERSION,

		HEADER_CR, HEADER_CR_NAME_SEPARATION, HEADER_NAME, HEADER_NAME_VALUE_SEPARATION, HEADER_VALUE,

		BODY_CR, BODY
	};

	/**
	 * {@link ParseState} which starts with the HTTP method.
	 */
	private ParseState parseState = ParseState.START;

	/**
	 * Maximum length of the body (entity).
	 */
	private final long maxBodyLength;

	/**
	 * Content length value for request.
	 */
	private long contentLength = -1;

	/**
	 * Length of current text of the {@link HttpRequest} being parsed.
	 */
	private int textLength = 0;

	private String text_method = "";

	private String text_path = "";

	private String text_version = "";

	private String text_headerName = "";

	private List<HttpHeader> headers = new LinkedList<HttpHeader>();

	private InputBufferStream body = null;

	/**
	 * Initiate.
	 *
	 * @param maxBodyLength
	 *            Maximum length of the body buffer. Requests with bodies
	 *            greater that this will fail parsing.
	 */
	public HttpRequestParserImpl(long maxBodyLength) {
		this.maxBodyLength = maxBodyLength;
	}

	/**
	 * Transforms the content to a {@link String}.
	 *
	 * @param inputBufferStream
	 *            {@link InputBufferStream} containing the content.
	 * @param tempBuffer
	 *            Temporary char array to reduce array creation.
	 * @param HTTP
	 *            error status on length being too long.
	 * @param HTTP
	 *            error message on length being too long.
	 * @return Transformed {@link String}.
	 * @throws IOException
	 *             If fails to transform.
	 * @throws ParseException
	 *             If text is too long.
	 */
	private String transformToString(InputBufferStream inputBufferStream,
			char[] tempBuffer, int httpErrorStatus, String httpErrorMessage)
			throws IOException, ParseException {

		// TODO taking into account %HH

		// Ensure text is too long
		if (this.textLength > tempBuffer.length) {
			throw new ParseException(httpErrorStatus, httpErrorMessage);
		}

		// Load the character values into the temporary buffer
		InputStream inputStream = inputBufferStream.getInputStream();
		for (int i = 0; i < this.textLength; i++) {
			char character = (char) inputStream.read();
			tempBuffer[i] = character;
		}

		// Create the string containing the text
		String text = new String(tempBuffer, 0, this.textLength);

		// Reset text length
		this.textLength = 0;

		// Return the String
		return text;
	}

	/*
	 * ====================== HttpRequestParser ===========================
	 */

	@Override
	public boolean parse(InputBufferStream inputBufferStream, char[] tempBuffer)
			throws IOException, ParseException {

		// TODO take into account %HH

		// Determine if parsing head
		if (this.parseState != ParseState.BODY) {

			// Loop parsing available content up to body
			InputStream browse = inputBufferStream.getBrowseStream();
			LOOP: for (int value = browse.read(); value != -1; value = browse
					.read()) {
				byte character = (byte) value;

				// TODO remove
				System.out.print(new String(new byte[] { character }, 0, 1));

				// Parse the character of the HTTP request
				switch (this.parseState) {
				case START:
					// Ignore leading white space and blank lines
					if ((character == CR) || (character == LF)
							|| isWs(character)) {
						// Skip over white space or end of line
						inputBufferStream.skip(1);
						break;
					}
					this.parseState = ParseState.METHOD;

				case METHOD:
					if (isAlpha(character)) {
						// Append character of the method
						this.textLength++;
					} else if (isWs(character)) {
						// Method name read in (consumes bytes)
						this.text_method = this.transformToString(
								inputBufferStream, tempBuffer, HttpStatus._400,
								"Method too long");

						// Skip white space and move to path separation
						inputBufferStream.skip(1);
						this.parseState = ParseState.METHOD_PATH_SEPARATION;
					} else {
						// Unexpected character
						throw new ParseException(HttpStatus._400,
								"Unexpected character in method '" + character
										+ "'");
					}
					break;

				case METHOD_PATH_SEPARATION:
					// Ignore separating linear white space
					if (isWs(character)) {
						// Skip over white space
						inputBufferStream.skip(1);
						break;
					}
					this.parseState = ParseState.PATH;

				case PATH:
					if (isWs(character)) {
						// Path read in (consumes bytes)
						this.text_path = this.transformToString(
								inputBufferStream, tempBuffer, HttpStatus._414,
								"Request-URI Too Long");

						// Skip white space and move to version separation
						inputBufferStream.skip(1);
						this.parseState = ParseState.PATH_VERSION_SEPARATION;
					} else if (!isCtl(character)) {
						// Append character to path
						this.textLength++;
					} else {
						// Unexpected character
						throw new ParseException(HttpStatus._400,
								"Unexpected character in path '" + character
										+ "'");
					}
					break;

				case PATH_VERSION_SEPARATION:
					// Ignore separating linear white space
					if (isWs(character)) {
						// Skip over white space
						inputBufferStream.skip(1);
						break;
					}
					this.parseState = ParseState.VERSION;

				case VERSION:
					if (character == CR) {
						// Version read in (consumes bytes)
						this.text_version = this.transformToString(
								inputBufferStream, tempBuffer, HttpStatus._400,
								"Version too long");

						// Skip CR and move to header
						inputBufferStream.skip(1);
						this.parseState = ParseState.HEADER_CR;
					} else {
						// Append character to version
						this.textLength++;
					}
					break;

				case HEADER_CR:
					if (character == LF) {
						// Skip over LF
						inputBufferStream.skip(1);

						// Expecting LF, so continue on with header
						this.parseState = ParseState.HEADER_CR_NAME_SEPARATION;
						break;
					}
					throw new ParseException(HttpStatus._400,
							"Should expect LF after a CR for status line");

				case HEADER_CR_NAME_SEPARATION:
					if (character == CR) {
						// Skip CR and move to body
						inputBufferStream.skip(1);
						this.parseState = ParseState.BODY_CR;
						break;
					} else if (isWs(character)) {
						// Continue previous header value
						this.parseState = ParseState.HEADER_NAME_VALUE_SEPARATION;
						break;
					} else {
						// New header
						this.parseState = ParseState.HEADER_NAME;
					}

				case HEADER_NAME:
					if (character == COLON) {
						// Header name obtained
						this.text_headerName = this.transformToString(
								inputBufferStream, tempBuffer, HttpStatus._400,
								"Header name too long");

						// Skip colon and move to name value separation
						inputBufferStream.skip(1);
						this.parseState = ParseState.HEADER_NAME_VALUE_SEPARATION;
					} else if (!isCtl(character)) {
						// Append the header name character
						this.textLength++;
					} else {
						// Unknown header name character
						throw new ParseException(HttpStatus._400,
								"Unknown header name character '" + character
										+ "'");
					}
					break;

				case HEADER_NAME_VALUE_SEPARATION:
					// Ignore separating linear white space
					if (isWs(character)) {
						// Skip the white space
						inputBufferStream.skip(1);
						break;
					}
					this.parseState = ParseState.HEADER_VALUE;

				case HEADER_VALUE:
					if (character == CR) {
						// Header name and value obtained
						String headerValue = this.transformToString(
								inputBufferStream, tempBuffer, HttpStatus._400,
								"Header value too long");
						this.headers
								.add(new HttpHeaderImpl(this.text_headerName
										.trim(), headerValue.trim()));

						// Skip CR and move to next header
						inputBufferStream.skip(1);
						this.parseState = ParseState.HEADER_CR;
					} else if (!isCtl(character)) {
						// Append the header value character
						this.textLength++;
					} else {
						// Unknown header value character
						throw new ParseException(HttpStatus._400,
								"Unknown header value character '" + character
										+ "'");
					}
					break;

				case BODY_CR:
					// Must have LF after CR for body
					if (character != LF) {
						throw new ParseException(HttpStatus._400,
								"Should expect LR after a CR after header");
					} else {
						// Skip the LF
						inputBufferStream.skip(1);
					}

					// Attempt to obtain the Content-Length
					String contentLengthValue = null;
					for (HttpHeader header : this.headers) {
						if (HEADER_NAME_CONTENT_LENGTH.equalsIgnoreCase(header
								.getName())) {
							contentLengthValue = header.getValue();
							break;
						}
					}

					// Ensure valid Content-Length
					if (contentLengthValue != null) {
						// Should always be able to convert to an integer
						try {
							this.contentLength = Long
									.parseLong(contentLengthValue);
						} catch (NumberFormatException ex) {
							throw new ParseException(HttpStatus._411,
									"Content-Length header value must be an integer");
						}
					}

					// Ensure the Content-Length within limits
					if (this.contentLength > 0) {
						if (this.contentLength > this.maxBodyLength) {
							throw new ParseException(HttpStatus._413,
									"Request entity must be less than maximum of "
											+ this.maxBodyLength + " bytes");
						}
					}

					// Ensure POST and PUT methods provided Content-Length
					if (("POST".equalsIgnoreCase(this.text_method))
							|| ("PUT".equalsIgnoreCase(this.text_method))) {
						if (this.contentLength < 0) {
							throw new ParseException(HttpStatus._411,
									"Must provide Content-Length header for "
											+ this.text_method);
						}
					}

					// Have LF and headers valid, so continue onto body
					this.parseState = ParseState.BODY;
					break LOOP;
				}
			}
		}

		// Only able to be complete if parsing the body
		if (this.parseState != ParseState.BODY) {

			// Determine if text too long
			if (this.textLength > tempBuffer.length) {
				switch (this.parseState) {
				case METHOD:
					throw new ParseException(HttpStatus._400, "Method too long");
				case PATH:
					throw new ParseException(HttpStatus._414,
							"Request-URI Too Long");
				case VERSION:
					throw new ParseException(HttpStatus._400,
							"Version too long");
				case HEADER_NAME:
					throw new ParseException(HttpStatus._400,
							"Header name too long");
				case HEADER_VALUE:
					throw new ParseException(HttpStatus._400,
							"Header value too long");
				}
			}

			// Text within size but request not fully received
			return false;
		}

		// Ensure all of the body is received
		if (this.contentLength > 0) {
			long bodyLength = inputBufferStream.available();
			if (this.contentLength > bodyLength) {
				// Body not yet fully received
				return false;
			}
		}

		// Create the body
		BufferStream bodyStream = new BufferStreamImpl(
				new NotCreateBufferSquirtFactory());
		OutputBufferStream bodyOutputStream = bodyStream
				.getOutputBufferStream();
		inputBufferStream.read((int) this.contentLength, bodyOutputStream);
		bodyOutputStream.close();
		this.body = bodyStream.getInputBufferStream();

		// All of request received and parsed
		return true;
	}

	@Override
	public void reset() {
		// TODO Implement HttpRequestParser.reset
		throw new UnsupportedOperationException("HttpRequestParser.reset");
	}

	@Override
	public String getMethod() {
		return this.text_method;
	}

	@Override
	public String getRequestURI() {
		return this.text_path;
	}

	@Override
	public String getHttpVersion() {
		return this.text_version;
	}

	@Override
	public List<HttpHeader> getHeaders() {
		return this.headers;
	}

	@Override
	public InputBufferStream getBody() {
		return this.body;
	}

}