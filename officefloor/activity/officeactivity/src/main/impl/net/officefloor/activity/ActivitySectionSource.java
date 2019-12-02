/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2019 Daniel Sagenschneider
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.officefloor.activity;

import net.officefloor.activity.model.ActivityModel;
import net.officefloor.compile.spi.section.SectionDesigner;
import net.officefloor.compile.spi.section.source.SectionSource;
import net.officefloor.compile.spi.section.source.SectionSourceContext;
import net.officefloor.compile.spi.section.source.impl.AbstractSectionSource;

/**
 * {@link SectionSource} to load the {@link ActivityModel}.
 * 
 * @author Daniel Sagenschneider
 */
public class ActivitySectionSource extends AbstractSectionSource {

	/*
	 * =================== SectionSource =====================
	 */

	@Override
	protected void loadSpecification(SpecificationContext context) {
		// TODO implement AbstractSectionSource.loadSpecification
		throw new UnsupportedOperationException("TODO implement AbstractSectionSource.loadSpecification");
	}

	@Override
	public void sourceSection(SectionDesigner designer, SectionSourceContext context) throws Exception {
		// TODO implement SectionSource.sourceSection
		throw new UnsupportedOperationException("TODO implement SectionSource.sourceSection");
	}

}