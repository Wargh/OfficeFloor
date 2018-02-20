/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2013 Daniel Sagenschneider
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
package net.officefloor.web;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.officefloor.compile.impl.util.CompileUtil;
import net.officefloor.compile.managedfunction.ManagedFunctionObjectType;
import net.officefloor.compile.managedfunction.ManagedFunctionType;
import net.officefloor.compile.properties.Property;
import net.officefloor.compile.spi.office.ManagedFunctionAugmentorContext;
import net.officefloor.compile.spi.office.OfficeArchitect;
import net.officefloor.compile.spi.office.OfficeFlowSinkNode;
import net.officefloor.compile.spi.office.OfficeFlowSourceNode;
import net.officefloor.compile.spi.office.OfficeManagedObject;
import net.officefloor.compile.spi.office.OfficeManagedObjectSource;
import net.officefloor.compile.spi.office.OfficeSection;
import net.officefloor.compile.spi.office.OfficeSectionInput;
import net.officefloor.compile.spi.office.OfficeSectionOutput;
import net.officefloor.compile.spi.office.source.OfficeSourceContext;
import net.officefloor.frame.internal.structure.ManagedObjectScope;
import net.officefloor.server.http.HttpMethod;
import net.officefloor.server.http.HttpRequest;
import net.officefloor.web.HttpRouteSectionSource.Interception;
import net.officefloor.web.HttpRouteSectionSource.Redirect;
import net.officefloor.web.HttpRouteSectionSource.RouteInput;
import net.officefloor.web.accept.AcceptNegotiatorBuilderImpl;
import net.officefloor.web.build.AcceptNegotiatorBuilder;
import net.officefloor.web.build.HttpArgumentParser;
import net.officefloor.web.build.HttpInput;
import net.officefloor.web.build.HttpObjectParserFactory;
import net.officefloor.web.build.HttpObjectResponderFactory;
import net.officefloor.web.build.HttpUrlContinuation;
import net.officefloor.web.build.HttpValueLocation;
import net.officefloor.web.build.WebArchitect;
import net.officefloor.web.response.ObjectResponseManagedObjectSource;
import net.officefloor.web.session.HttpSessionManagedObjectSource;
import net.officefloor.web.session.object.HttpSessionObjectManagedObjectSource;
import net.officefloor.web.state.HttpApplicationObjectManagedObjectSource;
import net.officefloor.web.state.HttpApplicationStateManagedObjectSource;
import net.officefloor.web.state.HttpArgumentManagedObjectSource;
import net.officefloor.web.state.HttpObjectManagedObjectSource;
import net.officefloor.web.state.HttpRequestObjectManagedObjectSource;
import net.officefloor.web.state.HttpRequestStateManagedObjectSource;
import net.officefloor.web.tokenise.FormHttpArgumentParser;

/**
 * {@link WebArchitect} implementation.
 * 
 * @author Daniel Sagenschneider
 */
public class WebArchitectEmployer implements WebArchitect {

	/**
	 * Name of {@link Property} specifying the context path.
	 */
	public static final String PROPERTY_CONTEXT_PATH = "context.path";

	/**
	 * Employs a {@link WebArchitect}.
	 * 
	 * @param officeArchitect
	 *            {@link OfficeArchitect}.
	 * @param officeSourceContext
	 *            {@link OfficeSourceContext} used to source {@link Property}
	 *            values to configure the {@link WebArchitect}.
	 * @return {@link WebArchitect}.
	 */
	public static WebArchitect employWebArchitect(OfficeArchitect officeArchitect,
			OfficeSourceContext officeSourceContext) {

		// Obtain the context path
		String contextPath = officeSourceContext.getProperty(PROPERTY_CONTEXT_PATH, null);

		// Employ the web architect
		return employWebArchitect(officeArchitect, contextPath);
	}

	/**
	 * Employs a {@link WebArchitect}.
	 * 
	 * @param officeArchitect
	 *            {@link OfficeArchitect}.
	 * @param contextPath
	 *            Context path for the web application. May be <code>null</code>
	 *            for no context path.
	 * @return {@link WebArchitect}.
	 */
	public static WebArchitect employWebArchitect(OfficeArchitect officeArchitect, String contextPath) {
		return new WebArchitectEmployer(officeArchitect, contextPath);
	}

