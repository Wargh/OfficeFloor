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
package net.officefloor.plugin.work.http.html.form;

import java.io.ByteArrayInputStream;

import net.officefloor.compile.spi.work.source.TaskTypeBuilder;
import net.officefloor.compile.spi.work.source.WorkTypeBuilder;
import net.officefloor.compile.test.work.WorkLoaderUtil;
import net.officefloor.compile.work.WorkType;
import net.officefloor.frame.api.build.None;
import net.officefloor.frame.api.execute.TaskContext;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.plugin.socket.server.http.HttpRequest;
import net.officefloor.plugin.socket.server.http.ServerHttpConnection;
import net.officefloor.plugin.stream.InputBufferStream;
import net.officefloor.plugin.work.http.HttpException;
import net.officefloor.plugin.work.http.html.form.HttpHtmlFormToBeanTask.HttpHtmlFormToBeanTaskDependencies;

/**
 * Tests the {@link HttpHtmlFormToBeanWorkSource}.
 *
 * @author Daniel Sagenschneider
 */
public class HttpHtmlFormToBeanWorkSourceTest extends OfficeFrameTestCase {

	/**
	 * Validates specification.
	 */
	public void testSpecification() {
		WorkLoaderUtil.validateSpecification(
				HttpHtmlFormToBeanWorkSource.class,
				HttpHtmlFormToBeanWorkSource.BEAN_CLASS_PROPERTY, "Bean Class");
	}

	/**
	 * Validates correctly loaded {@link WorkType}.
	 */
	public void testLoadWork() throws Exception {

		// Build the expected work
		HttpHtmlFormToBeanTask workTaskFactory = new HttpHtmlFormToBeanTask(
				Object.class, null);
		WorkTypeBuilder<HttpHtmlFormToBeanTask> work = WorkLoaderUtil
				.createWorkTypeBuilder(workTaskFactory);
		TaskTypeBuilder<HttpHtmlFormToBeanTaskDependencies, None> task = work
				.addTaskType("MapFormToBean", workTaskFactory,
						HttpHtmlFormToBeanTaskDependencies.class, None.class);
		task.addObject(ServerHttpConnection.class).setKey(
				HttpHtmlFormToBeanTaskDependencies.SERVER_HTTP_CONNECTION);
		task.addEscalation(HttpException.class);
		task.addEscalation(BeanMapException.class);

		// Verify work
		WorkLoaderUtil.validateWorkType(work,
				HttpHtmlFormToBeanWorkSource.class,
				HttpHtmlFormToBeanWorkSource.BEAN_CLASS_PROPERTY,
				HtmlFormBean.class.getName());
	}

	/**
	 * Ensures that alias mappings loaded.
	 */
	@SuppressWarnings("unchecked")
	public void testAliasLoaded() throws Throwable {

		final TaskContext context = this.createMock(TaskContext.class);
		final ServerHttpConnection connection = this
				.createMock(ServerHttpConnection.class);
		final HttpRequest request = this.createMock(HttpRequest.class);
		final InputBufferStream bodyInputBufferStream = this
				.createMock(InputBufferStream.class);
		final String VALUE = "VALUE";

		// Load the work
		WorkType<HttpHtmlFormToBeanTask> work = WorkLoaderUtil.loadWorkType(
				HttpHtmlFormToBeanWorkSource.class,
				HttpHtmlFormToBeanWorkSource.BEAN_CLASS_PROPERTY,
				HtmlFormBean.class.getName(),
				HttpHtmlFormToBeanWorkSource.ALIAS_PROPERTY_PREFIX + "another",
				"name");

		// Obtain the work (which is also the task)
		HttpHtmlFormToBeanTask task = work.getWorkFactory().createWork();

		// Record mapping value via alias onto the bean
		this
				.recordReturn(
						context,
						context
								.getObject(HttpHtmlFormToBeanTaskDependencies.SERVER_HTTP_CONNECTION),
						connection);
		this.recordReturn(connection, connection.getHttpRequest(), request);
		this.recordReturn(request, request.getMethod(), "GET");
		this.recordReturn(request, request.getRequestURI(), "path?another="
				+ VALUE);
		this.recordReturn(request, request.getBody(), bodyInputBufferStream);
		this.recordReturn(bodyInputBufferStream, bodyInputBufferStream
				.getInputStream(), new ByteArrayInputStream(new byte[0]));

		// Run the task
		this.replayMockObjects();
		HtmlFormBean bean = (HtmlFormBean) task.doTask(context);
		this.verifyMockObjects();

		// Verify the value loaded
		assertEquals("Expect to have alias mapping", VALUE, bean.name);
	}

}