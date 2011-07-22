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

package net.officefloor.plugin.autowire;

import java.lang.reflect.Array;
import java.sql.Connection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.officefloor.compile.impl.structure.OfficeObjectNodeImpl;
import net.officefloor.compile.internal.structure.OfficeObjectNode;
import net.officefloor.compile.internal.structure.SectionObjectNode;
import net.officefloor.compile.properties.PropertyList;
import net.officefloor.compile.spi.office.OfficeArchitect;
import net.officefloor.compile.spi.office.OfficeEscalation;
import net.officefloor.compile.spi.office.OfficeObject;
import net.officefloor.compile.spi.office.OfficeSection;
import net.officefloor.compile.spi.office.OfficeSectionInput;
import net.officefloor.compile.spi.office.OfficeSectionObject;
import net.officefloor.compile.spi.office.OfficeSectionOutput;
import net.officefloor.compile.spi.office.OfficeSubSection;
import net.officefloor.compile.spi.office.OfficeTask;
import net.officefloor.compile.spi.office.OfficeTeam;
import net.officefloor.compile.spi.office.TaskTeam;
import net.officefloor.compile.spi.office.source.OfficeSourceContext;
import net.officefloor.compile.spi.section.source.SectionSource;
import net.officefloor.compile.test.section.SectionLoaderUtil;
import net.officefloor.frame.api.build.OfficeFloorIssues.AssetType;
import net.officefloor.frame.api.escalate.Escalation;
import net.officefloor.frame.api.execute.Task;
import net.officefloor.frame.spi.team.Team;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.plugin.managedobject.clazz.ClassManagedObjectSource;
import net.officefloor.plugin.managedobject.clazz.Dependency;
import net.officefloor.plugin.section.clazz.ClassSectionSource;
import net.officefloor.plugin.section.clazz.ManagedObject;
import net.officefloor.plugin.section.clazz.Property;

/**
 * Tests the {@link AutoWireOfficeSource}.
 * 
 * @author Daniel Sagenschneider
 */
public class AutoWireOfficeSourceTest extends OfficeFrameTestCase {

	/**
	 * Mock {@link OfficeArchitect}.
	 */
	private final OfficeArchitect architect = this
			.createMock(OfficeArchitect.class);

	/**
	 * Mock {@link OfficeSourceContext}.
	 */
	private final OfficeSourceContext context = this
			.createMock(OfficeSourceContext.class);

	/**
	 * Ensure single section.
	 */
	public void testSingleSection() throws Exception {

		final String SECTION = "Section";

		// Create and configure the source
		AutoWireOfficeSource source = new AutoWireOfficeSource();
		this.addSection(source, SECTION, "name", "value");

		// Record creating the section
		this.recordTeam();
		this.recordOfficeSection(SECTION);
		this.recordSectionObjects(SECTION);
		this.recordSectionInputs(SECTION);
		this.recordSectionOutputs(SECTION);
		this.recordAssignTeams(SECTION);

		// Test
		this.replayMockObjects();
		source.sourceOffice(this.architect, context);
		this.verifyMockObjects();
	}

