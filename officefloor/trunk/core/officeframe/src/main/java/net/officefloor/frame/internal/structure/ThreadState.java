/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2011 Daniel Sagenschneider
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

package net.officefloor.frame.internal.structure;

import net.officefloor.frame.api.execute.FlowFuture;

/**
 * <p>
 * State of a thread within the {@link ProcessState}.
 * <p>
 * May be used as a {@link LinkedListSetEntry} in a list of {@link ThreadState}
 * instances for a {@link ProcessState}.
 * 
 * @author Daniel Sagenschneider
 */
public interface ThreadState extends FlowAsset, FlowFuture,
		LinkedListSetEntry<ThreadState, ProcessState> {

	/**
	 * Obtains the lock for this {@link ThreadState}.
	 * 
	 * @return Lock for this {@link ThreadState}.
	 */
	Object getThreadLock();

	/**
	 * Obtains the {@link ThreadMetaData} for this {@link ThreadState}.
	 * 
	 * @return {@link ThreadMetaData} for this {@link ThreadState}.
	 */
	ThreadMetaData getThreadMetaData();

	/**
	 * Returning a {@link Throwable} indicates that the thread has failed.
	 * 
	 * @return {@link Throwable} indicating the thread has failed or
	 *         <code>null</code> indicating thread still going fine.
	 */
	Throwable getFailure();

	/**
	 * Sets the {@link Throwable} cause to indicate that the thread has failed.
	 * 
	 * @param cause
	 *            Cause of the thread's failure.
	 */
	void setFailure(Throwable cause);

	/**
	 * Creates a {@link JobSequence} contained in this {@link ThreadState}.
	 * 
	 * @param flowMetaData
	 *            {@link FlowMetaData} for the new {@link JobSequence}.
	 * @return New {@link JobSequence}.
	 */
	JobSequence createFlow(FlowMetaData<?> flowMetaData);

	/**
	 * Flags that the input {@link JobSequence} has completed.
	 * 
	 * @param flow
	 *            {@link JobSequence} that has completed.
	 * @param activateSet
	 *            {@link JobNodeActivateSet} to add {@link JobNode} instances
	 *            waiting on this {@link ThreadState} if all {@link JobSequence}
	 *            instances of this {@link ThreadState} are complete.
	 */
	void flowComplete(JobSequence flow, JobNodeActivateSet activateSet);

	/**
	 * Obtains the {@link ProcessState} of the process containing this
	 * {@link ThreadState}.
	 * 
	 * @return {@link ProcessState} of the process containing this
	 *         {@link ThreadState}.
	 */
	ProcessState getProcessState();

	/**
	 * Obtains the {@link ManagedObjectContainer} for the input index.
	 * 
	 * @param index
	 *            Index of the {@link ManagedObjectContainer} to be returned.
	 * @return {@link ManagedObjectContainer} for the index.
	 */
	ManagedObjectContainer getManagedObjectContainer(int index);

	/**
	 * Obtains the {@link AdministratorContainer} for the input index.
	 * 
	 * @param index
	 *            Index of the {@link AdministratorContainer} to be returned.
	 * @return {@link AdministratorContainer} for the index.
	 */
	AdministratorContainer<?, ?> getAdministratorContainer(int index);

	/**
	 * <p>
	 * Flags that escalation is about to happen on this {@link ThreadState}.
	 * <p>
	 * This allows the {@link ThreadState} to know not to clean up should all
	 * its {@link JobSequence} instances be closed and a new one will be created for
	 * the {@link EscalationFlow}.
	 * 
	 * @param currentJobNode
	 *            Current {@link JobNode} being executed.
	 * @param activateSet
	 *            {@link JobNodeActivateSet}.
	 */
	void escalationStart(JobNode currentJobNode, JobNodeActivateSet activateSet);

	/**
	 * Flags that escalation has complete on this {@link ThreadState}.
	 * 
	 * @param currentJobNode
	 *            Current {@link JobNode} being executed.
	 * @param activateSet
	 *            {@link JobNodeActivateSet}.
	 */
	void escalationComplete(JobNode currentJobNode,
			JobNodeActivateSet activateSet);

	/**
	 * Obtains the {@link EscalationLevel} of this {@link ThreadState}.
	 * 
	 * @return {@link EscalationLevel} of this {@link ThreadState}.
	 */
	EscalationLevel getEscalationLevel();

	/**
	 * Specifies the {@link EscalationLevel} for this {@link ThreadState}.
	 * 
	 * @param escalationLevel
	 *            {@link EscalationLevel}.
	 */
	void setEscalationLevel(EscalationLevel escalationLevel);

}