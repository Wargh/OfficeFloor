/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2018 Daniel Sagenschneider
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
package net.officefloor.frame.impl.execute.function.threadlocal;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.officefloor.frame.api.build.Indexed;
import net.officefloor.frame.api.build.ManagedObjectBuilder;
import net.officefloor.frame.api.build.ManagingOfficeBuilder;
import net.officefloor.frame.api.build.None;
import net.officefloor.frame.api.managedobject.ManagedObject;
import net.officefloor.frame.api.managedobject.source.ManagedObjectExecuteContext;
import net.officefloor.frame.api.managedobject.source.ManagedObjectFunctionBuilder;
import net.officefloor.frame.api.managedobject.source.ManagedObjectSourceContext;
import net.officefloor.frame.api.managedobject.source.impl.AbstractManagedObjectSource;
import net.officefloor.frame.api.team.Job;
import net.officefloor.frame.api.team.Team;
import net.officefloor.frame.api.team.source.TeamSourceContext;
import net.officefloor.frame.api.team.source.impl.AbstractTeamSource;
import net.officefloor.frame.api.thread.ThreadSynchroniser;
import net.officefloor.frame.internal.structure.ManagedObjectScope;
import net.officefloor.frame.test.AbstractOfficeConstructTestCase;
import net.officefloor.frame.test.ReflectiveFlow;
import net.officefloor.frame.test.ReflectiveFunctionBuilder;

/**
 * Tests the {@link ThreadSynchroniser}.
 * 
 * @author Daniel Sagenschneider
 */
public class ThreadSynchroniserTest extends AbstractOfficeConstructTestCase {

	/**
	 * First {@link ThreadLocal} to keep synchronised.
	 */
	private static final ThreadLocal<String> threadLocalOne = new ThreadLocal<>();

	/**
	 * Second {@link ThreadLocal} to keep synchronised.
	 */
	private static final ThreadLocal<Integer> threadLocalTwo = new ThreadLocal<>();

	/**
	 * {@link Deque} of invocations.
	 */
	private final Deque<InvokedFunction> invokedFunctions = new ConcurrentLinkedDeque<>();

	/**
	 * Ensure no {@link Team} instances that {@link ThreadSynchroniser} not invoked.
	 */
	public void testThreadSynchroniserNoTeam() throws Exception {
		this.doThreadSynchroniserTest(false);
	}

	/**
	 * Ensure with {@link Team} instances that {@link ThreadSynchroniser} invoked to
	 * managed {@link ThreadLocal} across the {@link Team} {@link Thread} instances.
	 */
	public void testThreadSynchroniserWithTeams() throws Exception {
		this.doThreadSynchroniserTest(true);
	}

