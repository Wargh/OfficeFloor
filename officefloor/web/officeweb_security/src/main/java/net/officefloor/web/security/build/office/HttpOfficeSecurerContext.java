/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2018 Daniel Sagenschneider
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
package net.officefloor.web.security.build.office;

import net.officefloor.compile.spi.office.OfficeAdministration;
import net.officefloor.compile.spi.office.OfficeFlowSinkNode;
import net.officefloor.compile.spi.office.OfficeFlowSourceNode;
import net.officefloor.web.security.HttpAccessControl;

/**
 * Context for the {@link HttpOfficeSecurer}.
 * 
 * @author Daniel Sagenschneider
 */
public interface HttpOfficeSecurerContext {

	/**
	 * Obtains the {@link OfficeAdministration} to undertake
	 * {@link HttpAccessControl} for the {@link HttpOfficeSecurer}.
	 * 
	 * @return {@link OfficeAdministration}.
	 */
	OfficeAdministration getAdministration();

	/**
	 * Links the {@link OfficeFlowSourceNode} to either a secure / insecure
	 * {@link OfficeFlowSinkNode}.
	 * 
	 * @param flowSourceNode
	 *            {@link OfficeFlowSourceNode}.
	 * @param secureFlowSink
	 *            Secure {@link OfficeFlowSinkNode}.
	 * @param insecureFlowSink
	 *            Insecure {@link OfficeFlowSinkNode}.
	 */
	void link(OfficeFlowSourceNode flowSourceNode, OfficeFlowSinkNode secureFlowSink,
			OfficeFlowSinkNode insecureFlowSink);

}