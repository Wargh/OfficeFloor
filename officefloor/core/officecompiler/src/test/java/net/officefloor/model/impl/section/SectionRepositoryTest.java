/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2013 Daniel Sagenschneider
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

import java.sql.Connection;

import org.easymock.AbstractMatcher;

import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.model.ConnectionModel;
import net.officefloor.model.repository.ConfigurationItem;
import net.officefloor.model.repository.ModelRepository;
import net.officefloor.model.section.ExternalFlowModel;
import net.officefloor.model.section.ExternalManagedObjectModel;
import net.officefloor.model.section.FunctionEscalationModel;
import net.officefloor.model.section.FunctionEscalationToExternalFlowModel;
import net.officefloor.model.section.FunctionEscalationToFunctionModel;
import net.officefloor.model.section.FunctionFlowModel;
import net.officefloor.model.section.FunctionFlowToExternalFlowModel;
import net.officefloor.model.section.FunctionFlowToFunctionModel;
import net.officefloor.model.section.FunctionModel;
import net.officefloor.model.section.FunctionNamespaceModel;
import net.officefloor.model.section.FunctionToNextExternalFlowModel;
import net.officefloor.model.section.FunctionToNextFunctionModel;
import net.officefloor.model.section.ManagedFunctionModel;
import net.officefloor.model.section.ManagedFunctionObjectModel;
import net.officefloor.model.section.ManagedFunctionObjectToExternalManagedObjectModel;
import net.officefloor.model.section.ManagedFunctionObjectToSectionManagedObjectModel;
import net.officefloor.model.section.ManagedFunctionToFunctionModel;
import net.officefloor.model.section.SectionManagedObjectDependencyModel;
import net.officefloor.model.section.SectionManagedObjectDependencyToExternalManagedObjectModel;
import net.officefloor.model.section.SectionManagedObjectDependencyToSectionManagedObjectModel;
import net.officefloor.model.section.SectionManagedObjectModel;
import net.officefloor.model.section.SectionManagedObjectSourceFlowModel;
import net.officefloor.model.section.SectionManagedObjectSourceFlowToExternalFlowModel;
import net.officefloor.model.section.SectionManagedObjectSourceFlowToFunctionModel;
import net.officefloor.model.section.SectionManagedObjectSourceFlowToSubSectionInputModel;
import net.officefloor.model.section.SectionManagedObjectSourceModel;
import net.officefloor.model.section.SectionManagedObjectToSectionManagedObjectSourceModel;
import net.officefloor.model.section.SectionModel;
import net.officefloor.model.section.SectionRepository;
import net.officefloor.model.section.SubSectionInputModel;
import net.officefloor.model.section.SubSectionModel;
import net.officefloor.model.section.SubSectionObjectModel;
import net.officefloor.model.section.SubSectionObjectToExternalManagedObjectModel;
import net.officefloor.model.section.SubSectionObjectToSectionManagedObjectModel;
import net.officefloor.model.section.SubSectionOutputModel;
import net.officefloor.model.section.SubSectionOutputToExternalFlowModel;
import net.officefloor.model.section.SubSectionOutputToSubSectionInputModel;

/**
 * Tests the {@link SectionRepository}.
 *
 * @author Daniel Sagenschneider
 */
public class SectionRepositoryTest extends OfficeFrameTestCase {

	/**
	 * {@link ModelRepository}.
	 */
	private final ModelRepository modelRepository = this.createMock(ModelRepository.class);

	/**
	 * {@link ConfigurationItem}.
	 */
	private final ConfigurationItem configurationItem = this.createMock(ConfigurationItem.class);

	/**
	 * {@link SectionRepository} to be tested.
	 */
	private final SectionRepository sectionRepository = new SectionRepositoryImpl(this.modelRepository);

