package net.officefloor.frame.test;

import java.util.HashMap;
import java.util.Map;

import net.officefloor.frame.api.build.OfficeFloorBuilder;
import net.officefloor.frame.api.build.TeamBuilder;
import net.officefloor.frame.api.source.TestSource;
import net.officefloor.frame.api.team.Team;
import net.officefloor.frame.api.team.source.TeamSource;
import net.officefloor.frame.api.team.source.TeamSourceContext;
import net.officefloor.frame.api.team.source.TeamSourceSpecification;

/**
 * Mock {@link TeamSource}.
 * 
 * @author Daniel Sagenschneider
 */
@TestSource
public class MockTeamSource implements TeamSource {

	/**
	 * Property name to source the {@link Team}.
	 */
	private static final String TEAM_PROPERTY = "net.officefloor.frame.construct.team";

	/**
	 * Registry of the {@link Team} instances.
	 */
	private static final Map<String, Team> REGISTRY = new HashMap<String, Team>();

	/**
	 * Binds the {@link Team} to the name.
	 * 
	 * @param officeFloorBuilder
	 *            {@link OfficeFloorBuilder}.
	 * @param teamName
	 *            Name of {@link Team}.
	 * @param team
	 *            {@link Team}.
	 * @return {@link TeamBuilder}.
	 */
	public static TeamBuilder<?> bindTeamBuilder(OfficeFloorBuilder officeFloorBuilder, String teamName, Team team) {

		// Create the team builder
		TeamBuilder<?> teamBuilder = officeFloorBuilder.addTeam(teamName, MockTeamSource.class);

		// Bind team builder to team
		teamBuilder.addProperty(TEAM_PROPERTY, teamName);
		REGISTRY.put(teamName, team);

		// Return the team builder
		return teamBuilder;
	}

	/*
	 * ======================= TeamSource ===================================
	 */

	@Override
	public TeamSourceSpecification getSpecification() {
		// No specification
		return null;
	}

	@Override
	public Team createTeam(TeamSourceContext context) throws Exception {

		// Obtain the team
		String teamName = context.getProperty(TEAM_PROPERTY);
		Team team = REGISTRY.get(teamName);

		// Return the team
		return team;
	}

}