	/**
	 * {@link OfficeArchitect}.
	 */
	private final OfficeArchitect officeArchitect;

	/**
	 * Context path. May be <code>null</code> for no context path.
	 */
	private final String contextPath;

	/**
	 * {@link HttpRouteSectionSource}.
	 */
	private final HttpRouteSectionSource routing;

	/**
	 * Routing {@link OfficeSection}.
	 */
	private final OfficeSection routingSection;

	/**
	 * Registry of HTTP arguments to its {@link OfficeManagedObject}.
	 */
	private final Map<String, OfficeManagedObject> httpArguments = new HashMap<>();

	/**
	 * Singleton {@link List} provided to the
	 * {@link HttpObjectManagedObjectSource} for the registered
	 * {@link HttpObjectParserFactory} instances.
	 */
	private final List<HttpObjectParserFactory> singletonObjectParserList = new LinkedList<>();

	/**
	 * Registry of {@link HttpObject} {@link Annotation} alias to accepted
	 * <code>content-type</code> values. Note: the keys indicate the aliases, as
	 * accepted <code>content-type</code> values are optional.
	 */
	private final Map<Class<?>, String[]> httpObjectAliases = new HashMap<>();

	/**
	 * Registry of HTTP objects by their {@link Class}.
	 */
	private final Map<Class<?>, OfficeManagedObject> httpObjects = new HashMap<>();

	/**
	 * Registry of HTTP Application Object to its {@link OfficeManagedObject}.
	 */
	private final Map<String, OfficeManagedObject> httpApplicationObjects = new HashMap<>();

	/**
	 * Registry of HTTP Session Object to its {@link OfficeManagedObject}.
	 */
	private final Map<String, OfficeManagedObject> httpSessionObjects = new HashMap<>();

	/**
	 * Registry of HTTP Request Object to its {@link OfficeManagedObject}.
	 */
	private final Map<String, OfficeManagedObject> httpRequestObjects = new HashMap<>();

	/**
	 * {@link HttpObjectResponderFactory} instances.
	 */
	private final List<HttpObjectResponderFactory> objectResponderFactories = new LinkedList<>();

	/**
	 * {@link HttpInputImpl} instances.
	 */
	private final List<HttpInputImpl> inputs = new LinkedList<>();

	/**
	 * {@link Interceptor} instances.
	 */
	private final List<Interceptor> interceptors = new LinkedList<>();

	/**
	 * {@link ChainedServicer} instances.
	 */
	private final List<ChainedServicer> chainedServicers = new LinkedList<>();

	/**
	 * Instantiate.
	 * 
	 * @param officeArchitect
	 *            {@link OfficeArchitect}.
	 * @param contextPath
	 *            Context path for the web application. May be <code>null</code>
	 *            for no context path.
	 */
	private WebArchitectEmployer(OfficeArchitect officeArchitect, String contextPath) {
		this.officeArchitect = officeArchitect;
		this.contextPath = contextPath;
		this.routing = new HttpRouteSectionSource(this.contextPath);
		this.routingSection = this.officeArchitect.addOfficeSection(HANDLER_SECTION_NAME, this.routing, null);
	}

	/**
	 * Obtains the bind name for the {@link OfficeManagedObject}.
	 * 
	 * @param objectClass
	 *            {@link Class} of the {@link Object}.
	 * @param bindName
	 *            Optional bind name. May be <code>null</code>.
	 * @return Bind name for the {@link OfficeManagedObject};
	 */
	private static String getBindName(Class<?> objectClass, String bindName) {
		return (CompileUtil.isBlank(bindName) ? objectClass.getName() : bindName);
	}

	/*
	 * ======================== WebArchitect =========================
	 */

	@Override
	public OfficeManagedObject addHttpArgument(String parameterName, HttpValueLocation location) {

		// Obtain the bind name
		String bindName = "HTTP_" + (location == null ? "ANY" : location.name()) + "_" + parameterName;
		OfficeManagedObject object = this.httpArguments.get(bindName);
		if (object != null) {
			return object; // return the already register object
		}

		// Not registered, so register
		OfficeManagedObjectSource mos = this.officeArchitect.addOfficeManagedObjectSource(bindName,
				new HttpArgumentManagedObjectSource(parameterName, location));
		object = mos.addOfficeManagedObject(bindName, ManagedObjectScope.PROCESS);
		this.httpArguments.put(bindName, object);

		// Return the object
		return object;
	}

