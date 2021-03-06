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

package net.officefloor.frame.impl.execute.team;

import net.officefloor.frame.api.team.Team;
import net.officefloor.frame.internal.structure.TeamManagement;

/**
 * {@link TeamManagement} implementation.
 * 
 * @author Daniel Sagenschneider
 */
public class TeamManagementImpl implements TeamManagement {

	/**
	 * Identifier for the {@link Team} under this {@link TeamManagement}.
	 */
	private final Object teamIdentifier = new Object();

	/**
	 * {@link Team} under this {@link TeamManagement}.
	 */
	private final Team team;

	/**
	 * Initiate.
	 * 
	 * @param team
	 *            {@link Team} under this {@link TeamManagement}.
	 */
	public TeamManagementImpl(Team team) {
		this.team = team;
	}

	/*
	 * ====================== TeamManagement ================================
	 */

	@Override
	public Object getIdentifier() {
		return this.teamIdentifier;
	}

	@Override
	public Team getTeam() {
		return this.team;
	}

}
