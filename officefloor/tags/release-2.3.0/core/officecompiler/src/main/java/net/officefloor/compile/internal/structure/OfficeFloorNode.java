/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2012 Daniel Sagenschneider
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

package net.officefloor.compile.internal.structure;

import net.officefloor.compile.spi.officefloor.OfficeFloorDeployer;
import net.officefloor.compile.spi.officefloor.OfficeFloorManagedObjectSource;
import net.officefloor.compile.spi.officefloor.OfficeFloorSupplier;
import net.officefloor.frame.api.OfficeFrame;
import net.officefloor.frame.api.manage.OfficeFloor;

/**
 * {@link OfficeFloor} node.
 * 
 * @author Daniel Sagenschneider
 */
public interface OfficeFloorNode extends OfficeFloorDeployer {

	/**
	 * Adds a {@link OfficeFloorManagedObjectSource} supplied from an
	 * {@link OfficeFloorSupplier}.
	 * 
	 * @param managedObjectSourceName
	 *            Name of the {@link OfficeFloorManagedObjectSource}.
	 * @param suppliedManagedObject
	 *            {@link SuppliedManagedObjectNode} to supply the
	 *            {@link OfficeFloorManagedObjectSource}.
	 * @return {@link OfficeFloorManagedObjectSource}.
	 */
	OfficeFloorManagedObjectSource addManagedObjectSource(
			String managedObjectSourceName,
			SuppliedManagedObjectNode suppliedManagedObject);

	/**
	 * Deploys the {@link OfficeFloor}.
	 * 
	 * @param officeFrame
	 *            {@link OfficeFrame} to deploy the {@link OfficeFloor} within.
	 * @return {@link OfficeFloor}.
	 */
	OfficeFloor deployOfficeFloor(OfficeFrame officeFrame);

}