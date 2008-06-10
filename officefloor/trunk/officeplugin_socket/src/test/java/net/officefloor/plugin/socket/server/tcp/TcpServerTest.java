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
package net.officefloor.plugin.socket.server.tcp;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import net.officefloor.frame.api.build.BuildException;
import net.officefloor.frame.api.build.ManagedObjectBuilder;
import net.officefloor.frame.api.build.ManagedObjectHandlerBuilder;
import net.officefloor.frame.api.build.OfficeBuilder;
import net.officefloor.frame.api.build.OfficeEnhancer;
import net.officefloor.frame.api.build.OfficeEnhancerContext;
import net.officefloor.frame.api.manage.OfficeFloor;
import net.officefloor.frame.impl.spi.team.OnePersonTeam;
import net.officefloor.frame.impl.spi.team.PassiveTeam;
import net.officefloor.frame.impl.spi.team.WorkerPerTaskTeam;
import net.officefloor.frame.internal.structure.FlowInstigationStrategyEnum;
import net.officefloor.frame.test.AbstractOfficeConstructTestCase;
import net.officefloor.frame.test.ReflectiveWorkBuilder;
import net.officefloor.frame.test.ReflectiveWorkBuilder.ReflectiveTaskBuilder;
import net.officefloor.plugin.impl.socket.server.ServerSocketHandlerEnum;

/**
 * Tests the {@link TcpServerSocketManagedObjectSource}.
 * 
 * @author Daniel
 */
public class TcpServerTest extends AbstractOfficeConstructTestCase {

	/**
	 * Starting port number.
	 */
	public static int portStart = 12346;

	/**
	 * Port number to use for testing.
	 */
	public static int PORT;

	/**
	 * {@link OfficeFloor}.
	 */
	private OfficeFloor officeFloor;

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.officefloor.frame.test.AbstractOfficeConstructTestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// Specify the port
		PORT = portStart;
		portStart++; // increment for next test

		// Register the Server Socket Managed Object
		ManagedObjectBuilder<?> serverSocketBuilder = this
				.constructManagedObject("MO",
						TcpServerSocketManagedObjectSource.class, "OFFICE");
		serverSocketBuilder.addProperty("port", String.valueOf(PORT));
		serverSocketBuilder.addProperty("buffer_size", "1024");
		serverSocketBuilder.addProperty("message_size", "3");
		serverSocketBuilder.setDefaultTimeout(3000);

		// Register the necessary teams for socket listening
		this.constructTeam("ACCEPTER_TEAM", new OnePersonTeam(100));
		this.constructTeam("LISTENER_TEAM", new WorkerPerTaskTeam("Listener"));
		this.constructTeam("CLEANUP_TEAM", new PassiveTeam());
		OfficeBuilder officeBuilder = this.getOfficeBuilder();
		officeBuilder.registerTeam("of-MO.serversocket." + PORT
				+ ".Accepter.TEAM", "of-ACCEPTER_TEAM");
		officeBuilder.registerTeam("of-MO.serversocket." + PORT
				+ ".Listener.TEAM", "of-LISTENER_TEAM");
		officeBuilder.registerTeam("of-MO.tcp.connection.cleanup",
				"of-CLEANUP_TEAM");

		// Provide the process managed object to the office
		officeBuilder.addProcessManagedObject("MO", "MO");

		// Register team to do the work
		this.constructTeam("WORKER", new OnePersonTeam(100));

		// Register the work to process messages
		ReflectiveWorkBuilder workBuilder = this.constructWork(
				new MessageWork(), "servicer", "service");
		ReflectiveTaskBuilder taskBuilder = workBuilder.buildTask("service",
				"WORKER");
		taskBuilder.buildObject("P-MO", "MO");
		taskBuilder
				.buildFlow("service", FlowInstigationStrategyEnum.SEQUENTIAL);

		// Link handler to task
		officeBuilder.addOfficeEnhancer(new OfficeEnhancer() {
			@Override
			public void enhanceOffice(OfficeEnhancerContext context)
					throws BuildException {
				// Obtain the managed object handler builder
				ManagedObjectHandlerBuilder<ServerSocketHandlerEnum> handlerBuilder = context
						.getManagedObjectHandlerBuilder("of-MO",
								ServerSocketHandlerEnum.class);

				// Link in the handler task
				handlerBuilder.registerHandler(
						ServerSocketHandlerEnum.SERVER_SOCKET_HANDLER)
						.linkProcess(0, "servicer", "service");
			}
		});

		// Create the Office Floor
		this.officeFloor = this.constructOfficeFloor("OFFICE");

		// Open the Office Floor
		this.officeFloor.openOfficeFloor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.officefloor.frame.test.AbstractOfficeConstructTestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {

		// Close the office
		this.officeFloor.closeOfficeFloor();

		// Clean up
		super.tearDown();
	}

	/**
	 * Ensure able to answer a request.
	 */
	public void testSingleRequest() throws Exception {
		this.doRequests(1, true);
	}

	/**
	 * Ensures able to handle multiple requests.
	 */
	public void testMultipleRequests() throws Exception {
		this.doRequests(2, true);
	}

	/**
	 * Do the requests.
	 * 
	 * @param numberOfRequests
	 *            Number of requests to make.
	 * @param isLog
	 *            Flag indicating whether to log details.
	 */
	private void doRequests(int numberOfRequests, boolean isLog)
			throws Exception {

		// Open socket to Office
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), PORT),
				100);

		// Obtain the streams
		OutputStream outputStream = socket.getOutputStream();
		InputStream inputStream = socket.getInputStream();

		// Loop sending the messages
		for (int i = 0; i < numberOfRequests; i++) {

			// Obtain the index to send
			int requestIndex = (i % Messages.getSize());

			// Send the index
			long startTime = System.currentTimeMillis();
			outputStream.write(new byte[] { (byte) requestIndex });
			outputStream.flush();

			// Read a response (ends with 0 byte)
			ByteArrayOutputStream response = new ByteArrayOutputStream();
			for (int value; (value = inputStream.read()) != -1;) {
				if (value == 0) {
					break;
				}
				response.write(value);
			}
			long endTime = System.currentTimeMillis();
			String responseText = new String(response.toByteArray());
			if (isLog) {
				System.out.println("Message [" + requestIndex
						+ "] processed in " + (endTime - startTime)
						+ " milli-seconds (returned response '" + responseText
						+ "')");
			}

			// Ensure response correct
			assertEquals("Incorrect response", Messages
					.getMessage(requestIndex), responseText);
		}

		// Send finished
		long startTime = System.currentTimeMillis();
		outputStream.write(new byte[] { -1 });
		outputStream.flush();

		// Ensure no further data (and closed connection)
		assertEquals("Connection should be closed", -1, inputStream.read());

		// Indicate time to close connection
		long endTime = System.currentTimeMillis();
		System.out.println("Message [-1] processed in " + (endTime - startTime)
				+ " milli-seconds");
	}

}
