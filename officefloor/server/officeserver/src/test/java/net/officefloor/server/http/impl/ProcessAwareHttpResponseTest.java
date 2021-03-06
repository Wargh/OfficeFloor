/*-
 * #%L
 * HTTP Server
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

package net.officefloor.server.http.impl;

import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import net.officefloor.frame.api.escalate.Escalation;
import net.officefloor.frame.test.Closure;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.server.http.HttpHeader;
import net.officefloor.server.http.HttpHeaderValue;
import net.officefloor.server.http.HttpMethod;
import net.officefloor.server.http.HttpResponse;
import net.officefloor.server.http.HttpResponseCookie;
import net.officefloor.server.http.HttpResponseWriter;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.server.http.HttpVersion;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.server.http.WritableHttpCookie;
import net.officefloor.server.http.WritableHttpHeader;
import net.officefloor.server.http.mock.MockManagedObjectContext;
import net.officefloor.server.http.mock.MockStreamBufferPool;
import net.officefloor.server.stream.ServerOutputStream;
import net.officefloor.server.stream.ServerWriter;
import net.officefloor.server.stream.StreamBuffer;

/**
 * Tests the {@link ProcessAwareHttpResponse}.
 * 
 * @author Daniel Sagenschneider
 */
public class ProcessAwareHttpResponseTest extends OfficeFrameTestCase implements HttpResponseWriter<ByteBuffer> {

	/**
	 * {@link MockStreamBufferPool}.
	 */
	private final MockStreamBufferPool bufferPool = new MockStreamBufferPool();

	/**
	 * {@link ProcessAwareServerHttpConnectionManagedObject}.
	 */
	private final ProcessAwareServerHttpConnectionManagedObject<ByteBuffer> connection = new ProcessAwareServerHttpConnectionManagedObject<ByteBuffer>(
			new HttpServerLocationImpl(), false, () -> HttpMethod.GET, () -> "/", HttpVersion.HTTP_1_1, null, null,
			null, null, true, this, this.bufferPool);

	/**
	 * {@link ProcessAwareHttpResponse} to test.
	 */
	private final ProcessAwareHttpResponse<ByteBuffer> response = new ProcessAwareHttpResponse<ByteBuffer>(
			this.connection, HttpVersion.HTTP_1_1, new MockManagedObjectContext());

	/**
	 * Ensure correct defaults on writing.
	 */
	public void testWriteDefaults() throws IOException {

		// Write the response
		this.response.flushResponseToHttpResponseWriter(null);

		// Ensure correct defaults
		assertEquals("Incorrect version", HttpVersion.HTTP_1_1, this.version);
		assertEquals("Incorrect status", HttpStatus.NO_CONTENT, this.status);
		assertNull("Should be no headers", this.httpHeader);
		assertNull("Should be no cookeis", this.httpCookie);
		assertNull("Should be no entity data", this.contentHeadStreamBuffer);
	}

	/**
	 * Ensure can not provide <code>null</code> values.
	 */
	public void testDisallowNulls() throws IOException {

		Consumer<Runnable> checkNull = (run) -> {
			try {
				run.run();
				fail("Should not be able to set null value");
			} catch (IllegalArgumentException ex) {
				// Correct as null
			}
		};

		// Ensure disallow null values
		checkNull.accept(() -> this.response.setStatus(null));
		checkNull.accept(() -> this.response.setVersion(null));
	}

	/**
	 * Test mutating the {@link HttpResponse}.
	 */
	public void testChangeStatus() throws IOException {

		// Validate mutating status
		assertEquals("Incorrect default status", HttpStatus.OK, this.response.getStatus());
		this.response.setStatus(HttpStatus.CONTINUE);
		assertEquals("Should have status changed", HttpStatus.CONTINUE, this.response.getStatus());

		// Ensure writes the changes status
		this.response.flushResponseToHttpResponseWriter(null);
		assertEquals("Should have changed status", HttpStatus.CONTINUE, this.status);
	}

	/**
	 * Test mutating the {@link HttpVersion}.
	 */
	public void testChangeVersion() throws IOException {

		// Validate mutating version
		assertEquals("Incorrect default version", HttpVersion.HTTP_1_1, this.response.getVersion());
		this.response.setVersion(HttpVersion.HTTP_1_0);
		assertEquals("Should have version changed", HttpVersion.HTTP_1_0, this.response.getVersion());

		// Ensure writes the changed version
		this.response.flushResponseToHttpResponseWriter(null);
		assertEquals("Should have changed version", HttpVersion.HTTP_1_0, this.version);
	}

