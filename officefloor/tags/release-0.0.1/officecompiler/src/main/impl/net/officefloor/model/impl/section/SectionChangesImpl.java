/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2009 Daniel Sagenschneider
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
package net.officefloor.model.impl.section;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import net.officefloor.compile.properties.Property;
import net.officefloor.compile.properties.PropertyList;
import net.officefloor.compile.section.SectionInputType;
import net.officefloor.compile.section.SectionObjectType;
import net.officefloor.compile.section.SectionOutputType;
import net.officefloor.compile.section.SectionType;
import net.officefloor.model.ConnectionModel;
import net.officefloor.model.change.Change;
import net.officefloor.model.impl.change.AbstractChange;
import net.officefloor.model.impl.change.NoChange;
import net.officefloor.model.section.ExternalFlowModel;
import net.officefloor.model.section.ExternalManagedObjectModel;
import net.officefloor.model.section.PropertyModel;
import net.officefloor.model.section.SectionModel;
import net.officefloor.model.section.SectionChanges;
import net.officefloor.model.section.SubSectionInputModel;
import net.officefloor.model.section.SubSectionModel;
import net.officefloor.model.section.SubSectionObjectModel;
import net.officefloor.model.section.SubSectionObjectToExternalManagedObjectModel;
import net.officefloor.model.section.SubSectionOutputModel;
import net.officefloor.model.section.SubSectionOutputToExternalFlowModel;
import net.officefloor.model.section.SubSectionOutputToSubSectionInputModel;

/**
 * {@link SectionChanges} implementation.
 * 
 * @author Daniel Sagenschneider
 */
public class SectionChangesImpl implements SectionChanges {

	/**
	 * Sorts the {@link SubSectionModel} instances.
	 * 
	 * @param subSections
	 *            Listing of {@link SubSectionModel} instances to sort.
	 */
	public static void sortSubSections(List<SubSectionModel> subSections) {
		Collections.sort(subSections, new Comparator<SubSectionModel>() {
			@Override
			public int compare(SubSectionModel a, SubSectionModel b) {
				return a.getSubSectionName().compareTo(b.getSubSectionName());
			}
		});
	}

	/**
	 * <p>
	 * Sorts the {@link ExternalFlowModel} instances.
	 * <p>
	 * This enables easier merging of configuration under SCM.
	 * 
	 * @param externalFlows
	 *            {@link ExternalFlowModel} instances.
	 */
	public static void sortExternalFlows(List<ExternalFlowModel> externalFlows) {
		Collections.sort(externalFlows, new Comparator<ExternalFlowModel>() {
			@Override
			public int compare(ExternalFlowModel a, ExternalFlowModel b) {
				return a.getExternalFlowName().compareTo(
						b.getExternalFlowName());
			}
		});
	}

	/**
	 * <p>
	 * Sorts the {@link ExternalManagedObjectModel} instances.
	 * <p>
	 * This enables easier merging of configuration under SCM.
	 * 
	 * @param externalManagedObjects
	 *            {@link ExternalManagedObjectModel} instances.
	 */
	public static void sortExternalManagedObjects(
			List<ExternalManagedObjectModel> externalManagedObjects) {
		Collections.sort(externalManagedObjects,
				new Comparator<ExternalManagedObjectModel>() {
					@Override
					public int compare(ExternalManagedObjectModel a,
							ExternalManagedObjectModel b) {
						return a.getExternalManagedObjectName().compareTo(
								b.getExternalManagedObjectName());
					}
				});
	}

	/**
	 * {@link SectionModel} to be operated on.
	 */
	private final SectionModel section;

	/**
	 * Initiate.
	 * 
	 * @param section
	 *            {@link SectionModel}.
	 */
	public SectionChangesImpl(SectionModel section) {
		this.section = section;
	}

	/**
	 * Sorts the {@link SubSectionModel} instances on the {@link SectionModel}.
	 */
	public void sortSubSections() {
		sortSubSections(this.section.getSubSections());
	}

