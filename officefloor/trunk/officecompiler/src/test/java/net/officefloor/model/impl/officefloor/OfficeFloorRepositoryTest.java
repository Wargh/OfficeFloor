/*
 *  Office Floor, Application Server
 *  Copyright (C) 2006 Daniel Sagenschneider
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms 
 *  of the GNU General Public License as published by the Free Software Foundation; either 
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program; 
 *  if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
 *  MA 02111-1307 USA
 */
package net.officefloor.model.impl.officefloor;

import java.sql.Connection;

import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.model.ConnectionModel;
import net.officefloor.model.officefloor.DeployedOfficeModel;
import net.officefloor.model.officefloor.DeployedOfficeObjectModel;
import net.officefloor.model.officefloor.DeployedOfficeObjectToOfficeFloorManagedObjectModel;
import net.officefloor.model.officefloor.DeployedOfficeTeamModel;
import net.officefloor.model.officefloor.DeployedOfficeTeamToOfficeFloorTeamModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectToDeployedOfficeModel;
import net.officefloor.model.officefloor.OfficeFloorModel;
import net.officefloor.model.officefloor.OfficeFloorRepository;
import net.officefloor.model.officefloor.OfficeFloorTeamModel;
import net.officefloor.model.repository.ConfigurationItem;
import net.officefloor.model.repository.ModelRepository;

import org.easymock.AbstractMatcher;

/**
 * Tests the {@link OfficeFloorRepository}.
 * 
 * @author Daniel
 */
public class OfficeFloorRepositoryTest extends OfficeFrameTestCase {

	/**
	 * {@link ModelRepository}.
	 */
	private final ModelRepository modelRepository = this
			.createMock(ModelRepository.class);

	/**
	 * {@link ConfigurationItem}.
	 */
	private final ConfigurationItem configurationItem = this
			.createMock(ConfigurationItem.class);

	/**
	 * {@link OfficeFloorRepository} to be tested.
	 */
	private final OfficeFloorRepository officeRepository = new OfficeFloorRepositoryImpl(
			this.modelRepository);

	/**
	 * Ensures on retrieving a {@link OfficeFloorModel} that all
	 * {@link ConnectionModel} instances are connected.
	 */
	public void testRetrieveOfficeFloor() throws Exception {

		// Create the raw office floor to be connected
		OfficeFloorModel officeFloor = new OfficeFloorModel();
		OfficeFloorManagedObjectModel officeFloorManagedObject = new OfficeFloorManagedObjectModel(
				"MANAGED_OBJECT", "net.example.ExampleManagedObjectSource",
				Connection.class.getName());
		officeFloor.addOfficeFloorManagedObject(officeFloorManagedObject);
		OfficeFloorTeamModel officeFloorTeam = new OfficeFloorTeamModel(
				"OFFICE_FLOOR_TEAM", "net.example.ExampleTeamSource");
		officeFloor.addOfficeFloorTeam(officeFloorTeam);
		DeployedOfficeModel office = new DeployedOfficeModel("OFFICE",
				"net.example.ExampleOfficeSource", "OFFICE_LOCATION");
		officeFloor.addDeployedOffice(office);
		DeployedOfficeObjectModel officeObject = new DeployedOfficeObjectModel(
				"OBJECT", Connection.class.getName());
		office.addDeployedOfficeObject(officeObject);
		DeployedOfficeTeamModel officeTeam = new DeployedOfficeTeamModel(
				"OFFICE_TEAM");
		office.addDeployedOfficeTeam(officeTeam);

		// office floor managed object -> office
		OfficeFloorManagedObjectToDeployedOfficeModel moToOffice = new OfficeFloorManagedObjectToDeployedOfficeModel(
				"OFFICE", null);
		officeFloorManagedObject.setManagingOffice(moToOffice);

		// office object -> office floor managed object
		DeployedOfficeObjectToOfficeFloorManagedObjectModel officeObjectToManagedObject = new DeployedOfficeObjectToOfficeFloorManagedObjectModel(
				"MANAGED_OBJECT");
		officeObject.setOfficeFloorManagedObject(officeObjectToManagedObject);

		// office team -> office floor team
		DeployedOfficeTeamToOfficeFloorTeamModel officeTeamToFloorTeam = new DeployedOfficeTeamToOfficeFloorTeamModel(
				"OFFICE_FLOOR_TEAM");
		officeTeam.setOfficeFloorTeam(officeTeamToFloorTeam);

		// Record retrieving the office
		this.recordReturn(this.modelRepository, this.modelRepository.retrieve(
				null, this.configurationItem), officeFloor,
				new AbstractMatcher() {
					@Override
					public boolean matches(Object[] expected, Object[] actual) {
						assertTrue("Must be office model",
								actual[0] instanceof OfficeFloorModel);
						assertEquals(
								"Incorrect configuration item",
								OfficeFloorRepositoryTest.this.configurationItem,
								actual[1]);
						return true;
					}
				});

		// Retrieve the office floor
		this.replayMockObjects();
		OfficeFloorModel retrievedOfficeFloor = this.officeRepository
				.retrieveOfficeFloor(this.configurationItem);
		this.verifyMockObjects();
		assertEquals("Incorrect office", officeFloor, retrievedOfficeFloor);

		// Ensure office team connected
		assertEquals("office floor managed object <- office",
				officeFloorManagedObject, moToOffice
						.getOfficeFloorManagedObject());
		assertEquals("office floor managed object -> office", office,
				moToOffice.getManagingOffice());
		assertEquals("office object <- office floor managed object",
				officeObject, officeObjectToManagedObject
						.getDeployedOfficeObject());
		assertEquals("office object -> office floor managed object",
				officeFloorManagedObject, officeObjectToManagedObject
						.getOfficeFloorManagedObject());
		assertEquals("office team <- office floor team", officeTeam,
				officeTeamToFloorTeam.getDeployedOfficeTeam());
		assertEquals("office team -> office floor team", officeFloorTeam,
				officeTeamToFloorTeam.getOfficeFloorTeam());
	}

