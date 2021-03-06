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

package net.officefloor.compile.impl.executive;

import net.officefloor.compile.executive.ExecutionStrategyType;
import net.officefloor.compile.executive.ExecutiveType;
import net.officefloor.compile.executive.TeamOversightType;

/**
 * {@link ExecutiveType} implementation.
 * 
 * @author Daniel Sagenschneider
 */
public class ExecutiveTypeImpl implements ExecutiveType {

	/**
	 * {@link ExecutionStrategyType} instances.
	 */
	private final ExecutionStrategyType[] executionStrategyTypes;

	/**
	 * {@link TeamOversightType} instances.
	 */
	private final TeamOversightType[] teamOversightTypes;

	/**
	 * Instantiate.
	 * 
	 * @param executionStrategyTypes {@link ExecutionStrategyType} instances.
	 * @param teamOversightTypes     {@link TeamOversightType} instances.
	 */
	public ExecutiveTypeImpl(ExecutionStrategyType[] executionStrategyTypes, TeamOversightType[] teamOversightTypes) {
		this.executionStrategyTypes = executionStrategyTypes;
		this.teamOversightTypes = teamOversightTypes;
	}

	/*
	 * ================ ExecutiveType =====================
	 */

	@Override
	public ExecutionStrategyType[] getExecutionStrategyTypes() {
		return this.executionStrategyTypes;
	}

	@Override
	public TeamOversightType[] getTeamOversightTypes() {
		return this.teamOversightTypes;
	}

}
