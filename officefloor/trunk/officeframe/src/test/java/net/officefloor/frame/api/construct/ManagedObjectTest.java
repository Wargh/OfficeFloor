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
package net.officefloor.frame.api.construct;

import net.officefloor.frame.api.OfficeFrame;
import net.officefloor.frame.api.build.HandlerBuilder;
import net.officefloor.frame.api.build.HandlerFactory;
import net.officefloor.frame.api.build.ManagedObjectBuilder;
import net.officefloor.frame.api.build.ManagedObjectHandlerBuilder;
import net.officefloor.frame.api.build.None;
import net.officefloor.frame.api.execute.Handler;
import net.officefloor.frame.api.execute.HandlerContext;
import net.officefloor.frame.api.execute.Task;
import net.officefloor.frame.api.execute.Work;
import net.officefloor.frame.api.manage.Office;
import net.officefloor.frame.api.manage.OfficeFloor;
import net.officefloor.frame.api.manage.WorkManager;
import net.officefloor.frame.impl.spi.team.PassiveTeam;
import net.officefloor.frame.internal.structure.ProcessState;
import net.officefloor.frame.spi.managedobject.ManagedObject;
import net.officefloor.frame.spi.managedobject.source.ManagedObjectSource;
import net.officefloor.frame.spi.managedobject.source.impl.AbstractManagedObjectSource;
import net.officefloor.frame.test.AbstractOfficeConstructTestCase;
import net.officefloor.frame.test.ReflectiveWorkBuilder;
import net.officefloor.frame.test.ReflectiveWorkBuilder.ReflectiveTaskBuilder;

/**
 * Tests construction scenarios of a {@link ManagedObject}.
 * 
 * @author Daniel
 */
public class ManagedObjectTest extends AbstractOfficeConstructTestCase {

	/**
	 * {@link ManagedObjectSource}.
	 */
	private static TestManagedObjectSource managedObjectSource = null;

	/**
	 * {@link OfficeFloor}.
	 */
	private OfficeFloor officeFloor;

	/**
	 * {@link TestWork}.
	 */
	private TestWork work;

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.officefloor.frame.test.AbstractOfficeConstructTestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		// Initiate for construction
		super.setUp();

		// Reset static state between tests
		managedObjectSource = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.officefloor.frame.test.AbstractOfficeConstructTestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {

		// Close the office floor if created
		if (this.officeFloor != null) {
			this.officeFloor.closeOfficeFloor();
		}

		// Clear construction
		super.tearDown();
	}

	/**
	 * Ensures construction of a {@link ManagedObject} that invokes a
	 * {@link Task} of the {@link Office} but is not used by the {@link Office}.
	 */
	public void testManagedObjectOutsideOffice() throws Throwable {

		// Initiate so only available outside office
		this.initiateOfficeFloor(true, false, null);

		// Invoke the managed object externally to execute the task
		Object parameter = new Object();
		managedObjectSource.triggerByExternalEvent(parameter);

		// Ensure the task invoked
		this.validateTaskInvoked(parameter, null);
	}

	/**
	 * Ensures construction of a {@link ProcessState} bound
	 * {@link ManagedObject} that is a dependency of a {@link Task} within the
	 * {@link Office}.
	 */
	public void testProcessManagedObjectInsideOffice() throws Throwable {

		// Initiate so only available outside office
		this.initiateOfficeFloor(false, true, ManagedObjectScope.PROCESS);

		// Invoke the task
		Object parameter = new Object();
		WorkManager workManager = this.officeFloor.getOffice("OFFICE")
				.getWorkManager("WORK");
		workManager.invokeWork(parameter);

		// Ensure the task invoked
		this.validateTaskInvoked(parameter, managedObjectSource);
	}

	/**
	 * Ensures construction of a {@link Work} bound {@link ManagedObject} that
	 * is a dependency of a {@link Task} within the {@link Office}.
	 */
	public void testWorkManagedObjectInsideOffice() throws Throwable {

		// Initiate so only available outside office
		this.initiateOfficeFloor(false, true, ManagedObjectScope.WORK);

		// Invoke the task
		Object parameter = new Object();
		WorkManager workManager = this.officeFloor.getOffice("OFFICE")
				.getWorkManager("WORK");
		workManager.invokeWork(parameter);

		// Ensure the task invoked
		this.validateTaskInvoked(parameter, managedObjectSource);
	}