	/**
	 * Sorts the {@link ExternalFlowModel} instances.
	 */
	protected void sortExternalFlows() {
		sortExternalFlows(this.section.getExternalFlows());
	}

	/**
	 * Sorts the {@link ExternalManagedObjectModel} instances.
	 */
	protected void sortExternalManagedObjects() {
		sortExternalManagedObjects(this.section.getExternalManagedObjects());
	}

	/*
	 * ====================== SectionOperations ============================
	 */

	@Override
	public Change<SubSectionModel> addSubSection(String subSectionName,
			String sectionSourceClassName, String sectionLocation,
			PropertyList properties, SectionType sectionType) {

		// Create the sub section model
		final SubSectionModel subSection = new SubSectionModel(subSectionName,
				sectionSourceClassName, sectionLocation);

		// Add the properties in the order defined
		for (Property property : properties) {
			subSection.addProperty(new PropertyModel(property.getName(),
					property.getValue()));
		}

		// Add the inputs ensuring ordering
		for (SectionInputType inputType : sectionType.getSectionInputTypes()) {
			subSection.addSubSectionInput(new SubSectionInputModel(inputType
					.getSectionInputName(), inputType.getParameterType(),
					false, null));
		}
		Collections.sort(subSection.getSubSectionInputs(),
				new Comparator<SubSectionInputModel>() {
					@Override
					public int compare(SubSectionInputModel a,
							SubSectionInputModel b) {
						return a.getSubSectionInputName().compareTo(
								b.getSubSectionInputName());
					}
				});

		// Add the outputs ensuring ordering
		for (SectionOutputType outputType : sectionType.getSectionOutputTypes()) {
			subSection.addSubSectionOutput(new SubSectionOutputModel(outputType
					.getSectionOutputName(), outputType.getArgumentType(),
					outputType.isEscalationOnly()));
		}
		Collections.sort(subSection.getSubSectionOutputs(),
				new Comparator<SubSectionOutputModel>() {
					@Override
					public int compare(SubSectionOutputModel a,
							SubSectionOutputModel b) {
						return a.getSubSectionOutputName().compareTo(
								b.getSubSectionOutputName());
					}
				});

		// Add the objects ensuring ordering
		for (SectionObjectType objectType : sectionType.getSectionObjectTypes()) {
			subSection.addSubSectionObject(new SubSectionObjectModel(objectType
					.getSectionObjectName(), objectType.getObjectType()));
		}
		Collections.sort(subSection.getSubSectionObjects(),
				new Comparator<SubSectionObjectModel>() {
					@Override
					public int compare(SubSectionObjectModel a,
							SubSectionObjectModel b) {
						return a.getSubSectionObjectName().compareTo(
								b.getSubSectionObjectName());
					}
				});

		// Create the change to add the sub section
		return new AbstractChange<SubSectionModel>(subSection,
				"Add sub section " + subSectionName) {
			@Override
			public void apply() {
				// Add the sub section (ensuring ordering)
				SectionChangesImpl.this.section.addSubSection(subSection);
				SectionChangesImpl.this.sortSubSections();
			}

			@Override
			public void revert() {
				// Remove the sub section (should maintain order)
				SectionChangesImpl.this.section.removeSubSection(subSection);
			}
		};
	}

