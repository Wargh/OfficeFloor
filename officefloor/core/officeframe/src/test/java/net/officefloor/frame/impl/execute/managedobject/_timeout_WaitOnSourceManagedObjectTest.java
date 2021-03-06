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

package net.officefloor.frame.impl.execute.managedobject;

import java.util.LinkedList;
import java.util.List;

import net.officefloor.frame.api.escalate.SourceManagedObjectTimedOutEscalation;
import net.officefloor.frame.api.manage.Office;
import net.officefloor.frame.api.managedobject.ManagedObject;
import net.officefloor.frame.internal.structure.ManagedObjectScope;
import net.officefloor.frame.test.AbstractOfficeConstructTestCase;
import net.officefloor.frame.test.Closure;
import net.officefloor.frame.test.ReflectiveFlow;
import net.officefloor.frame.test.ReflectiveFunctionBuilder;
import net.officefloor.frame.test.TestObject;

/**
 * Ensure handle time out on wait on sourcing of {@link ManagedObject}.
 *
 * @author Daniel Sagenschneider
 */
public class _timeout_WaitOnSourceManagedObjectTest extends AbstractOfficeConstructTestCase {

	/**
	 * Ensure multiple tasks can wait on the {@link ManagedObject} to be sourced
	 * but times out to source.
	 */
	public void test_DelaySourceManagedObject_setFailure_MultipleFunctionsWaiting() throws Exception {

		// Construct the object
		TestObject object = new TestObject("MO", this);
		object.isDelaySource = true;
		object.managedObjectBuilder.setTimeout(10);

		// Construct the functions
		TestWork work = new TestWork();
		ReflectiveFunctionBuilder trigger = this.constructFunction(work, "trigger");
		trigger.buildParameter();
		trigger.buildFlow("spawnedTask", null, true);
		this.constructFunction(work, "spawnedTask").buildObject("MO", ManagedObjectScope.PROCESS);

		// Trigger the function
		final int numberOfFlows = 10;
		Closure<Throwable> failure = new Closure<>();
		Office office = this.triggerFunction("trigger", numberOfFlows, (escalation) -> failure.value = escalation);

		// Ensure flows invoked (but waiting on managed object)
		assertEquals("Incorrect number of flows invoked", numberOfFlows, work.flowsInvoked);
		assertEquals("All tasks should be waiting on process bound managed object", 0, work.failures.size());

		// Time out the managed object (releasing all tasks)
		this.adjustCurrentTimeMillis(100);
		office.runAssetChecks();

		// Ensure all spawned tasks run (with failure)
		assertEquals("All tasks should be run (failed)", numberOfFlows, work.failures.size());
		for (int i = 0; i < numberOfFlows; i++) {
			assertTrue("Incorrect failure " + i, work.failures.get(i) instanceof SourceManagedObjectTimedOutEscalation);
		}
		assertNull("Should handle all failures with callback", failure.value);
	}

	/**
	 * Test functionality.
	 */
	public class TestWork {

		private final List<Throwable> failures = new LinkedList<>();

		private int flowsInvoked = 0;

		public void trigger(Integer numberOfFlows, ReflectiveFlow flow) {
			for (int i = 0; i < numberOfFlows; i++) {
				this.flowsInvoked++;
				flow.doFlow(null, (escalation) -> failures.add(escalation));
			}
		}

		public void spawnedTask(TestObject object) {
		}
	}

}