	/**
	 * Ensures on retrieving a {@link SectionModel} that all
	 * {@link ConnectionModel} instances are connected.
	 */
	public void testRetrieveSection() throws Exception {

		// Create the raw section to be connected
		SectionModel section = new SectionModel();
		SubSectionModel subSection = new SubSectionModel("SUB_SECTION", "net.example.ExampleSectionSource",
				"SECTION_LOCATION");
		section.addSubSection(subSection);
		SubSectionInputModel input = new SubSectionInputModel("INPUT", Integer.class.getName(), false, null);
		subSection.addSubSectionInput(input);
		SubSectionOutputModel output = new SubSectionOutputModel("OUTPUT", Integer.class.getName(), false);
		subSection.addSubSectionOutput(output);
		ExternalFlowModel extFlow = new ExternalFlowModel("EXTERNAL_FLOW", Integer.class.getName());
		section.addExternalFlow(extFlow);
		ExternalManagedObjectModel extMo = new ExternalManagedObjectModel("EXTERNAL_MANAGED_OBJECT",
				Object.class.getName());
		section.addExternalManagedObject(extMo);
		SectionManagedObjectModel mo = new SectionManagedObjectModel("MANAGED_OBJECT", "THREAD");
		section.addSectionManagedObject(mo);
		SectionManagedObjectDependencyModel dependency = new SectionManagedObjectDependencyModel("DEPENDENCY",
				Object.class.getName());
		mo.addSectionManagedObjectDependency(dependency);
		SectionManagedObjectSourceModel mos = new SectionManagedObjectSourceModel("MANAGED_OBJECT_SOURCE",
				"net.example.ExampleManagedObjectSource", Connection.class.getName(), "0");
		section.addSectionManagedObjectSource(mos);
		SectionManagedObjectSourceFlowModel mosFlow = new SectionManagedObjectSourceFlowModel("FLOW",
				Object.class.getName());
		mos.addSectionManagedObjectSourceFlow(mosFlow);
		FunctionNamespaceModel namespace = new FunctionNamespaceModel("NAMESPACE",
				"net.example.ExampleManagedFunctionSource");
		section.addFunctionNamespace(namespace);
		ManagedFunctionModel managedFunction = new ManagedFunctionModel("MANAGED_FUNCTION");
		namespace.addManagedFunction(managedFunction);
		FunctionModel function = new FunctionModel("FUNCTION", false, "NAMESPACE", "MANAGED_FUNCTION",
				Object.class.getName());
		section.addFunction(function);
		ManagedFunctionObjectModel functionObject = new ManagedFunctionObjectModel();
		managedFunction.addManagedFunctionObject(functionObject);

		// managed object -> managed object source
		SectionManagedObjectToSectionManagedObjectSourceModel moToMos = new SectionManagedObjectToSectionManagedObjectSourceModel(
				"MANAGED_OBJECT_SOURCE");
		mo.setSectionManagedObjectSource(moToMos);

		// managed object source flow -> external flow
		SectionManagedObjectSourceFlowToExternalFlowModel flowToExtFlow = new SectionManagedObjectSourceFlowToExternalFlowModel(
				"EXTERNAL_FLOW");
		mosFlow.setExternalFlow(flowToExtFlow);

		// managed object source flow -> sub section input
		SectionManagedObjectSourceFlowToSubSectionInputModel flowToInput = new SectionManagedObjectSourceFlowToSubSectionInputModel(
				"SUB_SECTION", "INPUT");
		mosFlow.setSubSectionInput(flowToInput);

		// managed object source flow -> function
		SectionManagedObjectSourceFlowToFunctionModel mosFlowToFunction = new SectionManagedObjectSourceFlowToFunctionModel(
				"FUNCTION");
		mosFlow.setFunction(mosFlowToFunction);

		// dependency -> external managed object
		SectionManagedObjectDependencyToExternalManagedObjectModel dependencyToExtMo = new SectionManagedObjectDependencyToExternalManagedObjectModel(
				"EXTERNAL_MANAGED_OBJECT");
		dependency.setExternalManagedObject(dependencyToExtMo);

		// dependency -> managed object
		SectionManagedObjectDependencyToSectionManagedObjectModel dependencyToMo = new SectionManagedObjectDependencyToSectionManagedObjectModel(
				"MANAGED_OBJECT");
		dependency.setSectionManagedObject(dependencyToMo);

		// output -> input
		SubSectionOutputToSubSectionInputModel outputToInput = new SubSectionOutputToSubSectionInputModel("SUB_SECTION",
				"INPUT");
		output.setSubSectionInput(outputToInput);

		// output -> extFlow
		SubSectionOutputModel output_extFlow = new SubSectionOutputModel("OUTPUT_EXTERNAL_FLOW",
				Exception.class.getName(), true);
		subSection.addSubSectionOutput(output_extFlow);
		SubSectionOutputToExternalFlowModel outputToExtFlow = new SubSectionOutputToExternalFlowModel("EXTERNAL_FLOW");
		output_extFlow.setExternalFlow(outputToExtFlow);

		// section object -> extMo
		SubSectionObjectModel sectionObject_extMo = new SubSectionObjectModel("OBJECT_EXT", Object.class.getName());
		subSection.addSubSectionObject(sectionObject_extMo);
		SubSectionObjectToExternalManagedObjectModel sectionObjectToExtMo = new SubSectionObjectToExternalManagedObjectModel(
				"EXTERNAL_MANAGED_OBJECT");
		sectionObject_extMo.setExternalManagedObject(sectionObjectToExtMo);

		// object -> managed object
		SubSectionObjectModel object_mo = new SubSectionObjectModel("OBJECT_MO", Connection.class.getName());
		subSection.addSubSectionObject(object_mo);
		SubSectionObjectToSectionManagedObjectModel objectToMo = new SubSectionObjectToSectionManagedObjectModel(
				"MANAGED_OBJECT");
		object_mo.setSectionManagedObject(objectToMo);

		// functionObject -> extMo
		ManagedFunctionObjectToExternalManagedObjectModel functionObjectToExtMo = new ManagedFunctionObjectToExternalManagedObjectModel(
				"EXTERNAL_MANAGED_OBJECT");
		functionObject.setExternalManagedObject(functionObjectToExtMo);

		// functionObject -> managed object
		ManagedFunctionObjectToSectionManagedObjectModel functionObjectToMo = new ManagedFunctionObjectToSectionManagedObjectModel(
				"MANAGED_OBJECT");
		functionObject.setSectionManagedObject(functionObjectToMo);

		// functionFlow -> extFlow
		FunctionFlowModel functionFlow_extFlow = new FunctionFlowModel();
		function.addFunctionFlow(functionFlow_extFlow);
		ExternalFlowModel extFlow_functionFlow = new ExternalFlowModel("functionFlow - extFlow",
				String.class.getName());
		section.addExternalFlow(extFlow_functionFlow);
		FunctionFlowToExternalFlowModel functionFlowToExtFlow = new FunctionFlowToExternalFlowModel(
				"functionFlow - extFlow", false);
		functionFlow_extFlow.setExternalFlow(functionFlowToExtFlow);

		// functionFlow -> function
		FunctionFlowModel functionFlow_function = new FunctionFlowModel();
		function.addFunctionFlow(functionFlow_function);
		FunctionModel function_functionFlow = new FunctionModel("flow - function", false, "namespace",
				"managed_function", Object.class.getName());
		section.addFunction(function_functionFlow);
		FunctionFlowToFunctionModel functionFlowToFunction = new FunctionFlowToFunctionModel("flow - function", true);
		functionFlow_function.setFunction(functionFlowToFunction);

		// next -> extFlow
		ExternalFlowModel extFlow_next = new ExternalFlowModel("next - extFlow", Integer.class.getName());
		section.addExternalFlow(extFlow_next);
		FunctionToNextExternalFlowModel nextToExtFlow = new FunctionToNextExternalFlowModel("next - extFlow");
		function.setNextExternalFlow(nextToExtFlow);

		// next -> function
		FunctionModel function_next = new FunctionModel("next - function", false, "namespace", "managed_function",
				Integer.class.getName());
		section.addFunction(function_next);
		FunctionToNextFunctionModel nextToFunction = new FunctionToNextFunctionModel("next - function");
		function.setNextFunction(nextToFunction);

		// functionEscalation -> extFlow
		FunctionEscalationModel functionEscalation_extFlow = new FunctionEscalationModel();
		function.addFunctionEscalation(functionEscalation_extFlow);
		ExternalFlowModel extFlow_functionEscalation = new ExternalFlowModel("escalation - extFlow",
				Throwable.class.getName());
		section.addExternalFlow(extFlow_functionEscalation);
		FunctionEscalationToExternalFlowModel escalationToExtFlow = new FunctionEscalationToExternalFlowModel(
				"escalation - extFlow");
		functionEscalation_extFlow.setExternalFlow(escalationToExtFlow);

		// functionEscalation -> function
		FunctionEscalationModel functionEscalation_function = new FunctionEscalationModel();
		function.addFunctionEscalation(functionEscalation_function);
		FunctionModel function_functionEscalation = new FunctionModel("escalation - function", false, "NAMESPACE",
				"MANAGED_FUNCTION", Object.class.getName());
		section.addFunction(function_functionEscalation);
		FunctionEscalationToFunctionModel escalationToFunction = new FunctionEscalationToFunctionModel(
				"escalation - function");
		functionEscalation_function.setFunction(escalationToFunction);

		// Record retrieving the section
		this.recordReturn(this.modelRepository, this.modelRepository.retrieve(null, this.configurationItem), section,
				new AbstractMatcher() {
					@Override
					public boolean matches(Object[] expected, Object[] actual) {
						assertTrue("Must be section model", actual[0] instanceof SectionModel);
						assertEquals("Incorrect configuration item", SectionRepositoryTest.this.configurationItem,
								actual[1]);
						return true;
					}
				});

		// Retrieve the section
		this.replayMockObjects();
		SectionModel retrievedSection = this.sectionRepository.retrieveSection(this.configurationItem);
		this.verifyMockObjects();
		assertEquals("Incorrect section", section, retrievedSection);

		// Ensure managed object connected to its source
		assertEquals("mo -> mos", mos, moToMos.getSectionManagedObjectSource());
		assertEquals("mo <- mos", mo, moToMos.getSectionManagedObject());

		// Ensure managed object source flow connected to external flow
		assertEquals("mos flow <- external flow", mosFlow, flowToExtFlow.getSectionManagedObjectSourceFlow());
		assertEquals("mos flow -> external flow", extFlow, flowToExtFlow.getExternalFlow());

		// Ensure managed object source flow connected to sub section input
		assertEquals("mos flow <- sub section input", mosFlow, flowToInput.getSectionManagedObjectSourceFlow());
		assertEquals("mos flow -> sub section input", input, flowToInput.getSubSectionInput());

		// Ensure managed object source flow connected to function
		assertEquals("mos flow <- function", mosFlow, mosFlowToFunction.getSectionManagedObjectSourceFlow());
		assertEquals("mos flow -> function", function, mosFlowToFunction.getFunction());

		// Ensure dependency connected to external managed object
		assertEquals("dependency <- external mo", dependency, dependencyToExtMo.getSectionManagedObjectDependency());
		assertEquals("dependency -> managed object", extMo, dependencyToExtMo.getExternalManagedObject());

		// Ensure dependency connected to managed object
		assertEquals("dependency <- managed object", dependency, dependencyToMo.getSectionManagedObjectDependency());
		assertEquals("dependency -> managed object", mo, dependencyToMo.getSectionManagedObject());

		// Ensure output to input connected
		assertEquals("output -> input", input, outputToInput.getSubSectionInput());
		assertEquals("output <- input", output, outputToInput.getSubSectionOutput());

		// Ensure the external flow connected
		assertEquals("output -> extFlow", extFlow, outputToExtFlow.getExternalFlow());
		assertEquals("output <- extFlow", output_extFlow, outputToExtFlow.getSubSectionOutput());

		// Ensure the external managed object connected
		assertEquals("object -> extMo", extMo, sectionObjectToExtMo.getExternalManagedObject());
		assertEquals("object <- extMo", sectionObject_extMo, sectionObjectToExtMo.getSubSectionObject());

		// Ensure the section managed object connected
		assertEquals("object -> mo", mo, objectToMo.getSectionManagedObject());
		assertEquals("object <- mo", object_mo, objectToMo.getSubSectionObject());

		// Ensure the external managed object connected
		assertEquals("functionObject <- extMo", functionObject, functionObjectToExtMo.getManagedFunctionObject());
		assertEquals("functionObject -> extMo", extMo, functionObjectToExtMo.getExternalManagedObject());

		// Ensure function object connected to managed object
		assertEquals("functionObject <- managed object", functionObject, functionObjectToMo.getManagedFunctionObject());
		assertEquals("functionObject -> managed object", mo, functionObjectToMo.getSectionManagedObject());

		// Ensure the external flow connected
		assertEquals("functionFlow <- extFlow", functionFlow_extFlow, functionFlowToExtFlow.getFunctionFlow());
		assertEquals("functionFlow -> extFlow", extFlow_functionFlow, functionFlowToExtFlow.getExternalFlow());

		// Ensure the next external flow connected
		assertEquals("next -> extFlow", function, nextToExtFlow.getPreviousFunction());
		assertEquals("next <- extFlow", extFlow_next, nextToExtFlow.getNextExternalFlow());

		// Ensure flow to function connected
		assertEquals("functionFlow <- function", functionFlow_function, functionFlowToFunction.getFunctionFlow());
		assertEquals("functionFlow -> function", function_functionFlow, functionFlowToFunction.getFunction());

		// Ensure escalation to function connected
		assertEquals("functionEscalation <- function", functionEscalation_function,
				escalationToFunction.getEscalation());
		assertEquals("functionEscalation -> function", function_functionEscalation, escalationToFunction.getFunction());

		// Ensure escalation to external flow connected
		assertEquals("functionEscalation <- extFlow", functionEscalation_extFlow,
				escalationToExtFlow.getFunctionEscalation());
		assertEquals("functionEscalation -> extFlow", extFlow_functionEscalation,
				escalationToExtFlow.getExternalFlow());

		// Ensure the next function connected
		assertEquals("next <- function", function, nextToFunction.getPreviousFunction());
		assertEquals("next -> function", function_next, nextToFunction.getNextFunction());

		// Ensure the functions are connected to their namespace functions
		assertEquals("function <- managedFunction", managedFunction,
				function.getManagedFunction().getManagedFunction());
		assertEquals("function -> managedFunction", function, managedFunction.getFunctions().get(0).getFunction());
	}

