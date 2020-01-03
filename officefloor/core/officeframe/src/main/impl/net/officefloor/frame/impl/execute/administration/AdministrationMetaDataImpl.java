package net.officefloor.frame.impl.execute.administration;

import java.util.concurrent.Executor;

import net.officefloor.frame.api.administration.Administration;
import net.officefloor.frame.api.administration.AdministrationContext;
import net.officefloor.frame.api.administration.AdministrationFactory;
import net.officefloor.frame.api.function.AsynchronousFlow;
import net.officefloor.frame.api.governance.Governance;
import net.officefloor.frame.api.team.Team;
import net.officefloor.frame.internal.structure.AdministrationMetaData;
import net.officefloor.frame.internal.structure.AssetManager;
import net.officefloor.frame.internal.structure.EscalationProcedure;
import net.officefloor.frame.internal.structure.FlowMetaData;
import net.officefloor.frame.internal.structure.GovernanceActivity;
import net.officefloor.frame.internal.structure.ManagedFunctionMetaData;
import net.officefloor.frame.internal.structure.ManagedObjectExtensionExtractorMetaData;
import net.officefloor.frame.internal.structure.OfficeMetaData;
import net.officefloor.frame.internal.structure.TeamManagement;
import net.officefloor.frame.internal.structure.ThreadState;

/**
 * Implementation of the {@link AdministrationMetaData}.
 * 
 * @author Daniel Sagenschneider
 */
public class AdministrationMetaDataImpl<E, F extends Enum<F>, G extends Enum<G>>
		implements AdministrationMetaData<E, F, G> {

	/**
	 * Bound name of this {@link Administration}.
	 */
	private final String administrationName;

	/**
	 * {@link AdministrationFactory}.
	 */
	private final AdministrationFactory<E, F, G> administrationFactory;

	/**
	 * Extension interface.
	 */
	private final Class<E> extensionInterface;

	/**
	 * {@link ManagedObjectExtensionExtractorMetaData}.
	 */
	private final ManagedObjectExtensionExtractorMetaData<E>[] eiMetaData;

	/**
	 * {@link TeamManagement} of {@link Team} responsible for the
	 * {@link GovernanceActivity}.
	 */
	private final TeamManagement responsibleTeam;

	/**
	 * {@link AsynchronousFlow} timeout.
	 */
	private final long asynchronousFlowTimeout;

	/**
	 * {@link AssetManager} for the instigated {@link AsynchronousFlow} instances.
	 */
	private final AssetManager asynchronousFlowAssetManager;

	/**
	 * {@link FlowMetaData} instances for this {@link Administration}.
	 */
	private final FlowMetaData[] flowMetaData;

	/**
	 * Translates the index to a {@link ThreadState} {@link Governance} index.
	 */
	private final int[] governanceIndexes;

	/**
	 * {@link EscalationProcedure}.
	 */
	private final EscalationProcedure escalationProcedure;

	/**
	 * {@link OfficeMetaData}.
	 */
	private final OfficeMetaData officeMetaData;

	/**
	 * {@link Executor} for {@link AdministrationContext}.
	 */
	private final Executor executor;

	/**
	 * Instantiate.
	 * 
	 * @param administrationName           Bound name of this
	 *                                     {@link Administration}.
	 * @param administrationFactory        {@link AdministrationFactory}.
	 * @param extensionInterface           Extension interface.
	 * @param eiMetaData                   {@link ManagedObjectExtensionExtractorMetaData}.
	 * @param responsibleTeam              {@link TeamManagement} of {@link Team}
	 *                                     responsible for the
	 *                                     {@link GovernanceActivity}.
	 * @param asynchronousFlowAssetManager {@link AssetManager} for the instigated
	 *                                     {@link AsynchronousFlow} instances.
	 * @param asynchronousFlowTimeout      {@link AsynchronousFlow} tiemout.
	 * @param flowMetaData                 {@link FlowMetaData} instances for this
	 *                                     {@link Administration}.
	 * @param governanceIndexes            Translates the index to a
	 *                                     {@link ThreadState} {@link Governance}
	 *                                     index.
	 * @param escalationProcedure          {@link EscalationProcedure}.
	 * @param officeMetaData               {@link OfficeMetaData}.
	 * @param executor                     {@link Executor} for
	 *                                     {@link AdministrationContext}.
	 */
	public AdministrationMetaDataImpl(String administrationName, AdministrationFactory<E, F, G> administrationFactory,
			Class<E> extensionInterface, ManagedObjectExtensionExtractorMetaData<E>[] eiMetaData,
			TeamManagement responsibleTeam, long asynchronousFlowTimeout, AssetManager asynchronousFlowAssetManager,
			FlowMetaData[] flowMetaData, int[] governanceIndexes, EscalationProcedure escalationProcedure,
			OfficeMetaData officeMetaData, Executor executor) {
		this.administrationName = administrationName;
		this.administrationFactory = administrationFactory;
		this.extensionInterface = extensionInterface;
		this.eiMetaData = eiMetaData;
		this.responsibleTeam = responsibleTeam;
		this.asynchronousFlowTimeout = asynchronousFlowTimeout;
		this.asynchronousFlowAssetManager = asynchronousFlowAssetManager;
		this.flowMetaData = flowMetaData;
		this.governanceIndexes = governanceIndexes;
		this.escalationProcedure = escalationProcedure;
		this.officeMetaData = officeMetaData;
		this.executor = executor;
	}

	/*
	 * ================= ManagedFunctionContainerMetaData =================
	 */

	@Override
	public String getFunctionName() {
		return Administration.class.getSimpleName() + "-" + this.administrationName;
	}

	@Override
	public TeamManagement getResponsibleTeam() {
		return this.responsibleTeam;
	}

	@Override
	public long getAsynchronousFlowTimeout() {
		return this.asynchronousFlowTimeout;
	}

	@Override
	public AssetManager getAsynchronousFlowManager() {
		return this.asynchronousFlowAssetManager;
	}

	@Override
	public FlowMetaData getFlow(int flowIndex) {
		return this.flowMetaData[flowIndex];
	}

	@Override
	public ManagedFunctionMetaData<?, ?> getNextManagedFunctionMetaData() {
		return null; // no next function
	}

	@Override
	public EscalationProcedure getEscalationProcedure() {
		return this.escalationProcedure;
	}

	@Override
	public OfficeMetaData getOfficeMetaData() {
		return this.officeMetaData;
	}

	/*
	 * ================= AdministratorMetaData ============================
	 */

	@Override
	public ManagedObjectExtensionExtractorMetaData<E>[] getManagedObjectExtensionExtractorMetaData() {
		return this.eiMetaData;
	}

	@Override
	public String getAdministrationName() {
		return this.administrationName;
	}

	@Override
	public AdministrationFactory<E, F, G> getAdministrationFactory() {
		return this.administrationFactory;
	}

	@Override
	public Class<E> getExtensionInterface() {
		return this.extensionInterface;
	}

	@Override
	public int translateGovernanceIndexToThreadIndex(int governanceIndex) {
		return this.governanceIndexes[governanceIndex];
	}

	@Override
	public Executor getExecutor() {
		return this.executor;
	}

}