	@Override
	public Change<SubSectionModel> removeSubSection(
			final SubSectionModel subSection) {

		// Ensure the sub section in the section
		boolean isInSection = false;
		for (SubSectionModel subSectionModel : this.section.getSubSections()) {
			if (subSection == subSectionModel) {
				isInSection = true;
			}
		}
		if (!isInSection) {
			// No change as not in section
			return new NoChange<SubSectionModel>(subSection,
					"Remove sub section " + subSection.getSubSectionName(),
					"Sub section " + subSection.getSubSectionName()
							+ " not in section");
		}

		// Return the change to remove the sub section
		return new AbstractChange<SubSectionModel>(subSection,
				"Remove sub section " + subSection.getSubSectionName()) {

			/**
			 * {@link ConnectionModel} instances.
			 */
			private ConnectionModel[] connections;

			@Override
			public void apply() {

				// Remove the connections to the sub section
				List<ConnectionModel> connList = new LinkedList<ConnectionModel>();
				for (SubSectionInputModel input : subSection
						.getSubSectionInputs()) {
					for (SubSectionOutputToSubSectionInputModel conn : input
							.getSubSectionOutputs()) {
						conn.remove();
						connList.add(conn);
					}
				}
				for (SubSectionOutputModel output : subSection
						.getSubSectionOutputs()) {
					SubSectionOutputToSubSectionInputModel outputToInput = output
							.getSubSectionInput();
					if (outputToInput != null) {
						outputToInput.remove();
						connList.add(outputToInput);
					}
					SubSectionOutputToExternalFlowModel outputToExtFlow = output
							.getExternalFlow();
					if (outputToExtFlow != null) {
						outputToExtFlow.remove();
						connList.add(outputToExtFlow);
					}
				}
				for (SubSectionObjectModel object : subSection
						.getSubSectionObjects()) {
					SubSectionObjectToExternalManagedObjectModel conn = object
							.getExternalManagedObject();
					if (conn != null) {
						conn.remove();
						connList.add(conn);
					}
				}
				this.connections = connList.toArray(new ConnectionModel[0]);

				// Remove the sub section (should maintain order)
				SectionChangesImpl.this.section.removeSubSection(subSection);
			}

			@Override
			public void revert() {
				// Add the sub section (ensuring order)
				SectionChangesImpl.this.section.addSubSection(subSection);
				SectionChangesImpl.this.sortSubSections();

				// Reconnect the connections
				for (ConnectionModel conn : this.connections) {
					conn.connect();
				}
			}
		};
	}

	@Override
	public Change<SubSectionModel> renameSubSection(
			final SubSectionModel subSection, final String newSubSectionName) {

		// Ensure the sub section in the section
		boolean isInSection = false;
		for (SubSectionModel subSectionModel : this.section.getSubSections()) {
			if (subSection == subSectionModel) {
				isInSection = true;
			}
		}
		if (!isInSection) {
			// No change as not in section
			return new NoChange<SubSectionModel>(subSection,
					"Rename sub section " + subSection.getSubSectionName()
							+ " to " + newSubSectionName, "Sub section "
							+ subSection.getSubSectionName()
							+ " not in section");
		}

		// Maintain the old sub section name
		final String oldSubSectionName = subSection.getSubSectionName();

		// Return change to rename the sub section
		return new AbstractChange<SubSectionModel>(subSection,
				"Rename sub section " + subSection.getSubSectionName() + " to "
						+ newSubSectionName) {
			@Override
			public void apply() {
				// Rename the sub section (ensuring order)
				subSection.setSubSectionName(newSubSectionName);
				SectionChangesImpl.this.sortSubSections();
			}

			@Override
			public void revert() {
				// Revert to old name (ensuring order)
				subSection.setSubSectionName(oldSubSectionName);
				SectionChangesImpl.this.sortSubSections();
			}
		};
	}

	@Override
	public Change<SubSectionInputModel> setSubSectionInputPublic(
			final boolean isPublic, final String publicName,
			final SubSectionInputModel input) {

		// Ensure sub section input in the section
		boolean isInSection = false;
		for (SubSectionModel subSection : this.section.getSubSections()) {
			for (SubSectionInputModel inputModel : subSection
					.getSubSectionInputs()) {
				if (input == inputModel) {
					isInSection = true;
				}
			}
		}
		if (!isInSection) {
			// Not in section so can not change
			return new NoChange<SubSectionInputModel>(input,
					"Set sub section input " + input.getSubSectionInputName()
							+ " " + (isPublic ? "public" : "private"),
					"Sub section input " + input.getSubSectionInputName()
							+ " not in section");
		}

		// Maintain old values
		final boolean oldIsPublic = input.getIsPublic();
		final String oldPublicInputName = input.getPublicInputName();

		// Return the change to set input public/private
		return new AbstractChange<SubSectionInputModel>(input,
				"Set sub section input " + input.getSubSectionInputName() + " "
						+ (isPublic ? "public" : "private")) {
			@Override
			public void apply() {
				// Set public/private (only provide name if public)
				input.setIsPublic(isPublic);
				input.setPublicInputName(isPublic ? publicName : null);
			}

			@Override
			public void revert() {
				// Revert to old values
				input.setIsPublic(oldIsPublic);
				input.setPublicInputName(oldPublicInputName);
			}
		};
	}

