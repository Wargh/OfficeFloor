/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2018 Daniel Sagenschneider
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.officefloor.frame.impl.construct.managedobjectsource;

import java.util.HashMap;
import java.util.Map;

import net.officefloor.frame.api.build.DependencyMappingBuilder;
import net.officefloor.frame.api.build.ManagingOfficeBuilder;
import net.officefloor.frame.api.executive.ExecutionStrategy;
import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.api.manage.Office;
import net.officefloor.frame.api.managedobject.ManagedObject;
import net.officefloor.frame.impl.construct.managedfunction.ManagedFunctionReferenceImpl;
import net.officefloor.frame.impl.construct.managedobject.DependencyMappingBuilderImpl;
import net.officefloor.frame.impl.construct.util.ConstructUtil;
import net.officefloor.frame.internal.configuration.InputManagedObjectConfiguration;
import net.officefloor.frame.internal.configuration.ManagedFunctionReference;
import net.officefloor.frame.internal.configuration.ManagedObjectExecutionConfiguration;
import net.officefloor.frame.internal.configuration.ManagedObjectFlowConfiguration;
import net.officefloor.frame.internal.configuration.ManagingOfficeConfiguration;
import net.officefloor.frame.internal.structure.Flow;
import net.officefloor.frame.internal.structure.ProcessState;

/**
 * {@link ManagingOfficeBuilder} implementation.
 *
 * @author Daniel Sagenschneider
 */
public class ManagingOfficeBuilderImpl<F extends Enum<F>>
		implements ManagingOfficeBuilder<F>, ManagingOfficeConfiguration<F> {

	/**
	 * Name of the {@link Office} managing the {@link ManagedObject}.
	 */
	private final String officeName;

	/**
	 * {@link InputManagedObjectConfiguration} configuring binding the input
	 * {@link ManagedObject} to the {@link ProcessState}.
	 */
	private InputManagedObjectConfiguration<?> inputManagedObjectConfiguration = null;

	/**
	 * {@link ManagedObjectFlowConfiguration} instances by their index.
	 */
	private final Map<Integer, ManagedObjectFlowConfiguration<F>> flows = new HashMap<>();

	/**
	 * {@link ManagedObjectExecutionConfiguration} instances by their index.
	 */
	private final Map<Integer, ManagedObjectExecutionConfiguration> executions = new HashMap<>();

	/**
	 * Initiate.
	 *
	 * @param officeName Name of the {@link Office} managing the
	 *                   {@link ManagedObject}.
	 */
	public ManagingOfficeBuilderImpl(String officeName) {
		this.officeName = officeName;
	}

	/*
	 * ============== ManagingOfficeBuilder ===============================
	 */

	@Override
	@SuppressWarnings("rawtypes")
	public DependencyMappingBuilder setInputManagedObjectName(String inputManagedObjectName) {
		DependencyMappingBuilderImpl<?> builder = new DependencyMappingBuilderImpl(inputManagedObjectName);
		this.inputManagedObjectConfiguration = builder;
		return builder;
	}

	@Override
	public void linkFlow(F key, String functionName) {
		this.linkFlow(key.ordinal(), key, functionName);
	}

	@Override
	public void linkFlow(int flowIndex, String functionName) {
		this.linkFlow(flowIndex, null, functionName);
	}

	/**
	 * Links in a {@link Flow}.
	 *
	 * @param index        Index for the {@link Flow}.
	 * @param key          Key identifying the {@link Flow}. May be
	 *                     <code>null</code>.
	 * @param functionName Name of {@link ManagedFunction}.
	 */
	private void linkFlow(int index, F key, String functionName) {

		// Create the managed object flow configuration
		ManagedObjectFlowConfiguration<F> flow = new ManagedObjectFlowConfigurationImpl(key, null,
				new ManagedFunctionReferenceImpl(functionName, null));

		// Register the flow at its index
		this.flows.put(Integer.valueOf(index), flow);
	}

	@Override
	public void linkExecutionStrategy(int strategyIndex, String executionStrategyName) {

		// Create the managed object execution configuration
		ManagedObjectExecutionConfiguration execution = new ManagedObjectExecutionConfigurationImpl(
				executionStrategyName);

		// Register the execution at its index
		this.executions.put(Integer.valueOf(strategyIndex), execution);
	}

	/*
	 * ============= ManagingOfficeConfiguration ==========================
	 */

	@Override
	public String getOfficeName() {
		return this.officeName;
	}

	@Override
	public InputManagedObjectConfiguration<?> getInputManagedObjectConfiguration() {
		return this.inputManagedObjectConfiguration;
	}

	@Override
	public ManagingOfficeBuilder<F> getBuilder() {
		return this;
	}

	@Override
	public ManagedObjectFlowConfiguration<F>[] getFlowConfiguration() {
		return ConstructUtil.toArray(this.flows, new ManagedObjectFlowConfiguration[0]);
	}

	@Override
	public ManagedObjectExecutionConfiguration[] getExecutionConfiguration() {
		return ConstructUtil.toArray(this.executions, new ManagedObjectExecutionConfiguration[0]);
	}

	/**
	 * {@link ManagedObjectFlowConfiguration} implementation.
	 */
	private class ManagedObjectFlowConfigurationImpl implements ManagedObjectFlowConfiguration<F> {

		/**
		 * Flow key.
		 */
		private final F flowKey;

		/**
		 * Flow name.
		 */
		private final String flowName;

		/**
		 * {@link ManagedFunctionReference}.
		 */
		public ManagedFunctionReference functionReference;

		/**
		 * Initiate with flow key.
		 *
		 * @param flowKey           Flow key.
		 * @param flowName          Name of flow.
		 * @param functionReference {@link ManagedFunctionReference}.
		 */
		private ManagedObjectFlowConfigurationImpl(F flowKey, String flowName,
				ManagedFunctionReference functionReference) {
			this.flowKey = flowKey;
			this.flowName = flowName;
			this.functionReference = functionReference;
		}

		/*
		 * ================= ManagedObjectFlowConfiguration ===================
		 */

		@Override
		public F getFlowKey() {
			return this.flowKey;
		}

		@Override
		public String getFlowName() {
			return this.flowName;
		}

		@Override
		public ManagedFunctionReference getManagedFunctionReference() {
			return this.functionReference;
		}
	}

	/**
	 * {@link ManagedObjectExecutionConfiguration} implementation.
	 */
	private class ManagedObjectExecutionConfigurationImpl implements ManagedObjectExecutionConfiguration {

		/**
		 * {@link ExecutionStrategy} name.
		 */
		private final String executionStrategyName;

		private ManagedObjectExecutionConfigurationImpl(String executionStrategyName) {
			this.executionStrategyName = executionStrategyName;
		}

		/*
		 * =============== ManagedObjectExecutionConfiguration ================
		 */

		@Override
		public String getExecutionStrategyName() {
			return this.executionStrategyName;
		}
	}

}