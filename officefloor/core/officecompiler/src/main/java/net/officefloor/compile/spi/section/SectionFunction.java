package net.officefloor.compile.spi.section;

import net.officefloor.compile.managedfunction.ManagedFunctionEscalationType;
import net.officefloor.compile.managedfunction.ManagedFunctionFlowType;
import net.officefloor.compile.managedfunction.ManagedFunctionObjectType;
import net.officefloor.frame.api.escalate.Escalation;
import net.officefloor.frame.api.function.ManagedFunction;

/**
 * {@link ManagedFunction} for a {@link SectionFunctionNamespace}.
 * 
 * @author Daniel Sagenschneider
 */
public interface SectionFunction extends SectionFlowSinkNode, SectionFlowSourceNode {

	/**
	 * Obtains the name of this {@link SectionFunction}.
	 * 
	 * @return Name of this {@link SectionFunction}.
	 */
	String getSectionFunctionName();

	/**
	 * Obtains the {@link FunctionFlow} for the {@link ManagedFunctionFlowType}.
	 * 
	 * @param functionFlowName
	 *            Name of the {@link ManagedFunctionFlowType}.
	 * @return {@link FunctionFlow}.
	 */
	FunctionFlow getFunctionFlow(String functionFlowName);

	/**
	 * Obtains the {@link FunctionObject} for the
	 * {@link ManagedFunctionObjectType}.
	 * 
	 * @param functionObjectName
	 *            Name of the {@link ManagedFunctionObjectType}.
	 * @return {@link FunctionObject}.
	 */
	FunctionObject getFunctionObject(String functionObjectName);

	/**
	 * Obtains the {@link FunctionFlow} for the
	 * {@link ManagedFunctionEscalationType}.
	 * 
	 * @param escalationType
	 *            Fully qualified class name of the {@link Throwable}
	 *            identifying the {@link ManagedFunctionEscalationType}. The
	 *            {@link Escalation} type is used rather than the name as
	 *            handling is done by the {@link Escalation} type.
	 * @return {@link FunctionFlow} for the
	 *         {@link ManagedFunctionEscalationType}.
	 */
	FunctionFlow getFunctionEscalation(String escalationType);

}