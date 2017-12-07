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
package net.officefloor.web.spi.security;

import net.officefloor.compile.properties.Property;
import net.officefloor.compile.spi.section.SectionDesigner;
import net.officefloor.compile.test.section.SectionLoaderUtil;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.web.security.HttpAuthentication;
import net.officefloor.web.security.impl.FunctionAuthenticateContext;
import net.officefloor.web.security.impl.HttpAuthenticationRequiredException;
import net.officefloor.web.security.impl.HttpSecurityConfiguration;
import net.officefloor.web.security.impl.HttpSecuritySectionSource;
import net.officefloor.web.security.scheme.BasicHttpSecuritySource;
import net.officefloor.web.security.scheme.DigestHttpSecuritySource;
import net.officefloor.web.security.scheme.FormHttpSecuritySource;
import net.officefloor.web.security.store.CredentialStore;
import net.officefloor.web.security.type.HttpSecurityLoaderUtil;
import net.officefloor.web.session.HttpSession;
import net.officefloor.web.spi.security.HttpCredentials;
import net.officefloor.web.spi.security.HttpLogoutRequest;
import net.officefloor.web.spi.security.HttpSecuritySource;
import net.officefloor.web.state.HttpRequestState;

/**
 * Tests the {@link HttpSecuritySectionSource}.
 * 
 * @author Daniel Sagenschneider
 */
public class HttpSecuritySectionSourceTest extends OfficeFrameTestCase {

	/**
	 * Validates specification.
	 */
	public void testSpecification() {
		SectionLoaderUtil.validateSpecification(HttpSecuritySectionSource.class);
	}

	/**
	 * Validate type for {@link BasicHttpSecuritySource} (does not require
	 * application specific behaviour).
	 */
	public void testTypeForBasicAuthentication() {

		// Create the expected type
		SectionDesigner type = SectionLoaderUtil.createSectionDesigner();

		// Inputs
		type.addSectionInput("Challenge", HttpAuthenticationRequiredException.class.getName());
		type.addSectionInput("Authenticate", Void.class.getName());
		type.addSectionInput("ManagedObjectAuthenticate", FunctionAuthenticateContext.class.getName());
		type.addSectionInput("ManagedObjectLogout", HttpLogoutRequest.class.getName());

		// Outputs
		type.addSectionOutput("Recontinue", null, false);
		type.addSectionOutput("Failure", Throwable.class.getName(), true);

		// Objects
		type.addSectionObject("HTTP_AUTHENTICATION", HttpAuthentication.class.getName());
		type.addSectionObject("SERVER_HTTP_CONNECTION", ServerHttpConnection.class.getName());
		type.addSectionObject("HTTP_SESSION", HttpSession.class.getName());
		type.addSectionObject("HTTP_REQUEST_STATE", HttpRequestState.class.getName());
		type.addSectionObject("DEPENDENCY_CREDENTIAL_STORE", CredentialStore.class.getName());

		// Validate type
		validateType(type, BasicHttpSecuritySource.class, BasicHttpSecuritySource.PROPERTY_REALM, "TEST");
	}

	/**
	 * Validate type for {@link DigestHttpSecuritySource} (does not require
	 * application specific behaviour).
	 */
	public void testTypeForDigestAuthentication() {

		// Create the expected type
		SectionDesigner type = SectionLoaderUtil.createSectionDesigner();

		// Inputs
		type.addSectionInput("Challenge", HttpAuthenticationRequiredException.class.getName());
		type.addSectionInput("Authenticate", Void.class.getName());
		type.addSectionInput("ManagedObjectAuthenticate", FunctionAuthenticateContext.class.getName());
		type.addSectionInput("ManagedObjectLogout", HttpLogoutRequest.class.getName());

		// Outputs
		type.addSectionOutput("Recontinue", null, false);
		type.addSectionOutput("Failure", Throwable.class.getName(), true);

		// Objects
		type.addSectionObject("HTTP_AUTHENTICATION", HttpAuthentication.class.getName());
		type.addSectionObject("SERVER_HTTP_CONNECTION", ServerHttpConnection.class.getName());
		type.addSectionObject("HTTP_SESSION", HttpSession.class.getName());
		type.addSectionObject("HTTP_REQUEST_STATE", HttpRequestState.class.getName());
		type.addSectionObject("DEPENDENCY_CREDENTIAL_STORE", CredentialStore.class.getName());

		// Validate type
		validateType(type, DigestHttpSecuritySource.class, DigestHttpSecuritySource.PROPERTY_REALM, "TEST",
				DigestHttpSecuritySource.PROPERTY_PRIVATE_KEY, "PRIVATE_KEY");
	}

	/**
	 * Validate type for {@link FormHttpSecuritySource} (requires application
	 * specific behaviour).
	 */
	public void testTypeForFormAuthentication() {

		// Create the expected type
		SectionDesigner type = SectionLoaderUtil.createSectionDesigner();

		// Inputs
		type.addSectionInput("Challenge", HttpAuthenticationRequiredException.class.getName());
		type.addSectionInput("Authenticate", HttpCredentials.class.getName());
		type.addSectionInput("ManagedObjectAuthenticate", FunctionAuthenticateContext.class.getName());
		type.addSectionInput("ManagedObjectLogout", HttpLogoutRequest.class.getName());

		// Outputs
		type.addSectionOutput("Recontinue", null, false);
		type.addSectionOutput("Failure", Throwable.class.getName(), true);
		type.addSectionOutput("FORM_LOGIN_PAGE", Void.class.getName(), false);

		// Objects
		type.addSectionObject("HTTP_AUTHENTICATION", HttpAuthentication.class.getName());
		type.addSectionObject("SERVER_HTTP_CONNECTION", ServerHttpConnection.class.getName());
		type.addSectionObject("HTTP_SESSION", HttpSession.class.getName());
		type.addSectionObject("HTTP_REQUEST_STATE", HttpRequestState.class.getName());
		type.addSectionObject("DEPENDENCY_CREDENTIAL_STORE", CredentialStore.class.getName());

		// Validate type
		validateType(type, FormHttpSecuritySource.class, FormHttpSecuritySource.PROPERTY_REALM, "TEST");
	}

	/**
	 * Validates the type.
	 * 
	 * @param expectedType
	 *            Expected type.
	 * @param httpSecuritySourceClass
	 *            {@link HttpSecuritySource} class.
	 * @param propertyNameValuePairs
	 *            {@link Property} name/value pairs.
	 */
	private static <A, AC, C, O extends Enum<O>, F extends Enum<F>, HS extends HttpSecuritySource<A, AC, C, O, F>> void validateType(
			SectionDesigner expectedType, Class<HS> httpSecuritySourceClass, String... propertyNameValuePairs) {
		HS source = HttpSecurityLoaderUtil.newHttpSecuritySource(httpSecuritySourceClass);
		HttpSecurityConfiguration<A, AC, C, O, F> configuration = HttpSecurityLoaderUtil
				.loadHttpSecurityConfiguration(source, propertyNameValuePairs);
		SectionLoaderUtil.validateSectionType(expectedType, new HttpSecuritySectionSource(configuration), null);
	}

}