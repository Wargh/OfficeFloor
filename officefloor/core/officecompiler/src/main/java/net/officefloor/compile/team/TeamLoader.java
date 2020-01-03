package net.officefloor.compile.team;

import net.officefloor.compile.issues.CompilerIssues;
import net.officefloor.compile.officefloor.OfficeFloorTeamSourceType;
import net.officefloor.compile.properties.PropertyList;
import net.officefloor.frame.api.team.Team;
import net.officefloor.frame.api.team.source.TeamSource;
import net.officefloor.frame.api.team.source.TeamSourceProperty;
import net.officefloor.frame.api.team.source.TeamSourceSpecification;

/**
 * Loads the {@link TeamType} from the {@link TeamSource}.
 * 
 * @author Daniel Sagenschneider
 */
public interface TeamLoader {

	/**
	 * Loads and returns the {@link PropertyList} from the
	 * {@link TeamSourceSpecification} for the {@link TeamSource}.
	 * 
	 * @param <TS>
	 *            {@link TeamSource} type.
	 * @param teamSourceClass
	 *            Class of the {@link TeamSource}.
	 * @return {@link PropertyList} of the {@link TeamSourceProperty} instances
	 *         of the {@link TeamSourceSpecification} or <code>null</code> if
	 *         issues, which are reported to the {@link CompilerIssues}.
	 */
	<TS extends TeamSource> PropertyList loadSpecification(Class<TS> teamSourceClass);

	/**
	 * Loads and returns the {@link PropertyList} from the
	 * {@link TeamSourceSpecification} for the {@link TeamSource}.
	 * 
	 * @param teamSource
	 *            {@link TeamSource} instance.
	 * @return {@link PropertyList} of the {@link TeamSourceProperty} instances
	 *         of the {@link TeamSourceSpecification} or <code>null</code> if
	 *         issues, which are reported to the {@link CompilerIssues}.
	 */
	PropertyList loadSpecification(TeamSource teamSource);

	/**
	 * Loads and returns the {@link TeamType} sourced from the
	 * {@link TeamSource}.
	 * 
	 * @param <TS>
	 *            {@link TeamSource} type.
	 * @param teamName
	 *            Name of the {@link Team}.
	 * @param teamSourceClass
	 *            Class of the {@link TeamSource}.
	 * @param propertyList
	 *            {@link PropertyList} containing the properties to source the
	 *            {@link TeamType}.
	 * @return {@link TeamType} or <code>null</code> if issues, which are
	 *         reported to the {@link CompilerIssues}.
	 */
	<TS extends TeamSource> TeamType loadTeamType(String teamName, Class<TS> teamSourceClass,
			PropertyList propertyList);

	/**
	 * Loads and returns the {@link TeamType} sourced from the
	 * {@link TeamSource}.
	 * 
	 * @param teamName
	 *            Name of the {@link Team}.
	 * @param teamSource
	 *            {@link TeamSource} instance.
	 * @param propertyList
	 *            {@link PropertyList} containing the properties to source the
	 *            {@link TeamType}.
	 * @return {@link TeamType} or <code>null</code> if issues, which are
	 *         reported to the {@link CompilerIssues}.
	 */
	TeamType loadTeamType(String teamName, TeamSource teamSource, PropertyList propertyList);

	/**
	 * Loads and returns the {@link OfficeFloorTeamSourceType}.
	 * 
	 * @param <TS>
	 *            {@link TeamSource} type.
	 * @param teamName
	 *            Name of the {@link Team}.
	 * @param teamSourceClass
	 *            Class of the {@link TeamSource}.
	 * @param propertyList
	 *            {@link PropertyList} for configuring the {@link TeamSource}.
	 * @return {@link OfficeFloorTeamSourceType} or <code>null</code> if issues,
	 *         which are reported to the {@link CompilerIssues}.
	 */
	<TS extends TeamSource> OfficeFloorTeamSourceType loadOfficeFloorTeamSourceType(String teamName,
			Class<TS> teamSourceClass, PropertyList propertyList);

	/**
	 * Loads and returns the {@link OfficeFloorTeamSourceType}.
	 * 
	 * @param teamName
	 *            Name of the {@link Team}.
	 * @param teamSource
	 *            {@link TeamSource} instance.
	 * @param propertyList
	 *            {@link PropertyList} for configuring the {@link TeamSource}.
	 * @return {@link OfficeFloorTeamSourceType} or <code>null</code> if issues,
	 *         which are reported to the {@link CompilerIssues}.
	 */
	OfficeFloorTeamSourceType loadOfficeFloorTeamSourceType(String teamName, TeamSource teamSource,
			PropertyList propertyList);

}