	/**
	 * Ensure {@link ThreadSynchroniser} keeps {@link ThreadLocal} in sync.
	 * 
	 * @param isTeams Indicates if {@link Team} per function.
	 */
	public void doThreadSynchroniserTest(boolean isTeams) throws Exception {

		// Handle creating functions with teams
		TestWork work = new TestWork();
		List<MockTeamSource> teams = new ArrayList<>();
		BiFunction<String, Integer, ReflectiveFunctionBuilder> functionFactory = !isTeams
				? (functionName, invocationCount) -> this.constructFunction(work, functionName)
				: (functionName, invocationCount) -> {
					MockTeamSource teamSource = new MockTeamSource(functionName, invocationCount);
					teams.add(teamSource);
					this.constructTeam(functionName, teamSource);
					ReflectiveFunctionBuilder function = this.constructFunction(work, functionName);
					function.getBuilder().setResponsibleTeam(functionName);
					return function;
				};

		// Add managed object
		ManagedObjectBuilder<?> mo = this.constructManagedObject("INPUT", new MockInput(isTeams), null);
		ManagingOfficeBuilder<?> moOffice = mo.setManagingOffice(this.getOfficeName());
		moOffice.setInputManagedObjectName("INPUT");
		if (isTeams) {
			MockTeamSource teamSource = new MockTeamSource("inputMo", 1);
			teams.add(teamSource);
			this.constructTeam("of-INPUT.TEAM", teamSource);
		}

		// Configure the functions
		ReflectiveFunctionBuilder function = functionFactory.apply("function", 1);
		function.setNextFunction("next");
		ReflectiveFunctionBuilder next = functionFactory.apply("next", 3);
		next.buildFlow("parallelFlow", null, false);
		next.buildFlow("callbackFlow", null, false);
		next.buildFlow("sequentialFlow", null, false);
		functionFactory.apply("parallelFlow", 3);
		functionFactory.apply("callbackFlow", 1).setNextFunction("moFunction");
		ReflectiveFunctionBuilder sequentialFlow = functionFactory.apply("sequentialFlow", 4);
		sequentialFlow.buildFlow("differentThread", null, true);
		functionFactory.apply("differentThread", 2);
		ReflectiveFunctionBuilder moFunction = functionFactory.apply("moFunction", 1);
		moFunction.buildObject("INPUT", ManagedObjectScope.THREAD);

		// Create the different thread (will not share thread state)
		threadLocalOne.set(null);
		threadLocalTwo.set(null);
		Map<String, InvokedFunction> nonThreadFunctions = new HashMap<>();
		for (String name : new String[] { "inputMo", "differentThread" }) {
			nonThreadFunctions.put(name, new InvokedFunction(name));
		}
		Function<String, InvokedFunction> getFunction = (name) -> {
			InvokedFunction invoked = nonThreadFunctions.get(name);
			return (invoked != null) ? invoked : new InvokedFunction(name);
		};

		// Create the expected execution (with thread local state)
		threadLocalOne.set("TEST");
		threadLocalTwo.set(1);
		List<InvokedFunction> expectedFunctions = new ArrayList<>();
		for (String name : new String[] { "function", "next", "parallelFlow", "callback", "sequentialFlow",
				"differentThread", "callbackThread", "callbackFlow", "moFunction", "inputMo", "moCallback" }) {
			expectedFunctions.add(getFunction.apply(name));
		}
		this.invokedFunctions.clear();
		threadLocalOne.set(null);
		threadLocalTwo.set(null);

		// Add the thread synchronisers
		this.getOfficeBuilder().addThreadSynchroniser(() -> new ThreadSynchroniser() {

			private String value;

			@Override
			public void suspendThread() {
				this.value = threadLocalOne.get();
				threadLocalOne.set(null);
			}

			@Override
			public void resumeThread() {
				threadLocalOne.set(value);
			}
		});
		this.getOfficeBuilder().addThreadSynchroniser(() -> new ThreadSynchroniser() {

			private Integer value;

			@Override
			public void suspendThread() {
				this.value = threadLocalTwo.get();
				threadLocalTwo.set(null);
			}

			@Override
			public void resumeThread() {
				threadLocalTwo.set(this.value);
			}
		});

		// Invoke
		this.invokeFunction("function", null);

		// Wait for all functions to be invoked
		this.waitForTrue(() -> this.invokedFunctions.size() == expectedFunctions.size());

		// Ensure correct invocations (with correct thread state)
		for (InvokedFunction expectedFunction : expectedFunctions) {
			InvokedFunction actualFunction = this.invokedFunctions.poll();
			assertNotNull("Expecting function: " + expectedFunction.name, actualFunction);
			assertEquals("Incorrect function", expectedFunction.name, actualFunction.name);
			assertEquals("Incorrect value one for " + expectedFunction.name, expectedFunction.oneValue,
					actualFunction.oneValue);
			assertEquals("Incorrect value two for " + expectedFunction.name, expectedFunction.twoValue,
					actualFunction.twoValue);
		}
		assertEquals("Should be no further invoked functions: " + this.invokedFunctions, 0,
				this.invokedFunctions.size());

		// Ensure all the threads have had thread locals cleared
		// Also, ensures all threads are complete
		assertEquals("Incorrect number of teams", isTeams ? 8 : 0, teams.size());
		for (MockTeamSource teamSource : teams) {
			teamSource.waitForCompletion();
			assertNull("Should clear one value for team " + teamSource.functionName, teamSource.oneValue);
			assertNull("Should clear two value for team " + teamSource.functionName, teamSource.twoValue);
		}

		// Ensure clear thread state (on exit)
		assertNull("Should clear thread local one", threadLocalOne.get());
		assertNull("Should clear thread local two", threadLocalTwo.get());
	}

	/**
	 * Test functionality.
	 */
	public class TestWork {

		public void function() {
			threadLocalOne.set("TEST");
			threadLocalTwo.set(1);
			new InvokedFunction("function");
		}

		public void next(ReflectiveFlow parallelFlow, ReflectiveFlow callbackFlow, ReflectiveFlow sequentialFlow) {
			new InvokedFunction("next");
			parallelFlow.doFlow(null, (escalation) -> {
				new InvokedFunction("callback");
				callbackFlow.doFlow(null, null);
			});
			sequentialFlow.doFlow(null, null);
		}