	@Override
	public OfficeManagedObject addHttpApplicationObject(Class<?> objectClass, String bindName) {

		// Determine if already registered
		bindName = getBindName(objectClass, bindName);
		OfficeManagedObject object = this.httpApplicationObjects.get(bindName);
		if (object != null) {
			return object; // return the already registered object
		}

		// Not registered, so register
		OfficeManagedObjectSource mos = this.officeArchitect.addOfficeManagedObjectSource(bindName,
				HttpApplicationObjectManagedObjectSource.class.getName());
		mos.addProperty(HttpApplicationObjectManagedObjectSource.PROPERTY_CLASS_NAME, objectClass.getName());
		if ((bindName != null) && (bindName.trim().length() > 0)) {
			mos.addProperty(HttpApplicationObjectManagedObjectSource.PROPERTY_BIND_NAME, bindName);
		}
		object = mos.addOfficeManagedObject(bindName, ManagedObjectScope.PROCESS);
		this.httpApplicationObjects.put(bindName, object);

		// Return the object
		return object;
	}

	@Override
	public OfficeManagedObject addHttpApplicationObject(Class<?> objectClass) {
		return this.addHttpApplicationObject(objectClass, null);
	}

	@Override
	public OfficeManagedObject addHttpSessionObject(Class<?> objectClass, String bindName) {

		// Determine if already registered
		bindName = getBindName(objectClass, bindName);
		OfficeManagedObject object = this.httpSessionObjects.get(objectClass);
		if (object != null) {
			return object; // return the already registered object
		}

		// Not registered, so register
		OfficeManagedObjectSource mos = this.officeArchitect.addOfficeManagedObjectSource(bindName,
				HttpSessionObjectManagedObjectSource.class.getName());
		mos.addProperty(HttpSessionObjectManagedObjectSource.PROPERTY_CLASS_NAME, objectClass.getName());
		if ((bindName != null) && (bindName.trim().length() > 0)) {
			mos.addProperty(HttpSessionObjectManagedObjectSource.PROPERTY_BIND_NAME, bindName);
		}
		object = mos.addOfficeManagedObject(bindName, ManagedObjectScope.PROCESS);
		this.httpSessionObjects.put(bindName, object);

		// Return the object
		return object;
	}

	@Override
	public OfficeManagedObject addHttpSessionObject(Class<?> objectClass) {
		return this.addHttpSessionObject(objectClass, null);
	}

	@Override
	public OfficeManagedObject addHttpRequestObject(Class<?> objectClass, boolean isLoadParameters, String bindName) {

		// Determine if already registered
		bindName = getBindName(objectClass, bindName);
		OfficeManagedObject object = this.httpRequestObjects.get(bindName);
		if (object == null) {

			// Not registered, so register
			OfficeManagedObjectSource mos = this.officeArchitect.addOfficeManagedObjectSource(bindName,
					HttpRequestObjectManagedObjectSource.class.getName());
			mos.addProperty(HttpRequestObjectManagedObjectSource.PROPERTY_CLASS_NAME, objectClass.getName());
			if ((bindName != null) && (bindName.trim().length() > 0)) {
				mos.addProperty(HttpRequestObjectManagedObjectSource.PROPERTY_BIND_NAME, bindName);
			}
			object = mos.addOfficeManagedObject(bindName, ManagedObjectScope.PROCESS);
			this.httpRequestObjects.put(bindName, object);

			// Determine if load HTTP parameters
			if (isLoadParameters) {

				// Add the property to load parameters
				mos.addProperty(HttpRequestObjectManagedObjectSource.PROPERTY_IS_LOAD_HTTP_PARAMETERS,
						String.valueOf(true));
			}
		}

		// Return the object
		return object;
	}

	@Override
	public OfficeManagedObject addHttpRequestObject(Class<?> objectClass, boolean isLoadParameters) {
		return this.addHttpRequestObject(objectClass, isLoadParameters, null);
	}

