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
package net.officefloor.frame.test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import net.officefloor.frame.api.OfficeFrame;
import net.officefloor.frame.api.build.AdministratorBuilder;
import net.officefloor.frame.api.build.BuildException;
import net.officefloor.frame.api.build.ManagedObjectBuilder;
import net.officefloor.frame.api.build.OfficeBuilder;
import net.officefloor.frame.api.build.OfficeFloorBuilder;
import net.officefloor.frame.api.build.OfficeScope;
import net.officefloor.frame.api.build.TaskBuilder;
import net.officefloor.frame.api.build.TaskFactory;
import net.officefloor.frame.api.build.WorkBuilder;
import net.officefloor.frame.api.build.WorkFactory;
import net.officefloor.frame.api.execute.EscalationHandler;
import net.officefloor.frame.api.execute.Task;
import net.officefloor.frame.api.execute.Work;
import net.officefloor.frame.api.manage.Office;
import net.officefloor.frame.api.manage.OfficeFloor;
import net.officefloor.frame.api.manage.WorkManager;
import net.officefloor.frame.impl.OfficeFrameImpl;
import net.officefloor.frame.spi.administration.Administrator;
import net.officefloor.frame.spi.administration.Duty;
import net.officefloor.frame.spi.administration.source.AdministratorSource;
import net.officefloor.frame.spi.administration.source.AdministratorSourceMetaData;
import net.officefloor.frame.spi.managedobject.ManagedObject;
import net.officefloor.frame.spi.managedobject.source.ManagedObjectSource;
import net.officefloor.frame.spi.managedobject.source.ManagedObjectSourceMetaData;
import net.officefloor.frame.spi.team.Team;
import net.officefloor.frame.test.ReflectiveWorkBuilder.ReflectiveTaskBuilder;

/**
 * Abstract {@link TestCase} for construction testing of an Office.
 * 
 * @author Daniel
 */
