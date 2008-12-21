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
package net.officefloor.plugin.jdbc.vendor.mysql;

import java.util.Properties;

import net.officefloor.plugin.jdbc.JdbcManagedObjectSource;
import net.officefloor.plugin.jdbc.vendor.AbstractVendorJdbcTest;

/**
 * Tests MySQL.
 * 
 * @author Daniel
 */
public class MysqlJdbcTest extends AbstractVendorJdbcTest {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.officefloor.plugin.jdbc.vendor.AbstractVendorJdbcTest#loadProperties
	 * (java.util.Properties)
	 */
	@Override
	protected void loadProperties(Properties properties) {
		properties
				.setProperty(
						JdbcManagedObjectSource.CONNECTION_POOL_DATA_SOURCE_FACTORY_PROPERTY,
						MysqlConnectionPoolDataSourceFactory.class.getName());
		properties.setProperty("serverName", "localhost");
		properties.setProperty("port", "3306");
		properties.setProperty("databaseName", "officefloor");
		properties.setProperty("user", "officefloor");
		properties.setProperty("password", "password");
	}

}
