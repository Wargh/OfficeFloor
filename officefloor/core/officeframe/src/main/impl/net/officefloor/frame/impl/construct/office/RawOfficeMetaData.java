/*-
 * #%L
 * OfficeFrame
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

package net.officefloor.frame.impl.construct.office;

import java.util.Map;

import net.officefloor.frame.api.governance.Governance;
import net.officefloor.frame.api.manage.Office;
import net.officefloor.frame.impl.construct.governance.RawGovernanceMetaData;
import net.officefloor.frame.impl.construct.managedobject.RawBoundManagedObjectMetaData;
import net.officefloor.frame.impl.construct.managedobjectsource.RawManagedObjectMetaData;
import net.officefloor.frame.impl.construct.officefloor.RawOfficeFloorMetaData;
import net.officefloor.frame.internal.structure.OfficeMetaData;
import net.officefloor.frame.internal.structure.ProcessState;
import net.officefloor.frame.internal.structure.TeamManagement;
import net.officefloor.frame.internal.structure.ThreadState;

/**
 * {@link RawOfficeMetaData} implementation.
 * 
 * @author Daniel Sagenschneider
 */
public class RawOfficeMetaData {

	/**
	 * Name of the {@link Office}.
	 */
	private final String officeName;

	/**
	 * {@link RawOfficeFloorMetaData} containing this {@link Office}.
	 */
	private final RawOfficeFloorMetaData rawOfficeFloorMetaData;

	/**
	 * {@link TeamManagement} instances by their {@link Office} registered
	 * names.
	 */
	private final Map<String, TeamManagement> teams;

	/**
	 * {@link RawManagedObjectMetaData} instances by their {@link Office}
	 * registered names.
	 */
	private final Map<String, RawManagedObjectMetaData<?, ?>> managedObjectMetaData;

	/**
	 * {@link ProcessState} {@link RawBoundManagedObjectMetaData}.
	 */
	private final RawBoundManagedObjectMetaData[] processBoundManagedObjects;

	/**
	 * {@link ThreadState} {@link RawBoundManagedObjectMetaData}.
	 */
	private final RawBoundManagedObjectMetaData[] threadBoundManagedObjects;

	/**
	 * Scope {@link RawBoundManagedObjectMetaData} of the {@link Office} by the
	 * {@link ProcessState} and {@link ThreadState} bound names.
	 */
	private final Map<String, RawBoundManagedObjectMetaData> scopeMo;

	/**
	 * Indicates whether to manually manage {@link Governance}.
	 */
	private final boolean isManuallyManageGovernance;

	/**
	 * {@link RawGovernanceMetaData} of the {@link Office} by its {@link Office}
	 * registered name.
	 */
	private final Map<String, RawGovernanceMetaData<?, ?>> governanceMetaData;

	/**
	 * {@link OfficeMetaData}.
	 */
	OfficeMetaData officeMetaData;

	/**
	 * Initiate.
	 * 
	 * @param officeName
	 *            {@link Office} names.
	 * @param rawOfficeFloorMetaData
	 *            {@link RawOfficeFloorMetaData} containing this {@link Office}.
	 * @param teams
	 *            {@link TeamManagement} instances by their {@link Office}
	 *            registered names.
	 * @param managedObjectMetaData
	 *            {@link RawManagedObjectMetaData} instances by their
	 *            {@link Office} registered names.
	 * @param processBoundManagedObjects
	 *            {@link ProcessState} {@link RawBoundManagedObjectMetaData}
	 *            instances.
	 * @param threadBoundManagedObjects
	 *            {@link ThreadState} {@link RawBoundManagedObjectMetaData}
	 *            instances.
	 * @param scopeMo
	 *            Scope {@link RawBoundManagedObjectMetaData} of the
	 *            {@link Office} by the {@link ProcessState} and
	 *            {@link ThreadState} bound names.
	 * @param isManuallyManageGovernance
	 *            Indicates whether to manually manage {@link Governance}.
	 * @param governanceMetaData
	 *            {@link RawGovernanceMetaData} of the {@link Office} by its
	 *            {@link Office} registered name.
	 */
	public RawOfficeMetaData(String officeName, RawOfficeFloorMetaData rawOfficeFloorMetaData,
			Map<String, TeamManagement> teams, Map<String, RawManagedObjectMetaData<?, ?>> managedObjectMetaData,
			RawBoundManagedObjectMetaData[] processBoundManagedObjects,
			RawBoundManagedObjectMetaData[] threadBoundManagedObjects,
			Map<String, RawBoundManagedObjectMetaData> scopeMo, boolean isManuallyManageGovernance,
			Map<String, RawGovernanceMetaData<?, ?>> governanceMetaData) {
		this.officeName = officeName;
		this.rawOfficeFloorMetaData = rawOfficeFloorMetaData;
		this.teams = teams;
		this.managedObjectMetaData = managedObjectMetaData;
		this.processBoundManagedObjects = processBoundManagedObjects;
		this.threadBoundManagedObjects = threadBoundManagedObjects;
		this.scopeMo = scopeMo;
		this.isManuallyManageGovernance = isManuallyManageGovernance;
		this.governanceMetaData = governanceMetaData;
	}

