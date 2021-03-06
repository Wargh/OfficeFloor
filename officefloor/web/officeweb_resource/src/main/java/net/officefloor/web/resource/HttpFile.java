/*-
 * #%L
 * Web resources
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

package net.officefloor.web.resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.spi.FileTypeDetector;

import net.officefloor.server.http.HttpHeaderValue;
import net.officefloor.server.http.HttpResponse;
import net.officefloor.web.resource.spi.ResourceSystem;
import net.officefloor.web.resource.spi.ResourceSystemContext;
import net.officefloor.web.resource.spi.ResourceTransformer;

/**
 * HTTP file.
 * 
 * @author Daniel Sagenschneider
 */
public interface HttpFile extends HttpResource {

	/**
	 * Obtains the <code>Content-Encoding</code> for this {@link HttpFile}.
	 * 
	 * @return <code>Content-Encoding</code> for this {@link HttpFile}.
	 * 
	 * @see ResourceTransformer
	 */
	HttpHeaderValue getContentEncoding();

	/**
	 * Obtains the <code>Content-Type</code> for this {@link HttpFile}.
	 * 
	 * @return <code>Content-Type</code> for this {@link HttpFile}.
	 * 
	 * @see FileTypeDetector
	 */
	HttpHeaderValue getContentType();

	/**
	 * Obtains the {@link Charset} for the contents.
	 * 
	 * @return {@link Charset} or <code>null</code> if contents are not text or
	 *         the {@link Charset} is unknown.
	 * 
	 * @see ResourceSystem
	 * @see ResourceSystemContext
	 */
	Charset getCharset();

	/**
	 * Writes the {@link HttpFile} to the {@link HttpResponse}.
	 * 
	 * @param response
	 *            {@link HttpResponse}.
	 * @throws IOException
	 *             If failure in writing the {@link HttpFile} to the
	 *             {@link HttpResponse}.
	 */
	void writeTo(HttpResponse response) throws IOException;

}
