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
package net.officefloor.compile.integrate.office;

import net.officefloor.compile.integrate.AbstractCompileTestCase;
import net.officefloor.compile.properties.Property;
import net.officefloor.compile.properties.PropertyList;
import net.officefloor.compile.spi.office.OfficeAdministration;
import net.officefloor.compile.spi.office.OfficeGovernance;
import net.officefloor.compile.spi.office.OfficeManagedObjectSource;
import net.officefloor.compile.spi.office.OfficeSection;
import net.officefloor.compile.spi.section.SectionDesigner;
import net.officefloor.compile.spi.section.SectionFunctionNamespace;
import net.officefloor.compile.spi.section.source.SectionSource;
import net.officefloor.compile.spi.section.source.SectionSourceContext;
import net.officefloor.compile.spi.section.source.impl.AbstractSectionSource;
import net.officefloor.frame.api.manage.Office;
import net.officefloor.frame.api.source.TestSource;
import net.officefloor.plugin.managedfunction.clazz.ClassManagedFunctionSource;
import net.officefloor.plugin.managedobject.clazz.ClassManagedObjectSource;

/**
 * Ensure able to override the {@link PropertyList} for various aspects of the
 * {@link Office}.
 * 
 * @author Daniel Sagenschneider
 */
public class OfficeOverridePropertiesTest extends AbstractCompileTestCase {

	/**
	 * Ensure can override {@link Property} for the {@link OfficeSection}.
	 */
	public void testOverrideSectionProperties() {

		// Enables override of properties
		this.enableOverrideProperties();

		// Record creating the section types
		this.issues.recordCaptureIssues(false);

		// Record the OfficeFloor
		this.record_init();
		this.record_officeFloorBuilder_addOffice("OFFICE");
		this.record_officeBuilder_addFunction("SECTION", "another");
		this.record_officeBuilder_addFunction("SECTION", "overridden_function");

		// Compile the OfficeFloor
		this.compile(true);
	}

	/**
	 * Ensure can override {@link Property} for the {@link OfficeGovernance}.
	 */
	public void testOverrideGovernanceProperties() {
		fail("TODO implement");
	}

	/**
	 * Ensure can override {@link Property} for the
	 * {@link OfficeAdministration}.
	 */
	public void testOverrideAdministrationProperties() {
		fail("TODO implement");
	}

	/**
	 * Ensure can override {@link Property} for the
	 * {@link OfficeManagedObjectSource}.
	 */
	public void testOverrideManagedObjectSourceProperties() {

		// Enables override of properties
		this.enableOverrideProperties();

		// Record the OfficeFloor
		this.record_init();
		this.record_officeFloorBuilder_addOffice("OFFICE");
		this.record_officeFloorBuilder_addManagedObject("OFFICE.OVERRIDE_MANAGED_OBJECT_SOURCE",
				ClassManagedObjectSource.class, 0, "class.name", CompileManagedObject.class.getName(), "additional",
				"another");
		this.record_managedObjectBuilder_setManagingOffice("OFFICE");

		// Compile the OfficeFloor
		this.compile(true);
	}

	public static class CompileManagedObject {
	}

	/**
	 * Test {@link SectionSource}.
	 */
	@TestSource
	public static class TestSectionSource extends AbstractSectionSource {

		@Override
		protected void loadSpecification(SpecificationContext context) {
			context.addProperty("namespace");
		}

		@Override
		public void sourceSection(SectionDesigner designer, SectionSourceContext context) throws Exception {
			SectionFunctionNamespace namespace = designer.addSectionFunctionNamespace("NAMESPACE",
					ClassManagedFunctionSource.class.getName());
			namespace.addProperty(ClassManagedFunctionSource.CLASS_NAME_PROPERTY_NAME, CompileFunction.class.getName());
			String functionName = context.getProperty("function.name");
			namespace.addSectionFunction(functionName, "function");
			String additional = context.getProperty("additional", null);
			if (additional != null) {
				namespace.addSectionFunction(additional, "function");
			}
		}
	}

	public static class CompileFunction {
		public void function() {
		}
	}
}