	/**
	 * Ensure can override the {@link AutoWireSection}.
	 */
	public void testOverrideSection() throws Exception {

		final String SECTION = "Section";
		final PropertyList properties = this.createMock(PropertyList.class);
		final String SECTION_LOCATION = "SectionLocation";
		final OfficeSection officeSection = this
				.createMock(OfficeSection.class);

		// Factory to override section
		final AutoWireSection overridden = this
				.createMock(AutoWireSection.class);
		final AutoWireSection[] seedSection = new AutoWireSection[1];
		final AutoWireSectionFactory<AutoWireSection> factory = new AutoWireSectionFactory<AutoWireSection>() {
			@Override
			public AutoWireSection createAutoWireSection(AutoWireSection seed) {
				seedSection[0] = seed;
				return overridden;
			}
		};

		// Record creating the overridden section
		this.recordReturn(overridden, overridden.getProperties(), properties);
		this.recordReturn(overridden, overridden.getSectionName(), SECTION);
		this.recordReturn(overridden, overridden.getSectionSourceClass(),
				SectionSource.class);
		this.recordTeam();
		this.recordReturn(overridden, overridden.getSectionLocation(),
				SECTION_LOCATION);
		this.recordReturn(overridden, overridden.getProperties(), properties);
		this.recordReturn(this.architect, this.architect.addOfficeSection(
				SECTION, SectionSource.class.getName(), SECTION_LOCATION,
				properties), officeSection);
		this.recordReturn(officeSection,
				officeSection.getOfficeSectionObjects(),
				new OfficeSectionObject[0]);
		this.recordReturn(officeSection,
				officeSection.getOfficeSectionInputs(),
				new OfficeSectionInput[0]);
		this.recordReturn(officeSection,
				officeSection.getOfficeSectionOutputs(),
				new OfficeSectionOutput[0]);
		this.recordReturn(officeSection, officeSection.getOfficeTasks(),
				new OfficeTask[0]);
		this.recordReturn(officeSection, officeSection.getOfficeSubSections(),
				new OfficeSubSection[0]);

		// Test
		this.replayMockObjects();

		// Create and configure the source
		AutoWireOfficeSource source = new AutoWireOfficeSource();
		this.addSection(source, SECTION, factory);

		// Source the Office
		source.sourceOffice(this.architect, context);
		this.verifyMockObjects();
	}

	/**
	 * Ensure sub sections.
	 */
	public void testSubSections() throws Exception {

		final String SECTION = "Section";

		// Create and configure the source
		AutoWireOfficeSource source = new AutoWireOfficeSource();
		this.addSection(source, SECTION, "name", "value");

		// Record creating the section
		this.recordTeam();
		this.recordOfficeSection(SECTION);
		this.recordSectionObjects(SECTION);
		this.recordSectionInputs(SECTION);
		this.recordSectionOutputs(SECTION);
		this.recordAssignTeams(SECTION, "SubSection");
		this.recordSubSections("SubSection", "SubSubSectionOne",
				"SubSubSectionTwo");
		this.recordSubSections("SubSubSectionOne");
		this.recordSubSections("SubSubSectionTwo");

		// Test
		this.replayMockObjects();
		source.sourceOffice(this.architect, context);
		this.verifyMockObjects();
	}

	/**
	 * Ensure link flow between sections.
	 */
	public void testLinkFlow() throws Exception {

		final String ONE = "One";
		final String ONE_OUTPUT = "output";
		final String TWO = "Two";
		final String TWO_INPUT = "input";

		// Create and configure the source
		AutoWireOfficeSource source = new AutoWireOfficeSource();
		AutoWireSection one = this.addSection(source, ONE);
		AutoWireSection two = this.addSection(source, TWO);
		source.link(one, ONE_OUTPUT, two, TWO_INPUT);

		// Record creating the section
		this.recordTeam();
		this.recordOfficeSection(ONE);
		this.recordSectionObjects(ONE);
		this.recordSectionInputs(ONE);
		this.recordSectionOutputs(ONE, ONE_OUTPUT);
		this.recordAssignTeams(ONE);
		this.recordOfficeSection(TWO);
		this.recordSectionObjects(TWO);
		this.recordSectionInputs(TWO, TWO_INPUT);
		this.recordSectionOutputs(TWO);
		this.recordAssignTeams(TWO);
		OfficeSectionOutput output = this.outputs.get(ONE).get(ONE_OUTPUT);
		OfficeSectionInput input = this.inputs.get(TWO).get(TWO_INPUT);
		this.architect.link(output, input);

		// Test
		this.replayMockObjects();
		source.sourceOffice(this.architect, context);
		this.verifyMockObjects();
	}

