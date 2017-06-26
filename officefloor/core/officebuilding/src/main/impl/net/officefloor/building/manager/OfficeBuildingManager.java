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
package net.officefloor.building.manager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.net.ssl.KeyManagerFactory;

import mx4j.tools.remote.PasswordAuthenticator;
import net.officefloor.building.command.OfficeFloorCommand;
import net.officefloor.building.command.OfficeFloorCommandParser;
import net.officefloor.building.command.OfficeFloorCommandParserImpl;
import net.officefloor.building.command.officefloor.OfficeBuildingOpenOfficeFloorCommand;
import net.officefloor.building.decorate.OfficeFloorDecorator;
import net.officefloor.building.decorate.OfficeFloorDecoratorServiceLoader;
import net.officefloor.building.execute.OfficeFloorCommandContextImpl;
import net.officefloor.building.process.ProcessCompletionListener;
import net.officefloor.building.process.ProcessConfiguration;
import net.officefloor.building.process.ProcessException;
import net.officefloor.building.process.ProcessManager;
import net.officefloor.building.process.ProcessManagerMBean;
import net.officefloor.building.process.ProcessShell;
import net.officefloor.building.process.ProcessShellMBean;
import net.officefloor.building.process.ProcessStartListener;
import net.officefloor.building.process.officefloor.OfficeFloorManager;
import net.officefloor.compile.mbean.OfficeFloorMBean;
import net.officefloor.console.OfficeBuilding;
import net.officefloor.frame.api.manage.OfficeFloor;

/**
 * {@link OfficeBuilding} Manager.
 * 
 * @author Daniel Sagenschneider
 */
public class OfficeBuildingManager implements OfficeBuildingManagerMBean {

	/**
	 * {@link Logger}.
	 */
	private static final Logger LOGGER = Logger.getLogger(OfficeBuildingManager.class.getName());

	/**
	 * Maven Group Id for the {@link OfficeBuilding}.
	 */
	public static final String OFFICE_BUILDING_GROUP_ID = "net.officefloor.core";

	/**
	 * Maven Artifact Id for this {@link OfficeBuilding}.
	 */
	public static final String OFFICE_BUILDING_ARTIFACT_ID = "officebuilding";

	/**
	 * Prefix to property names from {@link System#getProperties()} for
	 * configuration.
	 */
	public static final String OFFICE_BUILDING_PROPERTY_PREFIX = OFFICE_BUILDING_GROUP_ID + "."
			+ OFFICE_BUILDING_ARTIFACT_ID;

	/**
	 * Name of the {@link OfficeBuilding} within the {@link Registry}.
	 */
	private static final String OFFICE_BUILDING_REGISTERED_NAME = "OfficeBuilding";

	/**
	 * {@link ObjectName} for the {@link OfficeBuildingManagerMBean}.
	 */
	private static final ObjectName OFFICE_BUILDING_MANAGER_OBJECT_NAME;

	static {
		// Load the Office Building Manager Object Name
		ObjectName officeBuildingManagerObjectName = null;
		try {
			officeBuildingManagerObjectName = new ObjectName(OFFICE_BUILDING_REGISTERED_NAME, "type",
					"OfficeBuildingManager");
		} catch (MalformedObjectNameException ex) {
			// This should never be the case
		}
		OFFICE_BUILDING_MANAGER_OBJECT_NAME = officeBuildingManagerObjectName;
	}

	/**
	 * Obtains {@link ObjectName} for the {@link OfficeBuildingManagerMBean}.
	 * 
	 * @return {@link ObjectName} for the {@link OfficeBuildingManagerMBean}.
	 */
	public static ObjectName getOfficeBuildingManagerObjectName() {
		return OFFICE_BUILDING_MANAGER_OBJECT_NAME;
	}

	/**
	 * Obtains the SSL protocol being used.
	 * 
	 * @return SSL protocol being used.
	 */
	public static String getSslProtocol() {
		String sslProtocol = System.getProperty(OFFICE_BUILDING_PROPERTY_PREFIX + ".ssl.protocol", "SSLv3");
		return sslProtocol;
	}

	/**
	 * Obtains the SSL algorithm being used.
	 * 
	 * @return SSL algorithm being used.
	 */
	public static String getSslAlgorithm() {
		String sslAlgorithm = System.getProperty(OFFICE_BUILDING_PROPERTY_PREFIX + ".ssl.algorithm",
				KeyManagerFactory.getDefaultAlgorithm());
		return sslAlgorithm;
	}

	/**
	 * Property specify the {@link Registry} client socket factory.
	 */
	private static final String PROPERTY_RMI_CLIENT_SOCKET_FACTORY = "com.sun.jndi.rmi.factory.socket";

