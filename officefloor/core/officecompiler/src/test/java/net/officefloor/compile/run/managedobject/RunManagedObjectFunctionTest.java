package net.officefloor.compile.run.managedobject;

import net.officefloor.compile.run.AbstractRunTestCase;
import net.officefloor.frame.api.build.Indexed;
import net.officefloor.frame.api.build.None;
import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.api.function.ManagedFunctionContext;
import net.officefloor.frame.api.function.ManagedFunctionFactory;
import net.officefloor.frame.api.managedobject.ManagedObject;
import net.officefloor.frame.api.managedobject.source.ManagedObjectExecuteContext;
import net.officefloor.frame.api.managedobject.source.ManagedObjectFunctionBuilder;
import net.officefloor.frame.api.managedobject.source.ManagedObjectSource;
import net.officefloor.frame.api.managedobject.source.ManagedObjectSourceContext;
import net.officefloor.frame.api.managedobject.source.impl.AbstractManagedObjectSource;
import net.officefloor.frame.api.source.TestSource;
import net.officefloor.frame.api.team.Team;

/**
 * Ensure able to execute {@link ManagedFunction} configured by the
 * {@link ManagedObjectSource}.
 * 
 * @author Daniel Sagenschneider
 */
public class RunManagedObjectFunctionTest extends AbstractRunTestCase {

	/**
	 * Ensure can execute the {@link ManagedFunction}.
	 */
	public void testManagedObjectFunction() throws Exception {

		// Setup without a team
		RunManagedObjectSource.reset(null);
		RunManagedObjectSource mos = new RunManagedObjectSource();

		// Open the OfficeFloor
		this.open();

		// Invoke the flow handled by function
		RunManagedObjectSource.instance.executeContext.invokeProcess(0, null, mos, 0, null);

		// Ensure function invoked
		assertSame("Function should be invoked", mos, RunManagedObjectSource.instance.mo);
	}

	/**
	 * Ensure can execute {@link ManagedFunction} with a {@link Team}.
	 */
	public void testManagedObjectFunctionWithTeam() throws Exception {

		// Setup with a team
		RunManagedObjectSource.reset("MO_TEAM");
		RunManagedObjectSource mos = new RunManagedObjectSource();

		// Open the OfficeFloor
		this.open();

		// Invoke the flow handled by function
		RunManagedObjectSource.instance.executeContext.invokeProcess(0, null, mos, 0, null);

		// Ensure function invoked
		assertSame("Function should be invoked", mos, RunManagedObjectSource.instance.mo);
	}

	@TestSource
	public static class RunManagedObjectSource extends AbstractManagedObjectSource<None, Indexed>
			implements ManagedObject, ManagedFunctionFactory<Indexed, None>, ManagedFunction<Indexed, None> {

		public static void reset(String functionTeamName) {
			instance = null;
			teamName = functionTeamName;
		}

		private static RunManagedObjectSource instance;

		private static String teamName;

		private ManagedObjectExecuteContext<Indexed> executeContext;

		private RunManagedObjectSource mo;

		/*
		 * ================== ManagedObjectSource =========================
		 */

		@Override
		protected void loadSpecification(SpecificationContext context) {
		}

		@Override
		protected void loadMetaData(MetaDataContext<None, Indexed> context) throws Exception {
			context.setObjectClass(this.getClass());

			// Configure the flow to the function
			context.addFlow(null);
			ManagedObjectSourceContext<Indexed> mosContext = context.getManagedObjectSourceContext();
			mosContext.getFlow(0).linkFunction("function");
			ManagedObjectFunctionBuilder<Indexed, None> function = mosContext.addManagedFunction("function", this);
			function.linkManagedObject(0);
			if (teamName != null) {
				function.setResponsibleTeam(teamName);
			}
		}

		@Override
		public void start(ManagedObjectExecuteContext<Indexed> context) throws Exception {
			instance = this;
			this.executeContext = context;
		}

		@Override
		protected ManagedObject getManagedObject() throws Throwable {
			throw new IllegalStateException("Should only be input");
		}

		/*
		 * ======================= ManagedObject ============================
		 */

		@Override
		public Object getObject() throws Throwable {
			return this;
		}

		/*
		 * ==================== ManagedFunctionFactory ====================
		 */

		@Override
		public ManagedFunction<Indexed, None> createManagedFunction() throws Throwable {
			return this;
		}

		/*
		 * ====================== ManagedFunction =========================
		 */

		@Override
		public void execute(ManagedFunctionContext<Indexed, None> context) throws Throwable {

			// Obtain the dependency
			Object dependency = context.getObject(0);
			assertNotSame("Should be passed in instance", this, dependency);

			// Capture the managed object
			this.mo = (RunManagedObjectSource) dependency;
		}
	}

}