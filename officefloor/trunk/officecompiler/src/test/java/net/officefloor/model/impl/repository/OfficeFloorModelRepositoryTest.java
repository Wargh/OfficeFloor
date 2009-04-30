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
package net.officefloor.model.impl.repository;

import java.sql.Connection;

import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.model.RemoveConnectionsAction;
import net.officefloor.model.desk.DeskModel;
import net.officefloor.model.impl.repository.filesystem.FileSystemConfigurationItem;
import net.officefloor.model.impl.repository.memory.MemoryConfigurationItem;
import net.officefloor.model.officefloor.DeployedOfficeInputModel;
import net.officefloor.model.officefloor.DeployedOfficeModel;
import net.officefloor.model.officefloor.DeployedOfficeObjectModel;
import net.officefloor.model.officefloor.DeployedOfficeObjectToOfficeFloorManagedObjectModel;
import net.officefloor.model.officefloor.DeployedOfficeTeamModel;
import net.officefloor.model.officefloor.DeployedOfficeTeamToOfficeFloorTeamModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectDependencyModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectDependencyToOfficeFloorManagedObjectModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectSourceFlowModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectSourceModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectSourceTeamModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectSourceToDeployedOfficeModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectToOfficeFloorManagedObjectSourceModel;
import net.officefloor.model.officefloor.OfficeFloorModel;
import net.officefloor.model.officefloor.OfficeFloorTeamModel;
import net.officefloor.model.officefloor.PropertyModel;
import net.officefloor.model.repository.ConfigurationItem;
import net.officefloor.model.repository.ModelRepository;

/**
 * Tests the marshaling/unmarshaling of the {@link OfficeFloorModel} via the
 * {@link ModelRepository}.
 * 
 * @author Daniel
 */
public class OfficeFloorModelRepositoryTest extends OfficeFrameTestCase {

	/**
	 * {@link ConfigurationItem} containing the {@link DeskModel}.
	 */
	private ConfigurationItem configurationItem;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {

		// Specify location of the configuration
		this.configurationItem = new FileSystemConfigurationItem(this.findFile(
				this.getClass(), "TestOfficeFloor.officefloor.xml"), null);
	}

