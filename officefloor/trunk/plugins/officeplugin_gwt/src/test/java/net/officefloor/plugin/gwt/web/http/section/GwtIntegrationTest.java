/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2012 Daniel Sagenschneider
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

package net.officefloor.plugin.gwt.web.http.section;

import java.io.ByteArrayOutputStream;

import net.officefloor.autowire.AutoWireManagement;
import net.officefloor.frame.impl.construct.source.SourcePropertiesImpl;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.plugin.gwt.service.MockGwtServiceInterface;
import net.officefloor.plugin.gwt.service.MockGwtServiceInterfaceAsync;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.web.http.application.HttpTemplateAutoWireSection;
import net.officefloor.plugin.web.http.server.HttpServerAutoWireOfficeFloorSource;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.gdevelop.gwt.syncrpc.SyncProxy;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Test integration of GWT.
 * 
 * @author Daniel Sagenschneider
 */
public class GwtIntegrationTest extends OfficeFrameTestCase {

	/**
	 * HTTP Server.
	 */
	private final HttpServerAutoWireOfficeFloorSource source = new HttpServerAutoWireOfficeFloorSource();

	/**
	 * HTTP Client.
	 */
	private final HttpClient client = new DefaultHttpClient();

	/**
	 * Ensure issue if no Template URI.
	 */
	public void testNoTemplateUri() throws Exception {

		// Create template with no URI
		HttpTemplateAutoWireSection section = this.source.addHttpTemplate(
				"TEMPLATE", GwtServiceTemplateLogic.class);

		try {
			// Add the GWT Extension
			GwtHttpTemplateSectionExtension.extendTemplate(section,
					this.source, new SourcePropertiesImpl(), Thread
							.currentThread().getContextClassLoader());
			fail("Should not be successful without Template URI");

		} catch (IllegalStateException ex) {
			assertEquals(
					"Incorrect reason",
					"Template must have a URI for extending with GWT (Template=TEMPLATE)",
					ex.getMessage());
		}
	}

	/**
	 * Ensure transforms HTML to include GWT.
	 */
	public void testTransformation() throws Exception {

		// Configure the template with GWT
		String templatePath = this.getFileLocation(this.getClass(),
				"Template.html");
		HttpTemplateAutoWireSection section = this.source.addHttpTemplate(
				templatePath, GwtTransformationTemplateLogic.class, "template");

		// Add the GWT Extension
		GwtHttpTemplateSectionExtension.extendTemplate(section, this.source,
				new SourcePropertiesImpl(), Thread.currentThread()
						.getContextClassLoader());

		// Start Server
		this.source.openOfficeFloor();

		// Request the template
		HttpResponse response = this.client.execute(new HttpGet(
				"http://localhost:7878/template"));
		assertEquals("Should be successful", 200, response.getStatusLine()
				.getStatusCode());

		// Obtain the response
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		response.getEntity().writeTo(buffer);
		String responseBody = buffer.toString();

		// Ensure expected response
		final String GWT_SCRIPT = "<script type=\"text/javascript\" language=\"javascript\" src=\"template/template.nocache.js\"></script>";
		final String GWT_HISTORY_IFRAME = "<iframe src=\"javascript:''\" id=\"__gwt_historyFrame\" tabIndex='-1' style=\"position:absolute;width:0;height:0;border:0\"></iframe>";
		final String EXPECTED_RESPONSE = "<html><head>" + GWT_SCRIPT
				+ "<title>GWT</title></head><body>" + GWT_HISTORY_IFRAME
				+ "<p>Test</p></body></html>";
		assertEquals("Incorrect response", EXPECTED_RESPONSE, responseBody);
	}

	/**
	 * Ensure able to invoke GWT Service.
	 */
	public void testGwtService() throws Exception {

		// Configure the template with GWT Service
		String templatePath = this.getFileLocation(this.getClass(),
				"Template.html");
		HttpTemplateAutoWireSection section = this.source.addHttpTemplate(
				templatePath, GwtServiceTemplateLogic.class, "template");

		// Add the GWT Extension
		SourcePropertiesImpl properties = new SourcePropertiesImpl();
		properties
				.addProperty(
						GwtHttpTemplateSectionExtension.PROPERTY_GWT_ASYNC_SERVICE_INTERFACES,
						MockGwtServiceInterfaceAsync.class.getName());
		GwtHttpTemplateSectionExtension.extendTemplate(section, this.source,
				properties, Thread.currentThread().getContextClassLoader());

		// Start Server
		this.source.openOfficeFloor();

		// Invoke GWT Service and validate returns successfully
		MockGwtServiceInterface service = (MockGwtServiceInterface) SyncProxy
				.newProxyInstance(MockGwtServiceInterface.class,
						"http://localhost:7878/template/", "GwtServicePath");
		String result = service.service(new Integer(10));
		assertEquals("Incorrect result", "10", result);
	}

	@Override
	protected void tearDown() throws Exception {
		// Shutdown
		this.client.getConnectionManager().shutdown();
		AutoWireManagement.closeAllOfficeFloors();
	}

	/**
	 * GWT Template logic.
	 */
	public static class GwtServiceTemplateLogic {
		public void service(@Parameter Integer parameter,
				AsyncCallback<String> callback) {
			callback.onSuccess(String.valueOf(parameter.intValue()));
		}
	}

	/**
	 * Transformation template logic (as does not GWT Service).
	 */
	public static class GwtTransformationTemplateLogic {
		public void service() {
		}
	}

}