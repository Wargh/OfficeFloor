/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2017 Daniel Sagenschneider
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

import java.util.function.Supplier;

/**
 * Auto wirer.
 * 
 * @author Daniel Sagenschneider
 */
public interface AutoWirer<N extends Node> {

	/**
	 * Adds an {@link AutoWire} target for selection.
	 * 
	 * @param targetNode
	 *            Target {@link Node}.
	 * @param targetAutoWires
	 *            Target {@link AutoWire} instances supported by the
	 *            {@link Node}.
	 */
	void addAutoWireTarget(N targetNode, AutoWire... targetAutoWires);

	/**
	 * Adds an {@link AutoWire} target for selection.
	 * 
	 * @param targetNodeFactory
	 *            {@link Supplier} to create the target {@link Node}. This
	 *            enables dynamically adding the target {@link Node} only if it
	 *            is selected for linking.
	 * @param targetAutoWires
	 *            Target {@link AutoWire} instances supported by the
	 *            {@link Node}.
	 */
	void addAutoWireTarget(Supplier<? extends N> targetNodeFactory, AutoWire... targetAutoWires);

	/**
	 * Selects the appropriate {@link AutoWireLink} instances.
	 * 
	 * @param sourceNode
	 *            Source {@link Node} to link target.
	 * @param sourceAutoWires
	 *            Source {@link AutoWire} instances to match against target
	 *            {@link AutoWire} instances.
	 * @return Matching {@link AutoWireLink} instances.
	 */
	AutoWireLink<N>[] getAutoWireLinks(N sourceNode, AutoWire... sourceAutoWires);

}