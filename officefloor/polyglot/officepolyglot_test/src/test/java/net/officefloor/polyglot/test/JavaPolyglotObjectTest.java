/*-
 * #%L
 * Polyglot Test
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

package net.officefloor.polyglot.test;

import net.officefloor.compile.test.officefloor.CompileOfficeContext;
import net.officefloor.frame.internal.structure.ManagedObjectScope;
import net.officefloor.plugin.clazz.Dependency;
import net.officefloor.plugin.managedobject.clazz.ClassManagedObjectSource;

/**
 * Confirms the tests with {@link ClassManagedObjectSource}.
 * 
 * @author Daniel Sagenschneider
 */
public class JavaPolyglotObjectTest extends AbstractPolyglotObjectTest {

	@Override
	protected ObjectInterface create() {
		return new ObjectInterfaceImpl();
	}

	@Override
	protected void object(CompileOfficeContext context) {
		context.addManagedObject("OBJECT", ObjectInterfaceImpl.class, ManagedObjectScope.THREAD);
	}

	public static class ObjectInterfaceImpl implements ObjectInterface {

		@Dependency
		private JavaObject dependency;

		@Override
		public String getValue() {
			return "test";
		}

		@Override
		public JavaObject getDependency() {
			return this.dependency;
		}
	}

}