	/**
	 * Ensure able to indicate if a flow is linked.
	 */
	public void testIsFlowLinked() throws Exception {

		final String ONE = "One";
		final String ONE_OUTPUT = "output";
		final String TWO = "Two";
		final String TWO_INPUT = "input";

		// Create source with linked flow
		AutoWireOfficeSource source = new AutoWireOfficeSource();
		AutoWireSection one = this.addSection(source, ONE);
		AutoWireSection two = this.addSection(source, TWO);
		source.link(one, ONE_OUTPUT, two, TWO_INPUT);

		// Ensure indicate if linked
		assertTrue("Should indicate linked", source.isLinked(one, ONE_OUTPUT));

		// Provide indications that not linked
		assertFalse("Output not linked", source.isLinked(one, "NotLinked"));
		assertFalse("Section without output not linked",
				source.isLinked(two, "output"));
	}

	/**
	 * Ensure issue if unknown output.
	 */
	public void testUnknownOutput() throws Exception {

		final String ONE = "One";
		final String ONE_OUTPUT = "output";
		final String TWO = "Two";
		final String TWO_INPUT = "input";

		// Create and configure the source
		AutoWireOfficeSource source = new AutoWireOfficeSource();
		AutoWireSection one = this.addSection(source, ONE);
		AutoWireSection two = this.addSection(source, TWO);
		source.link(one, ONE_OUTPUT, two, TWO_INPUT);

		// Record creating the section
		this.recordTeam();
		this.recordOfficeSection(ONE);
		this.recordSectionObjects(ONE);
		this.recordSectionInputs(ONE);
		this.recordSectionOutputs(ONE);
		this.recordAssignTeams(ONE);
		this.recordOfficeSection(TWO);
		this.recordSectionObjects(TWO);
		this.recordSectionInputs(TWO, TWO_INPUT);
		this.recordSectionOutputs(TWO);
		this.recordAssignTeams(TWO);
		this.architect
				.addIssue(
						"Unknown section output 'One:output' to link to section input 'Two:input'",
						AssetType.TASK, "One:output");

		// Test
		this.replayMockObjects();
		source.sourceOffice(this.architect, context);
		this.verifyMockObjects();
	}

	/**
	 * Ensure issue if unknown input.
	 */
	public void testUnknownInput() throws Exception {

		final String ONE = "One";
		final String ONE_OUTPUT = "output";
		final String TWO = "Two";
		final String TWO_INPUT = "input";

		// Create and configure the source
		AutoWireOfficeSource source = new AutoWireOfficeSource();
		AutoWireSection one = this.addSection(source, ONE);
		AutoWireSection two = this.addSection(source, TWO);
		source.link(one, ONE_OUTPUT, two, TWO_INPUT);

		// Record creating the section
		this.recordTeam();
		this.recordOfficeSection(ONE);
		this.recordSectionObjects(ONE);
		this.recordSectionInputs(ONE);
		this.recordSectionOutputs(ONE, ONE_OUTPUT);
		this.recordAssignTeams(ONE);
		this.recordOfficeSection(TWO);
		this.recordSectionObjects(TWO);
		this.recordSectionInputs(TWO);
		this.recordSectionOutputs(TWO);
		this.recordAssignTeams(TWO);
		this.architect
				.addIssue(
						"Unknown section input 'Two:input' for linking section output 'One:output'",
						AssetType.TASK, "Two:input");

		// Test
		this.replayMockObjects();
		source.sourceOffice(this.architect, this.context);
		this.verifyMockObjects();
	}

	/**
	 * Ensure able to link {@link Escalation} to {@link OfficeSectionInput}.
	 */
	public void testLinkEscalationToSectionInput() throws Exception {

		// Create and configure the source
		AutoWireOfficeSource source = new AutoWireOfficeSource();
		AutoWireSection section = this.addSection(source, "SECTION");
		source.linkEscalation(Exception.class, section, "INPUT");

		// Record linking escalation
		this.recordTeam();
		this.recordOfficeSection("SECTION");
		this.recordSectionObjects("SECTION");
		this.recordSectionInputs("SECTION", "INPUT");
		this.recordSectionOutputs("SECTION");
		this.recordAssignTeams("SECTION");
		this.recordEscalation(Exception.class, "SECTION", "INPUT");

		// Test
		this.replayMockObjects();
		source.sourceOffice(this.architect, this.context);
		this.verifyMockObjects();
	}