	/**
	 * Ensures on storing a {@link SectionModel} that all
	 * {@link ConnectionModel} instances are readied for storing.
	 */
	public void testStoreSection() throws Exception {

		// Create the section (without connections)
		SectionModel section = new SectionModel();
		SubSectionModel subSection = new SubSectionModel("SUB_SECTION", "net.example.ExampleSectionSource",
				"SECTION_LOCATION");
		section.addSubSection(subSection);
		SubSectionInputModel input = new SubSectionInputModel("INPUT", Integer.class.getName(), false, null);
		subSection.addSubSectionInput(input);
		SubSectionOutputModel output_input = new SubSectionOutputModel("OUTPUT_INPUT", Integer.class.getName(), false);
		subSection.addSubSectionOutput(output_input);
		SubSectionOutputModel output_extFlow = new SubSectionOutputModel("OUTPUT_EXTERNAL_FLOW",
				Exception.class.getName(), true);
		subSection.addSubSectionOutput(output_extFlow);
		ExternalFlowModel extFlow = new ExternalFlowModel("FLOW", Integer.class.getName());
		section.addExternalFlow(extFlow);
		SubSectionObjectModel object = new SubSectionObjectModel("OBJECT", Object.class.getName());
		subSection.addSubSectionObject(object);
		ExternalManagedObjectModel extMo = new ExternalManagedObjectModel("MO", Object.class.getName());
		section.addExternalManagedObject(extMo);
		SectionManagedObjectModel mo = new SectionManagedObjectModel("MANAGED_OBJECT", "THREAD");
		section.addSectionManagedObject(mo);
		SectionManagedObjectDependencyModel dependency = new SectionManagedObjectDependencyModel("DEPENDENCY",
				Object.class.getName());
		mo.addSectionManagedObjectDependency(dependency);
		SectionManagedObjectSourceModel mos = new SectionManagedObjectSourceModel("MANAGED_OBJECT_SOURCE",
				"net.example.ExampleManagedObjectSource", Connection.class.getName(), "0");
		section.addSectionManagedObjectSource(mos);
		SectionManagedObjectSourceFlowModel mosFlow = new SectionManagedObjectSourceFlowModel("MOS_FLOW",
				Object.class.getName());
		mos.addSectionManagedObjectSourceFlow(mosFlow);
		FunctionNamespaceModel namespace = new FunctionNamespaceModel("NAMESPACE",
				"net.example.ExampleManagedFunctionSource");
		section.addFunctionNamespace(namespace);
		ManagedFunctionModel managedFunction = new ManagedFunctionModel("MANAGED_FUNCTION");
		namespace.addManagedFunction(managedFunction);
		ManagedFunctionObjectModel functionObject = new ManagedFunctionObjectModel("OBJECT", null,
				Object.class.getName(), false);
		managedFunction.addManagedFunctionObject(functionObject);
		FunctionModel function = new FunctionModel("FUNCTION", false, "NAMESPACE", "MANAGED_FUNCTION",
				Object.class.getName());
		section.addFunction(function);
		FunctionFlowModel functionFlow = new FunctionFlowModel("FLOW", null, String.class.getName());
		function.addFunctionFlow(functionFlow);
		FunctionEscalationModel functionEscalation = new FunctionEscalationModel(Exception.class.getName());
		function.addFunctionEscalation(functionEscalation);
		section.addExternalManagedObject(extMo);
		section.addExternalFlow(extFlow);

		// mo -> mos
		SectionManagedObjectToSectionManagedObjectSourceModel moToMos = new SectionManagedObjectToSectionManagedObjectSourceModel();
		moToMos.setSectionManagedObject(mo);
		moToMos.setSectionManagedObjectSource(mos);
		moToMos.connect();

		// mos flow -> external flow
		SectionManagedObjectSourceFlowToExternalFlowModel mosFlowToExtFlow = new SectionManagedObjectSourceFlowToExternalFlowModel();
		mosFlowToExtFlow.setSectionManagedObjectSourceFlow(mosFlow);
		mosFlowToExtFlow.setExternalFlow(extFlow);
		mosFlowToExtFlow.connect();

		// mos flow -> sub section input
		SectionManagedObjectSourceFlowToSubSectionInputModel mosFlowToInput = new SectionManagedObjectSourceFlowToSubSectionInputModel();
		mosFlowToInput.setSectionManagedObjectSourceFlow(mosFlow);
		mosFlowToInput.setSubSectionInput(input);
		mosFlowToInput.connect();

		// managed object source flow -> function
		SectionManagedObjectSourceFlowToFunctionModel mosFlowToFunction = new SectionManagedObjectSourceFlowToFunctionModel();
		mosFlowToFunction.setSectionManagedObjectSourceFlow(mosFlow);
		mosFlowToFunction.setFunction(function);
		mosFlowToFunction.connect();

		// dependency -> extMo
		SectionManagedObjectDependencyToExternalManagedObjectModel dependencyToExtMo = new SectionManagedObjectDependencyToExternalManagedObjectModel();
		dependencyToExtMo.setSectionManagedObjectDependency(dependency);
		dependencyToExtMo.setExternalManagedObject(extMo);
		dependencyToExtMo.connect();

		// dependency -> mo
		SectionManagedObjectDependencyToSectionManagedObjectModel dependencyToMo = new SectionManagedObjectDependencyToSectionManagedObjectModel();
		dependencyToMo.setSectionManagedObjectDependency(dependency);
		dependencyToMo.setSectionManagedObject(mo);
		dependencyToMo.connect();

		// output -> input
		SubSectionOutputToSubSectionInputModel outputToInput = new SubSectionOutputToSubSectionInputModel();
		outputToInput.setSubSectionOutput(output_input);
		outputToInput.setSubSectionInput(input);
		outputToInput.connect();

		// output -> extFlow
		SubSectionOutputToExternalFlowModel outputToExtFlow = new SubSectionOutputToExternalFlowModel();
		outputToExtFlow.setSubSectionOutput(output_extFlow);
		outputToExtFlow.setExternalFlow(extFlow);
		outputToExtFlow.connect();

		// section object -> extMo
		SubSectionObjectToExternalManagedObjectModel sectionObjectToExtMo = new SubSectionObjectToExternalManagedObjectModel();
		sectionObjectToExtMo.setSubSectionObject(object);
		sectionObjectToExtMo.setExternalManagedObject(extMo);
		sectionObjectToExtMo.connect();

		// section object -> mo
		SubSectionObjectToSectionManagedObjectModel sectionObjectToMo = new SubSectionObjectToSectionManagedObjectModel();
		sectionObjectToMo.setSubSectionObject(object);
		sectionObjectToMo.setSectionManagedObject(mo);
		sectionObjectToMo.connect();

		// functionObject -> extMo
		ManagedFunctionObjectToExternalManagedObjectModel functionObjectToExtMo = new ManagedFunctionObjectToExternalManagedObjectModel();
		functionObjectToExtMo.setManagedFunctionObject(functionObject);
		functionObjectToExtMo.setExternalManagedObject(extMo);
		functionObjectToExtMo.connect();

		// functionObject -> mo
		ManagedFunctionObjectToSectionManagedObjectModel functionObjectToMo = new ManagedFunctionObjectToSectionManagedObjectModel();
		functionObjectToMo.setManagedFunctionObject(functionObject);
		functionObjectToMo.setSectionManagedObject(mo);
		functionObjectToMo.connect();

		// functionFlow -> extFlow
		FunctionFlowToExternalFlowModel flowToExtFlow = new FunctionFlowToExternalFlowModel();
		flowToExtFlow.setFunctionFlow(functionFlow);
		flowToExtFlow.setExternalFlow(extFlow);
		flowToExtFlow.connect();

		// next -> extFlow
		FunctionToNextExternalFlowModel nextToExtFlow = new FunctionToNextExternalFlowModel();
		nextToExtFlow.setPreviousFunction(function);
		nextToExtFlow.setNextExternalFlow(extFlow);
		nextToExtFlow.connect();

		// functionFlow -> function
		FunctionFlowToFunctionModel flowToFunction = new FunctionFlowToFunctionModel();
		flowToFunction.setFunctionFlow(functionFlow);
		flowToFunction.setFunction(function);
		flowToFunction.connect();

		// functionEscalation -> function
		FunctionEscalationToFunctionModel escalationToFunction = new FunctionEscalationToFunctionModel();
		escalationToFunction.setEscalation(functionEscalation);
		escalationToFunction.setFunction(function);
		escalationToFunction.connect();

		// functionEscalation -> extFlow
		FunctionEscalationToExternalFlowModel escalationToExtFlow = new FunctionEscalationToExternalFlowModel();
		escalationToExtFlow.setFunctionEscalation(functionEscalation);
		escalationToExtFlow.setExternalFlow(extFlow);
		escalationToExtFlow.connect();

		// next -> function
		FunctionToNextFunctionModel nextToFunction = new FunctionToNextFunctionModel();
		nextToFunction.setPreviousFunction(function);
		nextToFunction.setNextFunction(function);
		nextToFunction.connect();

		// Record storing the section
		this.modelRepository.store(section, this.configurationItem);

		// Store the section
		this.replayMockObjects();
		this.sectionRepository.storeSection(section, this.configurationItem);
		this.verifyMockObjects();

		// Ensure the connections have links to enable retrieving
		assertEquals("mo - mos", "MANAGED_OBJECT_SOURCE", moToMos.getSectionManagedObjectSourceName());
		assertEquals("mos flow - extFlow", "FLOW", mosFlowToExtFlow.getExternalFlowName());
		assertEquals("mos flow - input (sub section)", "SUB_SECTION", mosFlowToInput.getSubSectionName());
		assertEquals("mos flow - input (input)", "INPUT", mosFlowToInput.getSubSectionInputName());
		assertEquals("mos flow - function", "FUNCTION", mosFlowToFunction.getFunctionName());
		assertEquals("dependency - extMo", "MO", dependencyToExtMo.getExternalManagedObjectName());
		assertEquals("dependency - mo", "MANAGED_OBJECT", dependencyToMo.getSectionManagedObjectName());
		assertEquals("output - input (sub section)", "SUB_SECTION", outputToInput.getSubSectionName());
		assertEquals("output - input (input)", "INPUT", outputToInput.getSubSectionInputName());
		assertEquals("output - extFlow", "FLOW", outputToExtFlow.getExternalFlowName());
		assertEquals("section object - extMo", "MO", sectionObjectToExtMo.getExternalManagedObjectName());
		assertEquals("section object - mo", "MANAGED_OBJECT", sectionObjectToMo.getSectionManagedObjectName());
		assertEquals("function object - extMo", "EXTERNAL_MANAGED_OBJECT",
				functionObjectToExtMo.getExternalManagedObjectName());
		assertEquals("function object - mo", "MANAGED_OBJECT", functionObjectToMo.getSectionManagedObjectName());
		assertEquals("function flow - extFlow", "EXTERNAL_FLOW", flowToExtFlow.getExternalFlowName());
		assertEquals("next - extFlow", "EXTERNAL_FLOW", nextToExtFlow.getExternalFlowName());
		assertEquals("flow - function", "FUNCTION", flowToFunction.getFunctionName());
		assertEquals("escalation - function", "FUNCTION", escalationToFunction.getFunctionName());
		assertEquals("escalation - extFlow", "EXTERNAL_FLOW", escalationToExtFlow.getExternalFlowName());
		assertEquals("next - function", "FUNCTION", nextToFunction.getNextFunctionName());
	}

}