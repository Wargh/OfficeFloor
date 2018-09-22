/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2018 Daniel Sagenschneider
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
package net.officefloor.compile.managedobject;

import net.officefloor.frame.api.executive.ExecutionStrategy;
import net.officefloor.frame.api.managedobject.ManagedObject;

/**
 * <code>Type definition</code> of a {@link ExecutionStrategy} required by the
 * {@link ManagedObject}.
 *
 * @author Daniel Sagenschneider
 */
public interface ManagedObjectExecutionStrategyType {

	/**
	 * Obtains the name to identify requirement of a {@link ExecutionStrategy}.
	 * 
	 * @return Name to identify requirement of a {@link ExecutionStrategy}.
	 */
	String getExecutionStrategyName();

}