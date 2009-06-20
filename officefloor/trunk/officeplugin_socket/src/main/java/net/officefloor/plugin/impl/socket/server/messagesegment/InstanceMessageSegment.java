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
package net.officefloor.plugin.impl.socket.server.messagesegment;

import java.nio.ByteBuffer;

import net.officefloor.plugin.socket.server.spi.MessageSegment;

/**
 * {@link MessageSegment} used as an instance and then discarded. It therefore
 * can not be pooled.
 * 
 * @author Daniel Sagenschneider
 */
public class InstanceMessageSegment extends AbstractMessageSegment {

	/**
	 * Initiate.
	 * 
	 * @param buffer
	 *            {@link ByteBuffer}.
	 */
	public InstanceMessageSegment(ByteBuffer buffer) {
		super(buffer);
	}

	/*
	 * ====================================================================
	 * AbstractMessageSegment
	 * ====================================================================
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.officefloor.plugin.socket.server.impl.AbstractMessageSegment#canPool()
	 */
	boolean canPool() {
		return false;
	}

}
