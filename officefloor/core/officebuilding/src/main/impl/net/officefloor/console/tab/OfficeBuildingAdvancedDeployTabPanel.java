/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2014 Daniel Sagenschneider
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
package net.officefloor.console.tab;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.io.File;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import net.officefloor.building.manager.OfficeBuildingManager;
import net.officefloor.building.manager.OfficeBuildingManagerMBean;
import net.officefloor.building.manager.OpenOfficeFloorConfiguration;
import net.officefloor.building.manager.UploadArtifact;
import net.officefloor.compile.impl.util.CompileUtil;
import net.officefloor.console.OfficeBuilding;
import net.officefloor.frame.api.manage.OfficeFloor;

/**
 * {@link JPanel} for the advanced deploy of an {@link OfficeFloor} to an
 * {@link OfficeBuilding}.
 * 
 * @author Daniel Sagenschneider
 */
public class OfficeBuildingAdvancedDeployTabPanel extends AbstractOfficeBuildingPanel {

	/**
	 * {@link OfficeBuildingManageTabPanel}.
	 */
	private final OfficeBuildingManageTabPanel manageTab;

	/**
	 * Initiate.
	 * 
	 * @param officeBuildingManager
	 *            {@link OfficeBuildingManager}.
	 * @param manageTab
	 *            {@link OfficeBuildingManageTabPanel}.
	 */
	public OfficeBuildingAdvancedDeployTabPanel(OfficeBuildingManagerMBean officeBuildingManager,
			OfficeBuildingManageTabPanel manageTab) {
		super(officeBuildingManager);
		this.manageTab = manageTab;
	}

	/*
	 * ================= AbstractOfficeBuildingPanel ================
	 */