	/**
	 * Starts the {@link OfficeBuilding}.
	 * 
	 * @param hostName
	 *            Host for the {@link OfficeBuilding}. This is necessary as the
	 *            RMI {@link JMXConnector} uses this to connect to the
	 *            {@link OfficeBuilding}. May be <code>null</code> to use the
	 *            localhost's host name.
	 * @param port
	 *            Port for the {@link OfficeBuilding}.
	 * @param keyStore
	 *            {@link File} containing the security keys.
	 * @param keyStorePassword
	 *            Password to the key store {@link File}.
	 * @param userName
	 *            User name to allow connecting to the
	 *            {@link OfficeBuildingManager}.
	 * @param password
	 *            Password to allow connecting to the
	 *            {@link OfficeBuildingManager}.
	 * @param workspace
	 *            Work space for the {@link OfficeBuilding}. May be
	 *            <code>null</code> to use default location (temp directory).
	 * @param isIsolateProcesses
	 *            Flag indicating to isolate each {@link Process}.
	 *            <code>false</code> will have artifacts shared between
	 *            {@link Process} instances.
	 * @param environment
	 *            Environment {@link Properties}.
	 * @param mbeanServer
	 *            {@link MBeanServer}. May be <code>null</code> to use platform
	 *            {@link MBeanServer}.
	 * @param jvmOptions
	 *            JVM options for the spawned {@link Process} instances.
	 * @param isAllowClassPathEntries
	 *            Flag indicating if allow class path entries via the
	 *            {@link OpenOfficeFloorConfiguration}.
	 * @return {@link OfficeBuildingManager} managing the started
	 *         {@link OfficeBuilding}.
	 * @throws Exception
	 *             If fails to start the {@link OfficeBuilding}.
	 */
	public static OfficeBuildingManagerMBean startOfficeBuilding(String hostName, int port, File keyStore,
			String keyStorePassword, String userName, String password, File workspace, boolean isIsolateProcesses,
			Properties environment, MBeanServer mbeanServer, String[] jvmOptions, boolean isAllowClassPathEntries)
			throws Exception {

		// Obtain the start time
		Date startTime = new Date(System.currentTimeMillis());

		// Ensure have an the MBean Server
		if (mbeanServer == null) {
			mbeanServer = ManagementFactory.getPlatformMBeanServer();
		}

		// Use host name (rather than IP address) for client JMX connections
		if (hostName == null) {
			hostName = InetAddress.getLocalHost().getHostName();
		}

		// Obtain the work space location
		if (workspace == null) {
			// Use default location of user specific temporary directory.
			// Note: Avoids file permission errors on cleaning the workspace of
			// running under different users.
			workspace = new File(new File(System.getProperty("java.io.tmpdir"), System.getProperty("user.name")),
					"officebuilding");
		}

		// Ensure start with empty work space
		deleteDirectory(workspace, "Failed clearing work space for " + OfficeBuildingManager.class.getSimpleName());

		// Ensure the work space exists
		if (!(workspace.exists())) {
			workspace.mkdir();
		}

		// Create the socket factories
		String sslProtocol = getSslProtocol();
		String sslAlgorithm = getSslAlgorithm();
		byte[] keyStoreContent = OfficeBuildingRmiServerSocketFactory.getKeyStoreContent(keyStore);
		OfficeBuildingRmiServerSocketFactory serverSocketFactory = new OfficeBuildingRmiServerSocketFactory(sslProtocol,
				sslAlgorithm, keyStoreContent, keyStorePassword);
		RMIClientSocketFactory clientSocketFactory = new OfficeBuildingRmiClientSocketFactory(sslProtocol, sslAlgorithm,
				keyStoreContent, keyStorePassword);

		// Loop attempting to start
		boolean isStarted = false;
		final int START_ATTEMPTS = 3;
		int attemptsToStart = 0;
		Registry registry;
		JMXConnectorServer connectorServer = null;
		do {

			// Ensure have Registry on the port
			try {
				// Attempt to create on port
				registry = LocateRegistry.createRegistry(port, clientSocketFactory, serverSocketFactory);
			} catch (RemoteException ex) {
				// Registry already on port, so obtain it
				registry = LocateRegistry.getRegistry(null, port, clientSocketFactory);
			}

			// Determine if already running the Office Building
			Remote officeBuildingRemote = null;
			try {
				// Determine if Office Building Manager in registry
				officeBuildingRemote = registry.lookup(OFFICE_BUILDING_REGISTERED_NAME);
			} catch (NotBoundException | NoSuchObjectException ex) {
				// Not available, so carry on to start
			}
			if (officeBuildingRemote != null) {
				// Return the already running Office Building Manager
				return getOfficeBuildingManager(hostName, port, keyStore, keyStorePassword, userName, password);
			}

			// Provide secure server environment
			Map<String, Object> serverEnv = new HashMap<String, Object>();

			// Configure authenticated communication
			InputStream passwordStream = new ByteArrayInputStream((userName + "=" + password).getBytes());
			serverEnv.put(RMIConnectorServer.AUTHENTICATOR, new PasswordAuthenticator(passwordStream));

			// Configure encrypted communication
			serverEnv.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, clientSocketFactory);
			serverEnv.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, serverSocketFactory);
			serverEnv.put(PROPERTY_RMI_CLIENT_SOCKET_FACTORY, clientSocketFactory);

