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

package net.officefloor.frame.impl.spi.team;

import net.officefloor.frame.api.team.Job;
import net.officefloor.frame.api.team.Team;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.frame.util.TeamSourceStandAlone;

/**
 * Tests the {@link PassiveTeam}.
 * 
 * @author Daniel Sagenschneider
 */
public class PassiveTeamTest extends OfficeFrameTestCase {

	/**
	 * Ensures that passively executes the {@link Job}.
	 */
	public void testPassiveExecute() throws Exception {

		// Create the team
		Team team = new TeamSourceStandAlone().loadTeam(PassiveTeamSource.class);

		// Create the mock task (completes immediately)
		MockJob task = new MockJob();

		// Run team and execute a task
		team.startWorking();
		team.assignJob(task);
		team.stopWorking();

		// Ensure the task executed
		assertEquals("Task should be executed once", 1, task.doTaskInvocationCount);
	}

}
