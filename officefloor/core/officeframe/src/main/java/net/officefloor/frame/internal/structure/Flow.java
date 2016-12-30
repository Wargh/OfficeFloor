/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2013 Daniel Sagenschneider
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
package net.officefloor.frame.internal.structure;

import net.officefloor.frame.api.escalate.Escalation;
import net.officefloor.frame.api.execute.FlowCallback;
import net.officefloor.frame.api.execute.ManagedFunction;
import net.officefloor.frame.impl.execute.function.ManagedFunctionContainerImpl;
import net.officefloor.frame.spi.governance.Governance;

/**
 * Represents a sub-graph of the {@link ManagedFunctionContainerImpl} graph
 * making up the {@link ThreadState}. This enables knowing when to undertake the
 * {@link FlowCallback} on completion of all
 * {@link ManagedFunctionContainerImpl} instances of the {@link Flow}.
 * 
 * @author Daniel Sagenschneider
 */
public interface Flow extends LinkedListSetEntry<Flow, ThreadState> {

	/**
	 * Creates a {@link FunctionState} within this {@link Flow} for the
	 * {@link FunctionLogic}.
	 * 
	 * @param logic
	 *            {@link FunctionLogic}.
	 * @return {@link FunctionState} for the {@link FunctionLogic}.
	 */
	FunctionState createFunction(FunctionLogic logic);

	/**
	 * Creates a new managed {@link ManagedFunctionContainer} contained in this
	 * {@link Flow} for the {@link ManagedFunction}.
	 * 
	 * @param parameter
	 *            Parameter for the {@link ManagedFunction}.
	 * @param managedFunctionMetaData
	 *            {@link ManagedFunctionMetaData} for the new
	 *            {@link ManagedFunction}.
	 * @param isEnforceGovernance
	 *            <code>true</code> to enforce {@link Governance} on
	 *            deactivation.
	 * @param parallelFunctionOwner
	 *            {@link ManagedFunctionContainer} that is the parallel owner of
	 *            the new {@link ManagedFunction}.
	 * @return New {@link FunctionState}.
	 */
	<O extends Enum<O>, F extends Enum<F>> ManagedFunctionContainer createManagedFunction(Object parameter,
			ManagedFunctionMetaData<O, F> managedFunctionMetaData, boolean isEnforceGovernance,
			ManagedFunctionContainer parallelFunctionOwner);

	/**
	 * Creates a new {@link ManagedFunctionContainerImpl} contained in this
	 * {@link Flow} for the {@link GovernanceActivity}.
	 * 
	 * @param governanceActivity
	 *            {@link GovernanceActivity}.
	 * @return New {@link ManagedFunctionContainerImpl}.
	 */
	<F extends Enum<F>> FunctionState createGovernanceFunction(GovernanceActivity<F> governanceActivity,
			GovernanceMetaData<?, F> governanceMetaData);

	/**
	 * Flags that the input {@link FunctionState} has completed.
	 * 
	 * @param function
	 *            {@link FunctionState} that has completed.
	 * @param isCancel
	 *            Flags whether completing due to cancel.
	 * @return Optional {@link FunctionState} to handle completion of the
	 *         {@link FunctionState}.
	 */
	FunctionState managedFunctionComplete(FunctionState function, boolean isCancel);

	/**
	 * Cancels this {@link Flow}.
	 * 
	 * @return {@link FunctionState} to clean up this {@link Flow}.
	 */
	FunctionState cancel();

	/**
	 * Handles the {@link Escalation} from a {@link FunctionState} of this
	 * {@link Flow}.
	 * 
	 * @param escalation
	 *            {@link Escalation}.
	 * @return {@link FunctionState} to handle the {@link Escalation}.
	 */
	FunctionState handleEscalation(Throwable escalation);

	/**
	 * Obtains the {@link ThreadState} containing this {@link Flow}.
	 * 
	 * @return {@link ThreadState} containing this {@link Flow}.
	 */
	ThreadState getThreadState();

}