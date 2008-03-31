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
package net.officefloor.desk;

import java.util.Properties;

import net.officefloor.repository.ConfigurationContext;
import net.officefloor.work.WorkLoaderContext;
import net.officefloor.work.WorkUnknownPropertyError;

/**
 * {@link WorkLoaderContext} implementation.
 * 
 * @author Daniel
 */
public class WorkLoaderContextImpl implements WorkLoaderContext {

	/**
	 * {@link Properties}.
	 */
	private final Properties properties;

	/**
	 * {@link ClassLoader}.
	 */
	private final ClassLoader classLoader;

	/**
	 * Initiate.
	 * 
	 * @param properties
	 *            {@link Properties}.
	 * @param classLoader
	 *            {@link ClassLoader}.
	 */
	public WorkLoaderContextImpl(Properties properties, ClassLoader classLoader) {
		this.properties = properties;
		this.classLoader = classLoader;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.officefloor.work.WorkLoaderContext#getProperty(java.lang.String)
	 */
	@Override
	public String getProperty(String name) throws WorkUnknownPropertyError {
		String value = this.getProperty(name, null);
		if (value == null) {
			throw new WorkUnknownPropertyError("Unknown property '" + name
					+ "'", name);
		}
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.officefloor.work.WorkLoaderContext#getProperty(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public String getProperty(String name, String defaultValue) {
		String value = this.properties.getProperty(name);
		if ((value == null) || (value.trim().length() == 0)) {
			return defaultValue;
		}
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.officefloor.work.WorkLoaderContext#getProperties()
	 */
	@Override
	public Properties getProperties() {
		return this.properties;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.officefloor.work.WorkLoaderContext#getClassLoader()
	 */
	@Override
	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.officefloor.work.WorkLoaderContext#getConfigurationContext()
	 */
	@Override
	public ConfigurationContext getConfigurationContext() {
		throw new UnsupportedOperationException(
				"DEPRECATED: WorkLoaderContextImpl.getConfigurationContext");
	}

}