	@Override
	public Change<ExternalFlowModel> addExternalFlow(String externalFlowName,
			String argumentType) {

		// Create the external flow
		final ExternalFlowModel externalFlow = new ExternalFlowModel(
				externalFlowName, argumentType);

		// Return the change to add the external flow
		return new AbstractChange<ExternalFlowModel>(externalFlow,
				"Add external flow " + externalFlowName) {
			@Override
			public void apply() {
				// Add the external flow (ensuring ordering)
				SectionChangesImpl.this.section.addExternalFlow(externalFlow);
				SectionChangesImpl.this.sortExternalFlows();
			}

			@Override
			public void revert() {
				// Remove the external flow (should maintain order)
				SectionChangesImpl.this.section
						.removeExternalFlow(externalFlow);
			}
		};
	}

	@Override
	public Change<ExternalFlowModel> removeExternalFlow(
			final ExternalFlowModel externalFlow) {

		// Ensure the external flow is in the section
		boolean isInSection = false;
		for (ExternalFlowModel externalFlowModel : this.section
				.getExternalFlows()) {
			if (externalFlow == externalFlowModel) {
				isInSection = true;
			}
		}
		if (!isInSection) {
			// No change as not in the section
			return new NoChange<ExternalFlowModel>(externalFlow,
					"Remove external flow "
							+ externalFlow.getExternalFlowName(),
					"External flow " + externalFlow.getExternalFlowName()
							+ " not in section");
		}

		// Return change to remove the external flow
		return new AbstractChange<ExternalFlowModel>(externalFlow,
				"Remove external flow " + externalFlow.getExternalFlowName()) {

			/**
			 * {@link ConnectionModel} instances.
			 */
			private ConnectionModel[] connections;

			@Override
			public void apply() {

				// Remove the connections to the external flow
				List<ConnectionModel> connList = new LinkedList<ConnectionModel>();
				for (SubSectionOutputToExternalFlowModel conn : new ArrayList<SubSectionOutputToExternalFlowModel>(
						externalFlow.getSubSectionOutputs())) {
					conn.remove();
					connList.add(conn);
				}
				this.connections = connList.toArray(new ConnectionModel[0]);

				// Remove the external flow (should maintain order)
				SectionChangesImpl.this.section
						.removeExternalFlow(externalFlow);
			}

			@Override
			public void revert() {
				// Add the external flow (ensuring order)
				SectionChangesImpl.this.section.addExternalFlow(externalFlow);
				SectionChangesImpl.this.sortExternalFlows();

				// Reconnect the connections
				for (ConnectionModel conn : this.connections) {
					conn.connect();
				}
			}
		};
	}

	@Override
	public Change<ExternalFlowModel> renameExternalFlow(
			final ExternalFlowModel externalFlow,
			final String newExternalFlowName) {

		// TODO test this method (renameExternalFlow)

		// Obtain the old name
		final String oldExternalFlowName = externalFlow.getExternalFlowName();

		// Return change to rename the external flow
		return new AbstractChange<ExternalFlowModel>(externalFlow,
				"Rename exernal flow to " + newExternalFlowName) {
			@Override
			public void apply() {
				externalFlow.setExternalFlowName(newExternalFlowName);
			}

			@Override
			public void revert() {
				externalFlow.setExternalFlowName(oldExternalFlowName);
			}
		};
	}

