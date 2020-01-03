package net.officefloor.frame.impl.execute.managedobject;

import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.api.managedobject.ManagedObject;
import net.officefloor.frame.internal.structure.ManagedObjectScope;
import net.officefloor.frame.internal.structure.ProcessState;
import net.officefloor.frame.internal.structure.ThreadState;
import net.officefloor.frame.test.AbstractOfficeConstructTestCase;
import net.officefloor.frame.test.TestObject;

/**
 * Ensure can load the {@link ManagedObject} at later time.
 *
 * @author Daniel Sagenschneider
 */
public class DelayedSourceManagedObjectTest extends AbstractOfficeConstructTestCase {

	/**
	 * Ensure can delay loading the {@link ManagedObject} that is bound to the
	 * {@link ProcessState}.
	 */
	public void test_DelaySourceManagedObject_BoundTo_ProcessState() throws Exception {
		this.doDelaySourceManagedObjectTest(ManagedObjectScope.PROCESS);
	}

	/**
	 * Ensure can delay loading the {@link ManagedObject} that is bound to the
	 * {@link ThreadState}.
	 */
	public void test_DelaySourceManagedObject_BoundTo_ThreadState() throws Exception {
		this.doDelaySourceManagedObjectTest(ManagedObjectScope.THREAD);
	}

	/**
	 * Ensure can delay loading the {@link ManagedObject} that is bound to the
	 * {@link ManagedFunction}.
	 */
	public void test_DelaySourceManagedObject_BoundTo_ManagedFunction() throws Exception {
		this.doDelaySourceManagedObjectTest(ManagedObjectScope.FUNCTION);
	}

	/**
	 * Undertakes the test.
	 * 
	 * @param scope
	 *            {@link ManagedObjectScope}.
	 */
	public void doDelaySourceManagedObjectTest(ManagedObjectScope scope) throws Exception {

		// Create the object
		TestObject object = new TestObject("MO", this);
		object.isDelaySource = true;

		// Create the function
		TestWork work = new TestWork();
		this.constructFunction(work, "task").buildObject("MO", scope);

		// Invoke the function
		this.triggerFunction("task", null, null);

		// Should not invoke the task
		assertFalse("Should be waiting on managed object", work.isTaskInvoked);

		// Provide the managed object
		object.managedObjectUser.setManagedObject(object);

		// Task should be triggered with managed object
		assertTrue("Task should be invoked", work.isTaskInvoked);
		assertSame("Incorrect managed object", object, work.object);
	}

	/**
	 * Test functionality.
	 */
	public class TestWork {

		public boolean isTaskInvoked = false;

		public TestObject object = null;

		public void task(TestObject object) {
			this.isTaskInvoked = true;
			this.object = object;
		}
	}

}
