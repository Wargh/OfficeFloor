/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2017 Daniel Sagenschneider
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
package net.officefloor.web.build;

import net.officefloor.server.http.HttpRequest;

/**
 * HTTP parameter.
 * 
 * @author Daniel Sagenschneider
 */
public interface HttpParameter {

	/**
	 * Obtains the name.
	 * 
	 * @return Name.
	 */
	String getName();

	/**
	 * Obtains the {@link HttpValueLocation} on the {@link HttpRequest}.
	 * 
	 * @return {@link HttpValueLocation} on the {@link HttpRequest}.
	 */
	HttpValueLocation getLocation();

	/**
	 * Indicates if required.
	 * 
	 * @return <code>true</code> if required.
	 */
	boolean isRequired();

}