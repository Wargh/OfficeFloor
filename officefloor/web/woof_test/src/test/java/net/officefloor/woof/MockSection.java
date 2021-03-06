/*-
 * #%L
 * Web on OfficeFloor Testing
 * %%
 * Copyright (C) 2005 - 2020 Daniel Sagenschneider
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package net.officefloor.woof;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;

import net.officefloor.frame.api.team.Team;
import net.officefloor.plugin.section.clazz.Next;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.web.HttpObject;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.HttpSessionStateful;
import net.officefloor.web.ObjectResponse;

/**
 * Mock logic for the section.
 * 
 * @author Daniel Sagenschneider
 */
public class MockSection {

	@HttpSessionStateful
	public static class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private String previous = "-1";
	}

	/**
	 * Services.
	 * 
	 * @param parameter  Parameter.
	 * @param state      {@link State}.
	 * @param connection {@link ServerHttpConnection}.
	 * @param object     {@link MockObject} injected from configuration.
	 */
	public void service(@HttpQueryParameter("param") String parameter, State state, ServerHttpConnection connection,
			MockObject object) throws IOException {
		connection.getResponse().getEntityWriter()
				.write("param=" + parameter + ", previous=" + state.previous + ", object=" + object.getMessage());
		state.previous = parameter;
	}

	/**
	 * Provides testing of objects.
	 * 
	 * @param object   {@link MockObject} injected from configuration.
	 * @param response Sends the {@link MockObject} as a response.
	 */
	public void objects(MockObject object, ObjectResponse<MockObject> response) {
		response.send(object);
	}

	/**
	 * Initial service method for testing {@link Team}
	 * 
	 * @param flows {@link TeamFlows}.
	 */
	@Next("teamsDifferent")
	public String teams() {
		return Thread.currentThread().getName();
	}

	/**
	 * Ensure different {@link Team}.
	 */
	public void teamsDifferent(@Parameter String threadName, MockObject object, ObjectResponse<String> response) {
		boolean isSameThread = Thread.currentThread().getName().equals(threadName);
		response.send(isSameThread ? "SAME THREAD" : "DIFFERENT THREAD");
	}

	@HttpObject
	public static class MockJsonObject {

		private String text = "TEST";

		public MockJsonObject() {
		}

		public MockJsonObject(String text) {
			this.text = text;
		}

		public String getText() {
			return this.text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	public void json(ServerHttpConnection connection, MockJsonObject request, ObjectResponse<Object> response) {
		assertEquals("application/json", connection.getRequest().getHeaders().getHeader("content-type").getValue());
		response.send(request);
	}

	/**
	 * Failure to be thrown for testing.
	 */
	public static volatile Throwable failure = null;

	/**
	 * Services via throwing failure.
	 */
	public void failure() throws Throwable {
		throw failure;
	}

}
