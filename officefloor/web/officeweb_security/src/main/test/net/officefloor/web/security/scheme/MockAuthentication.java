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