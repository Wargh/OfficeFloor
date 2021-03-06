/*-
 * #%L
 * OfficeFrame
 * %%
 * Copyright (C) 2005 - 2020 Daniel Sagenschneider
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package net.officefloor.frame.impl.execute.managedobject.asynchronous;

import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.api.managedobject.AsynchronousContext;
import net.officefloor.frame.api.managedobject.AsynchronousManagedObject;
import net.officefloor.frame.api.managedobject.ManagedObject;
import net.officefloor.frame.internal.structure.ManagedObjectScope;
import net.officefloor.frame.internal.structure.ProcessState;
import net.officefloor.frame.internal.structure.ThreadState;
import net.officefloor.frame.test.AbstractOfficeConstructTestCase;
import net.officefloor.frame.test.Closure;
import net.officefloor.frame.test.ReflectiveFunctionBuilder;
import net.officefloor.frame.test.TestObject;

/**
 * Tests loading the {@link ManagedObject} asynchronously.
 * 
 * @author Daniel Sagenschneider
 */
public class WaitOnAsynchronousManagedObjectTest extends AbstractOfficeConstructTestCase {

	/**
	 * Ensure {@link ProcessState} bound {@link AsynchronousManagedObject} stops
	 * execution until {@link AsynchronousContext} flags completion.
	 */
	public void test_AsynchronousOperation_WaitOn_ProcessBound() throws Exception {
		this.doAsynchronousOperationTest(ManagedObjectScope.PROCESS);
	}

	/**
	 * Ensure {@link ThreadState} bound {@link AsynchronousManagedObject} stops
	 * execution until {@link AsynchronousContext} flags completion.
	 */
	public void test_AsynchronousOperation_WaitOn_ThreadBound() throws Exception {
		this.doAsynchronousOperationTest(ManagedObjectScope.THREAD);
	}

	/**
	 * Ensure {@link ManagedFunction} bound {@link AsynchronousManagedObject}
	 * stops execution until {@link AsynchronousContext} flags completion.
	 */
	public void test_AsynchronousOperation_WaitOn_FunctionBound() throws Exception {
		this.doAsynchronousOperationTest(ManagedObjectScope.FUNCTION);
	}

	/**
	 * Undertakes test.
	 * 
	 * @param scope
	 *            {@link ManagedObjectScope}.
	 */
	public void doAsynchronousOperationTest(ManagedObjectScope scope) throws Exception {

		// Construct the managed object
		TestObject object = new TestObject("MO", this);
		object.isAsynchronousManagedObject = true;
		object.managedObjectBuilder.setTimeout(10);

		// Construct functions
		TestWork work = new TestWork();
		ReflectiveFunctionBuilder task = this.constructFunction(work, "task");
		task.buildObject("MO", scope);
		task.setNextFunction("next");
		this.constructFunction(work, "next").setNextFunction("await");
		ReflectiveFunctionBuilder wait = this.constructFunction(work, "await");
		if (scope == ManagedObjectScope.FUNCTION) {
			wait.buildObject("MO", scope);
		} else {
			wait.buildObject("MO");
		}

		// Trigger function
		Closure<Boolean> isComplete = new Closure<>(false);
		this.triggerFunction("task", null, (escalation) -> isComplete.value = true);

		// Only the task should be invoked
		assertTrue("Task should be invoked", work.isTaskInvoked);
		assertTrue("Next should be invoked, as not dependent on managed object", work.isNextInvoked);

		// Different object if bound to function, so not wait
		if (scope == ManagedObjectScope.FUNCTION) {
			assertTrue("Should not wait, as different object", isComplete.value);
			return;
		}

		// Other scopes should wait
		assertFalse("Wait should be waiting on asynchronous operation", work.isAwaitInvoked);
		assertFalse("Process should not be complete", isComplete.value);

		// Complete the asynchronous operation
		object.asynchronousContext.complete(null);

		// Wait should now complete
		assertTrue("Wait should now complete", work.isNextInvoked);
		assertTrue("Process should be complete", isComplete.value);
	}

	/**
	 * Test functionality.
	 */
	public class TestWork {

		boolean isTaskInvoked = false;

		public boolean isNextInvoked = false;

		public boolean isAwaitInvoked = false;

		public void task(TestObject object) {
			this.isTaskInvoked = true;
			object.asynchronousContext.start(null);
		}

		public void next() {
			this.isNextInvoked = true;
		}

		public void await(TestObject object) {
			this.isAwaitInvoked = true;
		}
	}

}
