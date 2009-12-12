/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2009 Daniel Sagenschneider
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
package net.officefloor.building.process;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;

import net.officefloor.building.OfficeBuilding;

import mx4j.tools.remote.proxy.RemoteMBeanProxy;

/**
 * Manages a {@link Process}.
 * 
 * @author Daniel Sagenschneider
 */
public class ProcessManager implements ProcessManagerMBean {

	/**
	 * {@link ObjectName} for the {@link ProcessManagerMBean}.
	 */
	static ObjectName PROCESS_MANAGER_OBJECT_NAME;

	static {
		try {
			PROCESS_MANAGER_OBJECT_NAME = new ObjectName("process", "type",
					"ProcessManager");
		} catch (MalformedObjectNameException ex) {
			// This should never be the case
		}
	}

	/**
	 * Obtains the {@link ObjectName} for the {@link ProcessManagerMBean}.
	 * 
	 * @return {@link ObjectName} for the {@link ProcessManagerMBean}.
	 */
	public static ObjectName getProcessManagerObjectName() {
		return PROCESS_MANAGER_OBJECT_NAME;
	}

	/**
	 * Active set of MBean name spaces.
	 */
	private static final Set<String> activeMBeanNamespaces = new HashSet<String>();

	/**
	 * Starts the {@link Process} for the {@link ManagedProcess} returning the
	 * managing {@link ProcessManager}.
	 * 
	 * @param managedProcess
	 *            {@link ManagedProcess}.
	 * @param configuration
	 *            Optional {@link ProcessConfiguration}. May be
	 *            <code>null</code> to use defaults.
	 * @return {@link ProcessManager}.
	 * @throws ProcessException
	 *             If fails to start the {@link Process}.
	 */
	public static ProcessManager startProcess(ManagedProcess managedProcess,
			ProcessConfiguration configuration) throws ProcessException {

		// Ensure have configuration to prevent NPE
		if (configuration == null) {
			configuration = new ProcessConfiguration();
		}

		// Obtain the java bin directory location
		String javaHome = System.getProperty("java.home");
		File javaBinDir = new File(javaHome, "bin");
		if (!javaBinDir.isDirectory()) {
			throw new ProcessException("Can not find java bin directory at "
					+ javaBinDir.getAbsolutePath());
		}

		// Obtain the java executable
		File javaExecutable = new File(javaBinDir, "java");
		if (!javaExecutable.isFile()) {
			// Not unix/linux, so try for windows
			javaExecutable = new File(javaBinDir, "javaw.exe");
			if (!javaExecutable.isFile()) {
				// Not windows either, so can not find
				throw new ProcessException("Can not find java executable in "
						+ javaBinDir.getAbsolutePath());
			}
		}

		// Obtain the class path
		String classPath = System.getProperty("java.class.path");
		String additionalClassPath = configuration.getAdditionalClassPath();
		if (!OfficeBuilding.isBlank(additionalClassPath)) {
			classPath = classPath + File.pathSeparator + additionalClassPath;
		}

		// Add the JVM Options
		String[] jvmOptions = new String[0];
		String jvmOptionText = configuration.getJvmOptions();
		if (!OfficeBuilding.isBlank(jvmOptionText)) {
			// Split the options for the command
			jvmOptions = jvmOptionText.split(" ");
		}

		// Create the command to invoke process
		List<String> command = new LinkedList<String>();
		command.add(javaExecutable.getAbsolutePath());
		command.add("-cp");
		command.add(classPath);
		command.addAll(Arrays.asList(jvmOptions));
		command.add(ProcessShell.class.getName());

		// Obtain the process name
		String processName = configuration.getProcessName();
		if (processName == null) {
			processName = "Process";
		}

		// Obtain the unique MBean name space
		String mbeanNamespace = processName;
		synchronized (activeMBeanNamespaces) {
			int suffix = 1;
			while (activeMBeanNamespaces.contains(mbeanNamespace)) {
				suffix++;
				mbeanNamespace = processName + suffix;
			}

			// Reserve name space for the process
			activeMBeanNamespaces.add(mbeanNamespace);
		}

		try {
			// Create the from process communication pipe
			ServerSocket fromProcessServerSocket = new ServerSocket();
			fromProcessServerSocket.bind(null); // bind to any available port
			int fromProcessPort = fromProcessServerSocket.getLocalPort();

			// Invoke the process
			ProcessBuilder builder = new ProcessBuilder(command);
			Process process = builder.start();

			// Obtain the MBean Server
			MBeanServer mbeanServer = configuration.getMbeanServer();
			if (mbeanServer == null) {
				mbeanServer = ManagementFactory.getPlatformMBeanServer();
			}

			// Create the process manager for the process
			ProcessManager processManager = new ProcessManager(processName,
					mbeanNamespace, process, mbeanServer);

			// Gobble the process's stdout and stderr
			new StreamGobbler(process.getInputStream(), processManager).start();
			new StreamGobbler(process.getErrorStream(), processManager).start();

			// Handle process responses
			new ProcessNotificationHandler(processManager,
					fromProcessServerSocket).start();

			// Send managed process and response port to process
			ObjectOutputStream toProcessPipe = new ObjectOutputStream(process
					.getOutputStream());
			toProcessPipe.writeObject(managedProcess);
			toProcessPipe.writeInt(fromProcessPort);
			toProcessPipe.flush();

			try {
				// Wait until process is initialised (or complete)
				synchronized (processManager) {
					while ((!processManager.isInitialised)
							&& (!processManager.isComplete)) {
						processManager.wait(100);
					}
				}
			} catch (InterruptedException ex) {
				// Continue on as interrupted
			}

			// Return the manager for the process
			return processManager;

		} catch (IOException ex) {
			// Propagate failure
			throw new ProcessException(ex);
		}
	}

