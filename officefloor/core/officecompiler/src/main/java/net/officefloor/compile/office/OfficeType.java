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

package net.officefloor.compile.office;

import net.officefloor.compile.spi.office.OfficeInput;
import net.officefloor.compile.spi.office.OfficeOutput;
import net.officefloor.compile.spi.office.OfficeSectionInput;
import net.officefloor.frame.api.manage.Office;
import net.officefloor.frame.api.managedobject.ManagedObject;
import net.officefloor.frame.api.team.Team;

/**
 * <code>Type definition</code> of an {@link Office}.
 * 
 * @author Daniel Sagenschneider
 */
public interface OfficeType {

	/**
	 * Obtains the {@link OfficeInput} <code>type definitions</code> required by
	 * this {@link OfficeType}.
	 * 
	 * @return {@link OfficeInput} <code>type definitions</code> required by
	 *         this {@link OfficeType}.
	 */
	OfficeInputType[] getOfficeInputTypes();

	/**
	 * Obtains the {@link OfficeOutput} <code>type definitions</code> required
	 * by this {@link OfficeType}.
	 * 
	 * @return {@link OfficeOutput} <code>type definitions</code> required by
	 *         this {@link OfficeType}.
	 */
	OfficeOutputType[] getOfficeOutputTypes();

	/**
	 * Obtains the {@link Team} <code>type definitions</code> required by this
	 * {@link OfficeType}.
	 * 
	 * @return {@link Team} <code>type definitions</code> required by this
	 *         {@link OfficeType}.
	 */
	OfficeTeamType[] getOfficeTeamTypes();

	/**
	 * Obtains the {@link ManagedObject} <code>type definition</code> required
	 * by this {@link OfficeType}.
	 * 
	 * @return {@link ManagedObject} <code>type definition</code> required by
	 *         this {@link OfficeType}.
	 */
	OfficeManagedObjectType[] getOfficeManagedObjectTypes();

	/**
	 * Obtains the {@link OfficeSectionInput} <code>type definition</code>
	 * available for this {@link OfficeType}.
	 * 
	 * @return {@link OfficeSectionInput} <code>type definition</code> available
	 *         for this {@link OfficeType}.
	 */
	OfficeAvailableSectionInputType[] getOfficeSectionInputTypes();

}