public abstract class AbstractOfficeConstructTestCase extends
		OfficeFrameTestCase implements EscalationHandler {

	/**
	 * {@link OfficeFloorBuilder}.
	 */
	private OfficeFloorBuilder officeFloorBuilder;

	/**
	 * {@link OfficeBuilder}.
	 */
	private OfficeBuilder officeBuilder;

	/**
	 * {@link WorkBuilder}.
	 */
	private WorkBuilder<?> workBuilder;

	/**
	 * List of method names in order they are invoked by the
	 * {@link ReflectiveTaskBuilder} instances for the test.
	 */
	private List<String> reflectiveTaskInvokedMethods = new LinkedList<String>();

	/**
	 * {@link ParentEscalationProcedure}.
	 */
	protected volatile Throwable exception = null;

	/**
	 * {@link OfficeFloor}.
	 */
	private OfficeFloor officeFloor = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		// Clear the office
		((OfficeFrameImpl) OfficeFrameImpl.getInstance()).clearOfficeFloors();

		// Initiate for constructing office
		this.officeFloorBuilder = OfficeFrame.getInstance().getBuilderFactory()
				.createOfficeFloorBuilder();
		this.officeBuilder = OfficeFrame.getInstance().getBuilderFactory()
				.createOfficeBuilder();

		// Initiate to receive top level escalations to report back in tests
		this.officeBuilder.setOfficeEscalationHandler(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.officefloor.frame.api.execute.EscalationHandler#handleEscalation(
	 * java.lang.Throwable)
	 */
	@Override
	public synchronized void handleEscalation(Throwable escalation)
			throws Throwable {
		// Record exception to be thrown later
		this.exception = escalation;
	}

	/**
	 * <p>
	 * Validates that no top level escalation occurred.
	 * <p>
	 * This method will clear the escalation on exit.
	 */
	public synchronized void validateNoTopLevelEscalation() throws Throwable {
		try {
			if (this.exception != null) {
				throw this.exception;
			}
		} finally {
			// Exception thrown, so have it cleared
			this.exception = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected synchronized void tearDown() throws Exception {
		try {
			// Return if no failure
			if (this.exception == null) {
				return;
			}

			// Propagate failure
			if (this.exception instanceof Exception) {
				throw (Exception) this.exception;
			} else if (this.exception instanceof Error) {
				throw (Error) this.exception;
			} else {
				StringWriter buffer = new StringWriter();
				this.exception.printStackTrace(new PrintWriter(buffer));
				fail("Unknown failure " + this.exception.getClass().getName()
						+ ": " + buffer.toString());
			}
		} finally {
			super.tearDown();
		}
	}

	/**
	 * Obtains the {@link OfficeFloorBuilder}.
	 * 
	 * @return {@link OfficeFloorBuilder}.
	 */
	protected OfficeFloorBuilder getOfficeFloorBuilder() {
		return this.officeFloorBuilder;
	}

	/**
	 * Obtains the {@link OfficeBuilder}.
	 * 
	 * @return {@link OfficeBuilder}.
	 */
	protected OfficeBuilder getOfficeBuilder() {
		return this.officeBuilder;
	}

	/**
	 * Facade method to register a
	 * {@link net.officefloor.frame.api.execute.Work}.
	 * 
	 * @return {@link WorkBuilder} for the
	 *         {@link net.officefloor.frame.api.execute.Work}.
	 */
	protected <W extends Work> WorkBuilder<W> constructWork(String workName,
			WorkFactory<W> workFactory, Class<W> typeOfWork,
			String initialTaskName) throws BuildException {

		// Create the Work Builder
		WorkBuilder<W> workBuilder = OfficeFrame.getInstance()
				.getBuilderFactory().createWorkBuilder(typeOfWork);

		// Construct the work
		workBuilder.setWorkFactory(workFactory);
		workBuilder.setInitialTask(initialTaskName);

		// Register the work
		this.officeBuilder.addWork(workName, workBuilder);

		// Make current work builder
		this.workBuilder = workBuilder;

		// Return the work builder
		return workBuilder;
	}

	/**
	 * Facade method to register a
	 * {@link net.officefloor.frame.api.execute.Work}.
	 * 
	 * @return {@link WorkBuilder} for the
	 *         {@link net.officefloor.frame.api.execute.Work}.
	 */
	@SuppressWarnings("unchecked")
	protected <W extends Work> WorkBuilder<W> constructWork(String workName,
			final W work, String initialTaskName) throws BuildException {

		// Obtain the type of work
		Class typeOfWork = work.getClass();

		// Create the Work Factory
		WorkFactory<W> workFactory = new WorkFactory<W>() {
			public W createWork() {
				return work;
			}
		};

		// Return the constructed work
		return this.constructWork(workName, workFactory, typeOfWork,
				initialTaskName);
	}

	/**
	 * Constructs the {@link ReflectiveWorkBuilder}.
	 * 
	 * @param workObject
	 *            Work object.
	 * @param workName
	 *            Work name.
	 * @param initialTaskName
	 *            Initial task name.
	 * @return {@link ReflectiveWorkBuilder}.
	 * @throws BuildException
	 *             If fails to build.
	 */
	protected ReflectiveWorkBuilder constructWork(Object workObject,
			String workName, String initialTaskName) throws BuildException {
		// Return the created work builder
		return new ReflectiveWorkBuilder(this, workName, workObject,
				this.officeBuilder, initialTaskName);
	}

	/**
	 * Invoked by the {@link ReflectiveTaskBuilder} when it executes the method.
	 * 
	 * @param methodName
	 *            Name of method being invoked.
	 */
	protected synchronized void recordReflectiveTaskMethodInvoked(
			String methodName) {
		this.reflectiveTaskInvokedMethods.add(methodName);
	}

	/**
	 * Validates the order the {@link ReflectiveTaskBuilder} invoked the
	 * methods.
	 * 
	 * @param methodNames
	 *            Order that the reflective methods should be invoked.
	 */
	protected synchronized void validateReflectiveMethodOrder(
			String... methodNames) {

		// Create expected method calls
		StringBuilder actualMethods = new StringBuilder();
		for (String methodName : methodNames) {
			actualMethods.append(methodName.trim() + " ");
		}

		// Create the actual method calls
		StringBuilder expectedMethods = new StringBuilder();
		for (String methodName : this.reflectiveTaskInvokedMethods) {
			expectedMethods.append(methodName.trim() + " ");
		}

		// Validate appropriate methods called
		assertEquals("Incorrect methods invoked [ " + actualMethods.toString()
				+ "]", actualMethods.toString(), expectedMethods.toString());
	}

	/**
	 * Facade method to register a
	 * {@link net.officefloor.frame.api.execute.Task}.
	 * 
	 * @return {@link TaskBuilder} for the
	 *         {@link net.officefloor.frame.api.execute.Task}.
	 */
	@SuppressWarnings("unchecked")
	protected <P extends Object, W extends Work, M extends Enum<M>, F extends Enum<F>> TaskBuilder<P, W, M, F> constructTask(
			String taskName, Class parameterType,
			TaskFactory<P, W, M, F> taskFactory, String teamName,
			String moName, String nextTaskName) throws BuildException {

		// Create the Task Builder
		TaskBuilder taskBuilder = this.workBuilder.addTask(taskName,
				parameterType);

		// Construct the task
		taskBuilder.setTaskFactory(taskFactory);
		taskBuilder.setTeam(teamName);
		if (nextTaskName != null) {
			taskBuilder.setNextTaskInFlow(nextTaskName);
		}
		if (moName != null) {
			taskBuilder.linkManagedObject(0, moName);
		}

		// Return the task builder
		return taskBuilder;
	}

	/**
	 * Facade method to register a
	 * {@link net.officefloor.frame.api.execute.Task}.
	 * 
	 * @return {@link TaskBuilder} for the
	 *         {@link net.officefloor.frame.api.execute.Task}.
	 */
	@SuppressWarnings("unchecked")
	protected <P extends Object, W extends Work, M extends Enum<M>, F extends Enum<F>> TaskBuilder constructTask(
			String taskName, Class parameterType, final Task<P, W, M, F> task,
			String teamName, String nextTaskName) throws BuildException {

		// Create the Task Factory
		TaskFactory<P, W, M, F> taskFactory = new TaskFactory<P, W, M, F>() {

			public Task<P, W, M, F> createTask(W work) {
				return task;
			}
		};

		// Construct and return the Task
		return this.constructTask(taskName, parameterType, taskFactory,
				teamName, null, nextTaskName);
	}

	/**
	 * Facade method to register a {@link ManagedObject}.
	 */
	protected <D extends Enum<D>, H extends Enum<H>, MS extends ManagedObjectSource<D, H>> ManagedObjectBuilder<H> constructManagedObject(
			String managedObjectName, Class<MS> managedObjectSourceClass,
			String managingOffice) throws BuildException {

		// Create the Managed Object Builder
		ManagedObjectBuilder<H> managedObjectBuilder = OfficeFrame
				.getInstance().getBuilderFactory().createManagedObjectBuilder(
						managedObjectSourceClass);

		// Flag managing office
		managedObjectBuilder.setManagingOffice(managingOffice);

		// Obtain office floor id for managed object
		String managedObjectId = "of-" + managedObjectName;

		// Register the Managed Object with the current Office Floor
		this.officeFloorBuilder.addManagedObject(managedObjectId,
				managedObjectBuilder);

		// Link into the Office
		this.officeBuilder.registerManagedObject(managedObjectName,
				managedObjectId);

		// Return the Managed Object Builder
		return managedObjectBuilder;
	}

	/**
	 * Facade method to register a {@link ManagedObject}.
	 */
	protected <D extends Enum<D>, H extends Enum<H>> ManagedObjectBuilder<H> constructManagedObject(
			String managedObjectName,
			ManagedObjectSourceMetaData<D, H> metaData,
			ManagedObject managedObject, String managingOffice)
			throws BuildException {

		// Bind Managed Object
		ManagedObjectBuilder<H> managedObjectBuilder = MockManagedObjectSource
				.bindManagedObject(OfficeFrame.getInstance()
						.getBuilderFactory(), managedObjectName, managedObject,
						metaData);

		// Flag managing office
		managedObjectBuilder.setManagingOffice(managingOffice);

		// Obtain office floor id for managed object
		String managedObjectId = "of-" + managedObjectName;

		// Register the Managed Object with the current Office
		this.officeFloorBuilder.addManagedObject(managedObjectId,
				managedObjectBuilder);

		// Link into the Office
		this.officeBuilder.registerManagedObject(managedObjectName,
				managedObjectId);

		// Return the builder
		return managedObjectBuilder;
	}

	/**
	 * Facade method to register a
	 * {@link net.officefloor.frame.spi.managedobject.ManagedObject}.
	 */
	@SuppressWarnings("unchecked")
	protected void constructManagedObject(String managedObjectName,
			ManagedObject managedObject, String managingOffice)
			throws BuildException {

		// Create the mock Managed Object Source meta-data
		ManagedObjectSourceMetaData<?, ?> metaData = new MockManagedObjectSourceMetaData(
				managedObject);

		// Register the Managed Object
		this.constructManagedObject(managedObjectName, metaData, managedObject,
				managingOffice);
	}

	/**
	 * Facade method to register a
	 * {@link net.officefloor.frame.spi.managedobject.ManagedObject}.
	 */
	protected void constructManagedObject(final Object object,
			String managedObjectName, String managingOffice)
			throws BuildException {

		// Create the wrapping Managed Object
		ManagedObject managedObject = new ManagedObject() {
			public Object getObject() {
				return object;
			}
		};

		// Register the managed object
		this.constructManagedObject(managedObjectName, managedObject,
				managingOffice);
	}

	/**
	 * Facade method to create a {@link Team}.
	 */
	protected void constructTeam(String teamName, Team team)
			throws BuildException {

		// Obtain the office floor Id for the team
		String teamId = "of-" + teamName;

		// Add the team
		this.officeFloorBuilder.addTeam(teamId, team);

		// Link into the Office
		this.officeBuilder.registerTeam(teamName, teamId);
	}

	/**
	 * Facade method to create a {@link Administrator}.
	 * 
	 * @param adminName
	 *            Name of the {@link Administrator}.
	 * @param adminOne
	 *            {@link Administrator}.
	 * @param adminOneMetaData
	 *            Meta-data for the {@link AdministratorSourceMetaData}.
	 * @param adminScope
	 *            {@link OfficeScope} for the {@link Administrator}.
	 * @param teamName
	 *            Name of {@link Team} for {@link Administrator} {@link Duty}
	 *            instances.
	 * @return {@link AdministratorBuilder}.
	 */
	protected <I extends Object, A extends Enum<A>> AdministratorBuilder<A> constructAdministrator(
			String adminName, Administrator<I, A> adminOne,
			AdministratorSourceMetaData<I, A> adminOneMetaData,
			OfficeScope adminScope, String teamName) throws BuildException {

		// Bind the Administrator
		AdministratorBuilder<A> adminBuilder = MockAdministratorSource
				.bindAdministrator(OfficeFrame.getInstance()
						.getBuilderFactory(), adminName, adminOne,
						adminOneMetaData);

		// Configure the administrator
		adminBuilder.setAdministratorScope(adminScope);
		adminBuilder.setTeam(teamName);

		// Register the Administrator with the current Office
		this.officeBuilder.addAdministrator(adminName, adminBuilder);

		// Return the administrator builder
		return adminBuilder;
	}

	/**
	 * Facade method to construct an {@link Administrator}.
	 * 
	 * @param adminName
	 *            Name of the {@link Administrator}.
	 * @param adminSource
	 *            {@link AdministratorSource} {@link Class}.
	 * @param adminScope
	 *            {@link OfficeScope} of the {@link Administrator}.
	 * @param teamName
	 *            Name of {@link Team} for {@link Administrator} {@link Duty}
	 *            instances.
	 * @return {@link AdministratorBuilder}.
	 */
	protected <I extends Object, A extends Enum<A>, AS extends AdministratorSource<I, A>> AdministratorBuilder<A> constructAdministrator(
			String adminName, Class<AS> adminSource, OfficeScope adminScope,
			String teamName) throws BuildException {

		// Create the Administrator Builder
		AdministratorBuilder<A> adminBuilder = (AdministratorBuilder<A>) OfficeFrame
				.getInstance().getBuilderFactory().createAdministratorBuilder(
						adminSource);

		// Configure the administrator
		adminBuilder.setAdministratorScope(adminScope);
		adminBuilder.setTeam(teamName);

		// Register the Administrator with the current Office
		this.officeBuilder.addAdministrator(adminName, adminBuilder);

		// Return the administrator builder
		return adminBuilder;
	}

	/**
	 * Facade method to create the
	 * {@link net.officefloor.frame.api.manage.OfficeFloor}.
	 * 
	 * @param officeName
	 *            Name of the office.
	 * @return {@link net.officefloor.frame.api.manage.OfficeFloor}.
	 */
	protected OfficeFloor constructOfficeFloor(String officeName)
			throws Exception {

		// Construct the Office
		this.officeFloorBuilder.addOffice(officeName, this.officeBuilder);

		// Construct the Office Floor
		this.officeFloor = OfficeFrame.getInstance().registerOfficeFloor(
				"of-" + officeName, this.officeFloorBuilder);

		// Initiate for constructing another office
		this.officeFloorBuilder = OfficeFrame.getInstance().getBuilderFactory()
				.createOfficeFloorBuilder();
		this.officeBuilder = OfficeFrame.getInstance().getBuilderFactory()
				.createOfficeBuilder();

		// Return the Office Floor
		return this.officeFloor;
	}

	/**
	 * Facade method to invoke work of an office. It will create the office
	 * floor if necessary.
	 * 
	 * @param officeName
	 *            Name of the office.
	 * @param workName
	 *            Name of the work to invoke.
	 * @param parameter
	 *            Parameter.
	 * @throws Exception
	 *             If fails to construct office or work invocation failure.
	 */
	protected void invokeWork(String officeName, String workName,
			Object parameter) throws Exception {

		// Determine if required to construct work
		if (this.officeFloor == null) {
			// Construct the office floor
			this.officeFloor = this.constructOfficeFloor(officeName);

			// Open the office floor
			this.officeFloor.openOfficeFloor();
		}

		// Invoke the work
		Office office = this.officeFloor.getOffice(officeName);
		WorkManager workManager = office.getWorkManager(workName);
		workManager.invokeWork(parameter);
	}

}
