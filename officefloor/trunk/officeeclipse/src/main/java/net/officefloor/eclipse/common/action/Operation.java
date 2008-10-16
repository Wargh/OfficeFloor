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
package net.officefloor.eclipse.common.action;

import net.officefloor.eclipse.common.commands.OfficeFloorCommand;
import net.officefloor.eclipse.common.editparts.AbstractOfficeFloorEditPart;

import org.eclipse.gef.EditPart;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

/**
 * Operation done on {@link EditPart} instances to create resulting
 * {@link OfficeFloorCommand} to make changes.
 * 
 * @author Daniel
 */
public interface Operation {

	/**
	 * Obtains the text to use for the {@link IAction}.
	 * 
	 * @return Text to use for the {@link IAction}.
	 */
	String getActionText();

	/**
	 * Obtains the {@link EditPart} types that the items within the
	 * {@link ISelection} must be assignable.
	 * 
	 * @return Listing of {@link AbstractOfficeFloorEditPart} types.
	 */
	Class<? extends EditPart>[] getEditPartTypes();

	/**
	 * Performs this {@link Operation}.
	 * 
	 * @param context
	 *            {@link OperationContext}.
	 */
	void perform(OperationContext context);

}
