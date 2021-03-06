/*-
 * #%L
 * OfficeCompiler
 * %%
 * Copyright (C) 2005 - 2020 Daniel Sagenschneider
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package net.officefloor.compile.impl.type;

import net.officefloor.compile.internal.structure.CompileContext;
import net.officefloor.compile.internal.structure.SupplierNode;
import net.officefloor.compile.supplier.InitialSupplierType;

/**
 * Tests loading the {@link InitialSupplierType} from the
 * {@link CompileContext}.
 *
 * @author Daniel Sagenschneider
 */
public class SupplierTypeContextTest extends AbstractTestTypeContext<SupplierNode, InitialSupplierType> {

	/**
	 * Instantiate.
	 */
	public SupplierTypeContextTest() {
		super(SupplierNode.class, InitialSupplierType.class, (context, node) -> node.loadInitialSupplierType(false),
				(context, node) -> context.getOrLoadInitialSupplierType(node));
	}

}
