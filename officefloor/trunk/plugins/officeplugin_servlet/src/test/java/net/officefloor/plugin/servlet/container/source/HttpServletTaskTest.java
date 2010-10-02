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
package net.officefloor.plugin.servlet.container.source;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.officefloor.frame.api.build.None;
import net.officefloor.frame.api.execute.TaskContext;
import net.officefloor.frame.api.manage.Office;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.plugin.servlet.container.source.HttpServletTask.DependencyKeys;
import net.officefloor.plugin.servlet.context.OfficeServletContext;
import net.officefloor.plugin.socket.server.http.HttpRequest;
import net.officefloor.plugin.socket.server.http.HttpResponse;
import net.officefloor.plugin.socket.server.http.ServerHttpConnection;
import net.officefloor.plugin.socket.server.http.security.HttpSecurity;
import net.officefloor.plugin.socket.server.http.session.HttpSession;
import net.officefloor.plugin.stream.OutputBufferStream;

/**
 * Tests the {@link HttpServletTask}.
 * 
 * @author Daniel Sagenschneider
 */
public class HttpServletTaskTest extends OfficeFrameTestCase {

	/**
	 * {@link ServletContext} path.
	 */
	private static final String CONTEXT_PATH = "/context/path";

	/**
	 * {@link HttpServlet} name.
	 */
	private static final String SERVLET_NAME = "ServletName";

	/**
	 * {@link HttpServlet} path.
	 */
	private static final String SERVLET_PATH = "/servlet/path";

	/**
	 * {@link HttpServlet}.
	 */
	private final MockHttpServlet servlet = new MockHttpServlet();

	/**
	 * Initialisation parameters.
	 */
	private final Map<String, String> initParameters = new HashMap<String, String>();

	/**
	 * {@link TaskContext}.
	 */
	@SuppressWarnings("unchecked")
	private final TaskContext<HttpServletTask, DependencyKeys, None> taskContext = this
			.createMock(TaskContext.class);

	/**
	 * {@link OfficeServletContext}.
	 */
	private final OfficeServletContext officeServletContext = this
			.createMock(OfficeServletContext.class);

	/**
	 * {@link Office}.
	 */
	private final Office office = this.createMock(Office.class);

	/**
	 * {@link ServerHttpConnection}.
	 */
	private final ServerHttpConnection connection = this
			.createMock(ServerHttpConnection.class);

	/**
	 * {@link HttpRequest} attributes.
	 */
	private final Map<String, Object> attributes = new HashMap<String, Object>();

	/**
	 * {@link HttpSecurity}.
	 */
	private final HttpSecurity security = this.createMock(HttpSecurity.class);

	/**
	 * {@link HttpSession}.
	 */
	private final HttpSession session = this.createMock(HttpSession.class);

	/**
	 * {@link HttpRequest}.
	 */
	private final HttpRequest request = this.createMock(HttpRequest.class);

	/**
	 * {@link HttpResponse}.
	 */
	private final HttpResponse response = this.createMock(HttpResponse.class);

	/**
	 * {@link HttpServletTask} to test.
	 */
	private HttpServletTask task;

	@Override
	protected void setUp() throws Exception {
		// Setup the work factory (and task factory)
		HttpServletTask factory = new HttpServletTask(SERVLET_NAME,
				SERVLET_PATH, this.servlet, this.initParameters);
		factory.setOffice(this.office);

		// Create the task
		HttpServletTask work = factory.createWork();
		this.task = (HttpServletTask) factory.createTask(work);
	}

	/**
	 * Ensure can service the {@link HttpRequest} with the {@link HttpServlet}.
	 */
	public void testService() throws Throwable {

		// Record obtain context for initialising Servlet
		this.recordReturn(this.taskContext, this.taskContext
				.getObject(DependencyKeys.OFFICE_SERVLET_CONTEXT),
				this.officeServletContext);
		this.record_service();

		// Test
		this.replayMockObjects();
		this.task.doTask(this.taskContext);
		this.verifyMockObjects();

		// Ensure service is invoked
		assertTrue("HttpServlet should service", this.servlet.isServiceInvoked);
	}

	/**
	 * Ensure can service the {@link HttpRequest} by re-using the
	 * {@link HttpServlet}.
	 */
	public void testServiceAgain() throws Throwable {

		// Record servicing twice (second time without init)
		this.recordReturn(this.taskContext, this.taskContext
				.getObject(DependencyKeys.OFFICE_SERVLET_CONTEXT),
				this.officeServletContext);
		this.record_service();
		this.record_service();

		// Test
		this.replayMockObjects();
		this.task.doTask(this.taskContext);
		this.task.doTask(this.taskContext);
		this.verifyMockObjects();

		// Ensure service is invoked
		assertTrue("HttpServlet should service", this.servlet.isServiceInvoked);
	}

	/**
	 * Records servicing the {@link HttpRequest}.
	 */
	private void record_service() {

		final OutputBufferStream bufferStream = this
				.createMock(OutputBufferStream.class);
		final OutputStream outputStream = new ByteArrayOutputStream();

		// Record sourcing the dependencies for servicing the request
		this.recordReturn(this.taskContext, this.taskContext
				.getObject(DependencyKeys.HTTP_CONNECTION), this.connection);
		this.recordReturn(this.taskContext, this.taskContext
				.getObject(DependencyKeys.REQUEST_ATTRIBUTES), this.attributes);
		this.recordReturn(this.taskContext, this.taskContext
				.getObject(DependencyKeys.HTTP_SESSION), this.session);
		this.recordReturn(this.taskContext, this.taskContext
				.getObject(DependencyKeys.HTTP_SECURITY), this.security);

		// Load last access time
		this.attributes.put("#HttpServlet.LastAccessTime#", new Long(10));

		// Record obtaining the request and responses
		this.recordReturn(this.session, this.session.getTokenName(),
				"JSESSION_ID");
		this.recordReturn(this.connection, this.connection.getHttpRequest(),
				this.request);
		this.recordReturn(this.connection, this.connection.getHttpResponse(),
				this.response);
		this.recordReturn(this.request, this.request.getRequestURI(),
				"http://www.officefloor.net");
		this.recordReturn(this.request, this.request.getMethod(), "GET");
		this.recordReturn(this.response, this.response.getBody(), bufferStream);
		this.recordReturn(bufferStream, bufferStream.getOutputStream(),
				outputStream);

		// Record obtain context path
		this.recordReturn(this.officeServletContext, this.officeServletContext
				.getContextPath(this.office), CONTEXT_PATH);
	}

	/**
	 * Mock {@link HttpServlet} for testing.
	 */
	private class MockHttpServlet extends HttpServlet {

		/**
		 * Indicates if the <code>service</code> method is invoke.
		 */
		public boolean isServiceInvoked = false;

		/*
		 * ================= HttpServlet =========================
		 */

		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

			// Ensure correct context path
			assertEquals("Incorrect context path", CONTEXT_PATH, req
					.getContextPath());

			// Ensure correct servlet path
			assertEquals("Incorrect Servlet path", SERVLET_PATH, req
					.getServletPath());

			// Flag invoked
			this.isServiceInvoked = true;
		}
	}

}