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
package net.officefloor.compile.spi.officefloor;

import net.officefloor.compile.spi.managedobject.ManagedObjectFlow;

/**
 * Augmented {@link ManagedObjectFlow}.
 * 
 * @author Daniel Sagenschneider
 */
public interface AugmentedManagedObjectFlow {

	/**
	 * Obtains the name of this {@link ManagedObjectFlow}.
	 * 
	 * @return Name of this {@link ManagedObjectFlow}.
	 */
	String getManagedObjectFlowName();

	/**
	 * Indicates if the {@link ManagedObjectFlow} is already linked.
	 * 
	 * @return <code>true</code> if already linked.
	 */
	boolean isLinked();

}