	/**
	 * Obtains the local {@link ObjectName} for the remote MBean in the
	 * {@link Process} being managed within the <code>MBean name space</code>.
	 * 
	 * @param processNamespace
	 *            Name space of the {@link Process}.
	 * @param objectName
	 *            {@link ObjectName} of the remote MBean.
	 * @return Local {@link ObjectName} for the remote MBean in the
	 *         {@link Process} being managed.
	 * @throws MalformedObjectNameException
	 *             If resulting local name is malformed.
	 */
	public static ObjectName getLocalObjectName(String processNamespace,
			ObjectName remoteObjectName) throws MalformedObjectNameException {
		return new ObjectName(processNamespace + "."
				+ remoteObjectName.getDomain(), remoteObjectName
				.getKeyPropertyList());
	}

	/**
	 * Name of the {@link Process}.
	 */
	private final String processName;

	/**
	 * MBean name space for the {@link Process}.
	 */
	private final String mbeanNamespace;

	/**
	 * {@link Process} being managed.
	 */
	private final Process process;

	/**
	 * {@link MBeanServer}.
	 */
	private final MBeanServer mbeanServer;

	/**
	 * Listing of the {@link ObjectName} instances for the registered MBeans.
	 */
	private List<ObjectName> registeredMBeanNames = new LinkedList<ObjectName>();

	/**
	 * Host name of the {@link Process} being managed. This is provided once the
	 * {@link Process} has been started.
	 */
	private String processHostName = null;

	/**
	 * Port that the {@link Registry} containing the {@link MBeanServer} for the
	 * {@link Process} being managed resides on. This is provided once the
	 * {@link Process} has been started.
	 */
	private int processPort = -1;

	/**
	 * Flag indicating if the {@link Process} has been initialised.
	 */
	private volatile boolean isInitialised = false;

	/**
	 * Flag indicating if the {@link Process} is complete.
	 */
	private volatile boolean isComplete = false;

	/**
	 * Initiate.
	 * 
	 * @param processName
	 *            Name identifying the {@link Process}.
	 * @param mbeanNamespace
	 *            MBean name space for the {@link Process}.
	 * @param process
	 *            {@link Process} being managed.
	 * @param mbeanServer
	 *            {@link MBeanServer}.
	 */
	private ProcessManager(String processName, String mbeanNamespace,
			Process process, MBeanServer mbeanServer) {
		this.processName = processName;
		this.mbeanNamespace = mbeanNamespace;
		this.process = process;
		this.mbeanServer = mbeanServer;
	}

	/**
	 * Obtains the local {@link ObjectName} for the remote MBean in the
	 * {@link Process} being managed by this {@link ProcessManager}.
	 * 
	 * @param objectName
	 *            {@link ObjectName} of the remote MBean.
	 * @return Local {@link ObjectName} for the remote MBean in the
	 *         {@link Process} being managed.
	 * @throws MalformedObjectNameException
	 *             If resulting local name is malformed.
	 */
	public ObjectName getLocalObjectName(ObjectName objectName)
			throws MalformedObjectNameException {
		return getLocalObjectName(this.mbeanNamespace, objectName);
	}

