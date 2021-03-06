/*-
 * #%L
 * HTTP Server
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

package net.officefloor.server.stream.impl;

import java.io.IOException;

import net.officefloor.server.stream.ServerOutputStream;

/**
 * Handles closing the {@link ServerOutputStream}.
 * 
 * @author Daniel Sagenschneider
 */
public interface CloseHandler {

	/**
	 * Indicates if closed.
	 * 
	 * @return <code>true</code> if closed.
	 */
	boolean isClosed();

	/**
	 * Handles the close.
	 * 
	 * @throws IOException
	 *             If fails to close.
	 */
	void close() throws IOException;

}
