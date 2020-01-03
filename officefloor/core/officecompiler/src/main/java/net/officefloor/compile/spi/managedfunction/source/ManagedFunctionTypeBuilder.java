package net.officefloor.compile.spi.managedfunction.source;

import net.officefloor.frame.api.build.Indexed;
import net.officefloor.frame.api.escalate.Escalation;
import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.internal.structure.EscalationFlow;
import net.officefloor.frame.internal.structure.Flow;

/**
 * Provides means for the {@link ManagedFunctionSource} to provide a
 * <code>type definition</code> of the {@link ManagedFunction}.
 * 
 * @author Daniel Sagenschneider
 */
public interface ManagedFunctionTypeBuilder<D extends Enum<D>, F extends Enum<F>> {

	/**
	 * Adds an annotation.
	 * 
	 * @param annotation
	 *            Annotation.
	 */
	void addAnnotation(Object annotation);

	/**
	 * Specifies the type of {@link Object} returned from the
	 * {@link ManagedFunction} that is to be used as the argument to the next
	 * {@link ManagedFunction}.
	 * 
	 * @param returnType
	 *            Return type of the {@link ManagedFunction}.
	 */
	void setReturnType(Class<?> returnType);

	/**
	 * <p>
	 * Adds a {@link ManagedFunctionObjectTypeBuilder} to the
	 * {@link ManagedFunctionTypeBuilder} definition.
	 * <p>
	 * Should the dependent {@link Object} instances be {@link Indexed}, the
	 * order they are added is the order of indexing (starting at 0).
	 * 
	 * @param objectType
	 *            Type of the dependent {@link Object}.
	 * @return {@link ManagedFunctionObjectTypeBuilder} to provide the
	 *         <code>type definition</code> of the added dependent
	 *         {@link Object}.
	 */
	ManagedFunctionObjectTypeBuilder<D> addObject(Class<?> objectType);

	/**
	 * <p>
	 * Adds a {@link ManagedFunctionFlowTypeBuilder} to the
	 * {@link ManagedFunctionTypeBuilder} definition.
	 * <p>
	 * Should the {@link Flow} instigation be {@link Indexed}, the order they
	 * are added is the order of indexing (starting at 0).
	 * 
	 * @return {@link ManagedFunctionFlowTypeBuilder} to provide the
	 *         <code>type definition</code> of the possible instigated
	 *         {@link Flow} by the {@link ManagedFunction}.
	 */
	ManagedFunctionFlowTypeBuilder<F> addFlow();

	/**
	 * Adds a {@link ManagedFunctionEscalationTypeBuilder} to the
	 * {@link ManagedFunctionTypeBuilder} definition.
	 * 
	 * @param <E>
	 *            {@link Escalation} type.
	 * @param escalationType
	 *            Type to be handled by an {@link EscalationFlow}.
	 * @return {@link ManagedFunctionEscalationTypeBuilder} to provide the
	 *         <code>type definition</code>.
	 */
	<E extends Throwable> ManagedFunctionEscalationTypeBuilder addEscalation(Class<E> escalationType);

}