	/**
	 * Specifies the host and port of the {@link Registry} containing the
	 * {@link MBeanServer} for the {@link Process} being managed.
	 * 
	 * @param hostName
	 *            Host name.
	 * @param port
	 *            Port.
	 */
	private synchronized void setProcessHostAndPort(String hostName, int port) {
		this.processHostName = hostName;
		this.processPort = port;
	}

	/**
	 * Registers the remote MBean locally.
	 * 
	 * @param remoteMBeanName
	 *            {@link ObjectName} of the MBean in the remote
	 *            {@link MBeanServer} in the {@link Process}.
	 * @param connection
	 *            {@link MBeanServerConnection} to the
	 *            {@link JMXConnectorServer} for the {@link Process}.
	 * @throws Exception
	 *             If fails to register the MBean.
	 */
	private void registerRemoteMBeanLocally(ObjectName remoteMBeanName,
			MBeanServerConnection connection) throws Exception {
		// Register the remote MBean locally
		RemoteMBeanProxy mbeanProxy = new RemoteMBeanProxy(remoteMBeanName,
				connection);
		this.registerMBean(mbeanProxy, remoteMBeanName);
	}

	/**
	 * Registers the MBean.
	 * 
	 * @param mbean
	 *            Mbean.
	 * @param name
	 *            {@link ObjectName} for the MBean.
	 * @throws Exception
	 *             If fails to register the MBean.
	 */
	private synchronized void registerMBean(Object mbean, ObjectName name)
			throws Exception {

		// Do not register if already complete
		if (this.isComplete) {
			return;
		}

		// Register the MBean
		ObjectName localMBeanName = this.getLocalObjectName(name);
		this.mbeanServer.registerMBean(mbean, localMBeanName);

		// Keep track of the registered MBeans
		this.registeredMBeanNames.add(localMBeanName);
	}

	/**
	 * Flags the {@link Process} is initialised.
	 */
	private void flagInitialised() {

		// Flag initialised
		this.isInitialised = true;

		// Notify immediately that initialised
		synchronized (this) {
			this.notify();
		}
	}

	/**
	 * Flags the {@link Process} is complete.
	 */
	private synchronized void flagComplete() {

		// Do not run completion twice
		if (this.isComplete) {
			return;
		}

		// Unregister the MBeans
		for (ObjectName mbeanName : this.registeredMBeanNames) {
			try {
				this.mbeanServer.unregisterMBean(mbeanName);
			} catch (Throwable ex) {
				ex.printStackTrace();
			}
		}

		// Release process name space
		synchronized (activeMBeanNamespaces) {
			activeMBeanNamespaces.remove(this.mbeanNamespace);
		}

		// Flag complete
		this.isComplete = true;
	}

	/*
	 * ================== ProcessManagerMBean ======================
	 */

	@Override
	public String getProcessName() {
		return this.processName;
	}

	@Override
	public String getProcessNamespace() {
		return this.mbeanNamespace;
	}

	@Override
	public synchronized String getProcessHostName() {
		return this.processHostName;
	}

	@Override
	public synchronized int getProcessPort() {
		return this.processPort;
	}

	@Override
	public void triggerStopProcess() throws ProcessException {

		// Ignore if already complete
		if (this.isComplete) {
			return;
		}

		try {
			// Obtain the process shell MBean name
			ObjectName name = this
					.getLocalObjectName(ProcessShell.PROCESS_SHELL_OBJECT_NAME);

			// Trigger stopping the process
			this.mbeanServer.invoke(name, "triggerStopProcess", null, null);

		} catch (Exception ex) {
			// Propagate failure
			throw new ProcessException(ex);
		}
	}

	@Override
	public void destroyProcess() {
		this.process.destroy();
	}

	@Override
	public boolean isProcessComplete() {
		// Return whether process complete
		return this.isComplete;
	}

	/**
	 * Handler for the notifications from the {@link Process}.
	 */
	private static class ProcessNotificationHandler extends Thread {

		/**
		 * {@link ProcessManager}.
		 */
		private final ProcessManager processManager;

		/**
		 * {@link ServerSocket} to receive {@link ProcessResponse} instances.
		 */
		private final ServerSocket fromProcessServerSocket;

		/**
		 * {@link MBeanServerConnection} to the {@link ManagedProcess}
		 * {@link MBeanServer}. This is lazy created as needed.
		 */
		private MBeanServerConnection connection = null;

