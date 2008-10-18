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
import java.util.ArrayList;
import java.util.List;

import net.officefloor.eclipse.OfficeFloorPlugin;
import net.officefloor.eclipse.common.editparts.AbstractOfficeFloorEditPart;
import net.officefloor.eclipse.common.editparts.PropertyChangeHandler;
import net.officefloor.eclipse.common.editparts.RemovableEditPart;
import net.officefloor.eclipse.skin.OfficeFloorFigure;
import net.officefloor.eclipse.skin.office.RoomFigureContext;
import net.officefloor.model.RemoveConnectionsAction;
import net.officefloor.model.office.ExternalManagedObjectModel;
import net.officefloor.model.office.OfficeModel;
import net.officefloor.model.office.OfficeRoomModel;
import net.officefloor.model.office.OfficeRoomModel.OfficeRoomEvent;

/**
 * {@link org.eclipse.gef.EditPart} for the
 * {@link net.officefloor.model.office.OfficeRoomModel}.
 * 
 * @author Daniel
 */
public class RoomEditPart extends
		AbstractOfficeFloorEditPart<OfficeRoomModel, OfficeFloorFigure>
		implements RemovableEditPart, RoomFigureContext {

	/**
	 * Determines if this is the top level {@link OfficeRoomModel}.
	 * 
	 * @return <code>true</code> if top level {@link OfficeRoomModel}.
	 */
	public boolean isTopLevelRoom() {
		// Top level if parent the office
		Object parentModel = this.getParent().getModel();
		return (parentModel instanceof OfficeModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.officefloor.eclipse.common.editparts.AbstractOfficeFloorEditPart#
	 * populatePropertyChangeHandlers(java.util.List)
	 */
	@Override
	protected void populatePropertyChangeHandlers(
			List<PropertyChangeHandler<?>> handlers) {
		handlers.add(new PropertyChangeHandler<OfficeRoomEvent>(OfficeRoomEvent
				.values()) {
			@Override
			protected void handlePropertyChange(OfficeRoomEvent property,
					PropertyChangeEvent evt) {
				switch (property) {
				case ADD_DESK:
				case REMOVE_DESK:
				case ADD_SUB_ROOM:
				case REMOVE_SUB_ROOM:
					RoomEditPart.this.refreshChildren();
					break;
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.officefloor.eclipse.common.editparts.AbstractOfficeFloorEditPart#
	 * createOfficeFloorFigure()
	 */
	@Override
	protected OfficeFloorFigure createOfficeFloorFigure() {
		return OfficeFloorPlugin.getSkin().getOfficeFigureFactory()
				.createRoomFigure(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.officefloor.eclipse.common.editparts.AbstractOfficeFloorEditPart#
	 * isFreeformFigure()
	 */
	@Override
	protected boolean isFreeformFigure() {
		// Free-form if only top level room
		return this.isTopLevelRoom();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.officefloor.eclipse.common.editparts.AbstractOfficeFloorEditPart#
	 * populateModelChildren(java.util.List)
	 */
	@Override
	protected void populateModelChildren(List<Object> childModels) {
		childModels.addAll(this.getCastedModel().getSubRooms());
		childModels.addAll(this.getCastedModel().getDesks());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.officefloor.eclipse.common.editparts.RemovableEditPart#delete()
	 */
	@Override
	public void delete() {

		// Only allow top level room to be removed
		if (!"OFFICE ROOM".equals(this.getCastedModel().getName())) {
			return;
		}

		// Top level therefore disconnect and remove the room
		RemoveConnectionsAction<OfficeRoomModel> room = this.getCastedModel()
				.removeConnections();
		OfficeModel office = (OfficeModel) this.getParent().getParent()
				.getModel();
		office.setRoom(null);

		// Remove the external managed objects
		for (ExternalManagedObjectModel mo : new ArrayList<ExternalManagedObjectModel>(
				office.getExternalManagedObjects())) {
			room.addCascadeModel(mo.removeConnections());
			office.removeExternalManagedObject(mo);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.officefloor.eclipse.common.editparts.RemovableEditPart#undelete()
	 */
	@Override
	public void undelete() {
		// TODO Implement
		throw new UnsupportedOperationException(
				"TODO implement RoomEditPart.undelete");
	}

	/*
	 * ================== RoomFigureContext ===============================
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.officefloor.eclipse.skin.office.RoomFigureContext#getRoomName()
	 */
	@Override
	public String getRoomName() {
		return this.getCastedModel().getName();
	}

}
