/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2009 Daniel Sagenschneider
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
package net.officefloor.eclipse.office.operations;

import net.officefloor.eclipse.office.editparts.OfficeEditPart;
import net.officefloor.eclipse.wizard.managedobjectsource.ManagedObjectInstance;
import net.officefloor.eclipse.wizard.managedobjectsource.ManagedObjectSourceWizard;
import net.officefloor.model.change.Change;
import net.officefloor.model.office.OfficeChanges;
import net.officefloor.model.office.OfficeManagedObjectSourceModel;
import net.officefloor.model.office.OfficeModel;

/**
 * Adds an {@link OfficeManagedObjectSourceModel} to the {@link OfficeModel}.
 *
 * @author Daniel Sagenschneider
 */
public class AddOfficeManagedObjectSourceOperation extends
		AbstractOfficeChangeOperation<OfficeEditPart> {

	/**
	 * Initiate.
	 */
	public AddOfficeManagedObjectSourceOperation(OfficeChanges officeChanges) {
		super("Add managed object source", OfficeEditPart.class, officeChanges);
	}

	/*
	 * ================== AbstractOfficeChangeOperation =================
	 */

	@Override
	protected Change<?> getChange(OfficeChanges changes, Context context) {

		// Obtain the managed object source
		ManagedObjectInstance mo = ManagedObjectSourceWizard
				.getManagedObjectInstance(context.getEditPart(), null);
		if (mo == null) {
			return null; // must have the managed object
		}

		// Create change to add the managed object source
		Change<OfficeManagedObjectSourceModel> change = changes
				.addOfficeManagedObjectSource(mo.getManagedObjectName(), mo
						.getManagedObjectSourceClassName(), mo
						.getPropertylist(), mo.getTimeout(), mo
						.getManagedObjectType());

		// Position the managed object source
		context.positionModel(change.getTarget());

		// Return the change to add the managed object source
		return change;
	}

}