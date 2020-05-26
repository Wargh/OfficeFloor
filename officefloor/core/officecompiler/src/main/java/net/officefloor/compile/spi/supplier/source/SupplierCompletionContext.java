/*-
 * #%L
 * OfficeCompiler
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

package net.officefloor.compile.spi.supplier.source;

import net.officefloor.frame.api.managedobject.ManagedObject;

/**
 * Completion context for the {@link SupplierSource}.
 * 
 * @author Daniel Sagenschneider
 */
public interface SupplierCompletionContext extends SupplierCompileContext {

	/**
	 * <p>
	 * Obtains the {@link AvailableType} instances.
	 * <p>
	 * Note that {@link ManagedObject} instances provided by {@link SupplierSource}
	 * instances are not included. This is because {@link SupplierSource} instances
	 * are not completed at this time to list their available {@link ManagedObject}
	 * instances.
	 * 
	 * @return {@link AvailableType} instances.
	 */
	AvailableType[] getAvailableTypes();
}