	@Override
	public void addHttpObjectParser(HttpObjectParserFactory objectParserFactory) {
		this.singletonObjectParserList.add(objectParserFactory);
	}

	@Override
	public void addHttpObjectAnnotationAlias(Class<?> httpObjectAnnotationAliasClass, String... acceptedContentTypes) {
		this.httpObjectAliases.put(httpObjectAnnotationAliasClass, acceptedContentTypes);
	}

	@Override
	public OfficeManagedObject addHttpObject(Class<?> objectClass, String... acceptedContentTypes) {

		// Determine if already registered
		OfficeManagedObject object = this.httpObjects.get(objectClass);
		if (object == null) {

			// Not registered, so register
			OfficeManagedObjectSource mos = this.officeArchitect.addOfficeManagedObjectSource(objectClass.getName(),
					new HttpObjectManagedObjectSource<>(objectClass, acceptedContentTypes,
							this.singletonObjectParserList));
			object = mos.addOfficeManagedObject(objectClass.getName(), ManagedObjectScope.PROCESS);
			this.httpObjects.put(objectClass, object);
		}

		// Return the object
		return object;
	}

	@Override
	public void addHttpObjectResponder(HttpObjectResponderFactory objectResponderFactory) {
		this.objectResponderFactories.add(objectResponderFactory);
	}

	@Override
	public boolean isPathParameters(String path) {
		return this.routing.isPathParameters(path);
	}

	@Override
	public HttpUrlContinuation link(boolean isSecure, String applicationPath, OfficeFlowSinkNode flowSinkNode) {
		HttpUrlContinuationImpl continuation = new HttpUrlContinuationImpl(isSecure, applicationPath, flowSinkNode);
		this.inputs.add(continuation);
		return continuation;
	}

	@Override
	public HttpInput link(boolean isSecure, HttpMethod httpMethod, String applicationPath,
			OfficeFlowSinkNode flowSinkNode) {
		HttpInputImpl input = new HttpInputImpl(isSecure, httpMethod, applicationPath, flowSinkNode);
		this.inputs.add(input);
		return input;
	}

	@Override
	public void reroute(OfficeFlowSourceNode flowSourceNode) {
		this.officeArchitect.link(flowSourceNode, this.routingSection.getOfficeSectionInput(HANDLER_INPUT_NAME));
	}

	@Override
	public void intercept(OfficeFlowSinkNode flowSinkNode, OfficeFlowSourceNode flowSourceNode) {
		this.interceptors.add(new Interceptor(flowSinkNode, flowSourceNode));
	}

	@Override
	public void chainServicer(OfficeFlowSinkNode flowSinkNode, OfficeFlowSourceNode notHandledOutput) {
		this.chainedServicers.add(new ChainedServicer(flowSinkNode, notHandledOutput));
	}

	@Override
	public <H> AcceptNegotiatorBuilder<H> createAcceptNegotiator() {
		return new AcceptNegotiatorBuilderImpl<>();
	}