	/**
	 * Ensure dependency.
	 */
	public void testDependency() throws Exception {

		final String SECTION = "Section";

		// Create and configure the source
		AutoWireOfficeSource source = new AutoWireOfficeSource();
		this.addSection(source, SECTION);

		// Record creating the section
		this.recordTeam();
		this.recordOfficeSection(SECTION);
		this.recordSectionObjects(SECTION, Connection.class);
		this.recordSectionInputs(SECTION);
		this.recordSectionOutputs(SECTION);
		this.recordAssignTeams(SECTION);

		// Test
		this.replayMockObjects();
		source.sourceOffice(this.architect, this.context);
		this.verifyMockObjects();
	}

	/**
	 * Ensure can assign a {@link Team} responsibility.
	 */
	public void testAssignTeam() throws Exception {
		this.doAssignTeamTest(MockTeamSection.class);
	}

	/**
	 * Mock {@link Team} section class with direct dependency.
	 */
	public static class MockTeamSection {

		public void taskNotAssign(String value) {
		}

		public void taskAssign(Connection connection) {
		}
	}

	/**
	 * Ensure can assign a {@link Team} responsibility based on {@link Task}
	 * having dependency on a {@link ManagedObject} that depends on the object
	 * type..
	 */
	public void testAssignTeamBasedOnManagedObjectDependency() throws Exception {
		this.doAssignTeamTest(MockTeamDependencySection.class);
	}

	/**
	 * Mock {@link Team} {@link ManagedObject} that has dependency.
	 */
	public static class MockTeamDependencyManagedObject {

		@Dependency
		Connection connection;
	}

	/**
	 * Mock {@link Team} section class with dependency via {@link ManagedObject}
	 * dependency.
	 */
	public static class MockTeamDependencySection {

		@ManagedObject(source = ClassManagedObjectSource.class, properties = { @Property(name = ClassManagedObjectSource.CLASS_NAME_PROPERTY_NAME, valueClass = MockTeamDependencyManagedObject.class) })
		MockTeamDependencyManagedObject managedObject;

		/**
		 * {@link Task} depends on {@link #managedObject} which depends on
		 * {@link Connection}.
		 */
		public void taskAssign() {
		}
	}

	/**
	 * Does the assign {@link Team} test.
	 * 
	 * @param sectionClass
	 *            Section class.
	 */
	private void doAssignTeamTest(Class<?> sectionClass) throws Exception {

		// Create and configure the source
		AutoWireOfficeSource source = new AutoWireOfficeSource();
		this.addSection(source, sectionClass);
		AutoWireResponsibility responsibility = source
				.addResponsibility(Connection.class);
		assertEquals("Incorrect team name",
				"team-" + Connection.class.getName(),
				responsibility.getOfficeTeamName());

		// Record creating the teams (responsibilities)
		final OfficeTeam defaultTeam = this.recordTeam();
		final OfficeTeam connectionTeam = this.recordTeam(Connection.class);

		// Record creating the section
		this.recordOfficeSection(sectionClass, new TeamAssigner() {
			@Override
			public void recordAssignTeam(OfficeTask task) {

				// Determine team responsible
				OfficeTeam assignedTeam;
				if ("taskAssign".equals(task.getOfficeTaskName())) {
					assignedTeam = connectionTeam;
				} else {
					assignedTeam = defaultTeam;
				}

				// Assign the team
				AutoWireOfficeSourceTest.this.architect.link(
						task.getTeamResponsible(), assignedTeam);
			}
		});

		// Test
		this.replayMockObjects();
		source.sourceOffice(this.architect, context);
		this.verifyMockObjects();
	}

	/**
	 * Mock {@link OfficeTeam}.
	 */
	private final OfficeTeam team = this.createMock(OfficeTeam.class);