	/**
	 * Ensure retrieve the {@link OfficeFloorModel}.
	 */
	public void testRetrieveOfficeFloor() throws Exception {

		// Load the Office Floor
		ModelRepository repository = new ModelRepositoryImpl();
		OfficeFloorModel officeFloor = new OfficeFloorModel();
		officeFloor = repository.retrieve(officeFloor, this.configurationItem);

		// ----------------------------------------
		// Validate the office floor managed object sources
		// ----------------------------------------
		assertList(new String[] { "getOfficeFloorManagedObjectSourceName",
				"getManagedObjectSourceClassName", "getObjectType", "getX",
				"getY" }, officeFloor.getOfficeFloorManagedObjectSources(),
				new OfficeFloorManagedObjectSourceModel(
						"MANAGED_OBJECT_SOURCE",
						"net.example.ExampleManagedObjectSource",
						"net.orm.Session", null, null, null, null, null, 100,
						101));
		OfficeFloorManagedObjectSourceModel moSource = officeFloor
				.getOfficeFloorManagedObjectSources().get(0);
		assertList(new String[] { "getName", "getValue" }, moSource
				.getProperties(), new PropertyModel("MO_ONE", "VALUE_ONE"),
				new PropertyModel("MO_TWO", "VALUE_TWO"));

		// Managing office
		assertProperties(
				new OfficeFloorManagedObjectSourceToDeployedOfficeModel(
						"OFFICE", "PROCESS_MO"), moSource.getManagingOffice(),
				"getManagingOfficeName", "getProcessBoundManagedObjectName");

		// Flows
		assertList(new String[] { "getOfficeFloorManagedObjectSourceFlowName",
				"getArgumentType" }, moSource
				.getOfficeFloorManagedObjectSourceFlows(),
				new OfficeFloorManagedObjectSourceFlowModel("FLOW",
						Integer.class.getName()));

		// Teams
		assertList(
				new String[] { "getOfficeFloorManagedObjectSourceTeamName" },
				moSource.getOfficeFloorManagedObjectSourceTeams(),
				new OfficeFloorManagedObjectSourceTeamModel("MO_TEAM"));

		// ----------------------------------------
		// Validate the office floor managed objects
		// ----------------------------------------
		assertList(new String[] { "getOfficeFloorManagedObjectName",
				"getManagedObjectScope", "getX", "getY" }, officeFloor
				.getOfficeFloorManagedObjects(),
				new OfficeFloorManagedObjectModel("MANAGED_OBJECT_ONE",
						"THREAD", null, null, null, null, 200, 201),
				new OfficeFloorManagedObjectModel("MANAGED_OBJECT_TWO",
						"PROCESS", null, null, null, null, 210, 211));
		OfficeFloorManagedObjectModel mo = officeFloor
				.getOfficeFloorManagedObjects().get(0);

		// Link to managed object source
		assertProperties(
				new OfficeFloorManagedObjectToOfficeFloorManagedObjectSourceModel(
						"MANAGED_OBJECT_SOURCE"), mo
						.getOfficeFloorManagedObjectSource(),
				"getOfficeFloorManagedObjectSourceName");

		// Dependencies
		assertList(new String[] { "getOfficeFloorManagedObjectDependencyName",
				"getDependencyType" }, mo
				.getOfficeFloorManagedObjectDependencies(),
				new OfficeFloorManagedObjectDependencyModel("DEPENDENCY",
						Connection.class.getName()));
		OfficeFloorManagedObjectDependencyModel dependency = mo
				.getOfficeFloorManagedObjectDependencies().get(0);
		assertProperties(
				new OfficeFloorManagedObjectDependencyToOfficeFloorManagedObjectModel(
						"MANAGED_OBJECT_TWO"), dependency
						.getOfficeFloorManagedObject(),
				"getOfficeFloorManagedObjectName");

		// ----------------------------------------
		// Validate the office floor teams
		// ----------------------------------------
		assertList(new String[] { "getOfficeFloorTeamName",
				"getTeamSourceClassName", "getX", "getY" }, officeFloor
				.getOfficeFloorTeams(), new OfficeFloorTeamModel("TEAM",
				"net.example.ExampleTeamSource", null, null, 300, 301));
		OfficeFloorTeamModel team = officeFloor.getOfficeFloorTeams().get(0);
		assertList(new String[] { "getName", "getValue" },
				team.getProperties(),
				new PropertyModel("TEAM_ONE", "VALUE_ONE"), new PropertyModel(
						"TEAM_TWO", "VALUE_TWO"));

		// ----------------------------------------
		// Validate the deployed offices
		// ----------------------------------------
		assertList(
				new String[] { "getDeployedOfficeName",
						"getOfficeSourceClassName", "getOfficeLocation",
						"getX", "getY" }, officeFloor.getDeployedOffices(),
				new DeployedOfficeModel("OFFICE",
						"net.example.ExampleOfficeSource", "OFFICE_LOCATION",
						null, null, null, null, null, 400, 401));
		DeployedOfficeModel office = officeFloor.getDeployedOffices().get(0);
		assertList(new String[] { "getName", "getValue" }, office
				.getProperties(), new PropertyModel("OFFICE_ONE", "VALUE_ONE"),
				new PropertyModel("OFFICE_TWO", "VALUE_TWO"));

		// Deployed office objects
		assertList(new String[] { "getDeployedOfficeObjectName",
				"getObjectType" }, office.getDeployedOfficeObjects(),
				new DeployedOfficeObjectModel("OBJECT", "net.orm.Session"));
		DeployedOfficeObjectModel officeObject = office
				.getDeployedOfficeObjects().get(0);
		assertProperties(
				new DeployedOfficeObjectToOfficeFloorManagedObjectModel(
						"MANAGED_OBJECT"), officeObject
						.getOfficeFloorManagedObject(),
				"getOfficeFloorManagedObjectName");

		// Deployed office inputs
		assertList(new String[] { "getDeployedOfficeInputName",
				"getParameterType" }, office.getDeployedOfficeInputs(),
				new DeployedOfficeInputModel("INPUT", Integer.class.getName()));

		// Deployed office team
		assertList(new String[] { "getDeployedOfficeTeamName" }, office
				.getDeployedOfficeTeams(), new DeployedOfficeTeamModel(
				"OFFICE_TEAM"));
		DeployedOfficeTeamModel officeTeam = office.getDeployedOfficeTeams()
				.get(0);
		assertProperties(officeTeam.getOfficeFloorTeam(),
				new DeployedOfficeTeamToOfficeFloorTeamModel("TEAM"),
				"getOfficeFloorTeamName");
	}

	/**
	 * Ensure able to round trip storing and retrieving the
	 * {@link OfficeFloorModel}.
	 */
	public void testRoundTripStoreRetrieveOfficeFloor() throws Exception {

		// Load the Office Floor
		ModelRepository repository = new ModelRepositoryImpl();
		OfficeFloorModel officeFloor = new OfficeFloorModel();
		officeFloor = repository.retrieve(officeFloor, this.configurationItem);

		// Store the Office Floor
		MemoryConfigurationItem contents = new MemoryConfigurationItem();
		repository.store(officeFloor, contents);

		// Reload the Office Floor
		OfficeFloorModel reloadedOfficeFloor = new OfficeFloorModel();
		reloadedOfficeFloor = repository
				.retrieve(reloadedOfficeFloor, contents);

		// Validate round trip
		assertGraph(officeFloor, reloadedOfficeFloor,
				RemoveConnectionsAction.REMOVE_CONNECTIONS_METHOD_NAME);
	}

}