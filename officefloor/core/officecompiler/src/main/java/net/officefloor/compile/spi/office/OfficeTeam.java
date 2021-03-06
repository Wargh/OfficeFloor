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

package net.officefloor.compile.spi.office;

import net.officefloor.compile.section.TypeQualification;
import net.officefloor.compile.spi.officefloor.OfficeFloorResponsibility;
import net.officefloor.frame.api.manage.Office;
import net.officefloor.frame.api.team.Team;

/**
 * {@link Team} required by the {@link Office}.
 * 
 * @author Daniel Sagenschneider
 */
public interface OfficeTeam extends OfficeFloorResponsibility {

	/**
	 * Obtains the name of this {@link OfficeTeam}.
	 * 
	 * @return Name of this {@link OfficeTeam}.
	 */
	String getOfficeTeamName();

	/**
	 * <p>
	 * Adds an {@link TypeQualification} for this {@link OfficeTeam}.
	 * <p>
	 * This enables distinguishing {@link OfficeTeam} instances to enable, for
	 * example, dynamic {@link Team} assignment.
	 * 
	 * @param qualifier
	 *            Qualifier. May be <code>null</code> if no qualification.
	 * @param type
	 *            Type (typically the fully qualified type).
	 */
	void addTypeQualification(String qualifier, String type);

}