	/**
	 * {@link PropertyList} instances by section name.
	 */
	private final Map<String, PropertyList> sectionProperties = new HashMap<String, PropertyList>();

	/**
	 * {@link OfficeSection} instances by name.
	 */
	private final Map<String, OfficeSection> sections = new HashMap<String, OfficeSection>();

	/**
	 * {@link OfficeSectionInput} instances by {@link OfficeSection} name and
	 * input type.
	 */
	private final Map<String, Map<String, OfficeSectionInput>> inputs = new HashMap<String, Map<String, OfficeSectionInput>>();

	/**
	 * {@link OfficeSectionOutput} instances by {@link OfficeSection} name and
	 * output type.
	 */
	private final Map<String, Map<String, OfficeSectionOutput>> outputs = new HashMap<String, Map<String, OfficeSectionOutput>>();

	/**
	 * {@link OfficeSectionObject} instances by {@link OfficeSection} name and
	 * object type.
	 */
	private final Map<String, Map<String, OfficeSectionObject>> objects = new HashMap<String, Map<String, OfficeSectionObject>>();

	/**
	 * {@link OfficeObject} instances by type.
	 */
	private final Map<Class<?>, OfficeObject> dependencies = new HashMap<Class<?>, OfficeObject>();

	/**
	 * {@link OfficeSubSection} instances by name.
	 */
	private final Map<String, OfficeSubSection> subSections = new HashMap<String, OfficeSubSection>();

	/**
	 * Records the {@link OfficeTeam}.
	 * 
	 * @return {@link OfficeTeam}.
	 */
	private OfficeTeam recordTeam() {
		this.recordReturn(this.architect, this.architect.addOfficeTeam("team"),
				this.team);
		return this.team;
	}

	/**
	 * Records the {@link OfficeTeam} for a object.
	 * 
	 * @return {@link OfficeTeam}.
	 */
	private OfficeTeam recordTeam(Class<?> objectType) {
		OfficeTeam officeTeam = this.createMock(OfficeTeam.class);
		this.recordReturn(this.architect,
				this.architect.addOfficeTeam("team-" + objectType.getName()),
				officeTeam);
		return officeTeam;
	}

	/**
	 * Adds an {@link OfficeSection}.
	 * 
	 * @param source
	 *            {@link AutoWireOfficeSource}.
	 * @param sectionClass
	 *            {@link ClassSectionSource} class.
	 * @return {@link AutoWireSection}.
	 */
	private AutoWireSection addSection(AutoWireOfficeSource source,
			Class<?> sectionClass) {

		final String SECTION_NAME = sectionClass.getSimpleName();

		// Add the section
		AutoWireSection section = source.addSection(SECTION_NAME,
				ClassSectionSource.class, sectionClass.getName());

		// Register the properties
		PropertyList properties = section.getProperties();
		this.sectionProperties.put(SECTION_NAME, properties);

		// Return the section
		return section;
	}

	/**
	 * Records the {@link OfficeSection} for the {@link ClassSectionSource}
	 * class.
	 * 
	 * @param sectionClass
	 *            {@link ClassSectionSource} class.
	 * @param assigner
	 *            {@link TeamAssigner}.
	 * @return {@link OfficeSection}.
	 */
	private OfficeSection recordOfficeSection(Class<?> sectionClass,
			TeamAssigner assigner) {

		final String SECTION_NAME = sectionClass.getSimpleName();

		// Obtain the properties
		PropertyList properties = this.sectionProperties.get(SECTION_NAME);
		assertNotNull("Section " + SECTION_NAME + " should be added",
				properties);

		// Load the office section
		OfficeSection officeSection = SectionLoaderUtil.loadOfficeSection(
				SECTION_NAME, ClassSectionSource.class, sectionClass.getName());
		this.sections.put(SECTION_NAME, officeSection);

		// Record adding the office section
		this.recordReturn(this.architect, this.architect.addOfficeSection(
				SECTION_NAME, ClassSectionSource.class.getName(),
				sectionClass.getName(), properties), officeSection);

		// Record the office section objects
		for (OfficeSectionObject sectionObject : officeSection
				.getOfficeSectionObjects()) {
			String objectType = sectionObject.getObjectType();
			OfficeObjectNode officeObject = new OfficeObjectNodeImpl("TEST",
					"TEST", null);
			this.recordReturn(this.architect,
					this.architect.addOfficeObject(objectType, objectType),
					officeObject);

			// Record the link
			this.architect.link(sectionObject, officeObject);

			// Ensure linked
			((SectionObjectNode) sectionObject).linkObjectNode(officeObject);
		}

		// Should not require linking inputs and outputs

		// Record assigning teams
		for (OfficeTask task : officeSection.getOfficeTasks()) {
			assigner.recordAssignTeam(task);
		}
		// Should not require sub sections for team assignment

		// Return the office section
		return officeSection;
	}