	/*
	 * ============= RawOfficeMetaData =======================================
	 */

	/**
	 * Name of the {@link Office}.
	 * 
	 * @return Name of the {@link Office}.
	 */
	public String getOfficeName() {
		return this.officeName;
	}

	/**
	 * Obtains {@link RawOfficeFloorMetaData} containing this {@link Office}.
	 * 
	 * @return {@link RawOfficeFloorMetaData}.
	 */
	public RawOfficeFloorMetaData getRawOfficeFloorMetaData() {
		return this.rawOfficeFloorMetaData;
	}

	/**
	 * Obtains the {@link TeamManagement} instances by their {@link Office}
	 * registered names.
	 * 
	 * @return {@link TeamManagement} instances by their {@link Office}
	 *         registered names.
	 */
	public Map<String, TeamManagement> getTeams() {
		return this.teams;
	}

	/**
	 * Indicates if manually manage {@link Governance}.
	 * 
	 * @return <code>true</code> to manually manage {@link Governance}.
	 */
	public Map<String, RawManagedObjectMetaData<?, ?>> getManagedObjectMetaData() {
		return this.managedObjectMetaData;
	}

	/**
	 * Obtains the {@link RawGovernanceMetaData} by their {@link Office}
	 * registered names.
	 * 
	 * @return {@link RawGovernanceMetaData} by their {@link Office} registered
	 *         names.
	 */
	public RawBoundManagedObjectMetaData[] getProcessBoundManagedObjects() {
		return this.processBoundManagedObjects;
	}

	/**
	 * Obtains the {@link RawManagedObjectMetaData} by their {@link Office}
	 * registered names.
	 * 
	 * @return {@link RawManagedObjectMetaData} by their {@link Office}
	 *         registered names.
	 */
	public RawBoundManagedObjectMetaData[] getThreadBoundManagedObjects() {
		return this.threadBoundManagedObjects;
	}

	/**
	 * Obtains the {@link ProcessState} {@link RawBoundManagedObjectMetaData}
	 * instances.
	 * 
	 * @return {@link ProcessState} {@link RawBoundManagedObjectMetaData}
	 *         instances.
	 */
	public boolean isManuallyManageGovernance() {
		return this.isManuallyManageGovernance;
	}

	/**
	 * Obtains the {@link ThreadState} {@link RawBoundManagedObjectMetaData}
	 * instances.
	 * 
	 * @return {@link ThreadState} {@link RawBoundManagedObjectMetaData}
	 *         instances.
	 */
	public Map<String, RawGovernanceMetaData<?, ?>> getGovernanceMetaData() {
		return this.governanceMetaData;
	}

	/**
	 * Obtains the scope {@link RawBoundManagedObjectMetaData} instances of the
	 * {@link Office} by the {@link ProcessState} and {@link ThreadState} bound
	 * names.
	 * 
	 * @return Scope {@link RawBoundManagedObjectMetaData} instances of the
	 *         {@link Office} by the {@link ProcessState} and
	 *         {@link ThreadState} bound names.
	 */
	public Map<String, RawBoundManagedObjectMetaData> getOfficeScopeManagedObjects() {
		return this.scopeMo;
	}

	/**
	 * Obtains the {@link OfficeMetaData}.
	 * 
	 * @return {@link OfficeMetaData}.
	 */
	public OfficeMetaData getOfficeMetaData() {
		return this.officeMetaData;
	}

}
