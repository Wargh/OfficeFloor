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
package net.officefloor.plugin.work.http.html.template;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;

import net.officefloor.plugin.socket.server.http.HttpResponse;

/**
 * {@link HttpHtmlTemplateContentWriter} to write static content.
 *
 * @author Daniel Sagenschneider
 */
public class StaticHttpHtmlTemplateContentWriter implements
		HttpHtmlTemplateContentWriter {

	/**
	 * Content to write to the {@link HttpResponse}.
	 */
	private final ByteBuffer content;

	/**
	 * Initiate.
	 *
	 * @param staticContent
	 *            Static content to write.
	 */
	public StaticHttpHtmlTemplateContentWriter(String staticContent) {

		// Create byte buffer to contain the data
		byte[] data = staticContent.getBytes();
		ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
		buffer.put(data);
		buffer.flip();

		// Specify static content
		this.content = buffer.asReadOnlyBuffer();
	}

	/*
	 * ========================= HttpHtmlTemplateContentWriter =============
	 */

	@Override
	public void writeContent(Object bean, Writer httpBody,
			HttpResponse httpResponse) throws IOException {
		httpResponse.getBody().append(this.content);
	}

}
