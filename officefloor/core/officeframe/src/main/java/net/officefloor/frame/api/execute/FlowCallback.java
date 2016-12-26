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
package net.officefloor.frame.api.execute;

import net.officefloor.frame.api.escalate.Escalation;
import net.officefloor.frame.internal.structure.Flow;
import net.officefloor.frame.spi.managedobject.ManagedObject;

/**
 * <p>
 * Callback for completion of a {@link Flow}.
 * <p>
 * The return state of the {@link Flow} is available from the
 * {@link ManagedObject} instances manipulated by the {@link Flow} (hence there
 * is no returned value to the {@link FlowCallback}).
 *
 * @author Daniel Sagenschneider
 */
public interface FlowCallback {

	/**
	 * Invoked on completion of the {@link Flow}.
	 * 
	 * @param escalation
	 *            Possible {@link Throwable} from the {@link Flow}.
	 *            <code>null</code> indicates all {@link Escalation} instances
	 *            handled within {@link Flow}.
	 * @throws Throwable
	 *             {@link Escalation} within the callback logic.
	 */
	void run(Throwable escalation) throws Throwable;

}