	/**
	 * Ensures on storing a {@link OfficeFloorModel} that all
	 * {@link ConnectionModel} instances are readied for storing.
	 */
	public void testStoreOfficeFloor() throws Exception {

		// Create the office floor (without connections)
		OfficeFloorModel officeFloor = new OfficeFloorModel();
		OfficeFloorManagedObjectModel officeFloorManagedObject = new OfficeFloorManagedObjectModel(
				"MANAGED_OBJECT", "net.example.ExampleManagedObjectSource",
				Connection.class.getName());
		officeFloor.addOfficeFloorManagedObject(officeFloorManagedObject);
		OfficeFloorTeamModel officeFloorTeam = new OfficeFloorTeamModel(
				"OFFICE_FLOOR_TEAM", "net.example.ExampleTeamSource");
		officeFloor.addOfficeFloorTeam(officeFloorTeam);
		DeployedOfficeModel office = new DeployedOfficeModel("OFFICE",
				"net.example.ExampleOfficeSource", "OFFICE_LOCATION");
		officeFloor.addDeployedOffice(office);
		DeployedOfficeObjectModel officeObject = new DeployedOfficeObjectModel(
				"OBJECT", Connection.class.getName());
		office.addDeployedOfficeObject(officeObject);
		DeployedOfficeTeamModel officeTeam = new DeployedOfficeTeamModel(
				"OFFICE_TEAM");
		office.addDeployedOfficeTeam(officeTeam);

		// office floor managed object -> office
		OfficeFloorManagedObjectToDeployedOfficeModel moToOffice = new OfficeFloorManagedObjectToDeployedOfficeModel();
		moToOffice.setOfficeFloorManagedObject(officeFloorManagedObject);
		moToOffice.setManagingOffice(office);
		moToOffice.connect();

		// office object -> office floor managed object
		DeployedOfficeObjectToOfficeFloorManagedObjectModel officeObjectToManagedObject = new DeployedOfficeObjectToOfficeFloorManagedObjectModel();
		officeObjectToManagedObject.setDeployedOfficeObject(officeObject);
		officeObjectToManagedObject
				.setOfficeFloorManagedObject(officeFloorManagedObject);
		officeObjectToManagedObject.connect();

		// office team -> office floor team
		DeployedOfficeTeamToOfficeFloorTeamModel officeTeamToFloorTeam = new DeployedOfficeTeamToOfficeFloorTeamModel();
		officeTeamToFloorTeam.setDeployedOfficeTeam(officeTeam);
		officeTeamToFloorTeam.setOfficeFloorTeam(officeFloorTeam);
		officeTeamToFloorTeam.connect();

		// Record storing the office floor
		this.modelRepository.store(officeFloor, this.configurationItem);

		// Store the office floor
		this.replayMockObjects();
		this.officeRepository.storeOfficeFloor(officeFloor,
				this.configurationItem);
		this.verifyMockObjects();

		// Ensure the connections have links to enable retrieving
		assertEquals("office floor managed object - office", "OFFICE",
				moToOffice.getManagingOfficeName());
		assertEquals("office object - office floor managed object",
				"MANAGED_OBJECT", officeObjectToManagedObject
						.getOfficeFloorManagedObjectName());
		assertEquals("office team - office floor team", "OFFICE_FLOOR_TEAM",
				officeTeamToFloorTeam.getOfficeFloorTeamName());
	}

}