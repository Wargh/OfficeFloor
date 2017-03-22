/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2013 Daniel Sagenschneider
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
package net.officefloor.model.impl.desk;

import net.officefloor.model.change.Change;
import net.officefloor.model.desk.DeskModel;
import net.officefloor.model.desk.FunctionNamespaceModel;

/**
 * Tests renaming the {@link FunctionNamespaceModel}.
 * 
 * @author Daniel Sagenschneider
 */
public class RenameFunctionNamespaceTest extends AbstractDeskChangesTestCase {

	/**
	 * Ensures handles {@link FunctionNamespaceModel} not being on the
	 * {@link DeskModel}.
	 */
	public void testRenameFunctionNamespaceNotOnDesk() {
		FunctionNamespaceModel namespace = new FunctionNamespaceModel("NOT_ON_DESK", null);
		Change<FunctionNamespaceModel> change = this.operations.renameFunctionNamespace(namespace, "NEW_NAME");
		this.assertChange(change, namespace, "Rename namespace NOT_ON_DESK to NEW_NAME", false,
				"Function namespace NOT_ON_DESK not on desk");
	}

	/**
	 * Ensure can rename the {@link FunctionNamespaceModel}.
	 */
	public void testRenameFunctionNamespace() {
		FunctionNamespaceModel namespace = this.model.getFunctionNamespaces().get(0);
		Change<FunctionNamespaceModel> change = this.operations.renameFunctionNamespace(namespace, "NEW_NAME");
		this.assertChange(change, namespace, "Rename namespace OLD_NAME to NEW_NAME", true);
	}

	/**
	 * Ensures on renaming the {@link FunctionNamespaceModel} that order is
	 * maintained.
	 */
	public void testRenameFunctionNamespaceCausingFunctionNamespaceOrderChange() {
		this.useTestSetupModel();
		FunctionNamespaceModel namespace = this.model.getFunctionNamespaces().get(0);
		Change<FunctionNamespaceModel> change = this.operations.renameFunctionNamespace(namespace, "NAMESPACE_C");
		this.assertChange(change, namespace, "Rename namespace NAMESPACE_A to NAMESPACE_C", true);
	}
}