			// Start the JMX connector server (on local host)
			try {
				JMXServiceURL serviceUrl = getOfficeBuildingJmxServiceUrl(hostName, port);
				connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(serviceUrl, serverEnv, mbeanServer);
				connectorServer.start();

				// As here, flag that started
				isStarted = true;

			} catch (IOException ex) {
				// Failed to start on this attempt
				attemptsToStart++;

				// Stop trying after so many attempts
				if (attemptsToStart >= START_ATTEMPTS) {
					// Propagate the failure
					throw ex;
				}

				// Allow some time before trying again
				Thread.sleep(1000);
			}

		} while (!isStarted);

		// Obtain the address of the connector server
		JMXServiceURL connectorServerAddress = connectorServer.getAddress();

		// Create the Office Building Manager
		OfficeBuildingManager manager = new OfficeBuildingManager(startTime, connectorServerAddress, connectorServer,
				registry, mbeanServer, workspace, isIsolateProcesses, environment, jvmOptions, isAllowClassPathEntries);

		// Register the Office Building Manager
		mbeanServer.registerMBean(manager, OFFICE_BUILDING_MANAGER_OBJECT_NAME);

		// Ensure the Office Building is available
		boolean isOfficeBuildingAvailable = false;
		long timeoutTimestamp = System.currentTimeMillis();
		do {

			// Ensure time out to not keep checking forever
			if ((System.currentTimeMillis() - timeoutTimestamp) > 10000) {
				System.err.println("WARNING: Timed out waiting for confirmation of "
						+ OfficeBuildingManager.class.getSimpleName() + " starting.  Allowing use as is.");

				// Return the (possibly not ready) Office Building Manager.
				// (Best chance has been given for it to start)
				return manager;
			}

			// Determine if office building is available
			isOfficeBuildingAvailable = OfficeBuildingManager.isOfficeBuildingAvailable(hostName, port, keyStore,
					keyStorePassword, userName, password);

			// Sleep some time if not available
			if (!isOfficeBuildingAvailable) {
				Thread.sleep(100);
			}
		} while (!isOfficeBuildingAvailable);

		// Return the Office Building Manager
		return manager;
	}

	/**
	 * Determines if the {@link OfficeBuilding} is available for use.
	 * 
	 * @param hostName
	 *            Name of host where the {@link OfficeBuilding} should be
	 *            available.
	 * @param port
	 *            Port on which the {@link OfficeBuilding} should be available.
	 * @param trustStore
	 *            {@link File} containing the trusted security keys.
	 * @param trustStorePassword
	 *            Password to the trusted key store {@link File}.
	 * @param userName
	 *            User name to connect.
	 * @param password
	 *            Password to connect.
	 * @return <code>true</code> if the {@link OfficeBuilding} is available.
	 */
	public static boolean isOfficeBuildingAvailable(String hostName, int port, File trustStore,
			String trustStorePassword, String userName, String password) {
		try {
			// Obtain the OfficeBuilding manager
			OfficeBuildingManagerMBean manager = getOfficeBuildingManager(hostName, port, trustStore,
					trustStorePassword, userName, password);

			// Available if not stopped
			return (!manager.isOfficeBuildingStopped());
		} catch (Exception ex) {
			return false; // not available
		}
	}

	/**
	 * Spawns an {@link OfficeBuilding} in a new {@link Process}.
	 * 
	 * @param hostName
	 *            Host for the {@link OfficeBuilding}. This is necessary as the
	 *            RMI {@link JMXConnector} uses this to connect to the
	 *            {@link OfficeBuilding}. May be <code>null</code> to use the
	 *            localhost's host name.
	 * @param port
	 *            Port for the {@link OfficeBuilding}.
	 * @param keyStore
	 *            {@link File} containing the security keys.
	 * @param keyStorePassword
	 *            Password to the key store {@link File}.
	 * @param userName
	 *            User name to connect.
	 * @param password
	 *            Password to connect.
	 * @param workspace
	 *            Workspace for the {@link OfficeBuilding}.
	 * @param isIsolateProcesses
	 *            Flag indicating to isolate the {@link Process} instances.
	 * @param environment
	 *            Environment {@link Properties}. May be <code>null</code>.
	 * @param configuration
	 *            {@link ProcessConfiguration}. May be <code>null</code>.
	 * @param jvmOptions
	 *            JVM options for the {@link Process} instances.
	 * @param isAllowClassPathEntries
	 *            Flag indicating if the {@link OfficeBuilding} will allow
	 *            configured class path entries.
	 * @return {@link ProcessManager} managing the started
	 *         {@link OfficeBuilding}.
	 * @throws ProcessException
	 *             If fails to spawn the {@link OfficeBuilding}.
	 */
	public static ProcessManager spawnOfficeBuilding(String hostName, int port, File keyStore, String keyStorePassword,
			String userName, String password, File workspace, boolean isIsolateProcesses, Properties environment,
			String[] jvmOptions, boolean isAllowClassPathEntries, ProcessConfiguration configuration)
			throws ProcessException {

		// Ensure have environment
		if (environment == null) {
			environment = new Properties();
		}

		// Create the OfficeBuilding managed process
		OfficeBuildingManagedProcess managedProcess = new OfficeBuildingManagedProcess(hostName, port, keyStore,
				keyStorePassword, userName, password, workspace, isIsolateProcesses, environment, jvmOptions,
				isAllowClassPathEntries);

		// Ensure have process configuration
		if (configuration == null) {
			configuration = new ProcessConfiguration();
		}

		// Spawn the OfficeBuilding
		ProcessManager manager = ProcessManager.startProcess(managedProcess, configuration);

		// Return the Process Manager
		return manager;
	}

	/**
	 * <p>
	 * Obtains the {@link OfficeBuildingManagerMBean} for the
	 * {@link OfficeBuilding}.
	 * <p>
	 * This a utility method to obtain the {@link OfficeBuildingManagerMBean} of
	 * an existing {@link OfficeBuilding}.
	 * 
	 * @param hostName
	 *            Name of the host where the {@link OfficeBuilding} resides.
	 *            <code>null</code> indicates localhost.
	 * @param port
	 *            Port where the {@link OfficeBuilding} resides.
	 * @param trustStore
	 *            {@link File} containing the trusted security keys.
	 * @param trustStorePassword
	 *            Password to the trusted key store {@link File}.
	 * @param userName
	 *            User name to connect.
	 * @param password
	 *            Password to connect.
	 * @return {@link OfficeBuildingManagerMBean}.
	 * @throws Exception
	 *             If fails to obtain the {@link OfficeBuildingManagerMBean}.
	 */
	public static OfficeBuildingManagerMBean getOfficeBuildingManager(String hostName, int port, File trustStore,
			String trustStorePassword, String userName, String password) throws Exception {
		return getMBeanProxy(hostName, port, trustStore, trustStorePassword, userName, password,
				OFFICE_BUILDING_MANAGER_OBJECT_NAME, OfficeBuildingManagerMBean.class);
	}

	/**
	 * <p>
	 * Obtains the {@link ProcessManagerMBean} by the process name for the
	 * {@link OfficeBuilding}.
	 * <p>
	 * This a utility method to obtain the {@link ProcessManagerMBean} of an
	 * existing {@link Process} currently running within the
	 * {@link OfficeBuilding}.
	 * 
	 * @param hostName
	 *            Name of the host where the {@link OfficeBuilding} resides.
	 *            <code>null</code> indicates localhost.
	 * @param port
	 *            Port where the {@link OfficeBuilding} resides.
	 * @param processNamespace
	 *            Name of the {@link Process} to obtain its
	 *            {@link ProcessManagerMBean}.
	 * @param trustStore
	 *            {@link File} containing the trusted security keys.
	 * @param trustStorePassword
	 *            Password to the trusted key store {@link File}.
	 * @param userName
	 *            User name to connect.
	 * @param password
	 *            Password to connect.
	 * @return {@link ProcessManagerMBean}.
	 * @throws Exception
	 *             If fails to obtain the {@link ProcessManagerMBean}.
	 */
	public static ProcessManagerMBean getProcessManager(String hostName, int port, String processNamespace,
			File trustStore, String trustStorePassword, String userName, String password) throws Exception {
		ObjectName objectName = ProcessManager.getLocalObjectName(processNamespace,
				ProcessManager.getProcessManagerObjectName(processNamespace));
		return getMBeanProxy(hostName, port, trustStore, trustStorePassword, userName, password, objectName,
				ProcessManagerMBean.class);
	}

	/**
	 * <p>
	 * Obtains the {@link ProcessShellMBean} by the process name for the
	 * {@link OfficeBuilding}.
	 * <p>
	 * This a utility method to obtain the {@link ProcessShellMBean} of an
	 * existing {@link Process} currently being managed within the
	 * {@link OfficeBuilding}.
	 * 
	 * @param hostName
	 *            Name of the host where the {@link OfficeBuilding} resides.
	 *            <code>null</code> indicates localhost.
	 * @param port
	 *            Port where the {@link OfficeBuilding} resides.
	 * @param processNamespace
	 *            Name of the {@link Process} to obtain its
	 *            {@link ProcessShellMBean}.
	 * @param trustStore
	 *            {@link File} containing the trusted security keys.
	 * @param trustStorePassword
	 *            Password to the trusted key store {@link File}.
	 * @param userName
	 *            User name to connect.
	 * @param password
	 *            Password to connect.
	 * @return {@link ProcessShellMBean}.
	 * @throws Exception
	 *             If fails to obtain the {@link ProcessShellMBean}.
	 */
	public static ProcessShellMBean getProcessShell(String hostName, int port, String processNamespace, File trustStore,
			String trustStorePassword, String userName, String password) throws Exception {
		ObjectName objectName = ProcessManager.getLocalObjectName(processNamespace,
				ProcessShell.getProcessShellObjectName(processNamespace));
		return getMBeanProxy(hostName, port, trustStore, trustStorePassword, userName, password, objectName,
				ProcessShellMBean.class);
	}

	/**
	 * <p>
	 * Obtains the {@link OfficeFloorMBean}.
	 * <p>
	 * The <code>hostName</code> and <code>port</code> are of the
	 * {@link OfficeBuilding} managing the {@link OfficeFloor} {@link Process}.
	 * They are <i>not</i> of the specific {@link Process} containing the
	 * {@link OfficeFloor}.
	 * 
	 * @param hostName
	 *            Name of the host where the {@link OfficeBuilding} resides.
	 *            <code>null</code> indicates localhost.
	 * @param port
	 *            Port where the {@link OfficeBuilding} resides.
	 * @param officeFloorName
	 *            Name of the {@link OfficeFloor}.
	 * @param trustStore
	 *            {@link File} containing the trusted security keys.
	 * @param trustStorePassword
	 *            Password to the trusted key store {@link File}.
	 * @param userName
	 *            User name to connect.
	 * @param password
	 *            Password to connect.
	 * @return {@link OfficeFloorBean}.
	 * @throws Exception
	 *             If fails to obtain {@link OfficeFloorMBean}.
	 */
	public static OfficeFloorMBean getOfficeFloorManager(String hostName, int port, String officeFloorName,
			File trustStore, String trustStorePassword, String userName, String password) throws Exception {
		ObjectName objectName = ProcessManager.getLocalObjectName(officeFloorName,
				OfficeFloorManager.getOfficeFloorManagerObjectName(officeFloorName));
		return getMBeanProxy(hostName, port, trustStore, trustStorePassword, userName, password, objectName,
				OfficeFloorMBean.class);
	}

	/**
	 * <p>
	 * Obtains the {@link OfficeBuilding} {@link JMXConnectorServer}
	 * {@link JMXServiceURL}.
	 * <p>
	 * This a utility method to obtain the {@link JMXServiceURL} of an existing
	 * {@link OfficeBuilding}.
	 * 
	 * @param hostName
	 *            Name of the host where the {@link OfficeBuilding} resides.
	 *            <code>null</code> indicates localhost.
	 * @param port
	 *            Port the {@link OfficeBuilding} is residing on.
	 * @return {@link JMXServiceURL} to the {@link OfficeBuilding}
	 *         {@link JMXConnectorServer}.
	 * @throws IOException
	 *             If fails to obtain the {@link JMXServiceURL}.
	 */
	public static JMXServiceURL getOfficeBuildingJmxServiceUrl(String hostName, int port) throws IOException {

		// Ensure have the host name
		if ((hostName == null) || (hostName.trim().length() == 0)) {
			// Default to localhost (though by name to useful on the network)
			hostName = InetAddress.getLocalHost().getHostName();
		}

		// Create and return the JMX service URL
		return new JMXServiceURL("service:jmx:rmi://" + hostName + ":" + port + "/jndi/rmi://" + hostName + ":" + port
				+ "/OfficeBuilding");
	}

	/**
	 * <p>
	 * Obtains the MBean proxy.
	 * <p>
	 * This a utility method to obtain an MBean from an existing Office
	 * Building.
	 * 
	 * @param <I>
	 *            MBean interface type.
	 * @param hostName
	 *            Host where the {@link OfficeBuilding} resides.
	 * @param port
	 *            Port where the {@link OfficeBuilding} resides.
	 * @param trustStore
	 *            {@link File} containing the trusted security keys.
	 * @param trustStorePassword
	 *            Password to the trusted key store {@link File}.
	 * @param userName
	 *            User name to connect.
	 * @param password
	 *            Password to connect.
	 * @param mbeanName
	 *            {@link ObjectName} for the MBean.
	 * @param mbeanInterface
	 *            MBean interface of the MBean.
	 * @return Proxy to the MBean.
	 * @throws IOException
	 *             If fails to obtain the MBean proxy.
	 */
	public static <I> I getMBeanProxy(String hostName, int port, File trustStore, String trustStorePassword,
			String userName, String password, ObjectName mbeanName, Class<I> mbeanInterface) throws IOException {

		// Create the client socket factory
		String sslProtocol = getSslProtocol();
		String sslAlgorithm = getSslAlgorithm();
		byte[] trustStoreContent = OfficeBuildingRmiServerSocketFactory.getKeyStoreContent(trustStore);
		RMIClientSocketFactory clientSocketFactory = new OfficeBuildingRmiClientSocketFactory(sslProtocol, sslAlgorithm,
				trustStoreContent, trustStorePassword);

		// Obtain the MBean Server connection
		JMXServiceURL serviceUrl = getOfficeBuildingJmxServiceUrl(hostName, port);
		Map<String, Object> environment = new HashMap<String, Object>();
		environment.put(PROPERTY_RMI_CLIENT_SOCKET_FACTORY, clientSocketFactory);
		environment.put(JMXConnector.CREDENTIALS, new String[] { userName, password });
		JMXConnector connector = JMXConnectorFactory.connect(serviceUrl, environment);
		MBeanServerConnection connection = connector.getMBeanServerConnection();

		// Create and return the MBean proxy
		return JMX.newMBeanProxy(connection, mbeanName, mbeanInterface);
	}

	/**
	 * Deletes the directory.
	 * 
	 * @param directory
	 *            Directory to delete.
	 * @param errorMessage
	 *            Message to propagate if fials to delete the directory.
	 * @throws IOException
	 *             If fails to delete the directory.
	 */
	private static void deleteDirectory(File directory, String errorMessage) throws IOException {

		// Do nothing if directory not exists
		if (!(directory.exists())) {
			return; // not exist so no need to delete
		}

		// Delete the children of directory
		for (File child : directory.listFiles()) {
			if (child.isDirectory()) {
				// Recursively delete sub directories
				deleteDirectory(child, errorMessage);

			} else {
				// Delete the file
				if (!(child.delete())) {
					throw new IOException(errorMessage);
				}
			}
		}

		// Delete the directory
		if (!(directory.delete())) {
			throw new IOException(errorMessage);
		}
	}

	/**
	 * {@link ProcessManagerMBean} instances of currently running
	 * {@link Process} instances.
	 */
	private final List<ProcessManagerMBean> processManagers = new LinkedList<ProcessManagerMBean>();

	/**
	 * Flags if the {@link OfficeBuilding} is open.
	 */
	private boolean isOfficeBuildingOpen = true;

	/**
	 * Flags if the {@link OfficeBuilding} has been stopped.
	 */
	private volatile boolean isOfficeBuildingStopped = false;

	/**
	 * Start time of the {@link OfficeBuilding}.
	 */
	private final Date startTime;

	/**
	 * {@link OfficeBuilding} {@link JMXServiceURL}.
	 */
	private final JMXServiceURL officeBuildingServiceUrl;

	/**
	 * {@link JMXConnectorServer} for the {@link OfficeBuilding}.
	 */
	private final JMXConnectorServer connectorServer;

	/**
	 * {@link Registry} for the {@link OfficeBuilding}.
	 */
	private final Registry registry;

	/**
	 * {@link MBeanServer}.
	 */
	private final MBeanServer mbeanServer;

	/**
	 * Work space for the {@link OfficeBuilding}.
	 */
	private final File workspace;

	/**
	 * Environment {@link Properties}.
	 */
	private final Properties environment;

	/**
	 * Flag indicating whether to isolate the artifacts of the {@link Process}.
	 */
	private final boolean isIsolateProcesses;

	/**
	 * JVM options for the {@link Process}.
	 */
	private final String[] jvmOptions;

	/**
	 * Flag indicating if allow class path entries via the
	 * {@link OpenOfficeFloorConfiguration}.
	 */
	private final boolean isAllowClassPathEntries;

	/**
	 * Index of the next {@link Process}.
	 */
	private long processInstanceIndex = 0;

	/**
	 * May only create by starting.
	 * 
	 * @param startTime
	 *            Start time of the {@link OfficeBuilding}.
	 * @param officeBuildingServiceUrl
	 *            {@link OfficeBuilding} {@link JMXServiceURL}.
	 * @param connectorServer
	 *            {@link JMXConnectorServer} for the {@link OfficeBuilding}.
	 * @param registry
	 *            {@link Registry} for the {@link OfficeBuilding}.
	 * @param mbeanServer
	 *            {@link MBeanServer}.
	 * @param workspace
	 *            Work space for the {@link OfficeBuilding}.
	 * @param isIsolateProcesses
	 *            Flag indicating whether to isolate the artifacts of the
	 *            {@link Process}.
	 * @param environment
	 *            Environment {@link Properties}.
	 * @param jvmOptions
	 *            JVM options for the {@link Process}.
	 * @param isAllowClassPathEntries
	 *            Flag indicating if allow class path entries via the
	 *            {@link OpenOfficeFloorConfiguration}.
	 */
	private OfficeBuildingManager(Date startTime, JMXServiceURL officeBuildingServiceUrl,
			JMXConnectorServer connectorServer, Registry registry, MBeanServer mbeanServer, File workspace,
			boolean isIsolateProcesses, Properties environment, String[] jvmOptions, boolean isAllowClassPathEntries) {
		this.startTime = startTime;
		this.officeBuildingServiceUrl = officeBuildingServiceUrl;
		this.connectorServer = connectorServer;
		this.registry = registry;
		this.mbeanServer = mbeanServer;
		this.workspace = workspace;
		this.isIsolateProcesses = isIsolateProcesses;
		this.environment = environment;
		this.jvmOptions = (jvmOptions == null ? new String[0] : jvmOptions);
		this.isAllowClassPathEntries = isAllowClassPathEntries;
	}

	/**
	 * Obtains the instance index for the {@link Process}.
	 * 
	 * @return Instance index for the {@link Process}.
	 */
	private synchronized long getProcessInstanceIndex() {
		return ++this.processInstanceIndex;
	}

	/*
	 * ===================== OfficeBuildingManagerMBean =======================
	 */

	@Override
	public Date getStartTime() {
		return this.startTime;
	}

	@Override
	public String getOfficeBuildingJmxServiceUrl() {
		return this.officeBuildingServiceUrl.toString();
	}

	@Override
	public String getOfficeBuildingHostName() {
		return this.officeBuildingServiceUrl.getHost();
	}

	@Override
	public int getOfficeBuildingPort() {
		return this.officeBuildingServiceUrl.getPort();
	}

	@Override
	public String openOfficeFloor(String arguments) throws ProcessException {
		try {

			// Split out the arguments
			String[] argumentEntries = arguments.trim().split("\\s+");

			// Parse the parameters (always the one command)
			OfficeFloorCommandParser parser = new OfficeFloorCommandParserImpl(
					new OfficeBuildingOpenOfficeFloorCommand(false, false));
			OfficeFloorCommand[] commands = parser.parseCommands(argumentEntries);
			OfficeBuildingOpenOfficeFloorCommand command = (OfficeBuildingOpenOfficeFloorCommand) commands[0];

			// Obtain the open OfficeFloor configuration
			OpenOfficeFloorConfiguration configuration = command.getOpenOfficeFloorConfiguration();

			// Open the OfficeFloor
			return this.openOfficeFloor(configuration);

		} catch (Throwable ex) {
			throw ProcessException.propagate(ex);
		}
	}

	@Override
	public String openOfficeFloor(OpenOfficeFloorConfiguration configuration) throws ProcessException {
		try {

			// Ensure the OfficeBuilding is open
			synchronized (this) {
				if (!this.isOfficeBuildingOpen) {
					throw new IllegalStateException("OfficeBuilding closed");
				}
			}

			// Obtain the OfficeFloor name
			String officeFloorName = configuration.getOfficeFloorName();
			if ((officeFloorName == null) || (officeFloorName.trim().length() == 0)) {
				officeFloorName = "OfficeFloor";
			}

			// Obtain the process work space location
			String processWorkspaceFolderName = officeFloorName + this.getProcessInstanceIndex();
			final File processWorkspace = new File(this.workspace, processWorkspaceFolderName);

			// Ensure clean up process
			boolean isCleanup = true;
			try {

				// Create the process work space
				if (!(processWorkspace.mkdirs())) {
					// Failed to create the process workspace
					throw new IOException("Failed to create process workspace directory");
				}

				// Obtain the local repository (null will share the artifacts)
				File localRepository = null;
				if (this.isIsolateProcesses) {
					// Create isolated local repository
					localRepository = new File(processWorkspace, "repo");
					if (!(localRepository.mkdirs())) {
						// Failed to create isolated local repository
						throw new IOException("Failed to create isolated local repository");
					}
				}

				// Obtain the decorators
				OfficeFloorDecorator[] decorators = OfficeFloorDecoratorServiceLoader
						.loadOfficeFloorDecorators(Thread.currentThread().getContextClassLoader());

				// Create the context for building class path
				OfficeFloorCommandContextImpl commandContext = new OfficeFloorCommandContextImpl(processWorkspace,
						decorators);

				// Make the uploaded artifacts available on the class path
				for (UploadArtifact artifact : configuration.getUploadArtifacts()) {

					// Write the artifact to the workspace
					String fileName = artifact.getName();
					File artifactFile = new File(processWorkspace, fileName);
					FileOutputStream output = new FileOutputStream(artifactFile, false);
					output.write(artifact.getContent());
					output.close();

					// Add the uploaded artifact to the class path
					commandContext.includeClassPathEntry(artifactFile.getAbsolutePath());
				}

				// Make the class path entries available on the class path
				String[] configuredClassPathEntries = configuration.getClassPathEntries();
				if (configuredClassPathEntries.length > 0) {

					// Ensure allowed to provide class path entries
					if (!this.isAllowClassPathEntries) {
						throw new IllegalArgumentException(OfficeBuilding.class.getSimpleName()
								+ " is not allowing configured class path entries");
					}

					// Include the class path entries
					for (String configuredClassPathEntry : configuredClassPathEntries) {
						commandContext.includeClassPathEntry(configuredClassPathEntry);
					}
				}

				// Configure the process for the OfficeFloor
				ProcessConfiguration processConfig = new ProcessConfiguration();
				processConfig.setProcessName(officeFloorName);
				processConfig.setAdditionalClassPath(commandContext.getCommandClassPath());
				processConfig.setMbeanServer(this.mbeanServer);

				// Add the JVM options
				for (String jvmOption : this.jvmOptions) {
					processConfig.addJvmOption(jvmOption);
				}
				for (String jvmOption : configuration.getJvmOptions()) {
					processConfig.addJvmOption(jvmOption);
				}

				// Create the start listener
				ProcessStartListener startListener = new ProcessStartListener() {
					@Override
					public void processStarted(ProcessManagerMBean processManager) throws IOException {

						// Only register if process not already complete
						synchronized (OfficeBuildingManager.this) {
							synchronized (processManager) {
								if (!processManager.isProcessComplete()) {
									// Register manager for the running process
									OfficeBuildingManager.this.processManagers.add(processManager);
								}
							}
						}
					}
				};
				processConfig.setProcessStartListener(startListener);

				// Create the completion listener
				ProcessCompletionListener completionListener = new ProcessCompletionListener() {
					@Override
					public void processCompleted(ProcessManagerMBean manager) {

						// Unregister the process
						synchronized (OfficeBuildingManager.this) {
							// Remove manager as process no longer running
							OfficeBuildingManager.this.processManagers.remove(manager);

							// Notify that a process manager complete
							OfficeBuildingManager.this.notify();
						}

						// Clean up the process work space
						try {
							deleteDirectory(processWorkspace,
									"Failed cleaning up workspace for process " + manager.getProcessNamespace());
						} catch (IOException ex) {
							// Log warning but do no further
							LOGGER.log(Level.WARNING, ex.getMessage());
						}
					}
				};
				processConfig.setProcessCompletionListener(completionListener);

				// Create the OfficeFloor manager (process)
				String officeFloorSourceClassName = configuration.getOfficeFloorSourceClassName();
				String officeFloorLocation = configuration.getOfficeFloorLocation();
				Properties officeFloorProperties = new Properties();
				officeFloorProperties.putAll(this.environment);
				officeFloorProperties.putAll(configuration.getOfficeFloorProperties());
				OfficeFloorManager officeFloorManager = new OfficeFloorManager(officeFloorSourceClassName,
						officeFloorLocation, officeFloorProperties);

				// Determine if invoking task
				String officeName = configuration.getOfficeName();
				String functionName = configuration.getFunctionName();
				String parameter = configuration.getParameter();
				if (functionName != null) {
					officeFloorManager.addExecuteFunction(officeName, functionName, parameter);
				}

				// Open the OfficeFloor
				ProcessManager processManager = ProcessManager.startProcess(officeFloorManager, processConfig);

				// Started so allow process to clean itself
				isCleanup = false;

				// Return the process name space
				return processManager.getProcessNamespace();

			} finally {

				// Determine if clean up
				if (isCleanup) {
					try {
						deleteDirectory(processWorkspace, "Failed to clean up workspace for process");
					} catch (IOException ex) {
						// Log warning but do no further
						LOGGER.log(Level.WARNING, ex.getMessage());
					}
				}
			}

		} catch (Throwable ex) {
			throw ProcessException.propagate(ex);
		}
	}

	@Override
	public synchronized String[] listProcessNamespaces() throws ProcessException {
		try {

			// Create listing of process name spaces
			List<String> namespaces = new ArrayList<>(this.processManagers.size());
			for (ProcessManagerMBean manager : this.processManagers) {

				// Include the name space
				String namespace = manager.getProcessNamespace();
				namespaces.add(namespace);
			}

			// Return the listing of process name spaces
			return namespaces.toArray(new String[namespaces.size()]);

		} catch (Throwable ex) {
			throw ProcessException.propagate(ex);
		}
	}

	@Override
	public synchronized void closeOfficeFloor(String processNamespace, long waitTime) throws ProcessException {
		try {

			// Find the process manager for the OfficeFloor
			ProcessManagerMBean processManager = null;
			for (ProcessManagerMBean manager : this.processManagers) {
				if (manager.getProcessNamespace().equals(processNamespace)) {
					processManager = manager;
				}
			}
			if (processManager == null) {
				// OfficeFloor not running
				throw new ProcessException("OfficeFloor by process name space '" + processNamespace + "' not running");
			}

			// Close the OfficeFloor
			processManager.triggerStopProcess();

			// Wait until processes complete (or time out waiting)
			long startTime = System.currentTimeMillis();
			for (;;) {

				// Wait some time for OfficeFloor to close
				this.wait(1000);

				// Determine if OfficeFloor closed
				if (processManager.isProcessComplete()) {
					return; // OfficeFloor closed
				}

				// Determine if time out waiting
				long currentTime = System.currentTimeMillis();
				if ((currentTime - startTime) > waitTime) {
					// Timed out waiting, so destroy processes
					processManager.destroyProcess();

					// Indicate failure in closing OfficeFloor
					throw new ProcessException("Destroyed OfficeFloor '" + processNamespace
							+ "' as timed out waiting for close of " + waitTime + " milliseconds");
				}
			}

		} catch (Throwable ex) {
			throw ProcessException.propagate(ex);
		}
	}

	@Override
	public synchronized String stopOfficeBuilding(long waitTime) throws ProcessException {
		try {

			// Flag no longer open
			this.isOfficeBuildingOpen = false;

			// Status of stopping
			StringBuilder status = new StringBuilder();
			try {

				// Stop the running processes (if any)
				if (this.processManagers.size() > 0) {
					status.append("Stopping processes:\n");
					for (ProcessManagerMBean processManager : this.processManagers) {
						try {
							// Stop process, keeping track if successful
							status.append("\t" + processManager.getProcessName() + " ["
									+ processManager.getProcessNamespace() + "]");
							processManager.triggerStopProcess();
							status.append("\n");
						} catch (Throwable ex) {
							// Indicate failure in stopping process
							status.append(" failed: " + ex.getMessage() + " [" + ex.getClass().getName() + "]\n");
						}
					}
					status.append("\n");
				}

				// Wait until processes complete (or time out waiting)
				long startTime = System.currentTimeMillis();
				WAIT_FOR_COMPLETION: for (;;) {

					// Determine if all processes complete
					boolean isAllComplete = true;
					for (ProcessManagerMBean processManager : this.processManagers) {
						if (!processManager.isProcessComplete()) {
							// Process still running so not all complete
							isAllComplete = false;
						}
					}

					// No further checking if all processes complete
					if (isAllComplete) {
						break WAIT_FOR_COMPLETION;
					}

					// Determine if time out waiting
					if ((System.currentTimeMillis() - startTime) > waitTime) {
						// Timed out waiting, so destroy still running processes
						status.append("\nStop timeout, destroying processes:\n");
						for (ProcessManagerMBean processManager : this.processManagers) {

							// Ignore if already complete
							if (processManager.isProcessComplete()) {
								continue;
							}

							// Destroy the process
							try {
								status.append("\t" + processManager.getProcessName() + " ["
										+ processManager.getProcessNamespace() + "]");
								processManager.destroyProcess();
								status.append("\n");
							} catch (Throwable ex) {
								// Indicate failure in stopping process
								status.append(" failed: " + ex.getMessage() + " [" + ex.getClass().getName() + "]\n");
							}
						}
						status.append("\n");

						// Timed out so no further waiting
						break WAIT_FOR_COMPLETION;
					}

					// Wait some time for the processes to complete
					this.wait(1000);
				}

				// Return status of stopping
				status.append(OfficeBuilding.class.getSimpleName() + " stopped");
				return status.toString();

			} finally {
				// Flag the OfficeBuilding as stopped
				this.isOfficeBuildingStopped = true;

				// Stop the connector server
				this.connectorServer.stop();

				// Unregister the registry (closes its port)
				boolean isUnexported = false;
				long startTime = System.currentTimeMillis();
				do {

					// Determine if time out
					if ((System.currentTimeMillis() - startTime) > waitTime) {
						throw new ProcessException("Timed out waiting to stop OfficeBuilding");
					}

					// Attempt to unexport the object again
					try {
						isUnexported = UnicastRemoteObject.unexportObject(this.registry, false);
					} catch (NoSuchObjectException ex) {
						// Already unexported
						isUnexported = true;
					}

					// Wait some time if not unexported
					if (!isUnexported) {
						this.wait(100);
					}

				} while (!isUnexported);

				// Unregister the Office Building Manager MBean
				this.mbeanServer.unregisterMBean(OFFICE_BUILDING_MANAGER_OBJECT_NAME);

				// Notify that stopped (responsive stop spawned OfficeBuilding)
				this.notifyAll();
			}

		} catch (Throwable ex) {
			throw ProcessException.propagate(ex);
		}
	}

	@Override
	public boolean isOfficeBuildingStopped() {
		return this.isOfficeBuildingStopped;
	}

}