/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2013 Daniel Sagenschneider
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
package net.officefloor.plugin.web.http.security.integrate;

import java.io.IOException;
import java.io.PrintWriter;

import net.officefloor.autowire.AutoWireOfficeFloor;
import net.officefloor.autowire.AutoWireSection;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.plugin.section.clazz.ClassSectionSource;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.socket.server.http.HttpRequest;
import net.officefloor.plugin.socket.server.http.HttpTestUtil;
import net.officefloor.plugin.socket.server.http.ServerHttpConnection;
import net.officefloor.plugin.socket.server.http.protocol.HttpStatus;
import net.officefloor.plugin.web.http.application.HttpSecuritySection;
import net.officefloor.plugin.web.http.application.WebArchitect;
import net.officefloor.plugin.web.http.security.HttpAuthentication;
import net.officefloor.plugin.web.http.security.HttpCredentials;
import net.officefloor.plugin.web.http.security.HttpSecurity;
import net.officefloor.plugin.web.http.security.HttpSecuritySource;
import net.officefloor.plugin.web.http.server.HttpServerAutoWireOfficeFloorSource;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Abstract functionality for integration testing of the
 * {@link HttpSecuritySource} implementations.
 * 
 * @author Daniel Sagenschneider
 */
public abstract class AbstractHttpSecurityIntegrateTestCase extends
		OfficeFrameTestCase {

	/**
	 * Port to use for testing.
	 */
	private final int PORT = HttpTestUtil.getAvailablePort();

	/**
	 * {@link CloseableHttpClient} to use for testing.
	 */
	private CloseableHttpClient client = HttpTestUtil.createHttpClient(false);

	/**
	 * {@link HttpClientContext}.
	 */
	private HttpClientContext context = new HttpClientContext();

	/**
	 * FIXME: flag indicating if fix for Digest authentication in that the
	 * {@link HttpClient} does not send cookies on authentication request.
	 */
	private boolean isDigestHttpClientCookieBug = false;

	/**
	 * {@link AutoWireOfficeFloor}.
	 */
	private AutoWireOfficeFloor officeFloor;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// Configure the application
		WebArchitect source = new HttpServerAutoWireOfficeFloorSource(
				PORT);

		// Configure the HTTP Security
		assertNull("Should not have HTTP Security configured",
				source.getHttpSecurity());
		HttpSecuritySection security = this
				.configureHttpSecurity(source);

		// Add servicing methods
		AutoWireSection section = source.addSection("SERVICE",
				ClassSectionSource.class.getName(), Servicer.class.getName());
		source.linkUri("service", section, "service");
		source.linkUri("logout", section, "logout");

		// Determine if security configured
		if (security != null) {
			source.link(security, "Failure", section, "handleFailure");
		}

		// Start the office
		this.officeFloor = source.openOfficeFloor();
	}

	/**
	 * Configures the {@link HttpSecuritySection}.
	 * 
	 * @param application
	 *            {@link WebArchitect}.
	 * @return Confgured {@link HttpSecuritySection}.
	 */
	protected abstract HttpSecuritySection configureHttpSecurity(
			WebArchitect application) throws Exception;

	@Override
	protected void tearDown() throws Exception {
		// Stop client then server
		try {
			this.client.close();
		} finally {
			if (this.officeFloor != null) {
				this.officeFloor.closeOfficeFloor();
			}
		}
	}

	/**
	 * Use credentials.
	 * 
	 * @param realm
	 *            Security realm.
	 * @param scheme
	 *            Security scheme.
	 * @param username
	 *            User name.
	 * @param password
	 *            Password.
	 * @return {@link CredentialsProvider}.
	 * @throws IOException
	 *             If fails to use credentials.
	 */
	protected CredentialsProvider useCredentials(String realm, String scheme,
			String username, String password) throws IOException {

		// Close the existing client
		this.client.close();

		// Use client with credentials
		HttpClientBuilder builder = HttpClientBuilder.create();
		CredentialsProvider provider = HttpTestUtil.configureCredentials(
				builder, realm, scheme, username, password);
		this.client = builder.build();

		// Reset the client context
		this.context = new HttpClientContext();

		// FIXME: determine if HttpClient cookie authentication fix
		if ("Digest".equalsIgnoreCase(scheme)) {
			isDigestHttpClientCookieBug = true;
		}

		// Return the credentials provider
		return provider;
	}

	/**
	 * Asserts the response from the {@link HttpGet}.
	 * 
	 * @param requestUriPath
	 *            Request URI path.
	 * @param expectedStatus
	 *            Expected status.
	 * @param expectedBodyContent
	 *            Expected body content.
	 */
	protected void doRequest(String requestUriPath, int expectedStatus,
			String expectedBodyContent) {
		try {

			// Undertake the request
			HttpGet request = new HttpGet("http://localhost:" + PORT + "/"
					+ requestUriPath);

			// Execute the method
			org.apache.http.HttpResponse response;
			if (this.isDigestHttpClientCookieBug) {
				// Use the context to keep cookies
				response = this.client.execute(request, this.context);
			} else {
				// Follow normal use
				response = this.client.execute(request);
			}

			/*
			 * FIXME: work-around for bug in HttpClient no cookies on
			 * authentication
			 */
			if (this.isDigestHttpClientCookieBug
					&& (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED)) {
				// Try authentication again, with the cookie
				this.context.getTargetAuthState().reset();
				request.reset();
				response = this.client.execute(request, this.context);
			}

			// Obtain the details of the response
			int status = response.getStatusLine().getStatusCode();
			String body = HttpTestUtil.getEntityBody(response);

			// Verify response
			assertEquals("Should be successful. Response: " + body + " ["
					+ response.getStatusLine().getReasonPhrase() + "]",
					expectedStatus, status);
			assertEquals("Incorrect response body", expectedBodyContent, body);

		} catch (Exception ex) {
			throw fail(ex);
		}
	}

	/**
	 * Services the {@link HttpRequest}.
	 */
	public static class Servicer {

		/**
		 * Services the {@link HttpRequest}.
		 * 
		 * @param security
		 *            {@link HttpSecurity} dependency ensures authentication
		 *            before servicing.
		 * @param connection
		 *            {@link ServerHttpConnection}.
		 * @throws IOException
		 *             If fails.
		 */
		public void service(HttpSecurity security,
				ServerHttpConnection connection) throws IOException {
			connection
					.getHttpResponse()
					.getEntityWriter()
					.write("Serviced for "
							+ (security == null ? "guest" : security
									.getRemoteUser()));
		}

		/**
		 * Undertakes logging out.
		 * 
		 * @param authentication
		 *            {@link HttpAuthentication}.
		 * @param connection
		 *            {@link ServerHttpConnection}.
		 * @throws IOException
		 *             If fails.
		 */
		public void logout(
				HttpAuthentication<HttpSecurity, HttpCredentials> authentication,
				ServerHttpConnection connection) throws IOException {

			// Log out
			authentication.logout(null);

			// Indicate logged out
			connection.getHttpResponse().getEntityWriter().write("LOGOUT");
		}

		/**
		 * Handles failure.
		 * 
		 * @param failure
		 *            Failure.
		 * @param connection
		 *            {@link ServerHttpConnection}.
		 * @throws IOException
		 *             If fails.
		 */
		public void handleFailure(@Parameter Throwable failure,
				ServerHttpConnection connection) throws IOException {
			PrintWriter writer = new PrintWriter(connection.getHttpResponse()
					.getEntityWriter());
			writer.write("ERROR: ");
			failure.printStackTrace(writer);
			writer.flush();
		}
	}

}