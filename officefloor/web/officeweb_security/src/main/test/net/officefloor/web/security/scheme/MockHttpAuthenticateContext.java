/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2018 Daniel Sagenschneider
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.officefloor.web.security.scheme;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.officefloor.server.http.HttpHeader;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.server.http.mock.MockHttpRequestBuilder;
import net.officefloor.server.http.mock.MockHttpServer;
import net.officefloor.web.mock.MockWebApp;
import net.officefloor.web.security.impl.AuthenticationContextManagedObjectSource;
import net.officefloor.web.session.HttpSession;
import net.officefloor.web.spi.security.AuthenticateContext;
import net.officefloor.web.spi.security.HttpSecuritySource;
import net.officefloor.web.state.HttpRequestState;

/**
 * Mock {@link AuthenticateContext} for testing {@link HttpSecuritySource}
 * instances.
 * 
 * @author Daniel Sagenschneider
 */
public class MockHttpAuthenticateContext<AC extends Serializable, O extends Enum<O>>
		implements AuthenticateContext<AC, O> {

	/**
	 * Creates the {@link ServerHttpConnection} with authorization
	 * {@link HttpHeader} value.
	 * 
	 * @param authorizationHeaderValue Authorization {@link HttpHeader} value.
	 * @return {@link ServerHttpConnection}.
	 */
	public static ServerHttpConnection createRequestWithAuthorizationHeader(String authorizationHeaderValue) {

		// Create the HTTP request
		MockHttpRequestBuilder request = MockHttpServer.mockRequest();
		if (authorizationHeaderValue != null) {
			request.header("Authorization", authorizationHeaderValue);
		}

		// Return the connection with request
		return MockHttpServer.mockConnection(request);
	}

	/**
	 * {@link ServerHttpConnection}.
	 */
	private final ServerHttpConnection connection;

	/**
	 * {@link HttpSession}.
	 */
	private final HttpSession session;

	/**
	 * {@link HttpRequestState}.
	 */
	private final HttpRequestState requestState;

	/**
	 * Dependencies.
	 */
	private final Map<O, Object> dependencies = new HashMap<O, Object>();

	/**
	 * Access control.
	 */
	private AC accessControl = null;

	/**
	 * Escalation.
	 */
	private Throwable escalation = null;

	/**
	 * Initiate with no <code>authorization</code> {@link HttpHeader}.
	 */
	public MockHttpAuthenticateContext() {
		this((String) null);
	}

	/**
	 * Initiate.
	 * 
	 * @param authorizationHeaderValue <code>authorization</code> {@link HttpHeader}
	 *                                 value.
	 */
	public MockHttpAuthenticateContext(String authorizationHeaderValue) {
		this(createRequestWithAuthorizationHeader(authorizationHeaderValue));
	}

	/**
	 * Initiate.
	 * 
	 * @param connection {@link ServerHttpConnection}.
	 */
	public MockHttpAuthenticateContext(ServerHttpConnection connection) {
		this.connection = connection;
		this.session = MockWebApp.mockSession(this.connection);
		this.requestState = MockWebApp.mockRequestState(this.connection);
	}

	/**
	 * Registers and object.
	 * 
	 * @param key        Key for dependency.
	 * @param dependency Dependency object.
	 */
	public void registerObject(O key, Object dependency) {
		this.dependencies.put(key, dependency);
	}

	/**
	 * Obtains the registered access control.
	 * 
	 * @return Access control.
	 */
	public AC getAccessControl() {
		return this.accessControl;
	}

	/**
	 * Obtains the registered escalation.
	 * 
	 * @return Escalation.
	 */
	public Throwable getEscalation() {
		return this.escalation;
	}

	/*
	 * ==================== HttpAuthenticateContext =========================
	 */

	@Override
	public ServerHttpConnection getConnection() {
		return this.connection;
	}

	@Override
	public String getQualifiedAttributeName(String attributeName) {
		return AuthenticationContextManagedObjectSource.getQualifiedAttributeName("mock", attributeName);
	}

	@Override
	public HttpSession getSession() {
		return this.session;
	}

	@Override
	public HttpRequestState getRequestState() {
		return this.requestState;
	}

	@Override
	public Object getObject(O key) {
		return this.dependencies.get(key);
	}

	@Override
	public void accessControlChange(AC accessControl, Throwable escalation) {
		this.accessControl = accessControl;
		this.escalation = escalation;
	}

}