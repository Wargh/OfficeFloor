package net.officefloor.server.http;

import net.officefloor.server.stream.StreamBuffer;
import net.officefloor.server.stream.StreamBufferPool;

/**
 * <p>
 * Provides formatting of values for {@link HttpHeader} values.
 * <p>
 * Also provides means for common {@link HttpHeader} values in already encoded
 * HTTP bytes for faster writing.
 * 
 * @author Daniel Sagenschneider
 */
public class HttpHeaderValue {

	/**
	 * Value.
	 */
	private final String value;

	/**
	 * Pre-encoded bytes of value ready for HTTP output.
	 */
	private final byte[] encodedValue;

	/**
	 * Instantiate.
	 * 
	 * @param value
	 *            {@link HttpHeaderValue}.
	 */
	public HttpHeaderValue(String value) {
		this.value = value;
		this.encodedValue = this.value.getBytes(ServerHttpConnection.HTTP_CHARSET);
	}

	/**
	 * Obtains the value.
	 * 
	 * @return value.
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * Writes this {@link HttpHeaderValue} to the {@link StreamBuffer}.
	 * 
	 * @param <B>
	 *            Buffer type.
	 * @param head
	 *            Head {@link StreamBuffer} of linked list of {@link StreamBuffer}
	 *            instances.
	 * @param bufferPool
	 *            {@link StreamBufferPool}.
	 */
	public <B> void write(StreamBuffer<B> head, StreamBufferPool<B> bufferPool) {
		StreamBuffer.write(this.encodedValue, head, bufferPool);
	}

}