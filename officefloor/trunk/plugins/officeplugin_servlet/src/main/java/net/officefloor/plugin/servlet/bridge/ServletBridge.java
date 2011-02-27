/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2011 Daniel Sagenschneider
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
package net.officefloor.plugin.servlet.bridge;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides the {@link HttpServletRequest} and {@link HttpServletResponse} from
 * a {@link Servlet} container in servicing a request.
 * 
 * @author Daniel Sagenschneider
 */
public interface ServletBridge {

	/**
	 * Obtains the {@link HttpServletRequest}.
	 * 
	 * @return {@link HttpServletRequest}.
	 */
	HttpServletRequest getRequest();

	/**
	 * Obtains the {@link HttpServletResponse}.
	 * 
	 * @return {@link HttpServletResponse}.
	 */
	HttpServletResponse getResponse();

	/**
	 * <p>
	 * Obtains the object by type from the {@link Servlet}.
	 * <p>
	 * This allows access to the dependency injected objects for the
	 * {@link Servlet}.
	 * 
	 * @param objectType
	 *            Type of the {@link Object}.
	 * @return {@link Object}.
	 */
	<O> O getObject(Class<? extends O> objectType);

}