	@Override
	public Change<ExternalManagedObjectModel> addExternalManagedObject(
			String externalManagedObjectName, String objectType) {

		// Create the external managed object
		final ExternalManagedObjectModel extMo = new ExternalManagedObjectModel(
				externalManagedObjectName, objectType);

		// Return change to add the external managed object
		return new AbstractChange<ExternalManagedObjectModel>(extMo,
				"Add external managed object " + externalManagedObjectName) {
			@Override
			public void apply() {
				// Add the external managed object (ensuring order)
				SectionChangesImpl.this.section.addExternalManagedObject(extMo);
				SectionChangesImpl.this.sortExternalManagedObjects();
			}

			@Override
			public void revert() {
				// Remove the external managed object (should maintain order)
				SectionChangesImpl.this.section
						.removeExternalManagedObject(extMo);
			}
		};
	}

	@Override
	public Change<ExternalManagedObjectModel> removeExternalManagedObject(
			final ExternalManagedObjectModel externalManagedObject) {

		// Ensure the external managed object in the section
		boolean isInSection = false;
		for (ExternalManagedObjectModel externalMoModel : SectionChangesImpl.this.section
				.getExternalManagedObjects()) {
			if (externalManagedObject == externalMoModel) {
				isInSection = true;
			}
		}
		if (!isInSection) {
			// No change as not in section
			return new NoChange<ExternalManagedObjectModel>(
					externalManagedObject, "Remove external managed object "
							+ externalManagedObject
									.getExternalManagedObjectName(),
					"External managed object "
							+ externalManagedObject
									.getExternalManagedObjectName()
							+ " not in section");
		}

		// Return change to remove the external managed object
		return new AbstractChange<ExternalManagedObjectModel>(
				externalManagedObject, "Remove external managed object "
						+ externalManagedObject.getExternalManagedObjectName()) {

			/**
			 * {@link ConnectionModel} instances.
			 */
			private ConnectionModel[] connections;

			@Override
			public void apply() {

				// Remove the connections
				List<ConnectionModel> connList = new LinkedList<ConnectionModel>();
				for (SubSectionObjectToExternalManagedObjectModel conn : new ArrayList<SubSectionObjectToExternalManagedObjectModel>(
						externalManagedObject.getSubSectionObjects())) {
					conn.remove();
					connList.add(conn);
				}
				this.connections = connList.toArray(new ConnectionModel[0]);

				// Remove the external managed object (should maintain order)
				SectionChangesImpl.this.section
						.removeExternalManagedObject(externalManagedObject);
			}

			@Override
			public void revert() {
				// Add the external managed object (ensuring order)
				SectionChangesImpl.this.section
						.addExternalManagedObject(externalManagedObject);
				SectionChangesImpl.this.sortExternalManagedObjects();

				// Reconnect the connections
				for (ConnectionModel conn : this.connections) {
					conn.connect();
				}
			}
		};
	}

	@Override
	public Change<ExternalManagedObjectModel> renameExternalManagedObject(
			final ExternalManagedObjectModel externalManagedObject,
			final String newExternalManagedObjectName) {

		// TODO test this method (renameExternalManagedObject)

		// Obtain the old name
		final String oldExternalManagedObjectName = externalManagedObject
				.getExternalManagedObjectName();

		// Return change to rename the external managed object
		return new AbstractChange<ExternalManagedObjectModel>(
				externalManagedObject, "Rename exernal managed object to "
						+ newExternalManagedObjectName) {
			@Override
			public void apply() {
				externalManagedObject
						.setExternalManagedObjectName(newExternalManagedObjectName);
			}

			@Override
			public void revert() {
				externalManagedObject
						.setExternalManagedObjectName(oldExternalManagedObjectName);
			}
		};
	}

