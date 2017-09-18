/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2017 Daniel Sagenschneider
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
package net.officefloor.server.http.impl;

import java.util.function.Supplier;

import net.officefloor.server.http.HttpMethod;
import net.officefloor.server.http.HttpRequest;
import net.officefloor.server.http.HttpRequestHeaders;
import net.officefloor.server.http.HttpVersion;
import net.officefloor.server.stream.ServerInputStream;
import net.officefloor.server.stream.impl.ByteSequence;
import net.officefloor.server.stream.impl.ByteSequenceServerInputStream;

/**
 * {@link HttpRequest} implementation.
 * 
 * @author Daniel Sagenschneider
 */
public class MaterialisingHttpRequest implements HttpRequest {

	/**
	 * Supplies the {@link HttpMethod}.
	 */
	private final Supplier<HttpMethod> methodSupplier;

	/**
	 * {@link HttpMethod}.
	 */
	private HttpMethod method = null;

	/**
	 * Supplies the request URI.
	 */
	private final Supplier<String> requestUriSupplier;

	/**
	 * Request URI.
	 */
	private String requestUri = null;

	/**
	 * {@link HttpVersion}.
	 */
	private final HttpVersion version;

	/**
	 * {@link HttpRequestHeaders}.
	 */
	private final HttpRequestHeaders headers;

	/**
	 * {@link ByteSequence} for the HTTP entity.
	 */
	private final ByteSequence entity;

	/**
	 * {@link ByteSequenceServerInputStream}.
	 */
	private final ByteSequenceServerInputStream entityStream;

	/**
	 * Instantiate.
	 * 
	 * @param methodSupplier
	 *            {@link Supplier} for the {@link HttpMethod}.
	 * @param requestUriSupplier
	 *            {@link Supplier} for the request URI.
	 * @param version
	 *            {@link HttpVersion}.
	 * @param headers
	 *            {@link HttpRequestHeaders}.
	 * @param entity
	 *            {@link ByteSequence} for the HTTP entity.
	 */
	public MaterialisingHttpRequest(Supplier<HttpMethod> methodSupplier, Supplier<String> requestUriSupplier,
			HttpVersion version, HttpRequestHeaders headers, ByteSequence entity) {
		this.methodSupplier = methodSupplier;
		this.requestUriSupplier = requestUriSupplier;
		this.version = version;
		this.headers = headers;
		this.entity = entity;
		this.entityStream = new ByteSequenceServerInputStream(this.entity, 0);
	}

	/*
	 * ================= HttpRequest =====================
	 */

	@Override
	public HttpMethod getHttpMethod() {
		if (this.method == null) {
			this.method = this.methodSupplier.get();
		}
		return this.method;
	}

	@Override
	public String getRequestURI() {
		if (this.requestUri == null) {
			this.requestUri = this.requestUriSupplier.get();
		}
		return this.requestUri;
	}

	@Override
	public HttpVersion getHttpVersion() {
		return this.version;
	}

	@Override
	public HttpRequestHeaders getHttpHeaders() {
		return this.headers;
	}

	@Override
	public ServerInputStream getEntity() {
		return this.entityStream;
	}

}