	/**
	 * Add a {@link HttpHeader}.
	 */
	public void testAddHeader() throws IOException {

		// Add header
		this.response.getHeaders().addHeader("test", "value");

		// Ensure writes have HTTP header
		this.response.flushResponseToHttpResponseWriter(null);
		assertNotNull("Should have header", this.httpHeader);
		assertEquals("Incorrect header name", "test", this.httpHeader.getName());
		assertEquals("Incorrect header value", "value", this.httpHeader.getValue());
		assertNull("Should be no further headers", this.httpHeader.next);
	}

	/**
	 * Set a {@link HttpResponseCookie}.
	 */
	public void testSetCookie() throws IOException {

		// Set the cookie
		HttpResponseCookie initial = this.response.getCookies().setCookie("test", "initial");
		HttpResponseCookie value = this.response.getCookies().setCookie("test", "value");
		assertSame("Should be same cookie as same name", initial, value);

		// Ensure writes have HTTP header
		this.response.flushResponseToHttpResponseWriter(null);
		assertNotNull("Should have cookie", this.httpCookie);
		assertEquals("Incorrect cookie name", "test", this.httpCookie.getName());
		assertEquals("Incorrect cookie value", "value", this.httpCookie.getValue());
		assertNull("Should be no further cookies", this.httpCookie.next);
	}

	/**
	 * Ensure no HTTP entity content.
	 */
	public void testEmptyContent() throws IOException {

		// Send without content
		this.response.flushResponseToHttpResponseWriter(null);

		// Ensure no content
		assertEquals("Should be no content", 0, this.contentLength);
		assertNull("No content-type, as no content", this.contentType);
		assertNull("No content", this.contentHeadStreamBuffer);
	}

	/**
	 * Outputs the HTTP entity content.
	 */
	public void testOutputEntityContent() throws IOException {

		// Write some content
		ServerOutputStream output = this.response.getEntity();
		output.write(1);

		// Ensure same output stream provided
		assertSame("Should obtain same output stream", output, this.response.getEntity());

		// Send the response
		this.response.flushResponseToHttpResponseWriter(null);

		// Ensure correct content details
		assertEquals("Incorrect Content-Length", 1, this.contentLength);
		assertEquals("Incorrect Content-Type", "application/octet-stream", this.contentType.getValue());

		// Ensure correct content
		MockStreamBufferPool.releaseStreamBuffers(this.contentHeadStreamBuffer);
		InputStream input = MockStreamBufferPool.createInputStream(this.contentHeadStreamBuffer);
		assertEquals("Should be the written byte", 1, input.read());
		assertEquals("Should be no further content", -1, input.read());
	}

	/**
	 * Ensure default the <code>Content-Type</code> appropriately.
	 */
	public void testDefaultContentTypes() throws IOException {

		// Ensure correct default
		assertEquals("Incorrect default Content-Type", "application/octet-stream", this.response.getContentType());

		// Obtain writer (so becomes text output)
		this.response.getEntityWriter();
		assertEquals("Incorrect text default Content-Type", "text/plain", this.response.getContentType());
	}

	/**
	 * Ensure derive the <code>Content-Type</code> appropriately.
	 */
	public void testDeriveContentType() throws IOException {

		// Ensure keep defaulting charset
		this.response.setContentType("text/html", null);
		assertEquals("text/html", this.response.getContentType());
		assertEquals(ServerHttpConnection.DEFAULT_HTTP_ENTITY_CHARSET, this.response.getContentCharset());

		// Change charset
		Charset charset = Charset.forName("UTF-16");
		this.response.setContentType("application/json", charset);
		assertEquals("application/json; charset=UTF-16", this.response.getContentType());
		assertEquals(charset, this.response.getContentCharset());

		// Ensure specify value
		this.response.setContentType(new HttpHeaderValue("application/xml"), charset);
		assertEquals("application/xml", this.response.getContentType());
		assertEquals(charset, this.response.getContentCharset());
	}

