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
package net.officefloor.frame.api.build;

import net.officefloor.frame.api.execute.Work;

/**
 * <p>
 * Creates {@link Work} to be done.
 * <p>
 * Additional managed functionality is available by implementing the following
 * interfaces:
 * <ol>
 * <li>{@link NameAwareWorkFactory}</li>
 * <li>{@link OfficeAwareWorkFactory}</li>
 * </ol>
 * 
 * @author Daniel Sagenschneider
 */
@Deprecated // functions do not maintain state (use managed objects)
public interface WorkFactory<W extends Work> {

	/**
	 * Creates a new {@link Work} instance.
	 * 
	 * @return New {@link Work} instance.
	 */
	W createWork();

}
