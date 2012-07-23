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

package net.officefloor.plugin.objects;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.officefloor.autowire.AutoWire;
import net.officefloor.autowire.AutoWireApplication;
import net.officefloor.autowire.AutoWireObject;
import net.officefloor.autowire.AutoWireSupplier;
import net.officefloor.autowire.ManagedObjectSourceWirer;
import net.officefloor.autowire.ManagedObjectSourceWirerContext;
import net.officefloor.compile.impl.util.CompileUtil;
import net.officefloor.compile.properties.Property;
import net.officefloor.model.objects.AutoWireDependencyModel;
import net.officefloor.model.objects.AutoWireFlowModel;
import net.officefloor.model.objects.AutoWireManagedObjectModel;
import net.officefloor.model.objects.AutoWireModel;
import net.officefloor.model.objects.AutoWireObjectSourceModel;
import net.officefloor.model.objects.AutoWireObjectsModel;
import net.officefloor.model.objects.AutoWireObjectsRepository;
import net.officefloor.model.objects.AutoWireSupplierModel;
import net.officefloor.model.objects.AutoWireTeamModel;
import net.officefloor.model.objects.PropertyFileModel;
import net.officefloor.model.objects.PropertyModel;
import net.officefloor.model.objects.PropertySourceModel;
import net.officefloor.model.repository.ConfigurationItem;

/**
 * {@link AutoWireObjectsLoader} implementation.
 * 
 * @author Daniel Sagenschneider
 */
public class AutoWireObjectsLoaderImpl implements AutoWireObjectsLoader {

	/**
	 * {@link AutoWireObjectsRepository}.
	 */
	private final AutoWireObjectsRepository repository;

	/**
	 * Initiate.
	 * 
	 * @param repository
	 *            {@link AutoWireObjectsRepository}.
	 */
	public AutoWireObjectsLoaderImpl(AutoWireObjectsRepository repository) {
		this.repository = repository;
	}

	/*
	 * ======================= AutoWireObjectsLoader ===========================
	 */

	@Override
	public void loadAutoWireObjectsConfiguration(
			ConfigurationItem objectsConfiguration,
			AutoWireApplication application) throws Exception {

		// Load the objects model
		AutoWireObjectsModel objects = this.repository
				.retrieveAutoWireObjects(objectsConfiguration);

		// Configure the objects
		for (AutoWireObjectSourceModel objectSource : objects
				.getAutoWireObjectSources()) {

			// Load based on object source type
			if (objectSource instanceof AutoWireManagedObjectModel) {
				// Load the managed object
				this.loadAutoWireManagedObject(
						(AutoWireManagedObjectModel) objectSource, application);

			} else if (objectSource instanceof AutoWireSupplierModel) {
				// Load the supplier
				this.loadAutoWireSupplier((AutoWireSupplierModel) objectSource,
						application);

			} else {
				// Unknown object source
				throw new IllegalStateException(
						"Unknown object source configuration type "
								+ objectSource.getClass().getName());
			}
		}
	}

	/**
	 * Loads the {@link AutoWireManagedObjectModel}.
	 * 
	 * @param managedObject
	 *            {@link AutoWireManagedObjectModel}.
	 * @param application
	 *            {@link AutoWireApplication}.
	 * @throws IOException
	 *             If fails to load {@link Property}.
	 */
	private void loadAutoWireManagedObject(
			final AutoWireManagedObjectModel managedObject,
			AutoWireApplication application) throws IOException {

		// Obtain the managed object details
		String managedObjectSourceClassName = managedObject
				.getManagedObjectSourceClassName();
		long timeout = managedObject.getTimeout();

		// Obtain the auto-wiring
		List<AutoWire> autoWiring = new LinkedList<AutoWire>();
		String qualifier = managedObject.getQualifier();
		String type = managedObject.getType();
		if (!(CompileUtil.isBlank(type))) {
			// Short-cut auto-wire provided
			autoWiring.add(new AutoWire(qualifier, type));
		}
		for (AutoWireModel autoWire : managedObject.getAutoWiring()) {
			autoWiring.add(new AutoWire(autoWire.getQualifier(), autoWire
					.getType()));
		}

		// Create the wirer
		ManagedObjectSourceWirer wirer = new ManagedObjectSourceWirer() {
			@Override
			public void wire(ManagedObjectSourceWirerContext context) {

				// Configure the flows
				for (AutoWireFlowModel flow : managedObject.getFlows()) {
					context.mapFlow(flow.getName(), flow.getSection(),
							flow.getInput());
				}

				// Configure the teams
				for (AutoWireTeamModel team : managedObject.getTeams()) {
					context.mapTeam(team.getName(),
							new AutoWire(team.getQualifier(), team.getType()));
				}

				// Configure the dependencies
				for (AutoWireDependencyModel dependency : managedObject
						.getDependencies()) {
					context.mapDependency(dependency.getName(), new AutoWire(
							dependency.getQualifier(), dependency.getType()));
				}
			}
		};

		// Add the managed object
		AutoWireObject object = application.addManagedObject(
				managedObjectSourceClassName, wirer,
				autoWiring.toArray(new AutoWire[autoWiring.size()]));

		// Provide timeout (if provided)
		if (timeout > 0) {
			object.setTimeout(timeout);
		}

		// Load the properties
		for (PropertySourceModel propertySource : managedObject
				.getPropertySources()) {

			// Load based on property source type
			if (propertySource instanceof PropertyModel) {
				// Load the property
				PropertyModel property = (PropertyModel) propertySource;
				object.addProperty(property.getName(), property.getValue());

			} else if (propertySource instanceof PropertyFileModel) {
				// Load properties from file
				PropertyFileModel propertyFile = (PropertyFileModel) propertySource;
				object.loadProperties(propertyFile.getPath());

			} else {
				// Unknown property source
				throw new IllegalStateException("Unknown property source type "
						+ propertySource.getClass().getName());
			}
		}
	}

	/**
	 * Loads the {@link AutoWireSupplierModel}.
	 * 
	 * @param supplier
	 *            {@link AutoWireSupplierModel}.
	 * @param application
	 *            {@link AutoWireApplication}.
	 * @throws IOException
	 *             If failure loading {@link Property}.
	 */
	private void loadAutoWireSupplier(AutoWireSupplierModel supplier,
			AutoWireApplication application) throws IOException {

		// Obtain the supplier details
		String supplierSourceClassName = supplier.getSupplierSourceClassName();

		// Add the supplier
		AutoWireSupplier autoWireSupplier = application
				.addSupplier(supplierSourceClassName);

		// Load the properties
		for (PropertySourceModel propertySource : supplier.getPropertySources()) {

			// Load based on property source type
			if (propertySource instanceof PropertyModel) {
				// Load the property
				PropertyModel property = (PropertyModel) propertySource;
				autoWireSupplier.addProperty(property.getName(),
						property.getValue());

			} else if (propertySource instanceof PropertyFileModel) {
				// Load properties from file
				PropertyFileModel propertyFile = (PropertyFileModel) propertySource;
				autoWireSupplier.loadProperties(propertyFile.getPath());

			} else {
				// Unknown property source
				throw new IllegalStateException("Unknown property source type "
						+ propertySource.getClass().getName());
			}
		}
	}

}