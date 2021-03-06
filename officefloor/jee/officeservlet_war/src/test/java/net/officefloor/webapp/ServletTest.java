/*-
 * #%L
 * OfficeFloor integration of WAR
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

package net.officefloor.webapp;

import static org.junit.Assert.assertNotEquals;

import javax.servlet.Servlet;

import org.apache.catalina.connector.Connector;

import net.officefloor.frame.api.manage.OfficeFloor;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.plugin.managedobject.singleton.Singleton;
import net.officefloor.server.http.mock.MockHttpResponse;
import net.officefloor.server.http.mock.MockHttpServer;
import net.officefloor.servlet.archive.TutorialArchiveLocatorUtil;
import net.officefloor.tutorial.warapp.ServletDependency;
import net.officefloor.woof.compile.CompileWoof;
import net.officefloor.woof.mock.MockWoofServer;

/**
 * Ensure can provide {@link OfficeFloor} {@link Connector}.
 * 
 * @author Daniel Sagenschneider
 */
public class ServletTest extends OfficeFrameTestCase {

	/**
	 * Ensure can service simple GET.
	 */
	public void testSimpleGet() throws Exception {
		this.doWarTest("/simple", "SIMPLE");
	}

	/**
	 * Ensure can inject into WAR {@link Servlet} instances.
	 */
	public void testInjectGet() throws Exception {
		this.doWarTest("/inject", new OverrideServletDependency().getMessage());
	}

	/**
	 * Undertakes WAR {@link Servlet} test.
	 * 
	 * @param path           Path to {@link Servlet}.
	 * @param expectedEntity Expected entity in response.
	 */
	private void doWarTest(String path, String expectedEntity) throws Exception {

		// Ensure valid test
		ServletDependency injectedDependency = new OverrideServletDependency();
		assertNotEquals("INVALID TEST: should have different inject message", new ServletDependency().getMessage(),
				injectedDependency.getMessage());

		// Undertake test
		String webAppPath = TutorialArchiveLocatorUtil.getArchiveFile("WarApp", ".war").getAbsolutePath();
		CompileWoof compile = new CompileWoof(true);
		compile.office((context) -> {
			Singleton.load(context.getOfficeArchitect(), injectedDependency);
		});
		try (MockWoofServer server = compile.open(OfficeFloorWar.PROPERTY_WAR_PATH, webAppPath)) {
			MockHttpResponse response = server.send(MockHttpServer.mockRequest(path));
			response.assertResponse(200, expectedEntity);
		}
	}

	/**
	 * Provide own {@link ServletDependency}.
	 */
	private static class OverrideServletDependency extends ServletDependency {

		@Override
		public String getMessage() {
			return "OFFICEFLOOR INJECT";
		}
	}

}