	/**
	 * Records assigning the {@link Team} for the {@link OfficeTask}.
	 */
	private static interface TeamAssigner {

		/**
		 * Records assigning the {@link Team} for the {@link OfficeTask}.
		 * 
		 * @param task
		 *            {@link OfficeTask}.
		 */
		void recordAssignTeam(OfficeTask task);
	}

	/**
	 * Adds an {@link OfficeSection}.
	 * 
	 * @param source
	 *            {@link AutoWireOfficeSource}.
	 * @param sectionName
	 *            Name of the {@link OfficeSection}.
	 * @param factory
	 *            {@link AutoWireSectionFactory}.
	 * @param propertyNameValues
	 *            Property name value pairs.
	 * @return {@link AutoWireSection}.
	 */
	private <A extends AutoWireSection> AutoWireSection addSection(
			AutoWireOfficeSource source, String sectionName,
			AutoWireSectionFactory<A> factory, String... propertyNameValues) {

		// Add the section
		AutoWireSection section;
		final String sectionLocation = sectionName + "Location";
		if (factory == null) {
			section = source.addSection(sectionName, SectionSource.class,
					sectionLocation);
		} else {
			section = source.addSection(sectionName, SectionSource.class,
					sectionLocation, factory);
		}

		// Load the properties
		PropertyList properties = section.getProperties();
		for (int i = 0; i < propertyNameValues.length; i += 2) {
			String name = propertyNameValues[i];
			String value = propertyNameValues[i + 1];
			properties.addProperty(name).setValue(value);
		}
		this.sectionProperties.put(sectionName, properties);

		// Return the section
		return section;
	}

	/**
	 * Adds an {@link OfficeSection}.
	 * 
	 * @param source
	 *            {@link AutoWireOfficeSource}.
	 * @param sectionName
	 *            Name of the {@link OfficeSection}.
	 * @param propertyNameValues
	 *            Property name value pairs.
	 * @return {@link AutoWireSection}.
	 */
	private AutoWireSection addSection(AutoWireOfficeSource source,
			String sectionName, String... propertyNameValues) {
		return this.addSection(source, sectionName, null, propertyNameValues);
	}

	/**
	 * Records adding an {@link OfficeSection}.
	 * 
	 * @param sectionName
	 *            Name.
	 * @param subSectionNames
	 *            Sub section names.
	 * @return {@link OfficeSection}.
	 */
	private void recordOfficeSection(String sectionName) {
		assertNull("Already section by name " + sectionName,
				this.sections.get(sectionName));

		// Obtain the properties
		PropertyList properties = this.sectionProperties.get(sectionName);
		assertNotNull("Section " + sectionName + " should be added", properties);

		// Record creating the section
		OfficeSection section = this.createMock(OfficeSection.class);
		this.recordReturn(this.architect, this.architect.addOfficeSection(
				sectionName, SectionSource.class.getName(), sectionName
						+ "Location", properties), section);
		this.sections.put(sectionName, section);
	}

