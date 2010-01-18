/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2010 Daniel Sagenschneider
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

package net.officefloor.plugin.team;

import net.officefloor.compile.TeamSourceService;
import net.officefloor.frame.impl.spi.team.PassiveTeamSource;

/**
 * {@link TeamSourceService} for a {@link PassiveTeamSource}.
 * 
 * @author Daniel Sagenschneider
 */
public class PassiveTeamSourceService implements
		TeamSourceService<PassiveTeamSource> {

	/*
	 * ======================= TeamSourceService ===========================
	 */

	@Override
	public String getTeamSourceAlias() {
		return "PASSIVE";
	}

	@Override
	public Class<PassiveTeamSource> getTeamSourceClass() {
		return PassiveTeamSource.class;
	}

}