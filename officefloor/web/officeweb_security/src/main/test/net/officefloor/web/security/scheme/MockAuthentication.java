/*-
 * #%L
 * Web Security
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

package net.officefloor.web.security.scheme;

import java.util.function.Consumer;

import net.officefloor.frame.api.escalate.Escalation;
import net.officefloor.server.http.HttpException;
import net.officefloor.web.security.AuthenticationRequiredException;
import net.officefloor.web.spi.security.AuthenticationContext;

/**
 * Mock authentication.
 * 
 * @author Daniel Sagenschneider
 */
public class MockAuthentication {

	/**
	 * {@link AuthenticationContext}.
	 */
	private final AuthenticationContext<MockAccessControl, ?> authenticationContext;

	/**
	 * {@link MockAccessControl}.
	 */
	private MockAccessControl accessControl = null;

	/**
	 * {@link Escalation}.
	 */
	private Throwable escalation = null;

	/**
	 * Instantiate.
	 * 
	 * @param authenticationContext
	 *            {@link AuthenticationContext}.
	 */
	public MockAuthentication(AuthenticationContext<MockAccessControl, ?> authenticationContext) {
		this.authenticationContext = authenticationContext;

		// Listen for access control
		this.authenticationContext.register((accessControl, failure) -> {
			this.accessControl = accessControl;
			this.escalation = failure;
		});
	}

	/**
	 * Indicates if authenticated.
	 * 
	 * @return <code>true</code> if authenticated.
	 */
	public boolean isAuthenticated() {
		return this.authenticationContext.run(() -> this.accessControl != null);
	}

	/**
	 * Undertakes authentication.
	 *
	 * @param credentials
	 *            {@link MockCredentials}.
	 * @param completion
	 *            Optional completion listener.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void authenticate(MockCredentials credentials, Consumer<Throwable> completion) {
		((AuthenticationContext) this.authenticationContext).authenticate(credentials, (failure) -> {
			if (completion != null) {
				completion.accept(failure);
			}
		});
	}

	/**
	 * Obtains the {@link MockAccessControl}.
	 * 
	 * @return {@link MockAccessControl}.
	 */
	public MockAccessControl getAccessControl() {
		return this.authenticationContext.run(() -> {

			// Propagate potential failure
			if (this.escalation != null) {
				if (this.escalation instanceof HttpException) {
					throw (HttpException) this.escalation;
				} else {
					throw new HttpException(this.escalation);
				}
			}

			// Ensure have access control
			if (this.accessControl == null) {
				throw new AuthenticationRequiredException(this.authenticationContext.getQualifier());
			}

			// Return the access control
			return this.accessControl;
		});
	}

	/**
	 * Undertakes logout.
	 * 
	 * @param completion
	 *            Optional completion listener.
	 */
	public void logout(Consumer<Throwable> completion) {
		this.authenticationContext.logout((failure) -> {
			if (completion != null) {
				completion.accept(failure);
			}
		});
	}

}
