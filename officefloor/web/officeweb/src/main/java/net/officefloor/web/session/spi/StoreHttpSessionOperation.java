/*-
 * #%L
 * Web Plug-in
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

package net.officefloor.web.session.spi;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

import net.officefloor.web.session.HttpSession;

/**
 * Operation to obtain details of storing the {@link HttpSession} within the
 * {@link HttpSessionStore}.
 *
 * @author Daniel Sagenschneider
 */
public interface StoreHttpSessionOperation {

	/**
	 * Obtains the session Id of the {@link HttpSession} to store.
	 *
	 * @return Session Id of the {@link HttpSession} to store.
	 */
	String getSessionId();

	/**
	 * Obtains the creation time for the {@link HttpSession}.
	 *
	 * @return Creation time for the {@link HttpSession}.
	 */
	Instant getCreationTime();

	/**
	 * Obtains the time to expire the {@link HttpSession} should it be idle.
	 *
	 * @return Time to expire the {@link HttpSession} should it be idle.
	 */
	Instant getExpireTime();

	/**
	 * Obtains the attributes of the {@link HttpSession}.
	 *
	 * @return Attributes of the {@link HttpSession}.
	 */
	Map<String, Serializable> getAttributes();

	/**
	 * Flags the {@link HttpSession} was stored successfully within the
	 * {@link HttpSessionStore}.
	 */
	void sessionStored();

	/**
	 * Flags failed to store the {@link HttpSession} within the
	 * {@link HttpSessionStore}.
	 *
	 * @param cause
	 *            Cause of the failure.
	 */
	void failedToStoreSession(Throwable cause);

}
