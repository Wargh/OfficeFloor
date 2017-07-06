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
package net.officefloor.plugin.socket.server.impl;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.officefloor.frame.api.build.Indexed;
import net.officefloor.frame.api.build.None;
import net.officefloor.frame.api.managedobject.ManagedObject;
import net.officefloor.frame.api.managedobject.source.ManagedObjectExecuteContext;
import net.officefloor.frame.api.managedobject.source.ManagedObjectFunctionBuilder;
import net.officefloor.frame.api.managedobject.source.ManagedObjectSource;
import net.officefloor.frame.api.managedobject.source.ManagedObjectSourceContext;
import net.officefloor.frame.api.managedobject.source.impl.AbstractManagedObjectSource;
import net.officefloor.frame.api.source.SourceProperties;
import net.officefloor.plugin.socket.server.SocketManager;
import net.officefloor.plugin.socket.server.impl.SocketListener.SocketListenerFlows;
import net.officefloor.plugin.socket.server.protocol.CommunicationProtocol;
import net.officefloor.plugin.socket.server.protocol.CommunicationProtocolContext;
import net.officefloor.plugin.socket.server.protocol.CommunicationProtocolSource;
import net.officefloor.plugin.socket.server.protocol.Connection;

/**
 * Abstract {@link ManagedObjectSource} for a {@link ServerSocketChannel}.
 * 
 * @author Daniel Sagenschneider
 */
