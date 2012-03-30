/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2012 Daniel Sagenschneider
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

package net.officefloor.compile.internal.structure;

import net.officefloor.compile.administrator.AdministratorType;
import net.officefloor.compile.properties.Property;
import net.officefloor.compile.spi.office.OfficeAdministrator;
import net.officefloor.frame.api.build.OfficeBuilder;
import net.officefloor.frame.spi.administration.Administrator;

/**
 * {@link OfficeAdministrator} node.
 * 
 * @author Daniel Sagenschneider
 */
public interface AdministratorNode extends OfficeAdministrator, LinkTeamNode {

	/**
	 * <p>
	 * Obtains the {@link AdministratorType} for this {@link AdministratorNode}.
	 * <p>
	 * The {@link OfficeAdministrator} must be fully populated with the
	 * necessary {@link Property} instances before calling this.
	 * 
	 * @return {@link AdministratorType} for this {@link AdministratorNode}.
	 */
	AdministratorType<?, ?> getAdministratorType();

	/**
	 * Builds this {@link Administrator} into the {@link OfficeBuilder}.
	 * 
	 * @param officeBuilder
	 *            {@link OfficeBuilder}.
	 */
	void buildAdministrator(OfficeBuilder officeBuilder);

}