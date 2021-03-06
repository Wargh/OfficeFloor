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

package net.officefloor.frame.impl.execute.function;

import net.officefloor.frame.internal.structure.FunctionState;
import net.officefloor.frame.internal.structure.ThreadState;
import net.officefloor.frame.test.OfficeFrameTestCase;

/**
 * Tests the {@link Promise}.
 * 
 * @author Daniel Sagenschneider
 */
public class PromiseTest extends OfficeFrameTestCase {

	/**
	 * Ensure return <code>null</code> on no {@link FunctionState} instances.
	 */
	public void testNothingFuther() {
		FunctionState promise = Promise.then(null, null);
		this.replayMockObjects();
		assertNull("Should be no promise", promise);
		this.verifyMockObjects();
	}

	/**
	 * Ensure return {@link FunctionState} if no then {@link FunctionState}.
	 */
	public void testNoThenFunction() {
		FunctionState function = this.createMock(FunctionState.class);
		this.replayMockObjects();
		FunctionState promise = Promise.then(function, null);
		assertSame("Should just be function", function, promise);
		this.verifyMockObjects();
	}

	/**
	 * Ensure return {@link FunctionState} if no delegate {@link FunctionState}.
	 */
	public void testNoDelegateFunction() {
		FunctionState function = this.createMock(FunctionState.class);
		this.replayMockObjects();
		FunctionState promise = Promise.then(null, function);
		assertSame("Should just be function", function, promise);
		this.verifyMockObjects();
	}

	/**
	 * Ensure can chain {@link FunctionState} instances with {@link Promise}.
	 */
	public void testChainTogether() throws Throwable {

		ThreadState threadState = this.createMock(ThreadState.class);
		FunctionState functionOne = this.createMock(FunctionState.class);
		FunctionState functionTwo = this.createMock(FunctionState.class);
		FunctionState functionPromise = this.createMock(FunctionState.class);

		// Record setting up promise
		this.recordReturn(functionOne, functionOne.getThreadState(), threadState);
		this.recordReturn(threadState, threadState.then(functionOne, functionTwo), functionPromise);

		this.replayMockObjects();

		// Create promise
		FunctionState promise = Promise.then(functionOne, functionTwo);
		assertSame("Should be provided promise", functionPromise, promise);

		this.verifyMockObjects();
	}

}
