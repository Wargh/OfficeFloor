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

package net.officefloor.plugin.section.clazz.loader;

import net.officefloor.compile.managedfunction.FunctionNamespaceType;
import net.officefloor.compile.spi.section.SectionFunctionNamespace;

/**
 * {@link SectionFunctionNamespace} with meta-data for
 * {@link ClassSectionLoader}.
 * 
 * @author Daniel Sagenschneider
 */
public class ClassSectionFunctionNamespace {

	/**
	 * {@link SectionFunctionNamespace}.
	 */
	private final SectionFunctionNamespace namespace;

	/**
	 * {@link FunctionNamespaceType}.
	 */
	private final FunctionNamespaceType namespaceType;

	/**
	 * Instantiate.
	 * 
	 * @param namespace     {@link SectionFunctionNamespace}.
	 * @param namespaceType {@link FunctionNamespaceType}.
	 */
	public ClassSectionFunctionNamespace(SectionFunctionNamespace namespace, FunctionNamespaceType namespaceType) {
		this.namespace = namespace;
		this.namespaceType = namespaceType;
	}

	/**
	 * Obtains the {@link SectionFunctionNamespace}.
	 * 
	 * @return {@link SectionFunctionNamespace}.
	 */
	public SectionFunctionNamespace getFunctionNamespace() {
		return namespace;
	}

	/**
	 * Obtains the {@link FunctionNamespaceType}.
	 * 
	 * @return {@link FunctionNamespaceType}.
	 */
	public FunctionNamespaceType getFunctionNamespaceType() {
		return namespaceType;
	}

}
