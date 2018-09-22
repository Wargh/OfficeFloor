/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2018 Daniel Sagenschneider
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.officefloor.frame.api.executive.source;

import java.util.concurrent.ThreadFactory;

import net.officefloor.frame.api.executive.ExecutionStrategy;
import net.officefloor.frame.api.executive.Executive;
import net.officefloor.frame.api.source.SourceContext;

/**
 * Context for the {@link ExecutiveSource}.
 * 
 * @author Daniel Sagenschneider
 */
public interface ExecutiveSourceContext extends SourceContext {

	/**
	 * Creates the underlying {@link ThreadFactory} that should be used for
	 * {@link ExecutionStrategy} instances.
	 * 
	 * @param executionStrategyName Name of the {@link ExecutionStrategy} to
	 *                              associate {@link Thread} names to the
	 *                              {@link ExecutionStrategy}.
	 * @param executive             {@link Executive}.
	 * @return {@link ThreadFactory} to use for {@link ExecutionStrategy} instances.
	 */
	ThreadFactory createThreadFactory(String executionStrategyName, Executive executive);

}