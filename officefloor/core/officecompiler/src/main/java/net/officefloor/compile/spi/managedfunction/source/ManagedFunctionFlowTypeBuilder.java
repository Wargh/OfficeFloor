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

package net.officefloor.compile.spi.managedfunction.source;

import net.officefloor.frame.api.build.Indexed;
import net.officefloor.frame.api.build.None;
import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.internal.structure.Flow;

/**
 * Provides means for the {@link ManagedFunctionSource} to provide a
 * <code>type definition</code> of a possible {@link Flow} instigated by the
 * {@link ManagedFunction}.
 * 
 * @author Daniel Sagenschneider
 */
public interface ManagedFunctionFlowTypeBuilder<F extends Enum<F>> {

	/**
	 * Obtains the index of the {@link Flow}.
	 * 
	 * @return Index of the {@link Flow}.
	 */
	int getIndex();

	/**
	 * Specifies the {@link Enum} for this {@link ManagedFunctionFlowTypeBuilder}.
	 * This is required to be set if <code>F</code> is not {@link None} or
	 * {@link Indexed}.
	 * 
	 * @param key {@link Enum} for this {@link ManagedFunctionFlowTypeBuilder}.
	 * @return <code>this</code>.
	 */
	ManagedFunctionFlowTypeBuilder<F> setKey(F key);

	/**
	 * <p>
	 * Specifies the type of the argument passed by the {@link ManagedFunction} to
	 * the {@link Flow}.
	 * <p>
	 * Should there be no argument, do not call this method.
	 * 
	 * @param argumentType Type of argument passed to {@link Flow}.
	 * @return <code>this</code>.
	 */
	ManagedFunctionFlowTypeBuilder<F> setArgumentType(Class<?> argumentType);

	/**
	 * <p>
	 * Provides means to specify a display label for the {@link Flow}.
	 * <p>
	 * This need not be set as is only an aid to better identify the {@link Flow}.
	 * If not set the {@link ManagedFunctionTypeBuilder} will use the following
	 * order to get a display label:
	 * <ol>
	 * <li>{@link Enum} key name</li>
	 * <li>index value</li>
	 * </ol>
	 * 
	 * @param label Display label for the {@link Flow}.
	 * @return <code>this</code>.
	 */
	ManagedFunctionFlowTypeBuilder<F> setLabel(String label);

	/**
	 * Adds an annotation.
	 * 
	 * @param annotation Annotation.
	 * @return <code>this</code>.
	 */
	ManagedFunctionFlowTypeBuilder<F> addAnnotation(Object annotation);

}