	@Override
	protected void init() throws Exception {

		// Advanced panel
		GridBagLayout layoutManager = new GridBagLayout();
		JPanel panelAdvancedStart = new JPanel(layoutManager);

		// Grid constraints
		GridBagConstraints constraint = new GridBagConstraints();
		constraint.gridx = 0;
		constraint.gridy = -1; // will increment to first row

		// Obtain default open OfficeFloor configuration
		OpenOfficeFloorConfiguration configuration = this.createDefaultOpenOfficeFloorConfiguration();

		// OfficeFloor source
		final JTextField officeFloorSource = new JTextField(30);
		officeFloorSource.setText(configuration.getOfficeFloorSourceClassName());
		this.addAdvancedEntry(panelAdvancedStart, "OfficeFloor Source Class", officeFloorSource, constraint);

		// OfficeFloor location
		final JTextField officeFloorLocation = new JTextField(30);
		officeFloorLocation.setText(configuration.getOfficeFloorLocation());
		this.addAdvancedEntry(panelAdvancedStart, "OfficeFloor location", officeFloorLocation, constraint);

		// Process name
		final JTextField officeFloorName = new JTextField(30);
		officeFloorName.setText(configuration.getOfficeFloorName());
		this.addAdvancedEntry(panelAdvancedStart, "OfficeFloor name", officeFloorName, constraint);

		// Upload artifacts
		final OfficeTablePanel uploadArtifacts = new OfficeTablePanel(true, true, "Upload Artifact");
		for (UploadArtifact artifact : configuration.getUploadArtifacts()) {
			uploadArtifacts.addRow(artifact.getName());
		}
		this.addAdvancedEntry(panelAdvancedStart, "Upload artifact", uploadArtifacts, constraint);

		// Class path entries
		final OfficeTablePanel classPathEntries = new OfficeTablePanel("Class path entry");
		for (String classPathEntry : configuration.getClassPathEntries()) {
			classPathEntries.addRow(classPathEntry);
		}
		this.addAdvancedEntry(panelAdvancedStart, "Class path", classPathEntries, constraint);

		// OfficeFloor properties
		final OfficeTablePanel officeFloorProperties = new OfficeTablePanel("Name", "Value");
		Properties properties = configuration.getOfficeFloorProperties();
		for (String propertyName : properties.stringPropertyNames()) {
			String propertyValue = properties.getProperty(propertyName);
			officeFloorProperties.addRow(propertyName, propertyValue);
		}
		this.addAdvancedEntry(panelAdvancedStart, "OfficeFloor properties", officeFloorProperties, constraint);

		// JVM options
		final OfficeTablePanel jvmOptions = new OfficeTablePanel("JVM option");
		for (String jvmOption : configuration.getJvmOptions()) {
			jvmOptions.addRow(jvmOption);
		}
		this.addAdvancedEntry(panelAdvancedStart, "JVM options", jvmOptions, constraint);

		// Open task
		JPanel panelOpenTask = new JPanel();
		panelOpenTask.setLayout(new BoxLayout(panelOpenTask, BoxLayout.LINE_AXIS));
		panelOpenTask.add(new Label("Office"));
		final JTextField openFunctionOfficeName = (JTextField) panelOpenTask.add(new JTextField(5));
		openFunctionOfficeName.setText(configuration.getOfficeName());
		panelOpenTask.add(new Label("Function"));
		final JTextField openFunctionFunctionName = (JTextField) panelOpenTask.add(new JTextField(5));
		openFunctionFunctionName.setText(configuration.getFunctionName());
		panelOpenTask.add(new Label("Parameter"));
		final JTextField openFunctionParameter = (JTextField) panelOpenTask.add(new JTextField(5));
		openFunctionParameter.setText(configuration.getParameter());
		this.addAdvancedEntry(panelAdvancedStart, "Open Function", panelOpenTask, constraint);

		// Deploy button
		JButton deployButton = new JButton(new OfficeAction<String>("deploy") {
			@Override
			public OfficeAsyncAction<String> doAction() throws Exception {

				// Create the OfficeFloor configuration
				// (Clean instance as defaults loaded to components)
				String officeFloorLocationText = officeFloorLocation.getText();
				final OpenOfficeFloorConfiguration configuration = new OpenOfficeFloorConfiguration(
						officeFloorLocationText);

				// Load the OfficeFloor Source
				String officeFloorSourceText = officeFloorSource.getText();
				if (!CompileUtil.isBlank(officeFloorSourceText)) {
					configuration.setOfficeFloorSourceClassName(officeFloorSourceText);
				}

				// Load the OfficeFloor name
				String officeFloorNameText = officeFloorName.getText();
				if (!CompileUtil.isBlank(officeFloorNameText)) {
					configuration.setOfficeFloorName(officeFloorNameText);
				}

				// Load the upload artifacts
				for (String[] row : uploadArtifacts.getRows()) {
					String uploadArtifact = row[0];
					if (!CompileUtil.isBlank(uploadArtifact)) {

						// Ensure the upload artifact exists
						File uploadArtifactFile = new File(uploadArtifact);
						if (!(uploadArtifactFile.exists())) {
							throw new ErrorMessageException("Can not find upload artifact " + uploadArtifact);
						}

						// Add the upload artifact
						configuration.addUploadArtifact(new UploadArtifact(uploadArtifactFile));
					}
				}

				// Load class path entries
				for (String[] row : classPathEntries.getRows()) {
					String classPathEntry = row[0];
					if (!CompileUtil.isBlank(classPathEntry)) {
						configuration.addClassPathEntry(classPathEntry);
					}
				}

				// Load OfficeFloor properties
				for (String[] row : officeFloorProperties.getRows()) {
					String name = row[0];
					String value = row[1];
					if (!CompileUtil.isBlank(name)) {
						configuration.addOfficeFloorProperty(name, value);
					}
				}

				// Load JVM options
				for (String[] row : jvmOptions.getRows()) {
					String jvmOption = row[0];
					if (!CompileUtil.isBlank(jvmOption)) {
						configuration.addJvmOption(jvmOption);
					}
				}

				// Load open function
				String officeNameText = openFunctionOfficeName.getName();
				String functionNameText = openFunctionFunctionName.getName();
				String parameterText = openFunctionParameter.getName();
				if ((!CompileUtil.isBlank(officeNameText)) && (!CompileUtil.isBlank(functionNameText))) {
					configuration.setOpenFunction(officeNameText, functionNameText, parameterText);
				}

				// Return asynchronous action to open the OfficeFloor
				return new OfficeAsyncAction<String>("Deploying") {
					@Override
					public String doAction() throws Exception {

						// Open the OfficeFloor
						String processNameSpace = OfficeBuildingAdvancedDeployTabPanel.this.officeBuildingManager
								.openOfficeFloor(configuration);

						// Return the process name space
						return processNameSpace;
					}

					@Override
					public void done(String result) throws Exception {

						// Provide notification that opened
						OfficeBuildingAdvancedDeployTabPanel.this
								.notifyUser("Opened under process name space: " + result);

						// Refresh the OfficeFloor processes
						OfficeBuildingAdvancedDeployTabPanel.this.manageTab.refreshOfficeFloorProcesses();
					}
				};
			}
		});

		// Add the deploy button
		constraint.gridy++;
		constraint.gridx = 1;
		constraint.fill = GridBagConstraints.NONE;
		constraint.anchor = GridBagConstraints.CENTER;
		panelAdvancedStart.add(deployButton, constraint);

		// Add wrapped in a scroll pane
		this.add(new JScrollPane(panelAdvancedStart));

		// Layout to full width
		this.setLayout(new GridLayout(0, 1));
	}

	/**
	 * Adds an advanced entry to the {@link JPanel}.
	 * 
	 * @param panelAdvancedStart
	 *            {@link JPanel}.
	 * @param labelText
	 *            Text of the label.
	 * @param component
	 *            {@link Component} to add.
	 * @param constraint
	 *            {@link GridBagConstraints}.
	 */
	private void addAdvancedEntry(JPanel panelAdvancedStart, String labelText, Component component,
			GridBagConstraints constraint) {

		// Next row for entry
		constraint.gridy++;

		// Add the label
		Label label = new Label(labelText);
		constraint.gridx = 0;
		constraint.weightx = 0.1;
		constraint.fill = GridBagConstraints.NONE;
		constraint.anchor = GridBagConstraints.EAST;
		panelAdvancedStart.add(label, constraint);

		// Add the component
		constraint.gridx = 1;
		constraint.weightx = 0.9;
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.anchor = GridBagConstraints.WEST;
		constraint.ipady = (component instanceof OfficeTablePanel) ? 20 : 0;
		panelAdvancedStart.add(component, constraint);
	}

}