	@Override
	public Change<SubSectionObjectToExternalManagedObjectModel> linkSubSectionObjectToExternalManagedObject(
			SubSectionObjectModel subSectionObject,
			ExternalManagedObjectModel externalManagedObject) {

		// TODO test this method (linkSubSectionObjectToExternalManagedObject)

		// Create the connection
		final SubSectionObjectToExternalManagedObjectModel conn = new SubSectionObjectToExternalManagedObjectModel();
		conn.setSubSectionObject(subSectionObject);
		conn.setExternalManagedObject(externalManagedObject);

		// Return change to add connection
		return new AbstractChange<SubSectionObjectToExternalManagedObjectModel>(
				conn, "Connect") {
			@Override
			public void apply() {
				conn.connect();
			}

			@Override
			public void revert() {
				conn.remove();
			}
		};
	}

	@Override
	public Change<SubSectionObjectToExternalManagedObjectModel> removeSubSectionObjectToExternalManagedObject(
			final SubSectionObjectToExternalManagedObjectModel subSectionObjectToExternalManagedObject) {

		// TODO test this method (removeSubSectionObjectToExternalManagedObject)

		// Create change to remove connection
		return new AbstractChange<SubSectionObjectToExternalManagedObjectModel>(
				subSectionObjectToExternalManagedObject, "Remove") {
			@Override
			public void apply() {
				subSectionObjectToExternalManagedObject.remove();
			}

			@Override
			public void revert() {
				subSectionObjectToExternalManagedObject.connect();
			}
		};
	}

	@Override
	public Change<SubSectionOutputToSubSectionInputModel> linkSubSectionOutputToSubSectionInput(
			SubSectionOutputModel subSectionOutput,
			SubSectionInputModel subSectionInput) {

		// TODO test this method (linkSubSectionOutputToSubSectionInput)

		// Create the connection
		final SubSectionOutputToSubSectionInputModel conn = new SubSectionOutputToSubSectionInputModel();
		conn.setSubSectionOutput(subSectionOutput);
		conn.setSubSectionInput(subSectionInput);

		// Return change to add connection
		return new AbstractChange<SubSectionOutputToSubSectionInputModel>(conn,
				"Connect") {
			@Override
			public void apply() {
				conn.connect();
			}

			@Override
			public void revert() {
				conn.remove();
			}
		};
	}

	@Override
	public Change<SubSectionOutputToSubSectionInputModel> removeSubSectionOutputToSubSectionInput(
			final SubSectionOutputToSubSectionInputModel subSectionOutputToSubSectionInput) {

		// TODO test this method (removeSubSectionOutputToSubSectionInputModel)

		// Create change to remove connection
		return new AbstractChange<SubSectionOutputToSubSectionInputModel>(
				subSectionOutputToSubSectionInput, "Remove") {
			@Override
			public void apply() {
				subSectionOutputToSubSectionInput.remove();
			}

			@Override
			public void revert() {
				subSectionOutputToSubSectionInput.connect();
			}
		};
	}

	@Override
	public Change<SubSectionOutputToExternalFlowModel> linkSubSectionOutputToExternalFlow(
			SubSectionOutputModel subSectionOutput,
			ExternalFlowModel externalFlow) {

		// TODO test this method (linkSubSectionOutputToExternalFlow)

		// Create the connection
		final SubSectionOutputToExternalFlowModel conn = new SubSectionOutputToExternalFlowModel();
		conn.setSubSectionOutput(subSectionOutput);
		conn.setExternalFlow(externalFlow);

		// Return change to add connection
		return new AbstractChange<SubSectionOutputToExternalFlowModel>(conn,
				"Connect") {
			@Override
			public void apply() {
				conn.connect();
			}

			@Override
			public void revert() {
				conn.remove();
			}
		};
	}

	@Override
	public Change<SubSectionOutputToExternalFlowModel> removeSubSectionOutputToExternalFlow(
			final SubSectionOutputToExternalFlowModel subSectionOutputToExternalFlow) {

		// TODO test this method (removeSubSectionOutputToExternalFlow)

		// Create change to remove connection
		return new AbstractChange<SubSectionOutputToExternalFlowModel>(
				subSectionOutputToExternalFlow, "Remove") {
			@Override
			public void apply() {
				subSectionOutputToExternalFlow.remove();
			}

			@Override
			public void revert() {
				subSectionOutputToExternalFlow.connect();
			}
		};
	}

}