	/**
	 * Writes the HTTP entity content.
	 */
	public void testWriteEntityContent() throws IOException {

		// Write some content
		ServerWriter writer = this.response.getEntityWriter();
		writer.write("TEST");

		// Should not be able to change the charset (as committed with writer)
		Charset charset = Charset.forName("UTF-16");
		assertNotEquals("Should not be UTF-16", charset, this.response.getContentCharset());
		try {
			this.response.setContentType("text/html", charset);
			fail("Should not be able to change the charset");
		} catch (IOException ex) {
			assertEquals("Can not change Content-Type. Committed to writing text/plain (charset UTF-8)",
					ex.getMessage());
		}

		// Ensure same writer provided
		assertSame("Should obtain same writer", writer, this.response.getEntityWriter());

		// Send the response
		this.response.flushResponseToHttpResponseWriter(null);

		// Ensure correct content details
		assertEquals("Incorrect Content-Length",
				"TEST".getBytes(ServerHttpConnection.DEFAULT_HTTP_ENTITY_CHARSET).length, this.contentLength);
		assertEquals("Incorrect Content-Type", "text/plain", this.contentType.getValue());

		// Ensure correct content
		MockStreamBufferPool.releaseStreamBuffers(this.contentHeadStreamBuffer);
		String content = MockStreamBufferPool.getContent(this.contentHeadStreamBuffer,
				ServerHttpConnection.DEFAULT_HTTP_ENTITY_CHARSET);
		assertEquals("Incorrect sent content", "TEST", content);
	}

	/**
	 * Ensure able to write the HTTP entity with different {@link Charset}.
	 */
	public void testWriteEntityInUTF16() throws IOException {

		// Ensure different charset
		Charset charset = Charset.forName("UTF-16");
		assertNotEquals("Should not be UTF-16", charset, this.response.getContentCharset());

		// Specify the charset
		this.response.setContentType((String) null, charset);

		// Write some content
		ServerWriter writer = this.response.getEntityWriter();
		writer.write("TEST");

		// Send the response
		this.response.flushResponseToHttpResponseWriter(null);

		// Ensure correct content details
		assertEquals("Incorrect Content-Length", "TEST".getBytes(charset).length, this.contentLength);
		assertEquals("Incorrect Content-Type", "text/plain; charset=" + charset.name(), this.contentType.getValue());

		// Ensure correct content
		MockStreamBufferPool.releaseStreamBuffers(this.contentHeadStreamBuffer);
		String content = MockStreamBufferPool.getContent(this.contentHeadStreamBuffer, charset);
		assertEquals("Incorrect sent content", "TEST", content);
	}

	/**
	 * Ensure sends the {@link HttpResponse} on closing the
	 * {@link ServerOutputStream}.
	 */
	public void testSendOnCloseServerOutputStream() throws IOException {

		// Close the output stream
		this.response.getEntity().close();

		// Ensure response sent
		assertTrue("Should be sent", this.response.isClosed());
	}

	/**
	 * Ensure sends the {@link HttpResponse} on closing the {@link ServerWriter}.
	 */
	public void testSendOnCloseServerWriter() throws IOException {

		// Close the server writer
		this.response.getEntityWriter().close();

		// Ensure response sent
		assertTrue("Should be sent", this.response.isClosed());
	}

	/**
	 * Ensure send only once. Repeated sends will not do anything.
	 */
	public void testSendOnlyOnce() throws IOException {

		// Set altered status (to check does not change on sending again)
		this.response.setStatus(HttpStatus.BAD_REQUEST);

		// Obtain the entity (must be before close, otherwise exception)
		ServerOutputStream entity = this.response.getEntity();
		ServerWriter entityWriter = this.response.getEntityWriter();

		// Send
		this.response.flushResponseToHttpResponseWriter(null);
		assertTrue("Should be sent", this.response.isClosed());

		// Change response and send (but now should not send)
		response.setStatus(HttpStatus.OK);
		this.response.flushResponseToHttpResponseWriter(null);
		assertEquals("Should not re-send", HttpStatus.BAD_REQUEST, this.status);

		// Ensure close output stream does not re-send
		entity.close();
		assertEquals("Should not re-send", HttpStatus.BAD_REQUEST, this.status);

		// Ensure close writer does not re-send
		entityWriter.close();
		assertEquals("Should not re-send", HttpStatus.BAD_REQUEST, this.status);
	}

