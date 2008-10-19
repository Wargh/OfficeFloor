/*
 *  Office Floor, Application Server
 *  Copyright (C) 2006 Daniel Sagenschneider
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms 
 *  of the GNU General Public License as published by the Free Software Foundation; either 
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program; 
 *  if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
 *  MA 02111-1307 USA
 */
package net.officefloor.eclipse.skin.standard.desk;

import org.eclipse.draw2d.ColorConstants;

import net.officefloor.eclipse.skin.desk.FlowItemEscalationFigure;
import net.officefloor.eclipse.skin.desk.FlowItemEscalationFigureContext;
import net.officefloor.eclipse.skin.standard.AbstractOfficeFloorFigure;
import net.officefloor.eclipse.skin.standard.figure.LabelConnectorFigure;
import net.officefloor.eclipse.skin.standard.figure.ConnectorFigure.ConnectorDirection;
import net.officefloor.model.desk.FlowItemEscalationToExternalEscalationModel;
import net.officefloor.model.desk.FlowItemEscalationToFlowItemModel;

/**
 * Standard {@link FlowItemEscalationFigure}.
 * 
 * @author Daniel
 */
public class StandardFlowItemEscalationFigure extends AbstractOfficeFloorFigure
		implements FlowItemEscalationFigure {

	/**
	 * Initiate.
	 * 
	 * @param context
	 *            {@link FlowItemEscalationFigureContext}.
	 */
	public StandardFlowItemEscalationFigure(
			FlowItemEscalationFigureContext context) {

		// Obtain simple name of escalation
		String escalationType = context.getEscalationType();
		String simpleType = escalationType;
		if (simpleType.indexOf('.') > 0) {
			simpleType = simpleType.substring(simpleType.lastIndexOf('.') + 1);
		}

		LabelConnectorFigure figure = new LabelConnectorFigure(simpleType,
				ConnectorDirection.EAST, ColorConstants.red);
		this.registerConnectionAnchor(FlowItemEscalationToFlowItemModel.class,
				figure.getConnectionAnchor());
		this.registerConnectionAnchor(
				FlowItemEscalationToExternalEscalationModel.class, figure
						.getConnectionAnchor());
		this.setFigure(figure);
	}
}
