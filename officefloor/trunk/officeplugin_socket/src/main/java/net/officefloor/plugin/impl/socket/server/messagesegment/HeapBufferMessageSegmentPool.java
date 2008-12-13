/*
 *  Office Floor, Application Server
 *  Copyright (C) 2006 Daniel Sagenschneider
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms 
 *  of the GNU General Public License as published by the Free Software Foundation; either 
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program; 
 *  if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
 *  MA 02111-1307 USA
 */
package net.officefloor.plugin.impl.socket.server.messagesegment;

import java.nio.ByteBuffer;

import net.officefloor.plugin.socket.server.spi.MessageSegment;

/**
 * Pool of {@link java.nio.HeapByteBuffer} {@link MessageSegment} instances.
 * 
 * @author Daniel
 */
public class HeapBufferMessageSegmentPool extends
		AbstractBufferMessageSegmentPool {

	/**
	 * Initiate.
	 * 
	 * @param bufferSize
	 *            Size of the {@link ByteBuffer} instances being pooled.
	 */
	public HeapBufferMessageSegmentPool(int bufferSize) {
		super(bufferSize);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seenet.officefloor.plugin.impl.socket.server.messagesegment.
	 * AbstractBufferMessageSegmentPool#createByteBuffer(int)
	 */
	@Override
	protected ByteBuffer createByteBuffer(int bufferSize) {
		return ByteBuffer.allocate(bufferSize);
	}

}