	/**
	 * Ensure reset the {@link HttpResponse}.
	 */
	public void testReset() throws IOException {

		// Ensure different charset
		Charset charset = Charset.forName("UTF-16");
		assertNotEquals("Should not be UTF-16", charset, this.response.getContentCharset());

		// Mutate the response
		this.response.setStatus(HttpStatus.CONTINUE);
		this.response.setVersion(HttpVersion.HTTP_1_0);
		this.response.setContentType("text/html", charset);
		this.response.getHeaders().addHeader("name", "value");
		this.response.getCookies().setCookie("name", "value");
		ServerWriter writer = this.response.getEntityWriter();
		writer.write("TEST");
		writer.flush();

		// Ensure response mutated
		assertEquals("Incorrect mutated status", HttpStatus.CONTINUE, this.response.getStatus());
		assertEquals("Incorrect mutated version", HttpVersion.HTTP_1_0, this.response.getVersion());
		assertEquals("Incorrect mutated heders", "value", this.response.getHeaders().getHeader("name").getValue());
		assertEquals("Incorrect mutated cookies", "value", this.response.getCookies().getCookie("name").getValue());
		assertTrue("Should be using buffers for entity content", this.bufferPool.isActiveBuffers());

		// Reset
		this.response.reset();

		// Ensure response reset
		assertEquals("Inccorect reset status", HttpStatus.OK, this.response.getStatus());
		assertEquals("Incorrect reset version", HttpVersion.HTTP_1_1, this.response.getVersion());
		assertFalse("Should clear headers", this.response.getHeaders().iterator().hasNext());
		assertFalse("Should clear cookies", this.response.getCookies().iterator().hasNext());
		assertFalse("No entity, as should release all buffers", this.bufferPool.isActiveBuffers());

		// Ensure able to continue forming response (typically an error)
		this.response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		this.response.setVersion(HttpVersion.HTTP_1_0);
		this.response.getHeaders().addHeader("error", "occurred");
		this.response.getCookies().setCookie("store", "failure");
		this.response.getEntityWriter().write("ERROR: something");

		// Send response (to ensure reset entity)
		this.response.flushResponseToHttpResponseWriter(null);

		// Ensure correct details sent
		assertEquals("Incorrect version", HttpVersion.HTTP_1_0, this.version);
		assertEquals("Incorrect status", HttpStatus.INTERNAL_SERVER_ERROR, this.status);
		assertNotNull("Should be a header", this.httpHeader);
		assertEquals("Incorrect header name", "error", this.httpHeader.getName());
		assertEquals("incorrect header value", "occurred", this.httpHeader.getValue());
		assertNull("Should only be one header", this.httpHeader.next);
		assertNotNull("Should be a cookie", this.httpCookie);
		assertEquals("Incorrect cookie name", "store", this.httpCookie.getName());
		assertEquals("Incorrect cookie value", "failure", this.httpCookie.getValue());
		assertNull("Should only be one cookie", this.httpCookie.next);

		// Ensure only entity content after the reset is sent
		MockStreamBufferPool.releaseStreamBuffers(this.contentHeadStreamBuffer);
		String content = MockStreamBufferPool.getContent(this.contentHeadStreamBuffer,
				ServerHttpConnection.DEFAULT_HTTP_ENTITY_CHARSET);
		assertEquals("Incorrect entity content since reset", "ERROR: something", content);

		// Ensure can not reset after sending
		try {
			this.response.reset();
			fail("Should not be sucessful");
		} catch (IOException ex) {
			assertEquals("Already committed to send response", ex.getMessage());
		}
	}

	/**
	 * Ensure can send {@link Escalation}.
	 */
	public void testSendEscalation() throws IOException {

		// Send escalation
		final Exception escalation = new Exception("TEST");
		this.response.flushResponseToHttpResponseWriter(escalation);

		// Obtain the escalation content
		StringWriter content = new StringWriter();
		PrintWriter writer = new PrintWriter(content);
		escalation.printStackTrace(writer);
		writer.flush();
		String expected = content.toString();

		// Ensure correct escalation
		assertEquals("Incorrect version", HttpVersion.HTTP_1_1, this.version);
		assertSame("Incorrect status", HttpStatus.INTERNAL_SERVER_ERROR, this.status);
		assertNull("Should be no headers", this.httpHeader);
		assertNull("Should be no cookies", this.httpCookie);
		assertEquals("Incorrect content-length", expected.length(), this.contentLength);
		assertEquals("Incorrect content-type", "text/plain", this.contentType.getValue());
		MockStreamBufferPool.releaseStreamBuffers(this.contentHeadStreamBuffer);
		assertEquals("Incorrect content", expected, MockStreamBufferPool.getContent(this.contentHeadStreamBuffer,
				ServerHttpConnection.DEFAULT_HTTP_ENTITY_CHARSET));
	}