	@Override
	public void informOfficeArchitect() {

		// Auto wire the objects
		this.officeArchitect.enableAutoWireObjects();

		// Configure HTTP Session (allowing 10 seconds to retrieve session)
		OfficeManagedObjectSource httpSessionMos = this.officeArchitect.addOfficeManagedObjectSource("HTTP_SESSION",
				HttpSessionManagedObjectSource.class.getName());
		httpSessionMos.setTimeout(10 * 1000); // TODO make configurable
		httpSessionMos.addOfficeManagedObject("HTTP_SESSION", ManagedObjectScope.PROCESS);

		// Load the argument parsers
		HttpArgumentParser[] argumentParsers = new HttpArgumentParser[] { new FormHttpArgumentParser() };

		// Configure the HTTP Application and Request State
		this.officeArchitect
				.addOfficeManagedObjectSource("HTTP_APPLICATION_STATE",
						new HttpApplicationStateManagedObjectSource(this.contextPath))
				.addOfficeManagedObject("HTTP_APPLICATION_STATE", ManagedObjectScope.PROCESS);
		this.officeArchitect
				.addOfficeManagedObjectSource("HTTP_REQUEST_STATE",
						new HttpRequestStateManagedObjectSource(argumentParsers))
				.addOfficeManagedObject("HTTP_REQUEST_STATE", ManagedObjectScope.PROCESS);

		// Configure the object responder (if configured factories)
		if (this.objectResponderFactories.size() > 0) {
			ObjectResponseManagedObjectSource objectResponseMos = new ObjectResponseManagedObjectSource(
					this.objectResponderFactories);
			this.officeArchitect.addOfficeManagedObjectSource("OBJECT_RESPONSE", objectResponseMos)
					.addOfficeManagedObject("OBJECT_RESPONSE", ManagedObjectScope.PROCESS);
			this.routing.setHttpEscalationHandler(objectResponseMos);
		}

		// Determine if intercept
		if (this.interceptors.size() > 0) {

			// Obtain the interception
			Interception interception = routing.getInterception();

			// Obtain the section output
			OfficeFlowSourceNode interceptionOutput = this.routingSection
					.getOfficeSectionOutput(interception.getOutputName());
			for (Interceptor interceptor : this.interceptors) {

				// Link in interception
				this.officeArchitect.link(interceptionOutput, interceptor.flowSinkNode);

				// Set up for next iteration
				interceptionOutput = interceptor.flowSourceNode;
			}

			// Link interception back to routing
			OfficeSectionInput routingInput = this.routingSection.getOfficeSectionInput(interception.getInputName());
			this.officeArchitect.link(interceptionOutput, routingInput);
		}

		// Configure the routing
		for (HttpInputImpl input : this.inputs) {

			// Link route output to handling section input
			OfficeSectionOutput routeOutput = this.routingSection
					.getOfficeSectionOutput(input.routeInput.getOutputName());
			this.officeArchitect.link(routeOutput, input.flowSinkNode);
		}

		// Load in-line configured dependencies
		final Set<Class<?>> httpParameters = new HashSet<>();
		this.officeArchitect.addManagedFunctionAugmentor((context) -> {
			ManagedFunctionType<?, ?> functionType = context.getManagedFunctionType();
			for (ManagedFunctionObjectType<?> functionParameterType : functionType.getObjectTypes()) {
				Class<?> objectType = functionParameterType.getObjectType();

				// Determine if in-line configuration of dependency
				for (Object annotation : functionParameterType.getAnnotations()) {

					// Application object
					if (annotation instanceof HttpApplicationStateful) {
						HttpApplicationStateful stateful = (HttpApplicationStateful) annotation;
						this.addHttpApplicationObject(objectType, stateful.bind());
					}

					// Session object
					if (annotation instanceof HttpSessionStateful) {
						HttpSessionStateful stateful = (HttpSessionStateful) annotation;
						this.addHttpSessionObject(objectType, stateful.bind());
					}

					// HTTP parameters
					if (annotation instanceof HttpParameters) {
						// Load as HTTP parameters (only once)
						if (!httpParameters.contains(objectType)) {
							this.addHttpRequestObject(objectType, true);
							httpParameters.add(objectType);
						}
					}

					// HTTP object
					if (annotation instanceof HttpObject) {
						HttpObject httpObject = (HttpObject) annotation;
						String[] acceptedContentTypes = httpObject.acceptedContentTypes();
						this.addHttpObject(objectType, acceptedContentTypes);
					}

					// Determine if HTTP object alias annotation
					String[] acceptedContentTypes = WebArchitectEmployer.this.httpObjectAliases
							.get(annotation instanceof Annotation ? ((Annotation) annotation).annotationType()
									: annotation.getClass());
					if (acceptedContentTypes != null) {
						this.addHttpObject(objectType, acceptedContentTypes);
					}

					// Load HTTP arguments
					WebArchitectEmployer.this.loadInlineHttpArgument(annotation, HttpPathParameter.class,
							HttpValueLocation.PATH, objectType, context, (parameter) -> parameter.value(),
							(parameter) -> new HttpPathParameter.HttpPathParameterNameFactory()
									.getQualifierName(parameter));
					WebArchitectEmployer.this.loadInlineHttpArgument(annotation, HttpQueryParameter.class,
							HttpValueLocation.QUERY, objectType, context, (parameter) -> parameter.value(),
							(parameter) -> new HttpQueryParameter.HttpQueryParameterNameFactory()
									.getQualifierName(parameter));
					WebArchitectEmployer.this.loadInlineHttpArgument(annotation, HttpHeaderParameter.class,
							HttpValueLocation.HEADER, objectType, context, (parameter) -> parameter.value(),
							(parameter) -> new HttpHeaderParameter.HttpHeaderParameterNameFactory()
									.getQualifierName(parameter));
					WebArchitectEmployer.this.loadInlineHttpArgument(annotation, HttpCookieParameter.class,
							HttpValueLocation.COOKIE, objectType, context, (parameter) -> parameter.value(),
							(parameter) -> new HttpCookieParameter.HttpCookieParameterNameFactory()
									.getQualifierName(parameter));
					WebArchitectEmployer.this.loadInlineHttpArgument(annotation, HttpContentParameter.class,
							HttpValueLocation.ENTITY, objectType, context, (parameter) -> parameter.value(),
							(parameter) -> new HttpContentParameter.HttpContentParameterNameFactory()
									.getQualifierName(parameter));
				}
			}
		});

		// Chain in the servicer instances
		OfficeFlowSourceNode chainOutput = this.routingSection
				.getOfficeSectionOutput(HttpRouteSectionSource.UNHANDLED_OUTPUT_NAME);
		NEXT_CHAINED_SERVICER: for (ChainedServicer servicer : this.chainedServicers) {

			// Do nothing if no output (all handled by previous servicer)
			if (chainOutput == null) {
				continue NEXT_CHAINED_SERVICER;
			}

			// Link output to to input
			this.officeArchitect.link(chainOutput, servicer.flowSinkNode);

			// Set up for next chain
			chainOutput = servicer.notHandledOutput;
		}

		// Configure not handled
		if (chainOutput != null) {
			this.officeArchitect.link(chainOutput,
					this.routingSection.getOfficeSectionInput(HttpRouteSectionSource.NOT_FOUND_INPUT_NAME));
		}
	}