	/**
	 * Ensures construction of a {@link ProcessState} bound
	 * {@link ManagedObject} that both:
	 * <ol>
	 * <li>triggers a {@link Task} in the {@link Office}, and</li>
	 * <li>has a {@link Task} dependent on it.</li>
	 * </ol>
	 */
	public void testProcessManagedObjectOutsideAndInsideOffice()
			throws Throwable {

		// Initiate so available both outside and inside
		this.initiateOfficeFloor(true, true, ManagedObjectScope.PROCESS);

		Object parameter = new Object();

		// Ensure can externally trigger functionality
		managedObjectSource.triggerByExternalEvent(parameter);
		this.validateTaskInvoked(parameter, null);

		// Reset for invoking task
		this.resetTask();

		// Ensure can invoke task
		WorkManager workManager = this.officeFloor.getOffice("OFFICE")
				.getWorkManager("WORK");
		workManager.invokeWork(parameter);
		this.validateTaskInvoked(parameter, managedObjectSource);
	}

	/**
	 * Ensures construction of a {@link Work} bound {@link ManagedObject} that
	 * both:
	 * <ol>
	 * <li>triggers a {@link Task} in the {@link Office}, and</li>
	 * <li>has a {@link Task} dependent on it.</li>
	 * </ol>
	 */
	public void testWorkManagedObjectOutsideAndInsideOffice() throws Throwable {

		// Initiate so available both outside and inside
		this.initiateOfficeFloor(true, true, ManagedObjectScope.WORK);

		Object parameter = new Object();

		// Ensure can externally trigger functionality
		managedObjectSource.triggerByExternalEvent(parameter);
		this.validateTaskInvoked(parameter, null);

		// Reset for invoking task
		this.resetTask();

		// Ensure can invoke task
		WorkManager workManager = this.officeFloor.getOffice("OFFICE")
				.getWorkManager("WORK");
		workManager.invokeWork(parameter);
		this.validateTaskInvoked(parameter, managedObjectSource);
	}

	/**
	 * Scope for the {@link ManagedObject}.
	 */
	private enum ManagedObjectScope {
		WORK, PROCESS
	}

	/**
	 * Resets the {@link Task} to test invoking again.
	 */
	private void resetTask() {
		this.work.isTaskInvoked = false;
		this.work.parameter = null;
		this.work.managedObject = null;
	}

	/**
	 * Validates the {@link Task} was invoked.
	 * 
	 * @param parameter
	 *            Expected parameter.
	 * @param managedObject
	 *            Expected {@link ManagedObject}.
	 * @throws Throwable
	 *             If failure invoking {@link Task}.
	 */
	private void validateTaskInvoked(Object parameter,
			ManagedObject managedObject) throws Throwable {

		// Ensure no escalation failures invoking task
		this.validateNoTopLevelEscalation();

		// Validates the task was invoked
		assertTrue("Task should be executed", this.work.isTaskInvoked);
		assertEquals("Incorrect parameter to task", parameter,
				this.work.parameter);
		assertEquals("Incorrect managed object", managedObject,
				this.work.managedObject);
	}

