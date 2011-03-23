/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2011 Daniel Sagenschneider
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

package net.officefloor.eclipse.skin.standard.office;

import net.officefloor.eclipse.skin.office.OfficeManagedObjectSourceFlowFigure;
import net.officefloor.eclipse.skin.office.OfficeManagedObjectSourceFlowFigureContext;
import net.officefloor.eclipse.skin.standard.AbstractOfficeFloorFigure;
import net.officefloor.eclipse.skin.standard.StandardOfficeFloorColours;
import net.officefloor.eclipse.skin.standard.figure.LabelConnectorFigure;
import net.officefloor.eclipse.skin.standard.figure.ConnectorFigure.ConnectorDirection;
import net.officefloor.model.office.OfficeManagedObjectSourceFlowToOfficeSectionInputModel;

/**
 * Standard {@link OfficeManagedObjectSourceFlowFigure}.
 *
 * @author Daniel Sagenschneider
 */
public class StandardOfficeManagedObjectSourceFlowFigure extends
		AbstractOfficeFloorFigure implements
		OfficeManagedObjectSourceFlowFigure {

	/**
	 * Initiate.
	 *
	 * @param context
	 *            {@link OfficeManagedObjectSourceFlowFigureContext}.
	 */
	public StandardOfficeManagedObjectSourceFlowFigure(
			OfficeManagedObjectSourceFlowFigureContext context) {
		LabelConnectorFigure figure = new LabelConnectorFigure(context
				.getOfficeManagedObjectSourceFlowName(),
				ConnectorDirection.EAST, StandardOfficeFloorColours.BLACK());

		// Register the anchors
		this.registerConnectionAnchor(
				OfficeManagedObjectSourceFlowToOfficeSectionInputModel.class,
				figure.getConnectionAnchor());

		// Specify the figure
		this.setFigure(figure);
	}

}