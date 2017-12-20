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
package net.officefloor.web.security.impl;

import java.io.Serializable;

import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.api.function.ManagedFunctionContext;
import net.officefloor.frame.api.function.ManagedFunctionFactory;
import net.officefloor.frame.api.function.StaticManagedFunction;
import net.officefloor.web.spi.security.AuthenticationContext;

/**
 * {@link ManagedFunction} and {@link ManagedFunctionFactory} for triggering
 * authentication with application specific credentials.
 * 
 * @author Daniel Sagenschneider
 */
public class StartApplicationHttpAuthenticateFunction<AC extends Serializable, C> extends
		StaticManagedFunction<StartApplicationHttpAuthenticateFunction.Dependencies, StartApplicationHttpAuthenticateFunction.Flows> {

	/**
	 * Dependency keys.
	 */
	public static enum Dependencies {
		AUTHENTICATION_CONTEXT, CREDENTIALS
	}

	/**
	 * Flow keys.
	 */
	public static enum Flows {
		FAILURE
	}

	/*
	 * ====================== ManagedFunction =============================
	 */

	@Override
	@SuppressWarnings("unchecked")
	public Object execute(ManagedFunctionContext<Dependencies, Flows> context) throws Throwable {

		// Obtain the dependencies
		AuthenticationContext<AC, C> authenticationContext = (AuthenticationContext<AC, C>) context
				.getObject(Dependencies.AUTHENTICATION_CONTEXT);
		C credentials = (C) context.getObject(Dependencies.CREDENTIALS);

		// Trigger authentication
		try {
			authenticationContext.authenticate(credentials, (failure) -> {
				if (failure != null) {
					context.doFlow(Flows.FAILURE, failure, null);
				}
			});
		} catch (Throwable ex) {
			context.doFlow(Flows.FAILURE, ex, null);
		}

		// Nothing further
		return null;
	}

}