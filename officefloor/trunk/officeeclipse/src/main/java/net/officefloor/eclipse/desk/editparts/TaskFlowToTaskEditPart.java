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
package net.officefloor.eclipse.desk.editparts;

import net.officefloor.eclipse.OfficeFloorPlugin;
import net.officefloor.eclipse.common.editparts.AbstractOfficeFloorConnectionEditPart;
import net.officefloor.eclipse.desk.DeskEditor;
import net.officefloor.eclipse.skin.desk.TaskFlowToTaskFigureContext;
import net.officefloor.frame.internal.structure.FlowInstigationStrategyEnum;
import net.officefloor.model.desk.TaskFlowToTaskModel;

import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.EditPart;

/**
 * {@link EditPart} for the {@link TaskFlowToTaskModel}.
 * 
 * @author Daniel
 */
public class TaskFlowToTaskEditPart extends
		AbstractOfficeFloorConnectionEditPart<TaskFlowToTaskModel> implements
		TaskFlowToTaskFigureContext {

	/*
	 * ============= AbstractOfficeFloorConnectionEditPart ==================
	 */
	@Override
	protected void decorateFigure(PolylineConnection figure) {
		OfficeFloorPlugin.getSkin().getDeskFigureFactory()
				.decorateTaskFlowToTaskFigure(figure, this);
	}

	/*
	 * ==================== TaskFlowToTaskFigureContext ======================
	 */

	@Override
	public FlowInstigationStrategyEnum getFlowInstigationStrategy() {
		return DeskEditor.getFlowInstigationStrategy(this.getCastedModel()
				.getLinkType());
	}

}