		public void parallelFlow() {
			new InvokedFunction("parallelFlow");
		}

		public void sequentialFlow(ReflectiveFlow differentThread) {
			new InvokedFunction("sequentialFlow");
			differentThread.doFlow(null, (escalation) -> {
				new InvokedFunction("callbackThread");
			});
		}

		public void differentThread() {
			new InvokedFunction("differentThread");
		}

		public void callbackFlow() {
			new InvokedFunction("callbackFlow");
		}

		public void moFunction(MockInput mo) {
			new InvokedFunction("moFunction");
			mo.executeContext.invokeProcess(0, null, mo, 0, (escalation) -> {
				new InvokedFunction("moCallback");
			});
		}
	}

	private class InvokedFunction {

		private final String name;

		private final String oneValue;

		private final Integer twoValue;

		private InvokedFunction(String invocation) {
			this.name = invocation;
			this.oneValue = threadLocalOne.get();
			this.twoValue = threadLocalTwo.get();
			ThreadSynchroniserTest.this.invokedFunctions.add(this);
		}

		@Override
		public String toString() {
			return this.name + "[" + this.oneValue + "-" + this.twoValue + "]";
		}
	}

	private class MockInput extends AbstractManagedObjectSource<None, Indexed> implements ManagedObject {

		private final boolean isTeams;

		private ManagedObjectExecuteContext<Indexed> executeContext;

		private MockInput(boolean isTeams) {
			this.isTeams = isTeams;
		}

		@Override
		protected void loadSpecification(SpecificationContext context) {
			// no specification
		}

		@Override
		protected void loadMetaData(MetaDataContext<None, Indexed> context) throws Exception {
			ManagedObjectSourceContext<Indexed> mosContext = context.getManagedObjectSourceContext();
			context.setObjectClass(this.getClass());
			context.addFlow(null);
			mosContext.getFlow(0).linkFunction("input");
			ManagedObjectFunctionBuilder<None, Indexed> input = mosContext.addManagedFunction("input",
					() -> (function) -> {
						new InvokedFunction("inputMo");
						return null;
					});
			if (this.isTeams) {
				input.setResponsibleTeam("TEAM");
			}
		}

		@Override
		public void start(ManagedObjectExecuteContext<Indexed> context) throws Exception {
			this.executeContext = context;
		}

		@Override
		protected ManagedObject getManagedObject() throws Throwable {
			return this;
		}

		@Override
		public Object getObject() throws Throwable {
			return this;
		}
	}

	private class MockTeamSource extends AbstractTeamSource implements Team {

		private final String functionName;

		private final int expectedInvocationCount;

		private int invocationCount = 0;

		private Throwable throwable = null;

		private String oneValue = "OVERWRITE";

		private Integer twoValue = 2;

		private MockTeamSource(String functionName, int expectedInvocationCount) {
			this.functionName = functionName;
			this.expectedInvocationCount = expectedInvocationCount;
		}

		private void waitForCompletion() {
			ThreadSynchroniserTest.this.waitForTrue(() -> {
				synchronized (this) {
					if (this.throwable != null) {
						throw new RuntimeException(this.throwable);
					}
					assertTrue(
							this.functionName + " too many invocations (a=" + this.invocationCount + ", e="
									+ this.expectedInvocationCount + ")",
							(this.invocationCount <= this.expectedInvocationCount));
					return (this.invocationCount == this.expectedInvocationCount);
				}
			});
		}

		/*
		 * =================== TeamSource ============================
		 */

		@Override
		protected void loadSpecification(SpecificationContext context) {
			// no specification
		}

		@Override
		public Team createTeam(TeamSourceContext context) throws Exception {
			return this;
		}

		/*
		 * ====================== Team ===============================
		 */

		@Override
		public void startWorking() {
			// nothing to start
		}

		@Override
		public void assignJob(Job job) {
			new Thread(() -> {

				try {
					// Run the job
					job.run();

				} catch (Throwable ex) {
					synchronized (this) {
						this.throwable = ex;
					}
				}

				// Capture and notify complete
				synchronized (this) {
					this.oneValue = threadLocalOne.get();
					this.twoValue = threadLocalTwo.get();
					this.invocationCount++;
					this.notify();
				}

			}).start();
		}

		@Override
		public void stopWorking() {
			// nothing to stop
		}
	}

}