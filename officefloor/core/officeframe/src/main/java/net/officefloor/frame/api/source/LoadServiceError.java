/*-
 * #%L
 * OfficeFrame
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

package net.officefloor.frame.api.source;

/**
 * Indicates a service was not able to be loaded.
 * <p>
 * This is a critical error as services should always be able to be loaded.
 * 
 * @author Daniel Sagenschneider
 */
public class LoadServiceError extends AbstractSourceError {

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link ServiceFactory} {@link Class} name that failed to load.
	 */
	private final String serviceFactoryClassName;

	/**
	 * Initiate.
	 * 
	 * @param serviceFactoryClassName {@link ServiceFactory} {@link Class} name that
	 *                                failed to load.
	 * @param failure                 Cause.
	 */
	public LoadServiceError(String serviceFactoryClassName, Throwable failure) {
		super("Failed to create service from " + serviceFactoryClassName, failure);
		this.serviceFactoryClassName = serviceFactoryClassName;
	}

	/**
	 * Obtains the {@link ServiceFactory} {@link Class} name.
	 * 
	 * @return {@link ServiceFactory} {@link Class} name.
	 */
	public String getServiceFactoryClassName() {
		return this.serviceFactoryClassName;
	}

}