	/**
	 * Initiates the {@link OfficeFloor} with the {@link ManagedObject}
	 * available as per input flags.
	 * 
	 * @param isManagedObjectOutside
	 *            Flag indicating the {@link ManagedObject} is handling external
	 *            events.
	 * @param isManagedObjectInside
	 *            Flag indicating a {@link Task} is dependent on the
	 *            {@link ManagedObject}.
	 * @param scope
	 *            {@link ManagedObjectScope} when inside {@link Office}.
	 * @throws Exception
	 *             If fails to initialise the {@link OfficeFloor}.
	 */
	private void initiateOfficeFloor(boolean isManagedObjectOutside,
			boolean isManagedObjectInside, ManagedObjectScope scope)
			throws Exception {

		final String EXTERNAL_EVENT_TASK = "externalEvent";
		final String INVOKED_TASK = "invokedTask";

		// Create and register the managed object source
		ManagedObjectBuilder<HandlerKey> managedObjectBuilder = OfficeFrame
				.getInstance().getBuilderFactory().createManagedObjectBuilder(
						TestManagedObjectSource.class);
		managedObjectBuilder.setManagingOffice("OFFICE");
		this.getOfficeFloorBuilder().addManagedObject("MO",
				managedObjectBuilder);

		// Only provide handler if outside
		TestManagedObjectSource.setLoadHandler(isManagedObjectOutside);
		if (isManagedObjectOutside) {
			ManagedObjectHandlerBuilder<HandlerKey> moHandlerBuilder = managedObjectBuilder
					.getManagedObjectHandlerBuilder(HandlerKey.class);
			HandlerBuilder<HandlerProcess> handlerBuilder = moHandlerBuilder
					.registerHandler(HandlerKey.HANDLER, HandlerProcess.class);
			handlerBuilder.linkProcess(HandlerProcess.TASK, "WORK",
					EXTERNAL_EVENT_TASK);
		}

		// Create and register the work
		this.work = new TestWork();
		ReflectiveWorkBuilder workBuilder = this.constructWork(this.work,
				"WORK", (isManagedObjectInside ? INVOKED_TASK : null));
		if (isManagedObjectOutside) {
			// Provide the externally executed task from managed object
			ReflectiveTaskBuilder taskBuilder = workBuilder.buildTask(
					EXTERNAL_EVENT_TASK, "TEAM");
			taskBuilder.buildParameter();
		}
		if (isManagedObjectInside) {
			// Provide the invoked task dependent on process managed object
			ReflectiveTaskBuilder taskBuilder = workBuilder.buildTask(
					INVOKED_TASK, "TEAM");
			taskBuilder.buildParameter();

			// Register managed object to task based on scope
			switch (scope) {
			case PROCESS:
				// Register as process managed object
				taskBuilder.buildObject("DEPENDENCY", "MO_LINK");

				// Register the process managed object within the office
				this.getOfficeBuilder().addProcessManagedObject("MO_LINK",
						"OFFICE_MO");
				this.getOfficeBuilder()
						.registerManagedObject("OFFICE_MO", "MO");
				break;
			case WORK:
				// Register as work bound managed object
				taskBuilder.buildObject("MO");

				// Register the managed object within the office
				this.getOfficeBuilder().registerManagedObject("MO", "MO");
				break;
			default:
				fail("Unknown managed object scope " + scope);
			}
		}
		this.constructTeam("TEAM", new PassiveTeam());

		// Construct and open the office floor
		this.officeFloor = this.constructOfficeFloor("OFFICE");
		this.officeFloor.openOfficeFloor();

		// Ensure the managed object source and handler created
		assertNotNull("Managed Object Source not created", managedObjectSource);
		if (isManagedObjectOutside) {
			assertNotNull("Handler not created", managedObjectSource
					.getHandler());
		} else {
			assertNull("Handler should not be created", managedObjectSource
					.getHandler());
		}
	}

