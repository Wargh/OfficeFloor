/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2010 Daniel Sagenschneider
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

import java.util.List;

import net.officefloor.building.command.CommandLineBuilder;
import net.officefloor.building.manager.OfficeBuildingManager;
import net.officefloor.building.manager.OfficeBuildingManagerMBean;
import net.officefloor.console.OfficeBuilding;
import net.officefloor.frame.api.manage.OfficeFloor;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Maven goal to open the {@link OfficeFloor}.
 * 
 * @goal open
 * 
 * @author Daniel Sagenschneider
 */
public class OpenOfficeFloorGoal extends AbstractGoal {

	/**
	 * Default process name.
	 */
	public static final String DEFAULT_PROCESS_NAME = "maven-officefloor-plugin";

	/**
	 * {@link MavenProject}.
	 * 
	 * @parameter expression="${project}"
	 */
	private MavenProject project;

	/**
	 * Port that {@link OfficeBuilding} is running on.
	 * 
	 * @parameter
	 * @required
	 */
	private Integer port;

	/**
	 * Path to the {@link OfficeFloor} configuration.
	 * 
	 * @parameter
	 * @required
	 */
	private String officeFloorLocation;

	/**
	 * Process name to open the {@link OfficeFloor} within.
	 * 
	 * @parameter
	 */
	private String processName;

	/**
	 * JVM options for running the {@link OfficeFloor}.
	 * 
	 * @parameter
	 */
	private String[] jvmOptions;

	/**
	 * Indicates whether to provide verbose output.
	 * 
	 * @parameter
	 */
	private Boolean verbose = new Boolean(false);

	/*
	 * ======================== Mojo ==========================
	 */

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Ensure have required values
		assertNotNull("Must have project", this.project);
		assertNotNull("Port not configured for the "
				+ OfficeBuilding.class.getSimpleName(), this.port);
		assertNotNull(OfficeFloor.class.getSimpleName()
				+ " configuration location not specified",
				this.officeFloorLocation);

		// Ensure default non-required values
		this.processName = defaultValue(this.processName, DEFAULT_PROCESS_NAME);

		// Obtain the OfficeBuilding manager
		OfficeBuildingManagerMBean officeBuildingManager;
		try {
			officeBuildingManager = OfficeBuildingManager
					.getOfficeBuildingManager(null, this.port.intValue());
		} catch (Throwable ex) {
			throw this.newMojoExecutionException("Failed accessing the "
					+ OfficeBuilding.class.getSimpleName(), ex);
		}

		// Create the arguments for class path
		CommandLineBuilder arguments = new CommandLineBuilder();
		try {
			@SuppressWarnings("unchecked")
			List<String> elements = this.project.getCompileClasspathElements();
			for (String element : elements) {
				arguments.addClassPathEntry(element);
			}
		} catch (Throwable ex) {
			throw this.newMojoExecutionException(
					"Failed creating class path for the "
							+ OfficeFloor.class.getSimpleName(), ex);
		}

		// Specify location of OfficeFloor
		arguments.addOfficeFloor(this.officeFloorLocation);

		// Specify the process name
		arguments.addProcessName(this.processName);

		// Provide JVM options (if specified)
		if (this.jvmOptions != null) {
			for (String jvmOption : this.jvmOptions) {
				arguments.addJvmOption(jvmOption);
			}
		}

		// Open the OfficeFloor
		String processNameSpace;
		try {
			processNameSpace = officeBuildingManager.openOfficeFloor(arguments
					.getCommandLine());
		} catch (Throwable ex) {
			throw this.newMojoExecutionException("Failed opening the "
					+ OfficeFloor.class.getSimpleName(), ex);
		}

		// Log opened the OfficeFloor
		this.getLog().info(
				"Opened " + OfficeFloor.class.getSimpleName()
						+ " under process name space '" + processNameSpace
						+ "' for " + this.officeFloorLocation);

		// Determine if provide verbose output
		if (this.verbose.booleanValue()) {
			StringBuilder message = new StringBuilder();
			message.append("ARGUMENTS: ");
			for (String argument : arguments.getCommandLine()) {
				message.append(" ");
				message.append(argument);
			}
			this.getLog().info(message.toString());
		}
	}
}