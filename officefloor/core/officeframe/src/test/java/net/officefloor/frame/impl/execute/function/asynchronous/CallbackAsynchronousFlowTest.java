package net.officefloor.frame.impl.execute.function.asynchronous;

import net.officefloor.frame.api.function.AsynchronousFlow;
import net.officefloor.frame.api.function.FlowCallback;
import net.officefloor.frame.api.function.ManagedFunctionContext;
import net.officefloor.frame.test.AbstractOfficeConstructTestCase;
import net.officefloor.frame.test.ReflectiveFlow;
import net.officefloor.frame.test.ReflectiveFunctionBuilder;

/**
 * Ensure able to create {@link AsynchronousFlow} within {@link FlowCallback}.
 * 
 * @author Daniel Sagenschneider
 */
public class CallbackAsynchronousFlowTest extends AbstractOfficeConstructTestCase {

	/**
	 * Ensure able to create {@link AsynchronousFlow} within {@link FlowCallback}.
	 */
	public void testAsynchronousFlow() throws Exception {

		// Construct the functions
		TestWork work = new TestWork();
		ReflectiveFunctionBuilder trigger = this.constructFunction(work, "triggerAsynchronousFlow");
		trigger.buildFlow("flow", null, false);
		trigger.buildManagedFunctionContext();
		trigger.setNextFunction("servicingComplete");
		this.constructFunction(work, "flow");
		this.constructFunction(work, "servicingComplete");

		// Ensure halts execution until flow completes
		this.triggerFunction("triggerAsynchronousFlow", null, null);
		assertTrue("Should execute the flow", work.isFlowComplete);
		assertFalse("Should halt on async flow in callback and not complete servicing", work.isServicingComplete);

		// Complete flow confirming completes
		work.asyncFlow.complete(null);
		assertTrue("Should be complete servicing", work.isServicingComplete);
	}

	public class TestWork {

		private boolean isFlowComplete = false;

		private boolean isServicingComplete = false;

		private AsynchronousFlow asyncFlow;

		public void triggerAsynchronousFlow(ReflectiveFlow flow, ManagedFunctionContext<?, ?> context) {
			flow.doFlow(null, (escalation) -> {
				this.asyncFlow = context.createAsynchronousFlow();
			});
		}

		public void flow() {
			this.isFlowComplete = true;
		}

		public void servicingComplete() {
			this.isServicingComplete = true;
		}
	}

}