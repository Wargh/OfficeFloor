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
package net.officefloor.eclipse.room.operations;

import net.officefloor.eclipse.common.action.AbstractOperation;
import net.officefloor.eclipse.common.commands.OfficeFloorCommand;
import net.officefloor.eclipse.common.dialog.BeanDialog;
import net.officefloor.eclipse.room.editparts.RoomEditPart;
import net.officefloor.model.room.ExternalEscalationModel;
import net.officefloor.model.room.RoomModel;

/**
 * Adds an {@link ExternalEscalationModel} to the {@link RoomModel}.
 * 
 * @author Daniel
 */
public class AddExternalEscalationOperation extends
		AbstractOperation<RoomEditPart> {

	/**
	 * Initiate.
	 */
	public AddExternalEscalationOperation() {
		super("Add escalation", RoomEditPart.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seenet.officefloor.eclipse.common.action.AbstractOperation#perform(net.
	 * officefloor.eclipse.common.action.AbstractOperation.Context)
	 */
	@Override
	protected void perform(Context context) {

		// Obtain the edit part
		final RoomEditPart editPart = context.getEditPart();

		// Add the populated External Escalation
		final ExternalEscalationModel escalation = new ExternalEscalationModel();
		BeanDialog dialog = editPart.createBeanDialog(escalation,
				"Escalation Type", "X", "Y");
		if (!dialog.populate()) {
			// Not created
			return;
		}

		// Set location
		context.positionModel(escalation);

		// Make change
		context.execute(new OfficeFloorCommand() {

			@Override
			protected void doCommand() {
				editPart.getCastedModel().addExternalEscalation(escalation);
			}

			@Override
			protected void undoCommand() {
				editPart.getCastedModel().removeExternalEscalation(escalation);
			}
		});
	}

}