	/**
	 * Obtains the {@link OfficeSection}.
	 * 
	 * @param sectionName
	 *            Name.
	 * @return {@link OfficeSection}.
	 */
	private OfficeSection getOfficeSection(String sectionName) {
		OfficeSection section = this.sections.get(sectionName);
		assertNotNull("Unknown section " + sectionName, section);
		return section;
	}

	/**
	 * Records the {@link OfficeSectionInput} instances.
	 * 
	 * @param sectionName
	 *            Name of {@link OfficeSection}.
	 * @param inputNames
	 *            Names of the inputs.
	 */
	private void recordSectionInputs(String sectionName, String... inputNames) {
		OfficeSectionInput[] inputs = this.createSectionItems(sectionName,
				OfficeSectionInput.class, this.inputs, inputNames);
		OfficeSection section = this.getOfficeSection(sectionName);
		this.recordReturn(section, section.getOfficeSectionInputs(), inputs);
		for (int i = 0; i < inputNames.length; i++) {
			OfficeSectionInput input = inputs[i];
			this.recordReturn(input, input.getOfficeSectionInputName(),
					inputNames[i]);
		}
	}

	/**
	 * Records the {@link OfficeSectionOutput} instances.
	 * 
	 * @param sectionName
	 *            Name of {@link OfficeSection}.
	 * @param outputNames
	 *            Names of the outputs.
	 */
	private void recordSectionOutputs(String sectionName, String... outputNames) {
		OfficeSectionOutput[] outputs = this.createSectionItems(sectionName,
				OfficeSectionOutput.class, this.outputs, outputNames);
		OfficeSection section = this.getOfficeSection(sectionName);
		this.recordReturn(section, section.getOfficeSectionOutputs(), outputs);
		for (int i = 0; i < outputNames.length; i++) {
			OfficeSectionOutput output = outputs[i];
			this.recordReturn(output, output.getOfficeSectionOutputName(),
					outputNames[i]);
		}
	}

	/**
	 * Records the {@link OfficeSectionObject} instances.
	 * 
	 * @param sectionName
	 *            Name of {@link OfficeSection}.
	 * @param objectTypes
	 *            Types of the objects.
	 */
	private void recordSectionObjects(String sectionName,
			Class<?>... objectTypes) {

		// Create names of the objects
		String[] objectNames = new String[objectTypes.length];
		for (int i = 0; i < objectTypes.length; i++) {
			objectNames[i] = objectTypes[i].getName();
		}

		// Record obtaining the objects
		OfficeSectionObject[] objects = this.createSectionItems(sectionName,
				OfficeSectionObject.class, this.objects, objectNames);
		OfficeSection section = this.getOfficeSection(sectionName);
		this.recordReturn(section, section.getOfficeSectionObjects(), objects);

		// Link objects as dependencies
		for (int i = 0; i < objectTypes.length; i++) {
			Class<?> objectType = objectTypes[i];
			OfficeSectionObject object = objects[i];

			// Obtain object type
			this.recordReturn(object, object.getObjectType(),
					objectType.getName());

			// Lazy add the dependency
			OfficeObject dependency = this.dependencies.get(objectType);
			if (dependency == null) {
				dependency = this.createMock(OfficeObject.class);
				this.dependencies.put(objectType, dependency);
				this.recordReturn(this.architect, this.architect
						.addOfficeObject(objectType.getName(),
								objectType.getName()), dependency);
			}

			// Link the object to dependency
			this.architect.link(object, dependency);
		}
	}

	/**
	 * Records the assigning the {@link Team} instances.
	 * 
	 * @param sectionName
	 *            Section name.
	 * @param subSectionNames
	 *            Name of the {@link OfficeSubSection} instances.
	 */
	private void recordAssignTeams(String sectionName,
			String... subSectionNames) {

		// Obtain the section
		OfficeSection section = this.getOfficeSection(sectionName);

		// Record task on section
		OfficeTask task = this.createMock(OfficeTask.class);
		this.recordReturn(section, section.getOfficeTasks(),
				new OfficeTask[] { task });
		TaskTeam taskTeam = this.createMock(TaskTeam.class);
		this.recordReturn(task, task.getTeamResponsible(), taskTeam);
		this.architect.link(taskTeam, this.team);

		// Record the sub sections
		this.recordSubSections(sectionName, subSectionNames);
	}

