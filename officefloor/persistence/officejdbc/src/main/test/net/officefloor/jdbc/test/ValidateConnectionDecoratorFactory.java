/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2018 Daniel Sagenschneider
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
package net.officefloor.jdbc.test;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.sql.PooledConnection;

import net.officefloor.frame.api.manage.OfficeFloor;
import net.officefloor.frame.api.source.SourceContext;
import net.officefloor.jdbc.decorate.ConnectionDecorator;
import net.officefloor.jdbc.decorate.ConnectionDecoratorFactory;
import net.officefloor.jdbc.decorate.PooledConnectionDecorator;
import net.officefloor.jdbc.decorate.PooledConnectionDecoratorFactory;

/**
 * {@link ConnectionDecoratorFactory} to validate all created {@link Connection}
 * and {@link PooledConnection} instances are closed on closing
 * {@link OfficeFloor}.
 * 
 * @author Daniel Sagenschneider
 */
public class ValidateConnectionDecoratorFactory implements ConnectionDecoratorFactory, ConnectionDecorator,
		PooledConnectionDecoratorFactory, PooledConnectionDecorator {

	/**
	 * Listing of {@link Connection} instances created.
	 */
	private static Deque<Connection> connections = new ConcurrentLinkedDeque<>();

	/**
	 * Listing of {@link PooledConnection} instances created.
	 */
	private static Deque<PooledConnection> pooledConnections = new ConcurrentLinkedDeque<>();

	/**
	 * Obtains the number of connections registered.
	 * 
	 * @return Number of connections registered.
	 */
	public static int getConnectionsRegisteredCount() {
		return connections.size() + pooledConnections.size();
	}

	/**
	 * Ensure that the previous test has not leaked connections into this test.
	 */
	public static void assertNoPreviousTestConnections() {
		assertConnectionsClosed("Should be no previous open connections");
	}

	/**
	 * Ensures all connections are closed.
	 */
	public static void assertAllConnectionsClosed() {
		assertConnectionsClosed("All connections should be closed");
	}

	/**
	 * Ensures all connections are closed.
	 * 
	 * @param message Message to use in asserting connections are closed.
	 */
	private static void assertConnectionsClosed(String message) {

		// Flag to identify previous open connection
		List<Connection> openConnections = new LinkedList<>();
		List<PooledConnection> openPooledConnections = new LinkedList<>();

		// Clean up connections (so no further tests fail)
		StringBuilder connectionFlags = new StringBuilder();
		for (Connection connection : connections) {
			try {
				if (!connection.isClosed()) {
					openConnections.add(connection);
					connectionFlags.append('1');
					connection.close();
				} else {
					connectionFlags.append('0');
				}
			} catch (SQLException ex) {
				// ignore failure
			}
		}

		// Clean up pooled connections
		StringBuilder pooledConnectionFlags = new StringBuilder();
		for (PooledConnection connection : pooledConnections) {
			try {
				if (!connection.getConnection().isClosed()) {
					openPooledConnections.add(connection);
					pooledConnectionFlags.append('1');
					connection.close();
				} else {
					pooledConnectionFlags.append('0');
				}
			} catch (SQLException ex) {
				// ignore failure
			}
		}

		// Fail if previous open connection
		String openConnectionText = String.join(",",
				openConnections.stream().map((conn) -> conn.toString()).toArray(String[]::new));
		String openPooledConnectionText = String.join(",",
				openPooledConnections.stream().map((conn) -> conn.toString()).toArray(String[]::new));
		String invalidMessage = message + " (" + connectionFlags.toString() + " | " + pooledConnectionFlags + ") : "
				+ openConnectionText + " | " + openPooledConnectionText;

		// As all closed, now clear listing of connections
		connections.clear();
		pooledConnections.clear();

		// Ensure no open connections
		assertEquals(invalidMessage, 0, openConnections.size() + openPooledConnections.size());
	}

	/*
	 * ================== ConnectionDecoratorFactory ===========================
	 */

	@Override
	public ConnectionDecorator createConnectionDecorator(SourceContext context) throws Exception {
		return this;
	}

	/*
	 * ===================== ConnectionDecorator ===============================
	 */

	@Override
	public Connection decorate(Connection connection) {
		connections.push(connection);
		return connection;
	}

	/*
	 * =============== PooledConnectionDecoratorFactory =========================
	 */

	@Override
	public PooledConnectionDecorator createPooledConnectionDecorator(SourceContext context) throws Exception {
		return this;
	}

	/*
	 * ================== PooledConnectionDecorator ==============================
	 */

	@Override
	public PooledConnection decorate(PooledConnection connection) {
		pooledConnections.push(connection);
		return connection;
	}

}