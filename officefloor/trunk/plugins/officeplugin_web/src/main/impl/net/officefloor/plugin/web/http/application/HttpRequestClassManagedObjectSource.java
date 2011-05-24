/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2011 Daniel Sagenschneider
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
package net.officefloor.plugin.web.http.application;

import net.officefloor.frame.api.build.None;
import net.officefloor.frame.spi.managedobject.CoordinatingManagedObject;
import net.officefloor.frame.spi.managedobject.ManagedObject;
import net.officefloor.frame.spi.managedobject.NameAwareManagedObject;
import net.officefloor.frame.spi.managedobject.ObjectRegistry;
import net.officefloor.frame.spi.managedobject.source.ManagedObjectSource;
import net.officefloor.frame.spi.managedobject.source.ManagedObjectSourceContext;
import net.officefloor.frame.spi.managedobject.source.impl.AbstractManagedObjectSource;

/**
 * {@link ManagedObjectSource} to cache creation of an {@link Object} within the
 * {@link HttpRequestState}.
 * 
 * @author Daniel Sagenschneider
 */
public class HttpRequestClassManagedObjectSource
		extends
		AbstractManagedObjectSource<HttpRequestClassManagedObjectSource.Dependencies, None> {

	/**
	 * Dependency keys for the {@link HttpRequestClassManagedObject}.
	 */
	public static enum Dependencies {
		HTTP_REQUEST_STATE
	}

	/**
	 * Name of property containing the class name.
	 */
	public static final String PROPERTY_CLASS_NAME = "class.name";

	/**
	 * Name of property containing the name to bind the object within the
	 * {@link HttpRequestState}.
	 */
	public static final String PROPERTY_BIND_NAME = "bind.name";

	/**
	 * Class of the object.
	 */
	private Class<?> objectClass;

	/**
	 * Name to bind the object within the {@link HttpRequestState}.
	 */
	private String bindName;

	/*
	 * ======================= ManagedObjectSource ===========================
	 */

	@Override
	protected void loadSpecification(SpecificationContext context) {
		context.addProperty(PROPERTY_CLASS_NAME, "Class");
	}

	@Override
	protected void loadMetaData(MetaDataContext<Dependencies, None> context)
			throws Exception {
		ManagedObjectSourceContext<None> mosContext = context
				.getManagedObjectSourceContext();

		// Obtain the class
		String className = mosContext.getProperty(PROPERTY_CLASS_NAME);
		this.objectClass = mosContext.getClassLoader().loadClass(className);

		// Obtain the overridden bind name
		this.bindName = mosContext.getProperty(PROPERTY_BIND_NAME, null);

		// Specify the meta-data
		context.setObjectClass(this.objectClass);
		context.setManagedObjectClass(HttpRequestClassManagedObject.class);
		context.addDependency(Dependencies.HTTP_REQUEST_STATE,
				HttpRequestState.class);
	}

	@Override
	protected ManagedObject getManagedObject() throws Throwable {
		return new HttpRequestClassManagedObject(this.objectClass,
				this.bindName);
	}

	/**
	 * {@link ManagedObject} to retrieve the object from the
	 * {@link HttpRequestState}.
	 */
	public static class HttpRequestClassManagedObject implements
			NameAwareManagedObject, CoordinatingManagedObject<Dependencies> {

		/**
		 * Class of the object.
		 */
		private final Class<?> objectClass;

		/**
		 * Specific name to bind the object into the {@link HttpRequestState}.
		 */
		private final String bindName;

		/**
		 * Name to bind the object to the {@link HttpRequestState}.
		 */
		private String boundName;

		/**
		 * Object.
		 */
		private Object object;

		/**
		 * Initiate.
		 * 
		 * @param objectClass
		 *            Class of the object.
		 * @param bindName
		 *            Specific name to bind the object into the
		 *            {@link HttpRequestState}.
		 */
		public HttpRequestClassManagedObject(Class<?> objectClass,
				String bindName) {
			this.objectClass = objectClass;
			this.bindName = bindName;
		}

		/*
		 * ====================== ManagedObject =============================
		 */

		@Override
		public void setBoundManagedObjectName(String boundManagedObjectName) {
			// Use bind name in preference to managed object name
			this.boundName = (this.bindName != null ? this.bindName
					: boundManagedObjectName);
		}

		@Override
		public void loadObjects(ObjectRegistry<Dependencies> registry)
				throws Throwable {

			// Obtain the HTTP request state
			HttpRequestState state = (HttpRequestState) registry
					.getObject(Dependencies.HTTP_REQUEST_STATE);

			// Lazy obtain the object
			this.object = state.getAttribute(this.boundName);
			if (this.object == null) {
				this.object = this.objectClass.newInstance();
				state.setAttribute(this.boundName, this.object);
			}
		}

		@Override
		public Object getObject() throws Throwable {
			return this.object;
		}
	}

}