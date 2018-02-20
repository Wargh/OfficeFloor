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
package net.officefloor.web.security.integrate;

import java.nio.charset.Charset;
import java.util.Base64;

import net.officefloor.compile.spi.office.OfficeManagedObjectSource;
import net.officefloor.frame.internal.structure.ManagedObjectScope;
import net.officefloor.server.http.mock.MockHttpRequestBuilder;
import net.officefloor.server.http.mock.MockHttpResponse;
import net.officefloor.server.http.mock.MockHttpServer;
import net.officefloor.web.compile.CompileWebContext;
import net.officefloor.web.security.build.HttpSecurityArchitect;
import net.officefloor.web.security.build.HttpSecurityBuilder;
import net.officefloor.web.security.scheme.BasicHttpSecuritySource;
import net.officefloor.web.security.store.CredentialStore;
import net.officefloor.web.security.store.PasswordFileManagedObjectSource;

/**
 * Integrate tests the {@link BasicHttpSecuritySource}.
 * 
 * @author Daniel Sagenschneider
 */
public class BasicHttpSecurityIntegrateTest extends AbstractHttpSecurityIntegrateTestCase {

	@Override
	protected HttpSecurityBuilder configureHttpSecurity(CompileWebContext context,
			HttpSecurityArchitect securityArchitect) {

		// Configure the HTTP Security
		HttpSecurityBuilder security = securityArchitect.addHttpSecurity("BASIC",
				BasicHttpSecuritySource.class.getName());
		security.addProperty(BasicHttpSecuritySource.PROPERTY_REALM, "TestRealm");

		// Obtain the password file
		String passwordFilePath;
		try {
			passwordFilePath = this.findFile(this.getClass(), "basic-password-file.txt").getAbsolutePath();
		} catch (Throwable ex) {
			throw fail(ex);
		}

		// Password File Credential Store
		OfficeManagedObjectSource passwordFileMos = context.getOfficeArchitect().addOfficeManagedObjectSource(
				CredentialStore.class.getSimpleName(), PasswordFileManagedObjectSource.class.getName());
		passwordFileMos.addProperty(PasswordFileManagedObjectSource.PROPERTY_PASSWORD_FILE_PATH, passwordFilePath);
		passwordFileMos.addOfficeManagedObject(CredentialStore.class.getSimpleName(), ManagedObjectScope.PROCESS);

		// Return the HTTP Security
		return security;
	}

	/**
	 * Ensure can integrate.
	 */
	public void testIntegration() throws Exception {

		// Should not authenticate (without credentials)
		this.doRequest("/service", 401, "");

		// Should authenticate with credentials
		this.doRequest("/service", "daniel", "password", 200, "Serviced for daniel");
	}

	/**
	 * Ensure can go through multiple login attempts.
	 */
	public void testMultipleLoginAttempts() throws Exception {

		// Ensure can try multiple times
		this.doRequest("/service", 401, "");
		this.doRequest("/service", 401, "");
		this.doRequest("/service", 401, "");
		this.doRequest("/service", 401, "");

		// Should authenticate with credentials
		this.doRequest("/service", "daniel", "password", 200, "Serviced for daniel");
	}

	/**
	 * Ensure can logout.
	 */
	public void testLogout() throws Exception {

		// Authenticate with credentials
		MockHttpResponse response = this.doRequest("/service", "daniel", "password", 200, "Serviced for daniel");

		// Request again to ensure stay logged in
		this.doRequest("/service", response, 200, "Serviced for daniel");

		// Logout
		this.doRequest("/logout", response, 200, "LOGOUT");

		// Should require to log in (after the log out)
		this.doRequest("/service", response, 401, "");
	}

	/**
	 * Undertakes the request.
	 */
	private MockHttpResponse doRequest(String path, String username, String password, int status, String entity) {

		// Undertake the request
		MockHttpRequestBuilder request = MockHttpServer.mockRequest(path);
		if (username != null) {

			// Create the challenge
			String plain = username + ":" + password;
			String cypher = Base64.getEncoder().encodeToString(plain.getBytes(Charset.forName("UTF-8")));
			String authorisation = "Basic " + cypher;

			// Add the authorization
			request.header("Authorization", authorisation);
		}
		MockHttpResponse response = this.server.send(request);

		// Ensure appropriate response
		response.assertResponse(status, entity);

		// Return the response
		return response;
	}

}