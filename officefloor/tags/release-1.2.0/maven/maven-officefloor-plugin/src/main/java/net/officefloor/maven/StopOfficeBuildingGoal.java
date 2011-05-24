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

package net.officefloor.maven;

import net.officefloor.building.manager.OfficeBuildingManager;
import net.officefloor.building.manager.OfficeBuildingManagerMBean;
import net.officefloor.console.OfficeBuilding;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Maven goal to stop the {@link OfficeBuilding}.
 * 
 * @goal stop
 * 
 * @author Daniel Sagenschneider
 */
public class StopOfficeBuildingGoal extends AbstractGoal {

	/**
	 * Port that {@link OfficeBuilding} is running on.
	 * 
	 * @parameter
	 */
	private Integer port = StartOfficeBuildingGoal.DEFAULT_OFFICE_BUILDING_PORT;

	/**
	 * Time to wait in stopping the {@link OfficeBuilding}.
	 * 
	 * @parameter
	 */
	private Long waitTime;

	/*
	 * ======================== Mojo ==========================
	 */

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Ensure have required values
		assertNotNull(
				"Port not configured for the "
						+ OfficeBuilding.class.getSimpleName(), this.port);

		// Ensure default non-required values
		long stopWaitTime = defaultValue(this.waitTime, new Long(10000))
				.longValue();

		// Obtain the OfficeBuilding manager
		OfficeBuildingManagerMBean officeBuildingManager;
		try {
			officeBuildingManager = OfficeBuildingManager
					.getOfficeBuildingManager(null, this.port.intValue());
		} catch (Throwable ex) {
			throw this.newMojoExecutionException("Failed accessing the "
					+ OfficeBuilding.class.getSimpleName(), ex);
		}

		// Stop the OfficeBuilding
		try {
			officeBuildingManager.stopOfficeBuilding(stopWaitTime);
		} catch (Throwable ex) {
			throw this.newMojoExecutionException("Failed stopping the "
					+ OfficeBuilding.class.getSimpleName(), ex);
		}

		// Log started OfficeBuilding
		this.getLog().info(
				"Stopped the " + OfficeBuilding.class.getSimpleName()
						+ " running on port " + this.port.intValue());
	}

}