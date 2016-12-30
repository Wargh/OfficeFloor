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
package net.officefloor.frame.impl.execute.function;

import net.officefloor.frame.internal.structure.FunctionState;
import net.officefloor.frame.spi.managedobject.ManagedObject;

/**
 * Provides promise like functionality for {@link FunctionState} instances.
 *
 * @author Daniel Sagenschneider
 */
public class Promise {

	/**
	 * Continue with an array of {@link FunctionState} instances.
	 * 
	 * @param jobNodes
	 *            Listing of {@link FunctionState} instances to continue with.
	 * @return Next {@link FunctionState} to undertake all the
	 *         {@link FunctionState} instances.
	 */
	public static FunctionState all(FunctionState... jobNodes) {

		// Ensure have job nodes
		if ((jobNodes == null) || (jobNodes.length == 0)) {
			return null;
		}

		// Create the listing of continue job nodes
		// (load in reverse order to execute in order)
		FunctionState returnJobNode = null;
		for (int i = jobNodes.length - 1; i >= 0; i--) {
			returnJobNode = then(jobNodes[i], null);
		}

		// Return the head job node
		return returnJobNode;
	}

	/**
	 * <p>
	 * Execute the {@link FunctionState} then the {@link FunctionState}.
	 * <p>
	 * State is passed between {@link FunctionState} instances via
	 * {@link ManagedObject} instances, so no parameter is provided.
	 * 
	 * @param function
	 *            {@link FunctionState} to execute it and its sequence of
	 *            {@link FunctionState} instances. May be <code>null</code>.
	 * @param thenJobNode
	 *            {@link FunctionState} to then continue after the first input
	 *            {@link FunctionState} sequence completes. May be
	 *            <code>null</code>.
	 * @return Next {@link FunctionState} to undertake the {@link FunctionState}
	 *         sequence and then continue {@link FunctionState} sequence.
	 */
	public static FunctionState then(FunctionState function, FunctionState thenJobNode) {
		if (function == null) {
			// No initial function, so just continue
			return thenJobNode;

		} else if (thenJobNode != null) {
			// Create continue link
			return new ThenFunction(function, thenJobNode);
		}

		// Only the initial function
		return function;
	}

	/**
	 * All access via static methods.
	 */
	private Promise() {
	}

	/**
	 * Then {@link FunctionState}.
	 */
	private static class ThenFunction extends AbstractDelegateFunctionState {

		/**
		 * Then {@link FunctionState}.
		 */
		private final FunctionState thenFunction;

		/**
		 * Creation by static methods.
		 * 
		 * @param delegate
		 *            Delegate {@link FunctionState} to complete it and all
		 *            produced {@link FunctionState} instances before
		 *            continuing.
		 * @param thenFunction
		 *            Then {@link FunctionState}.
		 */
		private ThenFunction(FunctionState delegate, FunctionState thenFunction) {
			super(delegate);
			this.thenFunction = thenFunction;
		}

		/*
		 * =================== FunctionState ==============================
		 */

		@Override
		public FunctionState execute() throws Throwable {
			FunctionState next = this.delegate.execute();
			return Promise.then(next, this.thenFunction);
		}

		@Override
		public FunctionState cancel() {
			return Promise.then(this.delegate.cancel(), this.thenFunction.cancel());
		}
	}

}