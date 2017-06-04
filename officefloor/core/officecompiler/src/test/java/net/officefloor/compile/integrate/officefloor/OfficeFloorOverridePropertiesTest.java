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
package net.officefloor.compile.integrate.officefloor;

import net.officefloor.compile.integrate.AbstractCompileTestCase;
import net.officefloor.compile.properties.Property;
import net.officefloor.compile.properties.PropertyList;
import net.officefloor.compile.spi.office.OfficeArchitect;
import net.officefloor.compile.spi.office.source.OfficeSourceContext;
import net.officefloor.compile.spi.office.source.impl.AbstractOfficeSource;
import net.officefloor.compile.spi.officefloor.DeployedOffice;
import net.officefloor.compile.spi.officefloor.OfficeFloorManagedObjectSource;
import net.officefloor.compile.spi.officefloor.OfficeFloorTeam;
import net.officefloor.compile.spi.pool.source.impl.AbstractManagedObjectPoolSource;
import net.officefloor.frame.api.build.ManagedObjectBuilder;
import net.officefloor.frame.api.manage.OfficeFloor;
import net.officefloor.frame.api.managedobject.pool.ManagedObjectPool;
import net.officefloor.frame.api.managedobject.pool.ManagedObjectPoolFactory;
import net.officefloor.frame.api.source.TestSource;
import net.officefloor.frame.api.team.Team;
import net.officefloor.frame.api.team.source.TeamSourceContext;
import net.officefloor.frame.api.team.source.impl.AbstractTeamSource;
import net.officefloor.plugin.managedobject.clazz.ClassManagedObjectSource;

/**
 * Ensure able to override the {@link PropertyList} for various aspects of the
 * {@link OfficeFloor}.
 * 
 * @author Daniel Sagenschneider
 */
public class OfficeFloorOverridePropertiesTest extends AbstractCompileTestCase {

	/**
	 * Ensure can override {@link Property} for the {@link DeployedOffice}.
	 */
	public void testOverrideOfficeProperties() {

		// Enables override of properties
		this.enableOverrideProperties();

		// Record the OfficeFloor
		this.record_init();
		this.record_officeFloorBuilder_addOffice("OVERRIDE_OFFICE");

		// Compile the OfficeFloor
		this.compile(true);
	}

	/**
	 * Ensure can override {@link Property} for the
	 * {@link OfficeFloorManagedObjectSource}.
	 */
	public void testOverrideManagedObjectSourceProperties() {

		// Enables override of properties
		this.enableOverrideProperties();

		// Record the OfficeFloor
		this.record_init();
		this.record_officeFloorBuilder_addOffice("OFFICE");
		this.record_officeFloorBuilder_addManagedObject("OVERRIDE_MANAGED_OBJECT_SOURCE",
				ClassManagedObjectSource.class, 0, "class.name", CompileManagedObject.class.getName(), "additional",
				"another");
		this.record_managedObjectBuilder_setManagingOffice("OFFICE");

		// Compile the OfficeFloor
		this.compile(true);
	}

	/**
	 * Ensure can override the {@link ManagedObjectPool} properties.
	 */
	public void testOverrideManagedObjectPoolProperties() {

		// Create the managed object pool factory
		ManagedObjectPoolFactory poolFactory = this.createMock(ManagedObjectPoolFactory.class);
		TestManagedObjectPoolSource.managedObjectPoolFactory = poolFactory;

		// Enables override of properties
		this.enableOverrideProperties();

		// Record the OfficeFloor
		this.record_init();
		this.record_officeFloorBuilder_addOffice("OFFICE");
		ManagedObjectBuilder<?> moBuilder = this.record_officeFloorBuilder_addManagedObject("MANAGED_OBJECT_SOURCE",
				ClassManagedObjectSource.class, 0, "class.name", CompileManagedObject.class.getName());
		this.record_managedObjectBuilder_setManagingOffice("OFFICE");
		this.recordReturn(moBuilder, moBuilder.setManagedObjectPool(poolFactory), null);

		// Compile the OfficeFloor
		this.compile(true);
	}

	/**
	 * Ensure can override {@link Property} for the {@link OfficeFloorTeam}.
	 */
	public void testOverrideTeamProperties() {

		// Enables override of properties
		this.enableOverrideProperties();

		// Record the OfficeFloor
		this.record_init();
		this.record_officeFloorBuilder_addTeam("OVERRIDE_TEAM", TestTeamSource.class, "value", "override", "additional",
				"another");

		// Compile the OfficeFloor
		this.compile(true);
	}

	public static class CompileManagedObject {
	}

	@TestSource
	public static class TestOfficeSource extends AbstractOfficeSource {

		@Override
		protected void loadSpecification(SpecificationContext context) {
			context.addProperty("value");
		}

		@Override
		public void sourceOffice(OfficeArchitect officeArchitect, OfficeSourceContext context) throws Exception {
			assertEquals("Incorrect overridden value", "override", context.getProperty("value"));
			assertEquals("Incorrect additional value", "another", context.getProperty("additional"));
		}
	}

	@TestSource
	public static class TestTeamSource extends AbstractTeamSource {

		@Override
		protected void loadSpecification(SpecificationContext context) {
			context.addProperty("value");
		}

		@Override
		public Team createTeam(TeamSourceContext context) throws Exception {
			fail("Should not be invoked");
			return null;
		}
	}

	@TestSource
	public static class TestManagedObjectPoolSource extends AbstractManagedObjectPoolSource {

		private static ManagedObjectPoolFactory managedObjectPoolFactory;

		@Override
		protected void loadSpecification(SpecificationContext context) {
			context.addProperty("value");
		}

		@Override
		protected void loadMetaData(MetaDataContext context) throws Exception {
			assertEquals("Incorrect overridden value", "override",
					context.getManagedObjectPoolSourceContext().getProperty("value"));
			assertEquals("Incorrect additional value", "another",
					context.getManagedObjectPoolSourceContext().getProperty("additional"));
			context.setPooledObjectType(CompileManagedObject.class);
			context.setManagedObjectPoolFactory(managedObjectPoolFactory);
		}
	}

}