public abstract class AbstractServerSocketManagedObjectSource extends AbstractManagedObjectSource<None, Indexed>
		implements CommunicationProtocolContext {

	/**
	 * {@link Logger}.
	 */
	private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

	/**
	 * Port property name.
	 */
	public static final String PROPERTY_PORT = "port";

	/**
	 * Name of property to obtain the send buffer size.
	 */
	public static final String PROPERTY_SEND_BUFFER_SIZE = "send.buffer.size";

	/**
	 * Name of property to obtain the receive buffer size.
	 */
	public static final String PROPERTY_RECEIVE_BUFFER_SIZE = "receive.buffer.size";

	/**
	 * Name of property to obtain the default {@link Charset}.
	 */
	public static final String PROPERTY_DEFAULT_CHARSET = "default.charset";

	/**
	 * Name of property to specify the number of {@link SocketListener}
	 * instances. If not specified, will default to
	 * {@link Runtime#availableProcessors()}.
	 */
	public static final String PROPERTY_SOCKET_LISTENER_COUNT = "socket.listener.count";

	/**
	 * Default {@link Charset} to use if one is not configured.
	 */
	public static final String DEFAULT_CHARSET = "UTF-8";

	/**
	 * Singleton {@link SocketManager} for all {@link Connection} instances.
	 */
	private static SocketManager singletonSocketManager;

	/**
	 * Registered {@link AbstractServerSocketManagedObjectSource} instances.
	 */
	private static Set<AbstractServerSocketManagedObjectSource> registeredServerSocketManagedObjectSources = new HashSet<AbstractServerSocketManagedObjectSource>();

	/**
	 * Obtains the {@link Charset}.
	 * 
	 * @param properties
	 *            {@link SourceProperties}. May be <code>null</code> if no
	 *            {@link SourceProperties} are available.
	 * @return {@link Charset}.
	 */
	public static Charset getCharset(SourceProperties properties) {

		// Attempt to obtain configured charset
		String charsetName = (properties == null) ? null : properties.getProperty(PROPERTY_DEFAULT_CHARSET, null);
		if (charsetName != null) {
			// Use the configured charset
			return Charset.forName(charsetName);

		} else {
			try {
				// Not configured, use default for flexibility
				return Charset.forName(DEFAULT_CHARSET);
			} catch (IllegalCharsetNameException ex) {
				// Fall back to system default
				return Charset.defaultCharset();
			}
		}
	}

	/**
	 * Obtains the {@link SocketManager}.
	 * 
	 * @param mosContext
	 *            {@link ManagedObjectSourceContext}.
	 * @param instance
	 *            Instance of the
	 *            {@link AbstractServerSocketManagedObjectSource} using the
	 *            {@link SocketManager}.
	 * @param numberOfSocketListeners
	 *            Number of {@link SocketListener} instances.
	 * @param heartBeatInterval
	 *            Heart beat interval in milliseconds.
	 * @param sendBufferSize
	 *            Send buffer size.
	 * @param receiveBufferSize
	 *            Receive buffer size.
	 * @return {@link SocketManager}.
	 */
	private static synchronized SocketManager getSocketManager(ManagedObjectSourceContext<Indexed> mosContext,
			AbstractServerSocketManagedObjectSource instance, int numberOfSocketListeners, long heartBeatInterval,
			int sendBufferSize, int receiveBufferSize) {

		// Ensure consistent interface for teams
		mosContext.addManagedFunction("consistency", () -> (context) -> null).setResponsibleTeam("listener");

		// Do nothing if just loading type
		if (mosContext.isLoadingType()) {
			return null;
		}

		// Lazy create the singleton socket manager
		if (singletonSocketManager == null) {

			// Create the array of socket listeners
			SocketListener[] socketListeners = new SocketListener[numberOfSocketListeners];

			// Create the connection manager
			singletonSocketManager = new SocketManagerImpl(socketListeners);

			// Load the socket listeners
			for (int i = 0; i < socketListeners.length; i++) {

				// Create the socket listener
				SocketListener socketListener = new SocketListener(singletonSocketManager, receiveBufferSize,
						sendBufferSize, heartBeatInterval);

				// Register the socket listener
				socketListeners[i] = socketListener;

				// Register the listening of connections
				String listenerName = "listener-" + i;
				ManagedObjectFunctionBuilder<None, SocketListenerFlows> listenerFunction = mosContext
						.addManagedFunction(listenerName, socketListener);
				listenerFunction.setResponsibleTeam("listener");
				listenerFunction.linkFlow(SocketListenerFlows.REPEAT, listenerName, null, false);

				// Flag to start listener on server start up
				mosContext.addStartupFunction(listenerName);
			}
		}

		// Register the instance for use of the connection manager
		registeredServerSocketManagedObjectSources.add(instance);

		// Return the singleton connection manager
		return singletonSocketManager;
	}

	/**
	 * <p>
	 * Closes the possible open {@link SocketManager} and releases all
	 * {@link Selector} instances for the {@link SocketListener} instances.
	 * <p>
	 * Made public so that tests may use to close.
	 * 
	 * @throws IOException
	 *             If fails to close the {@link SocketManager}.
	 */
	public static synchronized void closeSocketManager() throws IOException {

		// Clear all registered server sockets
		registeredServerSocketManagedObjectSources.clear();

		// Determine if active connection manager
		if (singletonSocketManager != null) {
			// Close the socket listener selectors
			singletonSocketManager.closeSocketSelectors();

			// Wait for close
			singletonSocketManager.waitForClose();
		}

		// Close (release) the connection manager to create again
		singletonSocketManager = null;
	}

	/**
	 * <p>
	 * Releases the {@link AbstractServerSocketManagedObjectSource} instance
	 * from the {@link SocketManager}.
	 * <p>
	 * Once all {@link AbstractServerSocketManagedObjectSource} instances are
	 * release, the {@link SocketManager} itself is sclosed.
	 * 
	 * @param instance
	 *            {@link AbstractServerSocketManagedObjectSource}.
	 * @throws IOException
	 *             If fails to close the {@link SocketManager}.
	 */
	private static synchronized void releaseFromSocketManager(AbstractServerSocketManagedObjectSource instance)
			throws IOException {

		// Unregister from the connection manager
		registeredServerSocketManagedObjectSources.remove(instance);

		// Close connection manager if no further server sockets
		if (registeredServerSocketManagedObjectSources.size() == 0) {
			closeSocketManager();
		}
	}

	/**
	 * Port.
	 */
	private int port;

	/**
	 * {@link CommunicationProtocolSource}.
	 */
	private final CommunicationProtocolSource communicationProtocolSource;

	/**
	 * Send buffer size.
	 */
	private int sendBufferSize;

	/**
	 * Default {@link Charset}.
	 */
	private Charset defaultCharset;

	/**
	 * {@link ServerSocketChannel} backlog size.
	 */
	private int serverSocketBackLogSize;

	/**
	 * {@link SocketManager}.
	 */
	private SocketManager socketManager;

	/**
	 * {@link CommunicationProtocol}.
	 */
	private CommunicationProtocol communicationProtocol;

	/**
	 * Initiate.
	 */
	public AbstractServerSocketManagedObjectSource() {

		// Create the communication protocol source
		this.communicationProtocolSource = this.createCommunicationProtocolSource();
	}

	/**
	 * Creates the {@link CommunicationProtocolSource}.
	 * 
	 * @return {@link CommunicationProtocolSource}.
	 */
	protected abstract CommunicationProtocolSource createCommunicationProtocolSource();

	/*
	 * ====================== CommunicationProtocolContext =====================
	 */

	@Override
	public int getSendBufferSize() {
		return this.sendBufferSize;
	}

	@Override
	public Charset getDefaultCharset() {
		return this.defaultCharset;
	}

	/*
	 * =================== AbstractManagedObjectSource ==================
	 */

	@Override
	protected void loadSpecification(SpecificationContext context) {
		// Add required properties for Server Socket
		context.addProperty(PROPERTY_PORT);

		// Add communication protocol required properties
		this.communicationProtocolSource.loadSpecification(context);
	}

	@Override
	protected void loadMetaData(MetaDataContext<None, Indexed> context) throws Exception {

		// Obtain the managed object source context
		ManagedObjectSourceContext<Indexed> mosContext = context.getManagedObjectSourceContext();

		// Create socket to obtain operating system details
		Socket socket = new Socket();
		int osSendBufferSize = socket.getSendBufferSize();
		int osReceiveBufferSize = socket.getReceiveBufferSize();
		socket.close();

		// Obtain the configuration
		this.port = Integer.parseInt(mosContext.getProperty(PROPERTY_PORT));
		this.sendBufferSize = Integer
				.parseInt(mosContext.getProperty(PROPERTY_SEND_BUFFER_SIZE, String.valueOf(osSendBufferSize)));
		int receiveBufferSize = Integer
				.parseInt(mosContext.getProperty(PROPERTY_RECEIVE_BUFFER_SIZE, String.valueOf(osReceiveBufferSize)));

		// Obtain the default charset
		this.defaultCharset = getCharset(mosContext);

		// Obtain the number of socket listeners
		int numberOfSocketListeners = Integer.parseInt(mosContext.getProperty(PROPERTY_SOCKET_LISTENER_COUNT,
				String.valueOf(Runtime.getRuntime().availableProcessors())));

		// Obtain the server socket backlog
		this.serverSocketBackLogSize = 25000; // TODO make configurable

		// Obtain the heart beat interval
		long heartBeatInterval = 10000; // TODO make configurable

		// Obtain the connection manager
		this.socketManager = getSocketManager(mosContext, this, numberOfSocketListeners, heartBeatInterval,
				this.sendBufferSize, receiveBufferSize);

		// Create the communication protocol
		this.communicationProtocol = this.communicationProtocolSource.createCommunicationProtocol(context, this);
	}

	@Override
	public void start(ManagedObjectExecuteContext<Indexed> context) throws Exception {

		// Open selectors for socket listeners
		this.socketManager.openSocketSelectors();

		// Bind server socket for this managed object
		this.socketManager.bindServerSocket(this.port, this.serverSocketBackLogSize, this.communicationProtocol,
				context);
	}

	@Override
	protected ManagedObject getManagedObject() throws Throwable {
		// Should never be directly used by a function
		throw new IllegalStateException("Can not source managed object from a " + this.getClass().getSimpleName());
	}

	@Override
	public void stop() {

		// Release connection manager (closes socket listeners when appropriate)
		try {
			releaseFromSocketManager(this);

		} catch (IOException ex) {
			// Shutting down so just log issue
			if (LOGGER.isLoggable(Level.INFO)) {
				LOGGER.log(Level.INFO, "Failed to release " + SocketManager.class.getSimpleName(), ex);
			}
		}
	}

}