	/**
	 * Records the {@link OfficeSubSection} instances.
	 * 
	 * @param sectionName
	 *            Name of the {@link OfficeSection} or {@link OfficeSubSection}.
	 * @param subSectionNames
	 *            Names of the {@link OfficeSubSection}.
	 */
	private void recordSubSections(String sectionName,
			String... subSectionNames) {

		// Create the sub sections
		List<OfficeSubSection> list = new LinkedList<OfficeSubSection>();
		for (String subSectionName : subSectionNames) {
			assertNull("Sub section already registered " + subSectionName,
					this.subSections.get(subSectionName));
			OfficeSubSection subSection = this
					.createMock(OfficeSubSection.class);
			this.subSections.put(subSectionName, subSection);
			list.add(subSection);
		}
		OfficeSubSection[] subSections = list.toArray(new OfficeSubSection[list
				.size()]);

		// Record from office section
		OfficeSection section = this.sections.get(sectionName);
		if (section != null) {
			this.recordReturn(section, section.getOfficeSubSections(),
					subSections);

		} else {
			// Record from office sub section
			OfficeSubSection subSection = this.subSections.get(sectionName);
			if (subSection != null) {
				this.recordReturn(subSection,
						subSection.getOfficeSubSections(), subSections);

			} else {
				// Unknown section
				fail("Unknown section " + sectionName);
			}
		}

		// Link sub section tasks to team
		for (OfficeSubSection subSection : subSections) {
			OfficeTask task = this.createMock(OfficeTask.class);
			this.recordReturn(subSection, subSection.getOfficeTasks(),
					new OfficeTask[] { task });
			TaskTeam taskTeam = this.createMock(TaskTeam.class);
			this.recordReturn(task, task.getTeamResponsible(), taskTeam);
			this.architect.link(taskTeam, this.team);
		}
	}

	/**
	 * Records handling of the {@link Escalation}.
	 * 
	 * @param escalationType
	 *            Type of {@link Escalation}.
	 * @param sectionName
	 *            {@link AutoWireSection} name.
	 * @param inputName
	 *            Name of the {@link OfficeSectionInput}.
	 */
	private void recordEscalation(Class<? extends Throwable> escalationType,
			String sectionName, String inputName) {
		OfficeEscalation escalation = this.createMock(OfficeEscalation.class);
		this.recordReturn(this.architect,
				this.architect.addOfficeEscalation(escalationType.getName()),
				escalation);
		OfficeSectionInput sectionInput = this.inputs.get(sectionName).get(
				inputName);
		this.architect.link(escalation, sectionInput);
	}

	/**
	 * Records the {@link OfficeSectionInput} instances.
	 * 
	 * @param sectionName
	 *            Name of {@link OfficeSection}.
	 * @param itemType
	 *            Item type.
	 * @param items
	 *            Existing items.
	 * @param itemNames
	 *            Names of the items.
	 */
	@SuppressWarnings("unchecked")
	private <T> T[] createSectionItems(String sectionName, Class<T> itemType,
			Map<String, Map<String, T>> items, String... itemNames) {
		assertNull("Already obtained  " + itemType.getSimpleName()
				+ " for section " + sectionName, items.get(sectionName));

		// Create and register the items
		Map<String, T> entries = new HashMap<String, T>();
		items.put(sectionName, entries);
		List<T> list = new LinkedList<T>();
		for (String itemName : itemNames) {
			T item = this.createMock(itemType);
			entries.put(itemName, item);
			list.add(item);
		}

		// Return the listing of items
		return list.toArray((T[]) Array.newInstance(itemType, list.size()));
	}

}