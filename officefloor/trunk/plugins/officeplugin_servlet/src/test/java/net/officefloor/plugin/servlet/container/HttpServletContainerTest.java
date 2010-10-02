/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2010 Daniel Sagenschneider
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
package net.officefloor.plugin.servlet.container;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.officefloor.frame.api.execute.TaskContext;
import net.officefloor.frame.api.manage.Office;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.plugin.servlet.context.OfficeServletContext;
import net.officefloor.plugin.servlet.time.Clock;
import net.officefloor.plugin.socket.server.http.HttpHeader;
import net.officefloor.plugin.socket.server.http.HttpRequest;
import net.officefloor.plugin.socket.server.http.HttpResponse;
import net.officefloor.plugin.socket.server.http.ServerHttpConnection;
import net.officefloor.plugin.socket.server.http.parse.impl.HttpHeaderImpl;
import net.officefloor.plugin.socket.server.http.security.HttpSecurity;
import net.officefloor.plugin.socket.server.http.session.HttpSession;
import net.officefloor.plugin.stream.InputBufferStream;
import net.officefloor.plugin.stream.OutputBufferStream;

/**
 * Tests the {@link HttpServletContainer}.
 * 
 * @author Daniel Sagenschneider
 */
public class HttpServletContainerTest extends OfficeFrameTestCase {

	/**
	 * Token name for the Session Id.
	 */
	private static final String SESSION_ID_TOKEN_NAME = "JSESSIONID";

	/**
	 * Attribute for last access time.
	 */
	private static final String ATTRIBUTE_LAST_ACCESS_TIME = "#HttpServlet.LastAccessTime#";

	/**
	 * {@link ServerHttpConnection}.
	 */
	private final ServerHttpConnection connection = this
			.createMock(ServerHttpConnection.class);

	/**
	 * {@link HttpRequest}.
	 */
	private final HttpRequest request = this.createMock(HttpRequest.class);

	/**
	 * {@link HttpResponse}.
	 */
	private final HttpResponse response = this.createMock(HttpResponse.class);

	/**
	 * {@link HttpSession}.
	 */
	private final HttpSession session = this.createMock(HttpSession.class);

	/**
	 * {@link HttpSecurity}.
	 */
	private HttpSecurity security = this.createMock(HttpSecurity.class);

	/**
	 * {@link TaskContext}.
	 */
	private final TaskContext<?, ?, ?> taskContext = this
			.createMock(TaskContext.class);

	/**
	 * {@link OfficeServletContext}.
	 */
	private final OfficeServletContext officeServletContext = this
			.createMock(OfficeServletContext.class);

	/**
	 * {@link Office}.
	 */
	private final Office office = this.createMock(Office.class);

	/**
	 * {@link Clock}.
	 */
	private final Clock clock = this.createMock(Clock.class);

	/**
	 * {@link OutputStream} for {@link HttpResponse}.
	 */
	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	/**
	 * Init parameters.
	 */
	private final Map<String, String> initParameters = new HashMap<String, String>();

	/**
	 * Attributes.
	 */
	private final Map<String, Object> attributes = new HashMap<String, Object>();

	/**
	 * Servlet name.
	 */
	private String servletName = "ServletName";

	/**
	 * Servlet path.
	 */
	private String servletPath = "/servlet";

	/**
	 * HTTP method.
	 */
	private String httpMethod = "GET";

