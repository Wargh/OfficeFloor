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
package net.officefloor.frame.internal.configuration;

import net.officefloor.frame.api.execute.ManagedFunction;
import net.officefloor.frame.api.execute.Work;
import net.officefloor.frame.internal.structure.FunctionState;

/**
 * Reference to a {@link FunctionState}.
 * 
 * @author Daniel Sagenschneider
 */
public interface TaskNodeReference {

	/**
	 * Obtains the name of the {@link Work} containing the {@link ManagedFunction}.
	 * 
	 * @return Name of the {@link Work} containing the {@link ManagedFunction}.
	 */
	String getWorkName();

	/**
	 * Obtains the name of the {@link ManagedFunction}.
	 * 
	 * @return Name of the {@link ManagedFunction}.
	 */
	String getTaskName();

	/**
	 * Obtains the type of argument to be passed to the referenced {@link ManagedFunction}.
	 * 
	 * @return Type of argument to be passed to the referenced {@link ManagedFunction}.
	 */
	Class<?> getArgumentType();

}