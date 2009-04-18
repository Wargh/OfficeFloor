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
package net.officefloor.compile.spi.section;

import net.officefloor.compile.properties.Property;
import net.officefloor.compile.work.TaskType;
import net.officefloor.compile.work.WorkType;
import net.officefloor.frame.api.execute.Work;
import net.officefloor.frame.api.manage.Office;

/**
 * {@link Work} within the {@link Section} of the {@link Office}.
 * 
 * @author Daniel
 */
public interface SectionWork {

	/**
	 * Obtains the name of this {@link SectionWork}.
	 * 
	 * @return Name of this {@link SectionWork}.
	 */
	String getSectionWorkName();

	/**
	 * Adds a {@link Property} to source this {@link SectionWork}.
	 * 
	 * @param name
	 *            Name of the {@link Property}.
	 * @param value
	 *            Value of the {@link Property}.
	 */
	void addProperty(String name, String value);

	/**
	 * Adds a {@link SectionTask}.
	 * 
	 * @param taskName
	 *            Name of the {@link SectionTask}.
	 * @param taskTypeName
	 *            Name of the {@link TaskType} on the {@link WorkType}.
	 * @return {@link SectionTask}.
	 */
	SectionTask addSectionTask(String taskName, String taskTypeName);

}