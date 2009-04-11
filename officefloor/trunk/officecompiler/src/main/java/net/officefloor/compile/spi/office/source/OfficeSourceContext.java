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
package net.officefloor.compile.spi.office.source;

import net.officefloor.compile.properties.PropertyList;
import net.officefloor.compile.spi.office.OfficeAdministrator;
import net.officefloor.compile.spi.office.OfficeManagedObject;
import net.officefloor.compile.spi.office.OfficeSection;
import net.officefloor.compile.spi.section.source.SectionSource;

/**
 * Context for the {@link OfficeSource}.
 * 
 * @author Daniel
 */
public interface OfficeSourceContext {

	OfficeManagedObject getOfficeManagedObject(String managedObjectName);
	
	OfficeSection addSection(String sectionName, String sectionSourceClassName,
			String sectionLocation, PropertyList properties);

	OfficeSection addSection(String sectionName, SectionSource sectionSource,
			String sectionLocation, PropertyList properties);

	OfficeAdministrator addAdministrator(String administratorName,
			String administratorSourceClassName, PropertyList properties);

}