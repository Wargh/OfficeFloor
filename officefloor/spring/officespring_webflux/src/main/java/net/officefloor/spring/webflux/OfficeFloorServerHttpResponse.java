/*-
 * #%L
 * Spring Web Flux Integration
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

package net.officefloor.spring.webflux;

import java.io.IOException;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.reactive.AbstractListenerServerHttpResponse;
import org.springframework.http.server.reactive.AbstractListenerWriteFlushProcessor;
import org.springframework.http.server.reactive.AbstractListenerWriteProcessor;

import net.officefloor.frame.api.manage.OfficeFloor;
import net.officefloor.server.http.HttpResponse;
import net.officefloor.server.http.HttpResponseCookies;
import net.officefloor.server.http.HttpResponseHeaders;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.server.stream.ServerOutputStream;

/**
 * {@link OfficeFloor} {@link ServerHttpResponse}.
 * 
 * @author Daniel Sagenschneider
 */
public class OfficeFloorServerHttpResponse extends AbstractListenerServerHttpResponse {

	/**
	 * {@link HttpResponse}.
	 */
	private final HttpResponse httpResponse;

	/**
	 * Entity.
	 */
	private final ServerOutputStream entity;

	/**
	 * Instantiate.
	 * 
	 * @param httpResponse      {@link HttpResponse}.
	 * @param dataBufferFactory {@link DataBufferFactory}.
	 * @throws IOException If fails to setup response.
	 */
	public OfficeFloorServerHttpResponse(HttpResponse httpResponse, DataBufferFactory dataBufferFactory)
			throws IOException {
		super(dataBufferFactory);
		this.httpResponse = httpResponse;
		this.entity = httpResponse.getEntity();
	}

	/*
	 * ===================== ServerHttpResponse =======================
	 */

	@Override
	protected void applyStatusCode() {
		Integer statusCode = this.getRawStatusCode();
		if (statusCode != null) {
			this.httpResponse.setStatus(HttpStatus.getHttpStatus(statusCode));
		}
	}

	@Override
	protected void applyHeaders() {
		HttpResponseHeaders headers = this.httpResponse.getHeaders();
		this.getHeaders().forEach((name, values) -> {
			for (String value : values) {
				headers.addHeader(name, value);
			}
		});
	}

	@Override
	protected void applyCookies() {
		HttpResponseCookies responseCookies = this.httpResponse.getCookies();
		this.getCookies().forEach((name, cookies) -> {
			for (ResponseCookie cookie : cookies) {
				responseCookies.setCookie(cookie.getName(), cookie.getValue(), (init) -> {
					init.setDomain(cookie.getDomain());
					init.setHttpOnly(cookie.isHttpOnly());
					init.setMaxAge(cookie.getMaxAge().toMillis() / 1000);
					init.setPath(cookie.getPath());
					init.setSecure(cookie.isSecure());
				});
			}
		});
	}

	@Override
	protected Processor<? super Publisher<? extends DataBuffer>, Void> createBodyFlushProcessor() {
		return new OfficeFloorBodyFlushProcessor();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNativeResponse() {
		return (T) this.httpResponse;
	}

	/**
	 * {@link OfficeFloor} entity flush {@link Processor}.
	 */
	private class OfficeFloorBodyFlushProcessor extends AbstractListenerWriteFlushProcessor<DataBuffer> {

		@Override
		protected Processor<? super DataBuffer, Void> createWriteProcessor() {
			return new OfficeFloorBodyProcessor();
		}

		@Override
		protected boolean isWritePossible() {
			return true;
		}

		@Override
		protected boolean isFlushPending() {
			return false;
		}

		@Override
		protected void flush() throws IOException {
			// Handled flush
		}
	}

	/**
	 * {@link OfficeFloor} entity {@link Processor}.
	 */
	private class OfficeFloorBodyProcessor extends AbstractListenerWriteProcessor<DataBuffer> {

		@Override
		protected boolean isWritePossible() {
			return true;
		}

		@Override
		protected boolean isDataEmpty(DataBuffer dataBuffer) {
			return dataBuffer.readableByteCount() == 0;
		}

		@Override
		protected boolean write(DataBuffer dataBuffer) throws IOException {

			// Write data to response
			OfficeFloorServerHttpResponse.this.entity.write(dataBuffer.asByteBuffer());

			// Clean up buffer
			DataBufferUtils.release(dataBuffer);
			return true;
		}

		@Override
		protected void writingComplete() {
			// Nothing further
		}

		@Override
		protected void discardData(DataBuffer dataBuffer) {
			DataBufferUtils.release(dataBuffer);
		}
	}

}
