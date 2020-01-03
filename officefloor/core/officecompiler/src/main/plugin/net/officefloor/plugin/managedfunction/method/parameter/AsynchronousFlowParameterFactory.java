package net.officefloor.plugin.managedfunction.method.parameter;

import net.officefloor.frame.api.function.AsynchronousFlow;
import net.officefloor.frame.api.function.ManagedFunctionContext;
import net.officefloor.plugin.managedfunction.method.MethodParameterFactory;

/**
 * {@link MethodParameterFactory} for an {@link AsynchronousFlow}.
 * 
 * @author Daniel Sagenschneider
 */
public class AsynchronousFlowParameterFactory implements MethodParameterFactory {

	/*
	 * ====================== ParameterFactory =============================
	 */

	@Override
	public Object createParameter(ManagedFunctionContext<?, ?> context) {
		return context.createAsynchronousFlow();
	}

}