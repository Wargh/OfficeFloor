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
package net.officefloor.web;

import java.io.Serializable;

import net.officefloor.frame.api.build.None;
import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.api.function.ManagedFunctionContext;
import net.officefloor.frame.api.function.ManagedFunctionFactory;
import net.officefloor.server.http.HttpHeaderName;
import net.officefloor.server.http.HttpResponse;
import net.officefloor.server.http.HttpResponseCookie;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.web.build.HttpPathFactory;
import net.officefloor.web.session.HttpSession;
import net.officefloor.web.state.HttpRequestState;
import net.officefloor.web.state.HttpRequestStateManagedObjectSource;

/**
 * {@link ManagedFunction} to send a redirect.
 * 
 * @author Daniel Sagenschneider
 */
public class HttpRedirectFunction<T>
		implements ManagedFunctionFactory<HttpRedirectFunction.HttpRedirectDependencies, None>,
		ManagedFunction<HttpRedirectFunction.HttpRedirectDependencies, None> {

	/**
	 * Name of the {@link HttpSession} attribute containing the
	 * {@link HttpRequestState} momento.
	 */
	public static final String SESSION_ATTRIBUTE_REDIRECT_MOMENTO = "_redirect_";

	/**
	 * Name of {@link HttpResponseCookie} to indicate a redirect.
	 */
	public static final String REDIRECT_COOKIE_NAME = "ofr";

	/**
	 * Dependency keys.
	 */
	public static enum HttpRedirectDependencies {
		PATH_VALUES, SERVER_HTTP_CONNECTION, REQUEST_STATE, SESSION_STATE
	}

	/**
	 * <code>location</code> {@link HttpHeaderName}.
	 */
	private static final HttpHeaderName LOCATION = new HttpHeaderName("location");

	/**
	 * Indicates if redirect to secure port.
	 */
	private final boolean isSecure;

	/**
	 * {@link HttpPathFactory}.
	 */
	private final HttpPathFactory<T> pathFactory;

	/**
	 * Instantiate.
	 * 
	 * @param isSecure    Indicates if redirect to secure port.
	 * @param pathFactory {@link HttpPathFactory}.
	 */
	public HttpRedirectFunction(boolean isSecure, HttpPathFactory<T> pathFactory) {
		this.isSecure = isSecure;
		this.pathFactory = pathFactory;
	}

	/*
	 * ============ ManagedFunctionFactory =============
	 */

	@Override
	public ManagedFunction<HttpRedirectDependencies, None> createManagedFunction() {
		return this;
	}

	/*
	 * =============== ManagedFunction =================
	 */

	@Override
	@SuppressWarnings("unchecked")
	public void execute(ManagedFunctionContext<HttpRedirectDependencies, None> context) throws Exception {

		// Obtain the dependencies
		T pathValues = (T) context.getObject(HttpRedirectDependencies.PATH_VALUES);
		ServerHttpConnection connection = (ServerHttpConnection) context
				.getObject(HttpRedirectDependencies.SERVER_HTTP_CONNECTION);
		HttpRequestState requestState = (HttpRequestState) context.getObject(HttpRedirectDependencies.REQUEST_STATE);
		HttpSession session = (HttpSession) context.getObject(HttpRedirectDependencies.SESSION_STATE);

		// Obtain the application path on server for redirect
		String applicationPath = this.pathFactory.createApplicationClientPath(pathValues);

		// Determine if require upgrade to secure connection
		String redirectLocation = applicationPath;
		if (this.isSecure && (!connection.isSecure())) {
			// Upgrade redirect to secure path
			redirectLocation = connection.getServerLocation().createClientUrl(this.isSecure, redirectLocation);
		}

		// Send the redirect
		HttpResponse response = connection.getResponse();
		response.setStatus(HttpStatus.SEE_OTHER);
		response.getHeaders().addHeader(LOCATION, redirectLocation);

		// Export the request state
		Serializable momento = HttpRequestStateManagedObjectSource.exportHttpRequestState(requestState);

		// Create wrapping serialisable to line up with cookie
		SerialisedRequestState serialisable = new SerialisedRequestState(momento);

		// Store in session (to import on servicing redirect)
		session.setAttribute(SESSION_ATTRIBUTE_REDIRECT_MOMENTO, serialisable);

		// Load cookie indicating redirect
		response.getCookies().setCookie(REDIRECT_COOKIE_NAME, String.valueOf(serialisable.identifier),
				(cookie) -> cookie.setPath(applicationPath).setSecure(this.isSecure).setHttpOnly(true));
	}

}