	/**
	 * Test {@link ManagedObjectSource}.
	 */
	public static class TestManagedObjectSource extends
			AbstractManagedObjectSource<None, HandlerKey> implements
			ManagedObject {

		/**
		 * Flag indicating to load the {@link Handler}.
		 */
		private static boolean isLoadHandler = true;

		/**
		 * Flags whether to load the {@link Handler}.
		 * 
		 * @param isLoadHandler
		 *            <code>true</code> to load the {@link Handler}.
		 */
		public static void setLoadHandler(boolean isLoadHandler) {
			TestManagedObjectSource.isLoadHandler = isLoadHandler;
		}

		/**
		 * {@link Handler}.
		 */
		private TestHandler handler;

		/**
		 * Initiate.
		 */
		public TestManagedObjectSource() {
			// Specify managed object source
			ManagedObjectTest.managedObjectSource = this;
		}

		/**
		 * Obtains the {@link TestHandler}.
		 * 
		 * @return {@link TestHandler}.
		 */
		public TestHandler getHandler() {
			return this.handler;
		}

		/**
		 * {@link ManagedObjectSource} has an external event that triggers
		 * functionality to handle it.
		 * 
		 * @param parameter
		 *            Parameter providing detail of the event to be passed to
		 *            the initial {@link Task}.
		 */
		public void triggerByExternalEvent(Object parameter) {
			HandlerContext<HandlerProcess> handlerContext = this.handler
					.getHandlerContext();
			handlerContext.invokeProcess(HandlerProcess.TASK, parameter, this);
		}

		/*
		 * ================ ManagedObjectSource ======================
		 */

		/*
		 * (non-Javadoc)
		 * 
		 * @seenet.officefloor.frame.spi.managedobject.source.impl.
		 * AbstractAsyncManagedObjectSource
		 * #loadSpecification(net.officefloor.frame
		 * .spi.managedobject.source.impl
		 * .AbstractAsyncManagedObjectSource.SpecificationContext)
		 */
		@Override
		protected void loadSpecification(SpecificationContext context) {
			// No requirements
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seenet.officefloor.frame.spi.managedobject.source.impl.
		 * AbstractAsyncManagedObjectSource
		 * #loadMetaData(net.officefloor.frame.spi
		 * .managedobject.source.impl.AbstractAsyncManagedObjectSource
		 * .MetaDataContext)
		 */
		@Override
		protected void loadMetaData(MetaDataContext<None, HandlerKey> context)
				throws Exception {
			if (isLoadHandler) {
				// Load the handlers
				HandlerLoader<HandlerKey> handlerLoader = context
						.getHandlerLoader(HandlerKey.class);
				handlerLoader.mapHandlerType(HandlerKey.HANDLER, Handler.class);

				// Provide the handler
				ManagedObjectHandlerBuilder<HandlerKey> moHandlerBuilder = context
						.getManagedObjectSourceContext().getHandlerBuilder(
								HandlerKey.class);
				HandlerBuilder<HandlerProcess> handlerBuilder = moHandlerBuilder
						.registerHandler(HandlerKey.HANDLER,
								HandlerProcess.class);
				handlerBuilder.setHandlerFactory(new TestHandler());
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seenet.officefloor.frame.spi.managedobject.source.impl.
		 * AbstractAsyncManagedObjectSource
		 * #start(net.officefloor.frame.spi.managedobject
		 * .source.impl.AbstractAsyncManagedObjectSource.StartContext)
		 */
		@Override
		protected void start(StartContext<HandlerKey> startContext)
				throws Exception {
			if (isLoadHandler) {
				// Obtain the handler
				this.handler = (TestHandler) startContext.getContext(
						HandlerKey.class).getHandler(HandlerKey.HANDLER);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seenet.officefloor.frame.spi.managedobject.source.impl.
		 * AbstractManagedObjectSource#getManagedObject()
		 */
		@Override
		protected ManagedObject getManagedObject() throws Throwable {
			return this;
		}

		/*
		 * ================ ManagedObject ======================
		 */

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * net.officefloor.frame.spi.managedobject.ManagedObject#getObject()
		 */
		@Override
		public Object getObject() throws Exception {
			return this;
		}
	}

	/**
	 * Test {@link Handler} keys.
	 */
	public enum HandlerKey {
		HANDLER
	}

	/**
	 * Test {@link Handler} flows.
	 */
	public enum HandlerProcess {
		TASK
	}

	/**
	 * Test {@link Handler}.
	 */
	private static class TestHandler implements HandlerFactory<HandlerProcess>,
			Handler<HandlerProcess> {

		/**
		 * {@link HandlerContext}.
		 */
		private HandlerContext<HandlerProcess> context;

		/**
		 * Obtains the {@link HandlerContext}.
		 * 
		 * @return {@link HandlerContext}.
		 */
		public HandlerContext<HandlerProcess> getHandlerContext() {
			return this.context;
		}

		/*
		 * =============== HandlerFactory ==================
		 */

		/*
		 * (non-Javadoc)
		 * 
		 * @see net.officefloor.frame.api.build.HandlerFactory#createHandler()
		 */
		@Override
		public Handler<HandlerProcess> createHandler() {
			return this;
		}

		/*
		 * =============== Handler ==================
		 */

		/*
		 * (non-Javadoc)
		 * 
		 * @seenet.officefloor.frame.api.execute.Handler#setHandlerContext(net.
		 * officefloor.frame.api.execute.HandlerContext)
		 */
		@Override
		public void setHandlerContext(HandlerContext<HandlerProcess> context)
				throws Exception {
			this.context = context;
		}

	}

	/**
	 * Test reflective {@link Work}.
	 */
	public static class TestWork {

		/**
		 * Flags if {@link #task()} was invoked.
		 */
		public volatile boolean isTaskInvoked = false;

		/**
		 * Parameter of the {@link Task}.
		 */
		public volatile Object parameter = null;

		/**
		 * {@link TestManagedObjectSource}.
		 */
		public volatile TestManagedObjectSource managedObject = null;

		/**
		 * {@link Task} executed by the external event.
		 * 
		 * @param parameter
		 *            Parameter to the {@link Task}.
		 */
		public void externalEvent(Object parameter) {
			this.isTaskInvoked = true;
			this.parameter = parameter;
		}

		/**
		 * {@link Task} invoked that depends on {@link ManagedObject}.
		 * 
		 * @param parameter
		 *            Parameter to the {@link Task}.
		 * @param managedObject
		 *            {@link ManagedObject}.
		 */
		public void invokedTask(Object parameter,
				TestManagedObjectSource managedObject) {
			this.isTaskInvoked = true;
			this.parameter = parameter;
			this.managedObject = managedObject;
		}
	}
}