		/**
		 * Initiate.
		 * 
		 * @param processManager
		 *            {@link ProcessManager}.
		 * @param fromProcessServerSocket
		 *            {@link ServerSocket} to receive {@link ProcessResponse}
		 *            instances.
		 */
		public ProcessNotificationHandler(ProcessManager processManager,
				ServerSocket fromProcessServerSocket) {
			this.processManager = processManager;
			this.fromProcessServerSocket = fromProcessServerSocket;

			// Flag as daemon (should not stop process finishing)
			this.setDaemon(true);
		}

		/*
		 * ================= Thread ======================
		 */

		@Override
		public void run() {
			try {
				// Accept only the first connection (from process)
				Socket socket = this.fromProcessServerSocket.accept();

				// Register the Process Manager MBean
				this.processManager.registerMBean(this.processManager,
						ProcessManager.PROCESS_MANAGER_OBJECT_NAME);

				// Obtain pipe from process
				ObjectInputStream fromProcessPipe = new ObjectInputStream(
						socket.getInputStream());

				// Loop while still processing
				while (!this.processManager.isProcessComplete()) {

					// Read in the notification
					Object object = fromProcessPipe.readObject();
					if (!(object instanceof MBeanRegistrationNotification)) {
						throw new IllegalArgumentException("Unknown response: "
								+ object
								+ " ["
								+ (object == null ? "null" : object.getClass()
										.getName()));
					}
					MBeanRegistrationNotification registration = (MBeanRegistrationNotification) object;

					// Lazy create the JMX connector.
					// Must be lazy created as will be listening now.
					if (this.connection == null) {
						// Obtain the service URL for the process
						JMXServiceURL processServiceUrl = registration
								.getServiceUrl();

						// Specify the host and port
						this.processManager.setProcessHostAndPort(
								processServiceUrl.getHost(), processServiceUrl
										.getPort());

						// Connect to the process
						JMXConnector connector;
						try {
							connector = JMXConnectorFactory
									.connect(processServiceUrl);
						} catch (Exception ex) {
							// Likely that process completed quickly.
							// Wait some time to detect process completion.
							Thread.sleep(100);

							// Check if process is complete
							if (this.processManager.isProcessComplete()) {
								// Process complete, so stop
								return;
							} else {
								// Propagate the failure
								throw ex;
							}
						}
						this.connection = connector.getMBeanServerConnection();
					}

					// Register the remote MBean locally
					ObjectName remoteMBeanName = registration.getMBeanName();
					this.processManager.registerRemoteMBeanLocally(
							remoteMBeanName, connection);

					// Determine if process shell MBean.
					// Must be done after registering to ensure available.
					if (ProcessShell.PROCESS_SHELL_OBJECT_NAME
							.equals(remoteMBeanName)) {
						// Have process shell MBean so now initialised
						this.processManager.flagInitialised();
					}
				}
			} catch (EOFException ex) {
				// Process completed
				this.processManager.flagComplete();

			} catch (Throwable ex) {
				// Indicate failure
				ex.printStackTrace();

			} finally {
				// Close the socket as no longer listening
				try {
					this.fromProcessServerSocket.close();
				} catch (Throwable ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	/**
	 * Gobbles the Stream.
	 */
	private static class StreamGobbler extends Thread {

		/**
		 * {@link InputStream} to gobble.
		 */
		private final InputStream stream;

		/**
		 * {@link ProcessManager} to flag if complete. May be <code>null</code>.
		 */
		private final ProcessManager processManager;

		/**
		 * Initiate.
		 * 
		 * @param stream
		 *            {@link InputStream} to gobble.
		 * @param processManager
		 *            {@link ProcessManager} to flag if complete. May be
		 *            <code>null</code>.
		 */
		public StreamGobbler(InputStream stream, ProcessManager processManager) {
			this.stream = stream;
			this.processManager = processManager;

			// Flag as deamon (should not stop process finishing)
			this.setDaemon(true);
		}

		/*
		 * ================= Thread ======================
		 */

		@Override
		public void run() {
			try {
				// Consume from stream until EOF
				for (int value = this.stream.read(); value != -1; value = this.stream
						.read()) {
					System.out.write(value);
				}
			} catch (Throwable ex) {
				// Provide stack trace and exit
				ex.printStackTrace();
			} finally {
				// Flag process is complete
				this.processManager.flagComplete();
			}
		}
	}

}