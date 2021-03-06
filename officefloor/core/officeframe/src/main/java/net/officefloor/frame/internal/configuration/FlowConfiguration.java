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

package net.officefloor.frame.internal.configuration;

import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.internal.structure.Flow;
import net.officefloor.frame.internal.structure.ThreadState;

/**
 * Configuration for a {@link Flow}.
 * 
 * @author Daniel Sagenschneider
 */
public interface FlowConfiguration<F extends Enum<F>> {

	/**
	 * Obtains the name of this {@link Flow}.
	 * 
	 * @return Name of this {@link Flow}.
	 */
	String getFlowName();

	/**
	 * Obtains the reference to the initial {@link ManagedFunction} of this
	 * {@link Flow}.
	 * 
	 * @return Reference to the initial {@link ManagedFunction} of this
	 *         {@link Flow}.
	 */
	ManagedFunctionReference getInitialFunction();

	/**
	 * Indicates whether to spawn a {@link ThreadState} for the {@link Flow}.
	 * 
	 * @return <code>true</code> to spawn a {@link ThreadState} for the
	 *         {@link Flow}.
	 */
	boolean isSpawnThreadState();

	/**
	 * Obtains the index identifying this {@link Flow}.
	 * 
	 * @return Index identifying this {@link Flow}.
	 */
	int getIndex();

	/**
	 * Obtains the key identifying this {@link Flow}.
	 * 
	 * @return Key identifying this {@link Flow}. <code>null</code> if indexed.
	 */
	F getKey();

}
