/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2012 Daniel Sagenschneider
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

package net.officefloor.eclipse.desk.operations;

import net.officefloor.eclipse.common.action.Operation;
import net.officefloor.eclipse.desk.editparts.TaskEditPart;
import net.officefloor.model.change.Change;
import net.officefloor.model.desk.DeskChanges;
import net.officefloor.model.desk.TaskModel;

/**
 * {@link Operation} to delete a {@link TaskModel}.
 * 
 * @author Daniel Sagenschneider
 */
public class DeleteTaskOperation extends
		AbstractDeskChangeOperation<TaskEditPart> {

	/**
	 * Initiate.
	 * 
	 * @param deskChanges
	 *            {@link DeskChanges}.
	 */
	public DeleteTaskOperation(DeskChanges deskChanges) {
		super("Delete Task", TaskEditPart.class, deskChanges);
	}

	/*
	 * ================= AbstractDeskChangeOperation =================
	 */

	@Override
	protected Change<?> getChange(DeskChanges changes, Context context) {

		// Obtain the task
		TaskModel task = context.getEditPart().getCastedModel();

		// Remove the task
		return changes.removeTask(task);
	}

}