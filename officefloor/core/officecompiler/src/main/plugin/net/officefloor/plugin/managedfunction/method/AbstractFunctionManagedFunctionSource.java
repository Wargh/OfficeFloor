package net.officefloor.plugin.managedfunction.method;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import net.officefloor.compile.properties.Property;
import net.officefloor.compile.spi.managedfunction.source.FunctionNamespaceBuilder;
import net.officefloor.compile.spi.managedfunction.source.ManagedFunctionSource;
import net.officefloor.compile.spi.managedfunction.source.ManagedFunctionSourceContext;
import net.officefloor.compile.spi.managedfunction.source.impl.AbstractManagedFunctionSource;
import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.plugin.clazz.NonFunctionMethod;

/**
 * {@link ManagedFunctionSource} for a {@link Class} having the {@link Method}
 * instances as the {@link ManagedFunction} instances.
 * 
 * @author Daniel Sagenschneider
 */
public abstract class AbstractFunctionManagedFunctionSource extends AbstractManagedFunctionSource {

	/**
	 * {@link Property} name providing the {@link Class} name.
	 */
	public static final String CLASS_NAME_PROPERTY_NAME = "class.name";

	/**
	 * {@link Property} name specifying a single {@link Method} to use for the
	 * {@link ManagedFunction}.
	 */
	public static final String PROPERTY_FUNCTION_NAME = "function.name";

	/**
	 * Creates the {@link MethodObjectInstanceManufacturer}.
	 * 
	 * @param clazz {@link Class}.
	 * @return {@link MethodObjectInstanceManufacturer}.
	 * @throws Exception If fails to create
	 *                   {@link MethodObjectInstanceManufacturer}.
	 */
	protected MethodObjectInstanceManufacturer createMethodObjectInstanceManufacturer(Class<?> clazz) throws Exception {
		MethodObjectInstanceFactory instanceFactory = new DefaultConstructorMethodObjectInstanceFactory(clazz);
		return () -> instanceFactory;
	}

	/**
	 * Creates the {@link MethodManagedFunctionBuilder}.
	 * 
	 * @param namespaceBuilder {@link FunctionNamespaceBuilder}.
	 * @param context          {@link ManagedFunctionSourceContext}.
	 * @return {@link MethodManagedFunctionBuilder}.
	 * @throws Exception If fails to create {@link MethodManagedFunctionBuilder}.
	 */
	protected MethodManagedFunctionBuilder createMethodManagedFunctionBuilder(FunctionNamespaceBuilder namespaceBuilder,
			ManagedFunctionSourceContext context) throws Exception {
		return new MethodManagedFunctionBuilder();
	}

	/*
	 * =================== AbstractManagedFunctionSource ===================
	 */

	@Override
	protected void loadSpecification(SpecificationContext context) {
		context.addProperty(CLASS_NAME_PROPERTY_NAME, "Class");
	}

	public void sourceManagedFunctions(FunctionNamespaceBuilder namespaceBuilder, ManagedFunctionSourceContext context)
			throws Exception {

		// Obtain the class
		String className = context.getProperty(CLASS_NAME_PROPERTY_NAME);
		Class<?> clazz = context.loadClass(className);

		// Create the method object instance manufacturer
		MethodObjectInstanceManufacturer instanceManufacturer = this.createMethodObjectInstanceManufacturer(clazz);

		// Create the method managed function builder
		MethodManagedFunctionBuilder methodBuilder = this.createMethodManagedFunctionBuilder(namespaceBuilder, context);

		// Determine if only single method
		String singleMethodName = context.getProperty(PROPERTY_FUNCTION_NAME, null);

		// Work up the hierarchy of classes to inherit methods by name
		Set<String> includedMethodNames = new HashSet<String>();
		while ((clazz != null) && (!(Object.class.equals(clazz)))) {

			// Obtain the listing of functions from the methods of the class
			Set<String> currentClassMethods = new HashSet<String>();
			NEXT_METHOD: for (Method method : clazz.getDeclaredMethods()) {

				// Determine if include method
				if ((singleMethodName != null) && (!singleMethodName.equals(method.getName()))) {
					continue NEXT_METHOD;
				}

				// Ignore non-function methods
				if (!methodBuilder.isCandidateFunctionMethod(method)) {
					continue NEXT_METHOD;
				}

				// Determine if method already exists on the current class
				String methodName = method.getName();
				if (currentClassMethods.contains(methodName)) {
					throw new IllegalStateException("Two methods by the same name '" + methodName + "' in class "
							+ clazz.getName() + ".  Either rename one of the methods or annotate one with @"
							+ NonFunctionMethod.class.getSimpleName());
				}
				currentClassMethods.add(methodName);

				// Ignore if already included method
				if (includedMethodNames.contains(methodName)) {
					continue NEXT_METHOD;
				}
				includedMethodNames.add(methodName);

				// Build the managed function for the method
				methodBuilder.buildMethod(method, instanceManufacturer, namespaceBuilder, context);
			}

			// Add methods from the parent class on next iteration
			clazz = clazz.getSuperclass();
		}
	}

}