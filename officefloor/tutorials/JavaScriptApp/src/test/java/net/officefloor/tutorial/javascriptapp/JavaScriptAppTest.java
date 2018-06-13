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
package net.officefloor.tutorial.javascriptapp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Rule;
import org.junit.Test;

import net.officefloor.OfficeFloorMain;
import net.officefloor.server.http.HttpClientRule;
import net.officefloor.test.OfficeFloorRule;

/**
 * Tests the JavaScript application.
 * 
 * @author Daniel Sagenschneider
 */
public class JavaScriptAppTest {

	/**
	 * Run application.
	 */
	public static void main(String[] arguments) throws Exception {
		OfficeFloorMain.main(arguments);
	}

	// START SNIPPET: tutorial
	@Rule
	public OfficeFloorRule officeFloor = new OfficeFloorRule();

	@Rule
	public HttpClientRule client = new HttpClientRule();

	@Test
	public void testHttpParameters() throws IOException {
		String response = this.doAjax("addition", "numberOne=2&numberTwo=1");
		assertEquals("Incorrect response", "3", response);
	}

	@Test
	public void testHttpJson() throws IOException {
		String response = this.doAjax("subtraction", "{ \"numberOne\" : \"3\", \"numberTwo\" : \"1\" }");
		assertEquals("Incorrect response", "{\"result\":\"2\"}", response);
	}

	private String doAjax(String link, String payload) throws IOException {
		HttpPost post = new HttpPost("http://localhost:7878/template+" + link);
		post.setEntity(new StringEntity(payload));
		HttpResponse response = this.client.execute(post);
		assertEquals("Should be successful", 200, response.getStatusLine().getStatusCode());
		return EntityUtils.toString(response.getEntity());
	}
	// END SNIPPET: tutorial

}