	/**
	 * Loads the in-line HTTP argument.
	 * 
	 * @param annotation
	 *            {@link Annotation}.
	 * @param annotationType
	 *            Type of {@link Annotation}.
	 * @param valueLocation
	 *            {@link HttpValueLocation}.
	 * @param objectType
	 *            Parameter object type.
	 * @param context
	 *            {@link ManagedFunctionAugmentorContext}.
	 * @param getParameterName
	 *            {@link Function} to obtain the parameter name.
	 * @param getQualifierName
	 *            {@link Function} to obtain the type qualification name.
	 */
	private <P extends Annotation> void loadInlineHttpArgument(Object annotation, Class<P> annotationType,
			HttpValueLocation valueLocation, Class<?> objectType, ManagedFunctionAugmentorContext context,
			Function<P, String> getParameterName, Function<P, String> getQualifierName) {

		// Ensure appropriate annotation
		if (!(annotation instanceof Annotation)) {
			return;
		}
		if (((Annotation) annotation).annotationType() != annotationType) {
			return;
		}

		// Obtain the parameter
		@SuppressWarnings("unchecked")
		P parameterAnnotation = (P) annotation;

		// Ensure parameter object is a String
		if (objectType != String.class) {
			this.officeArchitect.addIssue("Parameter must be " + String.class.getName() + " but was "
					+ objectType.getName() + " for function " + context.getManagedFunctionName());
		}

		// Add the HTTP argument
		String parameterName = getParameterName.apply(parameterAnnotation);
		String typeQualifier = getQualifierName.apply(parameterAnnotation);
		this.addHttpArgument(parameterName, valueLocation).addTypeQualification(typeQualifier, String.class.getName());
	}

	/**
	 * {@link HttpInput} implementation.
	 */
	private class HttpInputImpl implements HttpInput {

		/**
		 * Indicates if secure.
		 */
		protected final boolean isSecure;

		/**
		 * {@link HttpMethod}.
		 */
		private final HttpMethod method;

		/**
		 * Application path.
		 */
		protected final String applicationPath;

		/**
		 * Handling {@link OfficeFlowSinkNode}.
		 */
		private final OfficeFlowSinkNode flowSinkNode;

