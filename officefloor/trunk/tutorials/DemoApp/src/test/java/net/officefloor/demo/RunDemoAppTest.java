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
package net.officefloor.demo;

import net.officefloor.autowire.AutoWireManagement;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.plugin.woof.WoofOfficeFloorSource;

/**
 * Test to run the application.
 * 
 * @author Daniel Sagenschneider
 */
public class RunDemoAppTest extends OfficeFrameTestCase {

	/**
	 * Runs the application.
	 */
	public void testRun() throws Exception {

		// Stop all other offices
		AutoWireManagement.closeAllOfficeFloors();

		// Start
		WoofOfficeFloorSource.main();

		// Wait to stop
		if ("wait".equals(System.getProperty("block.test"))) {
			System.out.print("Press enter to finish");
			System.out.flush();
			System.in.read();
		}
	}

	@Override
	protected void tearDown() throws Exception {
		// Stop the office
		AutoWireManagement.closeAllOfficeFloors();
	}

}