	/**
	 * Tests the deprecated functions.
	 */
	public void test_DeprecatedFunctions() {
		this.record_init("/test");
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(final HttpServletRequest req,
					HttpServletResponse resp) throws ServletException,
					IOException {
				try {

					// Deprecated request methods
					assertFail(UnsupportedOperationException.class, req,
							"isRequestedSessionIdFromUrl");

					// Deprecated response methods
					assertFail(UnsupportedOperationException.class, resp,
							"encodeUrl", "URL");
					assertFail(UnsupportedOperationException.class, resp,
							"encodeRedirectUrl", "URL");
					assertFail(UnsupportedOperationException.class, resp, resp
							.getClass().getMethod("setStatus", int.class,
									String.class), 200, "message");

				} catch (Throwable ex) {
					throw fail(ex);
				}
			}
		});
	}

	/**
	 * Ensure initialises the {@link HttpServlet}.
	 */
	public void test_init() {

		// Flag to ensure initialised
		final boolean[] isInitialised = new boolean[1];
		isInitialised[0] = false;

		// Register an init parameter
		this.initParameters.put("available", "value");

		// Record using Servlet Context (init parameter)
		this.record_init("/test");
		this.recordReturn(this.officeServletContext, this.officeServletContext
				.getInitParameter(this.office, "NAME"), "VALUE");

		// Test
		this.doTest(new MockHttpServlet() {

			@Override
			public void init() throws ServletException {
				isInitialised[0] = true; // Initialised
			}

			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				ServletConfig config = this.getServletConfig();
				assertEquals("Incorrect servlet name",
						HttpServletContainerTest.this.servletName, config
								.getServletName());
				assertEquals("Incorrect servlet context init parameter",
						"VALUE", config.getServletContext().getInitParameter(
								"NAME"));
				assertEquals("getInitParameter(available)", "value", config
						.getInitParameter("available"));
				assertNull("getInitParameter(none)", config
						.getInitParameter("none"));
			}
		});

		// Ensure initialised
		assertTrue("Should be initialised", isInitialised[0]);
	}

	/**
	 * Ensure context methods are correct.
	 */
	public void test_req_Context() {
		this.servletPath = "/servlet/path";

		// Record obtaining paths
		this.record_init("/context/path/servlet/path");
		this.recordReturn(this.officeServletContext, this.officeServletContext
				.getContextPath(this.office), "/context/path");

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertEquals("getContextPath()", "/context/path", req
						.getContextPath());
				assertEquals("getServletPath()", "/servlet/path", req
						.getServletPath());
				assertNull("getPathTranslated()", req.getPathTranslated());
			}
		});
	}

	/**
	 * Ensure status line methods are correct.
	 */
	public void test_req_StatusLine() {
		this.record_init("/server/path?one=1&two=2;three=3#fragment");
		this.recordReturn(this.request, this.request.getMethod(),
				this.httpMethod);
		this.recordReturn(this.request, this.request.getHeaders(), this
				.createHttpHeaders("host", "officefloor.net"));
		this.recordReturn(this.connection, this.connection.isSecure(), false);
		this.recordReturn(this.connection, this.connection.isSecure(), true);
		this.recordReturn(this.request, this.request.getVersion(), "HTTP/1.1");
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertEquals("getMethod()", "GET", req.getMethod());
				assertEquals("getPathInfor()", "/server/path", req
						.getPathInfo());
				assertEquals("getQueryString()", "one=1&two=2;three=3", req
						.getQueryString());
				assertEquals("getRequestURI()", "/server/path", req
						.getRequestURI());
				assertEquals("getRequestURL", "officefloor.net/server/path",
						req.getRequestURL().toString());
				assertEquals("getScheme - not secure", "http", req.getScheme());
				assertEquals("getScheme - secure", "https", req.getScheme());
				assertEquals("getVersion", "HTTP/1.1", req.getProtocol());
			}
		});
	}

	/**
	 * Ensure able to work with parameters.
	 */
	public void test_req_Parameters() {
		this
				.record_init("/server/path?one=1&two=2;three=3&duplicate=A;duplicate=B#fragment");
		this.doTest(new MockHttpServlet() {
			@Override
			@SuppressWarnings("unchecked")
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				// Validate single parameter value
				assertEquals("getParameter(one)", "1", req.getParameter("one"));
				assertEquals("getParameter(two)", "2", req.getParameter("two"));
				assertEquals("getParameter(three)", "3", req
						.getParameter("three"));
				assertEquals("getParameter(duplicate)", "A", req
						.getParameter("duplicate"));

				// Validate parameter names
				Enumeration<?> names = req.getParameterNames();
				for (String expectedName : new String[] { "one", "two",
						"three", "duplicate" }) {
					assertTrue("Expect another name", names.hasMoreElements());
					String actualName = (String) names.nextElement();
					assertEquals("Incorrect parameter name", expectedName,
							actualName);
				}
				assertFalse("Should be no further names", names
						.hasMoreElements());

				// Validate multiple parameter values
				assertArray(req.getParameterValues("one"), "1");
				assertArray(req.getParameterValues("duplicate"), "A", "B");

				// Validate parameter map
				Map<String, String[]> map = req.getParameterMap();
				assertArray(map.get("one"), "1");
				assertArray(map.get("duplicate"), "A", "B");
			}
		});
	}

	/**
	 * Validates the array.
	 * 
	 * @param actual
	 *            Actual array.
	 * @param expected
	 *            Expected array.
	 */
	private static void assertArray(String[] actual, String... expected) {
		assertEquals("Incorrect array count", expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals("Incorrect value " + i, expected[i], actual[i]);
		}
	}

	/**
	 * Ensure able to obtain cookies.
	 */
	public void test_req_Cookies() {
		this.record_init("/test");
		this.recordReturn(this.request, this.request.getHeaders(), this
				.createHttpHeaders("cookie", "name=\"value\""));
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				Cookie[] cookies = req.getCookies();
				assertEquals("Incorrect number of cookies", 1, cookies.length);
				Cookie cookie = cookies[0];
				assertEquals("Incorrect cookie name", "name", cookie.getName());
				assertEquals("Incorrect cookie value", "value", cookie
						.getValue());
			}
		});
	}

	/**
	 * Ensure able to obtain header values.
	 */
	public void test_req_Header() {
		this.record_init("/test");

		// Only single call as should cache headers
		this.recordReturn(this.request, this.request.getHeaders(), this
				.createHttpHeaders("name", "value", "int", "1", "date",
						"Sun, 06 Nov 1994 08:49:37 GMT", "Content-Length",
						"50", "Content-Type", "text/html"));

		// Validate able to obtain header values
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertNull("getHeader(missing)", req.getHeader("missing"));
				assertEquals("getHeader(name)", "value", req.getHeader("name"));
				assertEquals("getIntHeader(int)", 1, req.getIntHeader("int"));
				assertEquals("getDateHeader(date)", 784111777000l, req
						.getDateHeader("date"));
				assertEquals("getContentLength()", 50, req.getContentLength());
				assertEquals("getContentType()", "text/html", req
						.getContentType());
			}
		});
	}

	/**
	 * Ensure able to obtain headers.
	 */
	public void test_req_Headers() {
		this.record_init("/test");

		// Only single call as should cache headers
		this.recordReturn(this.request, this.request.getHeaders(), this
				.createHttpHeaders("one", "10", "one", "11", "two", "20"));

		// Validate able to obtain headers
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				// Validate unique header names
				Enumeration<?> names = req.getHeaderNames();
				for (String expectedName : new String[] { "one", "two" }) {
					assertTrue("Expect name " + expectedName, names
							.hasMoreElements());
					String actualName = (String) names.nextElement();
					assertEquals("Incorrect header name", expectedName,
							actualName);
				}
				assertFalse("No further names expected", names
						.hasMoreElements());

				// Validate the first header value
				assertEquals("getHeader(one)", "10", req.getHeader("one"));

				// Validate the multiple header values
				Enumeration<?> values = req.getHeaders("one");
				for (String expectedValue : new String[] { "10", "11" }) {
					assertTrue("Expect value " + expectedValue, values
							.hasMoreElements());
					String actualValue = (String) values.nextElement();
					assertEquals("Incorrect header value", expectedValue,
							actualValue);
				}
				assertFalse("No further values expected", names
						.hasMoreElements());
			}
		});
	}

	/**
	 * Ensure able to obtain details of Server from Host header.
	 */
	public void test_req_Server_FromHostHeader() {
		this.record_init("/test");
		this.recordReturn(this.request, this.request.getHeaders(), this
				.createHttpHeaders("Host", "officefloor.net:80"));
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertEquals("getServerName()", "officefloor.net", req
						.getServerName());
				assertEquals("getServerPort()", 80, req.getServerPort());
			}
		});
	}

	/**
	 * Ensure able to obtain details of Server without Host Header.
	 */
	public void test_req_Server_Default() throws Exception {

		// Local address for server
		final String serverName = "192.168.0.1";
		final byte[] serverAddr = new byte[] { (byte) 192, (byte) 168,
				(byte) 0, (byte) 1 };
		final int serverPort = 80;
		final InetSocketAddress localAddress = new InetSocketAddress(
				InetAddress.getByAddress(serverName, serverAddr), serverPort);

		// Record obtaining the local address for server
		this.record_init("/test");
		this.recordReturn(this.request, this.request.getHeaders(), this
				.createHttpHeaders());
		this.recordReturn(this.connection, this.connection.getLocalAddress(),
				localAddress);
		this.recordReturn(this.connection, this.connection.getLocalAddress(),
				localAddress);

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertEquals("getServerName()", serverName, req.getServerName());
				assertEquals("getServerPort()", serverPort, req.getServerPort());
			}
		});
	}

	/**
	 * Ensure able to obtain details of body.
	 */
	public void test_req_ServletInputStream() {
		// Mocks
		final InputBufferStream bufferStream = this
				.createMock(InputBufferStream.class);
		final ByteArrayInputStream inputStream = new ByteArrayInputStream("a"
				.getBytes(Charset.forName("ASCII")));

		// Record obtaining the input streams and data
		this.record_init("/test");
		this.recordReturn(this.request, this.request.getBody(), bufferStream);
		this.recordReturn(bufferStream, bufferStream.getInputStream(),
				inputStream);

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				// Validate the Servlet Input Stream
				ServletInputStream inputStream = req.getInputStream();
				assertEquals("Incorrect number of bytes available", 1,
						inputStream.available());
				assertEquals("Incorrect body value", 'a', inputStream.read());

				// Ensure not able to obtain Reader
				try {
					req.getReader();
					fail("Should not be able to obtain Reader");
				} catch (IllegalStateException ex) {
					// Correctly indicated not able to obtain reader
				}
			}
		});
	}

	/**
	 * Ensure able to obtain details of body.
	 */
	public void test_req_Reader() {
		// Mocks
		final InputBufferStream bufferStream = this
				.createMock(InputBufferStream.class);
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(
				"test line\n".getBytes());

		// Record obtaining the input streams and data
		this.record_init("/test");
		this.recordReturn(this.request, this.request.getBody(), bufferStream);
		this.recordReturn(bufferStream, bufferStream.getInputStream(),
				inputStream);

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				// Validate the Reader
				BufferedReader reader = req.getReader();
				assertEquals("Incorrect body line", "test line", reader
						.readLine());
				assertNull("Expecting no further body content", reader
						.readLine());

				// Ensure not able to obtain InputStream
				try {
					req.getInputStream();
					fail("Should not be able to obtain Reader");
				} catch (IllegalStateException ex) {
					// Correctly indicated not able to obtain reader
				}
			}
		});
	}

	/**
	 * Validates the use of attributes.
	 */
	public void test_req_Attributes() {
		this.record_init("/test");
		this.doTest(new MockHttpServlet() {
			@Override
			@SuppressWarnings("unchecked")
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				// Remove the last access time attribute for below testing
				req.removeAttribute(ATTRIBUTE_LAST_ACCESS_TIME);

				// Ensure initially no attributes
				assertFalse("Should be no attributes initially", req
						.getAttributeNames().hasMoreElements());

				// Load the attribute
				final Object attribute = new Object();
				req.setAttribute("attribute", attribute);

				// Validate the loaded attribute
				assertEquals("Incorrect attribute", attribute, req
						.getAttribute("attribute"));
				Enumeration<String> names = req.getAttributeNames();
				assertTrue("Expect an attribute loaded", names
						.hasMoreElements());
				assertEquals("Incorrect attribute name", "attribute", names
						.nextElement());
				assertFalse("Expect only one attribute loaded", names
						.hasMoreElements());

				// Remove the attribute
				req.removeAttribute("attribute");
				assertFalse("Attribute should be removed", req
						.getAttributeNames().hasMoreElements());
			}
		});
	}

	/**
	 * Validates details of the connection.
	 */
	public void test_req_ConnectionDetails() throws Exception {

		// Initiate remote address
		final String remoteTextAddr = "192.168.0.1";
		final byte[] remoteByteAddr = new byte[] { (byte) 192, (byte) 168,
				(byte) 0, (byte) 1 };
		final String remoteHost = "client";
		final int remotePort = 43100;
		final InetSocketAddress remoteAddress = new InetSocketAddress(
				InetAddress.getByAddress(remoteHost, remoteByteAddr),
				remotePort);

		// Initiate local address
		final String localTextAddr = "192.168.0.2";
		final byte[] localByteAddr = new byte[] { (byte) 192, (byte) 168,
				(byte) 0, (byte) 2 };
		final String localName = "server";
		final int localPort = 80;
		final InetSocketAddress localAddress = new InetSocketAddress(
				InetAddress.getByAddress(localName, localByteAddr), localPort);

		// Record obtaining remote and local addresses appropriate times
		this.record_init("/test");
		for (int i = 0; i < 3; i++) {
			this.recordReturn(this.connection, this.connection
					.getRemoteAddress(), remoteAddress);
			this.recordReturn(this.connection, this.connection
					.getLocalAddress(), localAddress);
		}
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				// Validate address
				assertEquals("getRemoteAddr", remoteTextAddr, req
						.getRemoteAddr());
				assertEquals("getLocalAddr", localTextAddr, req.getLocalAddr());

				// Validate host/name
				assertEquals("getRemoteHost", remoteHost, req.getRemoteHost());
				assertEquals("getLocalName", localName, req.getLocalName());

				// Validate port
				assertEquals("getRemotePort", remotePort, req.getRemotePort());
				assertEquals("getLocalPort", localPort, req.getLocalPort());
			}
		});
	}

	/**
	 * Ensure secure channel methods are correct.
	 */
	public void test_req_SecureChannel() {
		this.record_init("/test");
		this.recordReturn(this.connection, this.connection.isSecure(), true);
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertTrue("isSecure()", req.isSecure());
			}
		});
	}

	/**
	 * Ensure security methods are correct.
	 */
	public void test_req_Security() {
		final Principal principal = this.createMock(Principal.class);
		this.record_init("/test");
		this.recordReturn(this.security, this.security
				.getAuthenticationScheme(), "BASIC");
		this.recordReturn(this.security, this.security.getUserPrincipal(),
				principal);
		this.recordReturn(this.security, this.security.getRemoteUser(),
				"daniel");
		this.recordReturn(this.security, this.security.isUserInRole("role"),
				true);
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertEquals("getAuthType()", "BASIC", req.getAuthType());
				assertEquals("getUserPrincipal()", principal, req
						.getUserPrincipal());
				assertEquals("getRemoteUser()", "daniel", req.getRemoteUser());
				assertTrue("isUserInRole(role)", req.isUserInRole("role"));
			}
		});
	}

	/**
	 * Ensure handle security methods for anonymous request.
	 */
	public void test_req_Security_Anonymous() {
		// Anonymous request so no security
		this.security = null;

		// Test
		this.record_init("/test");
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertNull("getAuthType()", req.getAuthType());
				assertNull("getUserPrincipal()", req.getUserPrincipal());
				assertNull("getRemoteUser()", req.getRemoteUser());
				assertFalse("isUserInRole(role)", req.isUserInRole("role"));
			}
		});
	}

	/**
	 * Ensure obtain HTTP session.
	 */
	public void test_req_HttpSession() {
		final String SESSION_ID = "SessionId";
		this.record_init("/test");
		this
				.recordReturn(this.session, this.session.getSessionId(),
						SESSION_ID);
		this
				.recordReturn(this.session, this.session.getSessionId(),
						SESSION_ID);
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				// Ensure always obtain session
				javax.servlet.http.HttpSession httpSession = req.getSession();
				assertEquals("Must have session", SESSION_ID, httpSession
						.getId());

				// Will always obtain session
				httpSession = req.getSession(false);
				assertEquals("Always have session", SESSION_ID, httpSession
						.getId());
			}
		});
	}

	/**
	 * Ensure able to obtain last access time from the request attributes.
	 */
	public void test_req_HttpSession_LastAccessTimeViaRequest() {
		final Long LAST_ACCESS_TIME = new Long(1000);

		// Obtain from request attributes
		this.record_init("/test");

		// Last access time from request
		this.attributes.put(ATTRIBUTE_LAST_ACCESS_TIME, LAST_ACCESS_TIME);

		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				javax.servlet.http.HttpSession httpSession = req.getSession();
				assertEquals("Incorrect last access time", LAST_ACCESS_TIME
						.longValue(), httpSession.getLastAccessedTime());
			}
		});
	}

	/**
	 * Ensure able to obtain last access time from the session attributes.
	 */
	public void test_req_HttpSession_LastAccessTimeViaSession() {
		final Long LAST_ACCESS_TIME = new Long(2000);
		final long CURRENT_TIME = 3000;

		// Last access time from session
		this.session.getAttribute(ATTRIBUTE_LAST_ACCESS_TIME);
		this.control(this.session).setReturnValue(LAST_ACCESS_TIME);
		this.recordReturn(this.clock, this.clock.currentTimeMillis(),
				CURRENT_TIME);
		this.session.setAttribute(ATTRIBUTE_LAST_ACCESS_TIME, new Long(
				CURRENT_TIME));

		// Record remaining
		this.record_init("/test");

		// Remove after initialising (to obtain from session)
		this.attributes.remove(ATTRIBUTE_LAST_ACCESS_TIME);

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				javax.servlet.http.HttpSession httpSession = req.getSession();
				assertEquals("Incorrect last access time", LAST_ACCESS_TIME
						.longValue(), httpSession.getLastAccessedTime());
			}
		});

		// Ensure last access time in request attributes
		assertEquals("Incorrect last access time", LAST_ACCESS_TIME,
				this.attributes.get(ATTRIBUTE_LAST_ACCESS_TIME));
	}

	/**
	 * Ensure able to obtain last access time for first request.
	 */
	public void test_req_HttpSession_LastAccessTimeForFirstRequest() {
		final long CURRENT_TIME = 5000;

		// Record using current time (no previous request)
		this.recordReturn(this.session, this.session
				.getAttribute(ATTRIBUTE_LAST_ACCESS_TIME), null);
		this.recordReturn(this.clock, this.clock.currentTimeMillis(),
				CURRENT_TIME);
		this.session.setAttribute(ATTRIBUTE_LAST_ACCESS_TIME, new Long(
				CURRENT_TIME));

		// Record remaining
		this.record_init("/test");

		// Remove after initialising (to use current time)
		this.attributes.remove(ATTRIBUTE_LAST_ACCESS_TIME);

		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				javax.servlet.http.HttpSession httpSession = req.getSession();
				assertEquals("Incorrect last access time", CURRENT_TIME,
						httpSession.getLastAccessedTime());
			}
		});

		// Ensure last access time in request attributes
		assertEquals("Incorrect last access time", new Long(CURRENT_TIME),
				this.attributes.get(ATTRIBUTE_LAST_ACCESS_TIME));
	}

	/**
	 * Ensure session methods are correct.
	 */
	public void test_req_Session_ViaCookie() {
		final String SESSION_ID = "SessionId";
		this.record_init("/test");
		this.recordReturn(this.request, this.request.getHeaders(), this
				.createHttpHeaders("cookie", SESSION_ID_TOKEN_NAME + "=\""
						+ SESSION_ID + "\""));
		this
				.recordReturn(this.session, this.session.getSessionId(),
						SESSION_ID);
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertEquals("getRequestSessionId", SESSION_ID, req
						.getRequestedSessionId());
				assertTrue("isRequestedSessionIdFromCookie", req
						.isRequestedSessionIdFromCookie());
				assertFalse("isRequestedSessionIdFromUrl", req
						.isRequestedSessionIdFromURL());
				assertTrue("isRequestedSessionIdValid", req
						.isRequestedSessionIdValid());
			}
		});
	}

	/**
	 * Ensure session methods are correct.
	 */
	public void test_req_Session_ViaParameter() {
		final String SESSION_ID = "SessionId";
		this.record_init("/test?" + SESSION_ID_TOKEN_NAME + "=" + SESSION_ID);
		this.recordReturn(this.request, this.request.getHeaders(), this
				.createHttpHeaders());
		this.recordReturn(this.session, this.session.getSessionId(),
				"Not same Session Id");
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertEquals("getRequestSessionId", SESSION_ID, req
						.getRequestedSessionId());
				assertFalse("isRequestedSessionIdFromCookie", req
						.isRequestedSessionIdFromCookie());
				assertTrue("isRequestedSessionIdFromUrl", req
						.isRequestedSessionIdFromURL());
				assertFalse("isRequestedSessionIdValid", req
						.isRequestedSessionIdValid());
			}
		});
	}

	/**
	 * Validates session methods are correct.
	 */
	public void test_req_Session_NoId() {
		this.record_init("/test");
		this.recordReturn(this.request, this.request.getHeaders(), this
				.createHttpHeaders());
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertNull("getRequestSessionId", req.getRequestedSessionId());
				assertFalse("isRequestedSessionIdFromCookie", req
						.isRequestedSessionIdFromCookie());
				assertFalse("isRequestedSessionIdFromUrl", req
						.isRequestedSessionIdFromURL());
				assertFalse("isRequestedSessionIdValid", req
						.isRequestedSessionIdValid());
			}
		});
	}

	/**
	 * Validates the request dispatcher methods.
	 */
	public void test_req_RequestDispatcher() {
		final RequestDispatcher dispatcher = this
				.createMock(RequestDispatcher.class);

		// Record obtaining the request dispatcher
		this.record_init("/test");
		this.recordReturn(this.officeServletContext, this.officeServletContext
				.getRequestDispatcher(this.office, "/none"), null);
		this.recordReturn(this.officeServletContext, this.officeServletContext
				.getRequestDispatcher(this.office, "/absolute"), dispatcher);
		this.recordReturn(this.officeServletContext, this.officeServletContext
				.getRequestDispatcher(this.office, this.servletPath
						+ "/relative"), dispatcher);

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertNull("getRequestDispathcer(none)", req
						.getRequestDispatcher("/none"));
				assertEquals("getRequestDispatcher(absolute)", dispatcher, req
						.getRequestDispatcher("/absolute"));
				assertEquals("getRequestDispatcher(relative)", dispatcher, req
						.getRequestDispatcher("relative"));
			}
		});
	}

	/**
	 * Ensure providing the {@link ServletRequestForwarder} via the
	 * {@link ServletRequest}.
	 */
	public void test_req_ServletRequestForwarder() throws Exception {

		final String WORK_NAME = "WORK";
		final String TASK_NAME = "TASK";
		final Object PARAMETER = "PARAMETER";

		// Record forwarding the request
		this.record_init("/test");
		this.taskContext.doFlow(WORK_NAME, TASK_NAME, PARAMETER);

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				// Obtain the forwarder
				ServletRequestForwarder forwarder = (ServletRequestForwarder) req
						.getAttribute(ServletRequestForwarder.ATTRIBUTE_FORWARDER);
				assertNotNull("Expect to always obtain access", forwarder);

				// Forward to ensure correctly forwards
				forwarder.forward(WORK_NAME, TASK_NAME, PARAMETER);
			}
		});

		// Ensure not added to attributes
		assertNull("Should not be added to attributes", this.attributes
				.get(ServletRequestForwarder.ATTRIBUTE_FORWARDER));
	}

	/**
	 * Allow specifying {@link Locale} for {@link HttpRequest}.
	 */
	public void test_req_Locale() {
		this.record_init("/test");
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				// TODO take Locale from Accept-Language header

				// Only default locale
				assertEquals("Default locale", Locale.getDefault(), req
						.getLocale());

				// Only default locale
				Enumeration<?> enumeration = req.getLocales();
				assertTrue("Expecting default locale", enumeration
						.hasMoreElements());
				assertEquals("Should have default locale", Locale.getDefault(),
						enumeration.nextElement());
				assertFalse("Only expecting default locale", enumeration
						.hasMoreElements());
			}
		});
	}

	/**
	 * Ensure able to work with the {@link ServletOutputStream}.
	 */
	public void test_resp_ServletOutputStream() {
		final byte DATA = 1;
		this.record_init("/test");
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				// Validate the Servlet Output Stream
				ServletOutputStream outputStream = resp.getOutputStream();
				outputStream.write(DATA);
				// No need to flush as container should ensure flushing buffers

				// Ensure not able to obtain Writer
				try {
					resp.getWriter();
					fail("Should not be able to obtain Writer");
				} catch (IllegalStateException ex) {
					// Correctly indicated not able to obtain writer
				}
			}
		});

		// Validate written data
		byte[] writtenData = this.outputStream.toByteArray();
		assertEquals("Incorrect number of bytes written", 1, writtenData.length);
		assertEquals("Incorrect data written", DATA, writtenData[0]);
	}

	/**
	 * Ensure able to work with the {@link ServletOutputStream}.
	 */
	public void test_resp_Writer() {
		final String DATA = "test";
		this.record_init("/test");
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				// Validate the Writer
				PrintWriter writer = resp.getWriter();
				writer.write(DATA);
				// No need to flush as container should ensure flushing buffers

				// Ensure not able to obtain Servlet Output Stream
				try {
					resp.getOutputStream();
					fail("Should not be able to obtain ServletOutputStream");
				} catch (IllegalStateException ex) {
					// Correctly indicated not able to obtain output stream
				}
			}
		});

		// Validate written data
		assertText(DATA, this.outputStream.toByteArray());
	}

	/**
	 * Ensure able to buffer content.
	 */
	public void test_resp_Buffer() {
		final String DATA = "test data";
		this.record_init("/test");
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				// Ensure large buffer and not write out data
				resp.setBufferSize(10000);
				assertEquals("Incorrect buffer size", 10000, resp
						.getBufferSize());

				// Write small data (below buffer size)
				PrintWriter writer = resp.getWriter();
				writer.write(DATA);

				// Ensure no content written
				assertEquals("Content should still be buffered", 0,
						HttpServletContainerTest.this.outputStream
								.toByteArray().length);
				assertFalse("Should not yet be committed", resp.isCommitted());

				// Flush the buffer
				resp.flushBuffer();
				assertTrue("Committed to content", resp.isCommitted());
				assertText("test data",
						HttpServletContainerTest.this.outputStream
								.toByteArray());

				// Ensure not able to change buffer size
				try {
					resp.setBufferSize(10);
					fail("Should not be able to change buffer size after the fact");
				} catch (IllegalStateException ex) {
					// Correctly indicated not able to specify buffer size
				}
			}
		});
	}

	/**
	 * Ensure able to reset buffer.
	 */
	public void test_resp_ResetBuffer() {
		this.record_init("/test");
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				// Ensure large buffer and not write out data
				resp.setBufferSize(10000);

				// Write small data (below buffer size)
				PrintWriter writer = resp.getWriter();
				writer.write("test data");

				// Ensure no content written
				assertEquals("Content should still be buffered", 0,
						HttpServletContainerTest.this.outputStream
								.toByteArray().length);
				assertFalse("Should not yet be committed", resp.isCommitted());

				// Reset the buffer
				resp.resetBuffer();

				// Write other data and ensure reset data is not sent
				writer.write("other data");
				writer.flush(); // triggers flushing buffer
				assertTrue("Committed to content", resp.isCommitted());
				assertText("other data",
						HttpServletContainerTest.this.outputStream
								.toByteArray());
			}
		});
	}

	/**
	 * Ensure able to reset the response.
	 */
	public void test_resp_Reset() {

		// Mocks
		final HttpHeader header = this.createMock(HttpHeader.class);

		// Record obtaining the output streams
		this.record_init("/test");
		this.recordReturn(this.response, this.response.addHeader("test",
				"value"), header);
		this.recordReturn(this.response, this.response.getHeaders(),
				new HttpHeader[] { header });
		this.response.removeHeader(header);
		this.recordReturn(this.response, this.response.addHeader("test",
				"another"), header);

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				// Ensure large buffer and not write out data
				resp.setBufferSize(10000);

				// Write small data (below buffer size)
				PrintWriter writer = resp.getWriter();
				writer.write("test data");

				// Add a header
				resp.addHeader("test", "value");

				// Ensure no content written
				assertEquals("Content should still be buffered", 0,
						HttpServletContainerTest.this.outputStream
								.toByteArray().length);
				assertFalse("Should not yet be committed", resp.isCommitted());

				// Reset
				resp.reset();

				// Write other data and header and ensure reset data is not sent
				resp.addHeader("test", "another");
				writer.write("other data");
				resp.flushBuffer();
				assertTrue("Committed to content", resp.isCommitted());
				assertText("other data",
						HttpServletContainerTest.this.outputStream
								.toByteArray());
			}
		});
	}

	/**
	 * Ensure can specify the content details.
	 */
	public void test_resp_ContentDetails() {
		final String CONTENT_LENGTH = "Content-Length";
		final String CONTENT_TYPE = "Content-Type";
		final HttpHeader header = this.createMock(HttpHeader.class);

		// Record
		this.record_init("/test");

		// Record specify content length
		this.response.removeHeaders(CONTENT_LENGTH);
		this.recordReturn(this.response, this.response.addHeader(
				CONTENT_LENGTH, "10"), header);

		// Record specifying content type without charset
		this.response.removeHeaders(CONTENT_TYPE);
		this.recordReturn(this.response, this.response.addHeader(CONTENT_TYPE,
				"text/html"), header);
		this.recordReturn(this.response, this.response.getHeader(CONTENT_TYPE),
				header);
		this.recordReturn(header, header.getValue(), "text/html");

		// Record specifying content type with charset
		this.response.removeHeaders(CONTENT_TYPE);
		this.recordReturn(this.response, this.response.addHeader(CONTENT_TYPE,
				"text/html; charset=UTF-8; another=parameter"), header);
		this.recordReturn(this.response, this.response.getHeader(CONTENT_TYPE),
				header);
		this.recordReturn(header, header.getValue(),
				"text/html; charset=UTF-8; another=parameter");

		// Record specifying character encoding
		this.recordReturn(this.response, this.response.getHeader(CONTENT_TYPE),
				header);
		this.recordReturn(header, header.getValue(),
				"text/html; charset=UTF-8; another=parameter");

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				resp.setContentLength(10);

				// Ensure default charset not changed if not specified
				resp.setContentType("text/html");
				assertEquals("Incorrect Content-Type", "text/html", resp
						.getContentType());

				// Ensure default character encoding
				assertEquals("Incorrect default character encoding",
						"ISO-8859-1", resp.getCharacterEncoding());

				// Ensure can change via content type
				resp
						.setContentType("text/html; charset=UTF-8; another=parameter");
				assertEquals("Incorrect Content-Type",
						"text/html; charset=UTF-8; another=parameter", resp
								.getContentType());
				assertEquals("Incorrect charset from Content-Type", "UTF-8",
						resp.getCharacterEncoding());

				// Ensure can change via character encoding
				resp.setCharacterEncoding("UTF-16");
				assertEquals("Incorrect specified charset", "UTF-16", resp
						.getCharacterEncoding());

				// Does not change charset for content type
				assertEquals("Content-Type charset should not be overwritten",
						"text/html; charset=UTF-8; another=parameter", resp
								.getContentType());

				// Write content
				Writer writer = resp.getWriter();
				writer.write("test data");
				writer.flush();
			}
		});

		// Validate the content written
		assertText("test data", this.outputStream.toByteArray(), "UTF-16");
	}

	/**
	 * Ensure can specify cookies on the {@link HttpResponse}.
	 */
	public void test_resp_Cookies() {
		final long TIME = 1280920637665l;
		final HttpHeader header = this.createMock(HttpHeader.class);

		// Record specifying the cookie
		this.record_init("/test");
		this.recordReturn(this.response, this.response.getHeaders(),
				new HttpHeader[0]);
		this.recordReturn(this.clock, this.clock.currentTimeMillis(), TIME);
		this
				.recordReturn(
						this.response,
						this.response
								.addHeader(
										"set-cookie",
										"name=\"value\"; expires=Wed, 04-Aug-2010 11:17:18 GMT; domain=.officefloor.net"),
						header);

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				Cookie cookie = new Cookie("name", "value");
				cookie.setMaxAge(600);
				cookie.setDomain(".officefloor.net");
				resp.addCookie(cookie);
			}
		});
	}

	/**
	 * Ensure {@link HttpResponse} header functionality works.
	 */
	public void test_resp_Headers() {
		final HttpHeader header = this.createMock(HttpHeader.class);
		this.record_init("/test");
		this.recordReturn(this.response, this.response.getHeaders(),
				new HttpHeader[] { header });
		this.recordReturn(header, header.getName(), "test");
		this.recordReturn(this.response, this.response.getHeaders(),
				new HttpHeader[0]);
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertTrue("Should contain 'test' header", resp
						.containsHeader("test"));
				assertFalse("Should not contain 'missing' header", resp
						.containsHeader("missing"));
			}
		});
	}

	/**
	 * Ensure can encode URL.
	 */
	public void test_resp_Encode() {
		final String URL = "/test";
		this.record_init("/test");
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertEquals("Session Id always via cookie", URL, resp
						.encodeURL(URL));
				assertEquals("Session Id not included in redirects", URL, resp
						.encodeRedirectURL(URL));
			}
		});
	}

	/**
	 * Ensure can send error.
	 */
	public void test_resp_SendErrorWithMessage() throws Exception {

		// Record
		this.record_init("/test");
		this.response.setStatus(404, "test message");
		this.response.send();

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				resp.sendError(404, "test message");
			}
		});

		// Validate sent message as body
		assertText("<html><body>test message</body></html>", this.outputStream
				.toByteArray());
	}

	/**
	 * Ensure can send error.
	 */
	public void test_resp_SendError() throws Exception {
		this.record_init("/test");
		this.response.setStatus(404);
		this.response.send();
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				resp.sendError(404);
			}
		});
	}

	/**
	 * Ensure able to send a relative redirect.
	 */
	public void test_resp_SendRelativeRedirect() throws Exception {

		// Mocks
		final HttpHeader header = this.createMock(HttpHeader.class);

		// Record obtaining details for redirect
		this.record_init("/test");
		this.recordReturn(this.connection, this.connection.isSecure(), false);
		this.recordReturn(this.officeServletContext, this.officeServletContext
				.getContextPath(this.office), "/context");
		this.recordReturn(this.request, this.request.getHeaders(), this
				.createHttpHeaders("host", "officefloor.net:8080"));

		// Record sending redirect
		this.response.setStatus(307);
		this.recordReturn(this.response, this.response.addHeader("Location",
				"http://officefloor.net:8080/context" + this.servletPath
						+ "/redirect.txt"), header);
		this.response.send();

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				resp.sendRedirect("redirect.txt");
			}
		});
	}

	/**
	 * Ensure able to send an absolute redirect.
	 */
	public void test_resp_SendAbsoluteRedirect() throws Exception {

		// Mock
		final HttpHeader header = this.createMock(HttpHeader.class);

		// Record obtaining details for redirect
		this.record_init("/test");
		this.recordReturn(this.connection, this.connection.isSecure(), true);
		this.recordReturn(this.officeServletContext, this.officeServletContext
				.getContextPath(this.office), "/context");
		this.recordReturn(this.request, this.request.getHeaders(), this
				.createHttpHeaders("host", "officefloor.net:443"));

		// Record sending redirect
		this.response.setStatus(307);
		this.recordReturn(this.response, this.response.addHeader("Location",
				"https://officefloor.net:443/context" + "/redirect.html"),
				header);
		this.response.send();

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				resp.sendRedirect("/redirect.html");
			}
		});
	}

	/**
	 * Ensure can load header values to {@link HttpResponse}.
	 */
	public void test_resp_Header() {
		final long dateValue = 784111777000l;
		final String dateText = "Sun, 06 Nov 1994 08:49:37 GMT";
		final HttpHeader header = this.createMock(HttpHeader.class);

		// Record specifying headers
		this.record_init("/test");
		this.recordReturn(this.response, this.response.addHeader("date",
				dateText), header);
		this.response.removeHeaders("OnlyDate");
		this.recordReturn(this.response, this.response.addHeader("OnlyDate",
				dateText), header);
		this.recordReturn(this.response, this.response.addHeader("header",
				"value"), header);
		this.response.removeHeaders("OnlyHeader");
		this.recordReturn(this.response, this.response.addHeader("OnlyHeader",
				"value"), header);
		this.recordReturn(this.response, this.response.addHeader("integer",
				"10"), header);
		this.response.removeHeaders("OnlyInteger");
		this.recordReturn(this.response, this.response.addHeader("OnlyInteger",
				"5"), header);

		// Test
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				resp.addDateHeader("date", dateValue);
				resp.setDateHeader("OnlyDate", dateValue);
				resp.addHeader("header", "value");
				resp.setHeader("OnlyHeader", "value");
				resp.addIntHeader("integer", 10);
				resp.setIntHeader("OnlyInteger", 5);
			}
		});
	}

	/**
	 * Ensure can specify the status.
	 */
	public void test_resp_Status() {
		this.record_init("/test");
		this.response.setStatus(203);
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				resp.setStatus(203);
			}
		});
	}

	/**
	 * Allow specifying {@link Locale} for {@link HttpResponse}.
	 */
	public void test_resp_Locale() {
		this.record_init("/test");
		this.doTest(new MockHttpServlet() {
			@Override
			protected void test(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				assertEquals("Initially default locale", Locale.getDefault(),
						resp.getLocale());
				resp.setLocale(Locale.GERMANY);
				assertEquals("Incorrect changed locale", Locale.GERMANY, resp
						.getLocale());
			}
		});
	}

	/**
	 * Asserts the written bytes to match the expected text (charset ASCII).
	 * 
	 * @param expectedText
	 *            Expected text written.
	 * @param writtenBytes
	 *            Actual bytes written.
	 */
	private static void assertText(String expectedText, byte[] writtenBytes) {
		assertText(expectedText, writtenBytes, "ASCII");
	}

	/**
	 * Asserts the written bytes to match the expected text.
	 * 
	 * @param expectedText
	 *            Expected text written.
	 * @param writtenBytes
	 *            Actual bytes written.
	 * @param charSetName
	 *            Name of {@link Charset}.
	 */
	private static void assertText(String expectedText, byte[] writtenBytes,
			String charSetName) {
		String writtenString = new String(writtenBytes, Charset
				.forName(charSetName));
		assertEquals("Incorrect written text", expectedText, writtenString);
	}

	/**
	 * Creates the {@link HttpHeader} listing.
	 * 
	 * @param httpHeaderNameValues
	 *            {@link HttpHeader} name value pairs.
	 * @return Listing of {@link HttpHeader} instances.
	 */
	private List<HttpHeader> createHttpHeaders(String... httpHeaderNameValues) {
		List<HttpHeader> headers = new LinkedList<HttpHeader>();
		for (int i = 0; i < httpHeaderNameValues.length; i += 2) {
			String name = httpHeaderNameValues[i];
			String value = httpHeaderNameValues[i + 1];
			headers.add(new HttpHeaderImpl(name, value));
		}
		return headers;
	}

	/**
	 * Records initialising the {@link HttpServletContainer}.
	 * 
	 * @param requestUri
	 *            Request URI.
	 */
	private void record_init(String requestUri) {

		final OutputBufferStream bufferStream = this
				.createMock(OutputBufferStream.class);

		// Load last access time
		this.attributes.put(ATTRIBUTE_LAST_ACCESS_TIME, new Long(10));

		// Record obtaining the request and responses
		this.recordReturn(this.session, this.session.getTokenName(),
				SESSION_ID_TOKEN_NAME);
		this.recordReturn(this.connection, this.connection.getHttpRequest(),
				this.request);
		this.recordReturn(this.connection, this.connection.getHttpResponse(),
				this.response);
		this.recordReturn(this.request, this.request.getRequestURI(),
				requestUri);
		this.recordReturn(this.request, this.request.getMethod(),
				this.httpMethod);
		this.recordReturn(this.response, this.response.getBody(), bufferStream);
		this.recordReturn(bufferStream, bufferStream.getOutputStream(),
				this.outputStream);
	}

	/**
	 * Does the test.
	 * 
	 * @param servlet
	 *            {@link HttpServlet} containing the functionality for testing.
	 */
	private void doTest(HttpServlet servlet) {
		try {

			// Create additional unused mocks
			final Locale locale = Locale.getDefault();

			// Replay
			this.replayMockObjects();

			// Create the HTTP Servlet container
			HttpServletContainer container = new HttpServletContainerImpl(
					this.servletName, this.servletPath, servlet,
					this.initParameters, this.officeServletContext,
					this.office, this.clock, locale);

			// Process a request
			container.service(this.connection, this.attributes, this.session,
					this.security, this.taskContext);

			// Verify functionality
			this.verifyMockObjects();

		} catch (Exception ex) {
			throw fail(ex);
		}
	}

	/**
	 * Mock {@link HttpServlet} for testing.
	 */
	private static abstract class MockHttpServlet extends HttpServlet {

		/**
		 * Implement to provide testing of the {@link HttpServlet}.
		 * 
		 * @param req
		 *            {@link HttpServletRequest}.
		 * @param resp
		 *            {@link HttpServletResponse}.
		 * @throws ServletException
		 *             As per API.
		 * @throws IOException
		 *             As per API.
		 */
		protected abstract void test(HttpServletRequest req,
				HttpServletResponse resp) throws ServletException, IOException;

		/*
		 * ====================== HttpServlet ==========================
		 */

		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {
			this.test(req, resp);
		}
	}

}