		/**
		 * {@link RouteInput}
		 */
		protected final RouteInput routeInput;

		/**
		 * Instantiate.
		 * 
		 * @param isSecure
		 *            Indicates if secure.
		 * @param method
		 *            {@link HttpMethod}.
		 * @param applicationPath
		 *            Application path.
		 * @param flowSinkNode
		 *            Handling {@link OfficeFlowSinkNode}.
		 */
		private HttpInputImpl(boolean isSecure, HttpMethod method, String applicationPath,
				OfficeFlowSinkNode flowSinkNode) {
			this.isSecure = isSecure;
			this.method = method;
			this.applicationPath = applicationPath;
			this.flowSinkNode = flowSinkNode;
			this.routeInput = WebArchitectEmployer.this.routing.addRoute(isSecure, this.method, this.applicationPath);
		}

		/*
		 * ================== HttpInput ====================
		 */

		@Override
		public HttpInputPath getPath() {
			return this.routeInput.getHttpInputPath();
		}

	}

	/**
	 * {@link HttpUrlContinuation} implementation.
	 */
	private class HttpUrlContinuationImpl extends HttpInputImpl implements HttpUrlContinuation {

		/**
		 * Instantiate.
		 * 
		 * @param isSecure
		 *            Indicates if secure.
		 * @param applicationPath
		 *            Application path.
		 * @param flowSinkNode
		 *            Handling {@link OfficeFlowSinkNode}.
		 */
		private HttpUrlContinuationImpl(boolean isSecure, String applicationPath, OfficeFlowSinkNode flowSinkNode) {
			super(isSecure, HttpMethod.GET, applicationPath, flowSinkNode);
		}

		/*
		 * =============== HttpUrlContinuation =============
		 */

		@Override
		public OfficeFlowSinkNode getRedirect(Class<?> parameterType) {
			try {

				// Create the redirect
				Redirect redirect = WebArchitectEmployer.this.routing.addRedirect(this.isSecure, this.routeInput,
						parameterType);

				// Return the section input for redirect
				return WebArchitectEmployer.this.routingSection.getOfficeSectionInput(redirect.getInputName());

			} catch (Exception ex) {
				throw WebArchitectEmployer.this.officeArchitect
						.addIssue(
								"Failed to create redirect to " + this.applicationPath + (parameterType == null
										? " with null value type" : " with values type " + parameterType.getName()),
								ex);
			}
		}
	}

	/**
	 * Intercepts the {@link HttpRequest} before web application functionality.
	 */
	private static class Interceptor {

		/**
		 * {@link OfficeFlowSinkNode}.
		 */
		public final OfficeFlowSinkNode flowSinkNode;

		/**
		 * {@link OfficeFlowSourceNode}.
		 */
		public final OfficeFlowSourceNode flowSourceNode;

		/**
		 * Initiate.
		 * 
		 * @param flowSinkNode
		 *            {@link OfficeFlowSinkNode}.
		 * @param flowSourceNode
		 *            {@link OfficeFlowSourceNode}.
		 */
		private Interceptor(OfficeFlowSinkNode flowSinkNode, OfficeFlowSourceNode flowSourceNode) {
			this.flowSinkNode = flowSinkNode;
			this.flowSourceNode = flowSourceNode;
		}
	}

	/**
	 * Chained servicer.
	 */
	private static class ChainedServicer {

		/**
		 * {@link OfficeFlowSinkNode}.
		 */
		public final OfficeFlowSinkNode flowSinkNode;

		/**
		 * {@link OfficeFlowSourceNode}. May be <code>null</code>.
		 */
		public final OfficeFlowSourceNode notHandledOutput;

		/**
		 * Initiate.
		 * 
		 * @param flowSinkNode
		 *            {@link OfficeFlowSinkNode}.
		 * @param notHandledOutput
		 *            {@link OfficeFlowSourceNode}. May be <code>null</code>.
		 */
		private ChainedServicer(OfficeFlowSinkNode flowSinkNode, OfficeFlowSourceNode notHandledOutput) {
			this.flowSinkNode = flowSinkNode;
			this.notHandledOutput = notHandledOutput;
		}
	}

}