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
package net.officefloor.compile;

import java.util.ServiceLoader;

/**
 * {@link ServiceLoader} service to enable configuring the
 * {@link OfficeFloorCompiler}.
 * 
 * @author Daniel Sagenschneider
 */
public interface OfficeFloorCompilerConfigurationService {

	/**
	 * Enables configuring the {@link OfficeFloorCompiler}.
	 * 
	 * @param compiler
	 *            {@link OfficeFloorCompiler}.
	 * @throws Exception
	 *             If fails to configured the {@link OfficeFloorCompiler}.
	 */
	void configureOfficeFloorCompiler(OfficeFloorCompiler compiler) throws Exception;

}