	/**
	 * Ensure can provide {@link HttpEscalationHandler}.
	 */
	public void testHandleEscalation() throws IOException {

		final Exception escalation = new Exception("TEST");

		// Configure escalation handling
		Closure<Boolean> isInvoked = new Closure<>(false);
		this.response.setEscalationHandler((context) -> {
			assertSame("Incorrect escalation", escalation, context.getEscalation());
			isInvoked.value = true;
			this.response.setContentType("application/mock", null);
			this.response.getEntityWriter().write("{error: '" + escalation.getMessage() + "'}");
			return true; // handled
		});

		// Send escalation
		this.response.flushResponseToHttpResponseWriter(escalation);

		// Obtain the escalation content
		final String expected = "{error: 'TEST'}";

		// Ensure correct escalation
		assertEquals("Incorrect version", HttpVersion.HTTP_1_1, this.version);
		assertSame("Should default to internal status", HttpStatus.INTERNAL_SERVER_ERROR, this.status);
		assertNull("Should be no headers", this.httpHeader);
		assertNull("Should be no cookies", this.httpCookie);
		assertEquals("Incorrect content-type", "application/mock", this.contentType.getValue());
		assertEquals("Incorrect content", expected, MockStreamBufferPool.getContent(this.contentHeadStreamBuffer,
				ServerHttpConnection.DEFAULT_HTTP_ENTITY_CHARSET));
	}

	/**
	 * Ensure can reset {@link HttpResponse} send {@link Escalation}.
	 */
	public void testResetForSendingEscalation() throws IOException {

		// Provide details and send response
		this.response.setVersion(HttpVersion.HTTP_1_0);
		this.response.setStatus(HttpStatus.NOT_FOUND);
		this.response.getHeaders().addHeader("TEST", "VALUE");
		this.response.getCookies().setCookie("TEST", "VALUE");
		this.response.getEntityWriter().write("TEST");

		// Send escalation
		final Exception escalation = new Exception("TEST");
		this.response.flushResponseToHttpResponseWriter(escalation);

		// Obtain the escalation content
		StringWriter content = new StringWriter();
		PrintWriter writer = new PrintWriter(content);
		escalation.printStackTrace(writer);
		writer.flush();
		String expected = content.toString();

		// Ensure correct escalation
		assertEquals("Incorrect version", HttpVersion.HTTP_1_1, this.version);
		assertSame("Incorrect status", HttpStatus.INTERNAL_SERVER_ERROR, this.status);
		assertNull("Should be no headers", this.httpHeader);
		assertNull("Should be no cookies", this.httpCookie);
		assertEquals("Incorrect content-length", expected.length(), this.contentLength);
		assertEquals("Incorrect content-type", "text/plain", this.contentType.getValue());
		MockStreamBufferPool.releaseStreamBuffers(this.contentHeadStreamBuffer);
		assertEquals("Incorrect content", expected, MockStreamBufferPool.getContent(this.contentHeadStreamBuffer,
				ServerHttpConnection.DEFAULT_HTTP_ENTITY_CHARSET));
	}

	/*
	 * ===================== HttpResponseWriter ============================
	 */

	private HttpVersion version = null;

	private HttpStatus status = null;

	private WritableHttpHeader httpHeader = null;

	private WritableHttpCookie httpCookie = null;

	private long contentLength;

	private HttpHeaderValue contentType = null;

	private StreamBuffer<ByteBuffer> contentHeadStreamBuffer = null;

	@Override
	public void writeHttpResponse(HttpVersion version, HttpStatus status, WritableHttpHeader httpHeader,
			WritableHttpCookie httpCookie, long contentLength, HttpHeaderValue contentType,
			StreamBuffer<ByteBuffer> contentHeadStreamBuffer) {
		this.version = version;
		this.status = status;
		this.httpHeader = httpHeader;
		this.httpCookie = httpCookie;
		this.contentLength = contentLength;
		this.contentType = contentType;
		this.contentHeadStreamBuffer = contentHeadStreamBuffer;
	}

}
