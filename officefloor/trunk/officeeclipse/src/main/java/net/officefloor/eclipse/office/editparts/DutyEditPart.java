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
package net.officefloor.eclipse.office.editparts;

import java.beans.PropertyChangeEvent;
import java.util.List;

import net.officefloor.eclipse.OfficeFloorPlugin;
import net.officefloor.eclipse.common.editparts.AbstractOfficeFloorNodeEditPart;
import net.officefloor.eclipse.common.editparts.PropertyChangeHandler;
import net.officefloor.eclipse.skin.OfficeFloorFigure;
import net.officefloor.eclipse.skin.office.DutyFigureContext;
import net.officefloor.model.office.DutyModel;
import net.officefloor.model.office.DutyModel.DutyEvent;

import org.eclipse.gef.EditPart;

/**
 * {@link EditPart} for the {@link DutyModel}.
 * 
 * @author Daniel
 */
public class DutyEditPart extends
		AbstractOfficeFloorNodeEditPart<DutyModel, OfficeFloorFigure> implements
		DutyFigureContext {

	@Override
	protected void populateConnectionSourceModels(List<Object> models) {
		// Never a source
	}

	@Override
	protected void populateConnectionTargetModels(List<Object> models) {
//		models.addAll(this.getCastedModel().getPreAdminFlowItems());
//		models.addAll(this.getCastedModel().getPostAdminFlowItems());
	}

	@Override
	protected void populatePropertyChangeHandlers(
			List<PropertyChangeHandler<?>> handlers) {
		handlers.add(new PropertyChangeHandler<DutyEvent>(DutyEvent.values()) {
			@Override
			protected void handlePropertyChange(DutyEvent property,
					PropertyChangeEvent evt) {
				switch (property) {
//				case ADD_FLOW:
//				case REMOVE_FLOW:
//					DutyEditPart.this.refreshChildren();
//					break;
//				case ADD_PRE_ADMIN_FLOW_ITEM:
//				case REMOVE_PRE_ADMIN_FLOW_ITEM:
//				case ADD_POST_ADMIN_FLOW_ITEM:
//				case REMOVE_POST_ADMIN_FLOW_ITEM:
//					DutyEditPart.this.refreshTargetConnections();
//					break;
				}
			}
		});
	}

	@Override
	protected OfficeFloorFigure createOfficeFloorFigure() {
		return OfficeFloorPlugin.getSkin().getOfficeFigureFactory()
				.createDutyFigure(this);
	}

	@Override
	protected void populateModelChildren(List<Object> childModels) {
//		childModels.addAll(this.getCastedModel().getFlows());
	}

	/*
	 * =================== DutyFigureContext ========================
	 */

	@Override
	public String getDutyName() {
		return this.getCastedModel().getDutyName();
	}

}