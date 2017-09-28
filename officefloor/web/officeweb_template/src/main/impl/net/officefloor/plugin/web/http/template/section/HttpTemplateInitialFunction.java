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
package net.officefloor.plugin.web.http.template.section;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.api.function.ManagedFunctionContext;
import net.officefloor.frame.api.function.StaticManagedFunction;
import net.officefloor.plugin.web.http.location.HttpApplicationLocation;
import net.officefloor.plugin.web.http.route.HttpRouteFunction;
import net.officefloor.plugin.web.http.session.HttpSession;
import net.officefloor.plugin.web.http.template.parse.HttpTemplate;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.web.state.HttpRequestState;

/**
 * Initial {@link ManagedFunction} to ensure appropriate conditions for
 * rendering the {@link HttpTemplate}.
 * 
 * @author Daniel Sagenschneider
 */
public class HttpTemplateInitialFunction
		extends StaticManagedFunction<HttpTemplateInitialFunction.Dependencies, HttpTemplateInitialFunction.Flows> {

	/**
	 * Keys for the {@link HttpTemplateInitialFunction} dependencies.
	 */
	public static enum Dependencies {
		SERVER_HTTP_CONNECTION, HTTP_APPLICATION_LOCATION, REQUEST_STATE, HTTP_SESSION
	}

	/**
	 * Keys for the {@link HttpTemplateInitialFunction} flows.
	 */
	public static enum Flows {
		RENDER
	}

	/**
	 * Default HTTP methods to redirect before rendering the
	 * {@link HttpTemplate}.
	 */
	public static final String[] DEFAULT_RENDER_REDIRECT_HTTP_METHODS = new String[] { "POST", "PUT" };

	/**
	 * URI path for the {@link HttpTemplate}.
	 */
	private final String templateUriPath;

	/**
	 * Indicates if a secure {@link ServerHttpConnection} is required.
	 */
	private final boolean isRequireSecure;

	/**
	 * HTTP methods to redirect before rendering the {@link HttpTemplate}.
	 */
	private final Set<String> renderRedirectHttpMethods;

	/**
	 * Content-type for the {@link HttpTemplate}. May be <code>null</code>.
	 */
	private final String contentType;

	/**
	 * {@link Charset} for the {@link HttpTemplate}.
	 */
	private final Charset charset;

	/**
	 * Initiate.
	 * 
	 * @param templateUriPath
	 *            URI path for the {@link HttpTemplate}.
	 * @param isRequireSecure
	 *            Indicates if a secure {@link ServerHttpConnection} is
	 *            required.
	 * @param renderRedirectHttpMethods
	 *            Listing of HTTP methods that require a redirect before
	 *            rendering the {@link HttpTemplate}.
	 * @param contentType
	 *            Content-type for the {@link HttpTemplate}. May be
	 *            <code>null</code>.
	 * @param charset
	 *            {@link Charset} for {@link HttpTemplate}.
	 */
	public HttpTemplateInitialFunction(String templateUriPath, boolean isRequireSecure,
			String[] renderRedirectHttpMethods, String contentType, Charset charset) {
		this.templateUriPath = templateUriPath;
		this.isRequireSecure = isRequireSecure;
		this.contentType = contentType;
		this.charset = charset;

		// Add the render redirect HTTP methods
		Set<String> methods = new HashSet<String>();
		for (String method : (renderRedirectHttpMethods == null ? DEFAULT_RENDER_REDIRECT_HTTP_METHODS
				: renderRedirectHttpMethods)) {
			methods.add(method.trim().toUpperCase());
		}
		this.renderRedirectHttpMethods = methods;
	}

	/*
	 * ======================= ManagedFunction ===============================
	 */

	@Override
	public Object execute(ManagedFunctionContext<Dependencies, Flows> context) throws IOException {

		// Obtain the dependencies
		ServerHttpConnection connection = (ServerHttpConnection) context.getObject(Dependencies.SERVER_HTTP_CONNECTION);
		HttpApplicationLocation location = (HttpApplicationLocation) context
				.getObject(Dependencies.HTTP_APPLICATION_LOCATION);
		HttpRequestState requestState = (HttpRequestState) context.getObject(Dependencies.REQUEST_STATE);
		HttpSession session = (HttpSession) context.getObject(Dependencies.HTTP_SESSION);

		// Flag indicating if redirect is required
		boolean isRedirectRequired = false;

		// Determine if requires a secure connection
		if (this.isRequireSecure) {

			/*
			 * Request may have come in on another URL continuation which did
			 * not require a secure connection and is to now to render this HTTP
			 * template. Therefore trigger redirect for a secure connection.
			 * 
			 * Note that do not down grade to non-secure connection as already
			 * have the request and no need to close the existing secure
			 * connection and establish a new non-secure connection.
			 */
			boolean isConnectionSecure = connection.isSecure();
			if (!isConnectionSecure) {
				// Flag redirect for secure connection
				isRedirectRequired = true;
			}
		}

		// Determine if POST/redirect/GET pattern to be applied
		if (!isRedirectRequired) {
			// Request likely overridden to POST, so use client HTTP method
			String method = connection.getClientHttpMethod().getName();
			if (this.renderRedirectHttpMethods.contains(method.toUpperCase())) {
				// Flag redirect for POST/redirect/GET pattern
				isRedirectRequired = true;
			}
		}

		// Undertake the redirect
		if (isRedirectRequired) {
			HttpRouteFunction.doRedirect(this.templateUriPath, this.isRequireSecure, connection, location, requestState,
					session);
			return null; // redirected, do not render template
		}

		// Configure the response
		if (this.contentType != null) {
			connection.getHttpResponse().setContentType(this.contentType, this.charset);
		}

		// Render the template
		context.doFlow(Flows.RENDER, null, null);
		return null;
	}

}