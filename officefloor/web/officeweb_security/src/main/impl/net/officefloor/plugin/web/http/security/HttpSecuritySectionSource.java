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
package net.officefloor.plugin.web.http.security;

import net.officefloor.compile.spi.section.FunctionFlow;
import net.officefloor.compile.spi.section.SectionDesigner;
import net.officefloor.compile.spi.section.SectionFunction;
import net.officefloor.compile.spi.section.SectionFunctionNamespace;
import net.officefloor.compile.spi.section.SectionInput;
import net.officefloor.compile.spi.section.SectionObject;
import net.officefloor.compile.spi.section.SectionOutput;
import net.officefloor.compile.spi.section.source.SectionSource;
import net.officefloor.compile.spi.section.source.SectionSourceContext;
import net.officefloor.compile.spi.section.source.impl.AbstractSectionSource;
import net.officefloor.plugin.web.http.security.type.HttpSecurityDependencyType;
import net.officefloor.plugin.web.http.security.type.HttpSecurityFlowType;
import net.officefloor.plugin.web.http.security.type.HttpSecurityType;
import net.officefloor.plugin.web.http.session.HttpSession;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.web.state.HttpRequestState;

/**
 * {@link SectionSource} for the {@link HttpSecuritySource}.
 * 
 * @author Daniel Sagenschneider
 */
public class HttpSecuritySectionSource extends AbstractSectionSource {

	/**
	 * Name of the {@link SectionInput} for challenging.
	 */
	public static final String INPUT_CHALLENGE = "Challenge";

	/**
	 * Name of the {@link SectionInput} for undertaking authentication with
	 * application provided credentials.
	 */
	public static final String INPUT_AUTHENTICATE = "Authenticate";

	/**
	 * Name of the {@link SectionOutput} for handling failure.
	 */
	public static final String OUTPUT_FAILURE = "Failure";

	/**
	 * Name of the {@link SectionOutput} for handling re-continuing after
	 * authentication.
	 */
	public static final String OUTPUT_RECONTINUE = "Recontinue";

	/**
	 * {@link HttpSecurityConfiguration}.
	 */
	private final HttpSecurityConfiguration<?, ?, ?, ?> httpSecurityConfiguration;

	/**
	 * Instantiate.
	 * 
	 * @param httpSecurityConfiguration
	 *            {@link HttpSecurityConfiguration}.
	 */
	public HttpSecuritySectionSource(HttpSecurityConfiguration<?, ?, ?, ?> httpSecurityConfiguration) {
		this.httpSecurityConfiguration = httpSecurityConfiguration;
	}

	/*
	 * ===================== SectionSource ===============================
	 */

	@Override
	protected void loadSpecification(SpecificationContext context) {
		// No properties required
	}

	@Override
	public void sourceSection(SectionDesigner designer, SectionSourceContext context) throws Exception {

		// Obtain the HTTP Security Type
		HttpSecurityType<?, ?, ?, ?> securityType = this.httpSecurityConfiguration.getHttpSecurityType();

		// Obtain the credentials type
		Class<?> credentialsType = HttpSecurityManagedFunctionSource.getCredentialsClass(securityType);

		// Create the dependent objects
		SectionObject serverHttpConnection = designer.addSectionObject("SERVER_HTTP_CONNECTION",
				ServerHttpConnection.class.getName());
		SectionObject httpSession = designer.addSectionObject("HTTP_SESSION", HttpSession.class.getName());
		SectionObject httpRequestState = designer.addSectionObject("HTTP_REQUEST_STATE",
				HttpRequestState.class.getName());
		SectionObject httpAuthentication = designer.addSectionObject("HTTP_AUTHENTICATION",
				HttpAuthentication.class.getName());
		HttpSecurityDependencyType<?>[] dependencyTypes = securityType.getDependencyTypes();
		SectionObject[] dependencyObjects = new SectionObject[dependencyTypes.length];
		for (int i = 0; i < dependencyObjects.length; i++) {
			HttpSecurityDependencyType<?> dependencyType = dependencyTypes[i];
			dependencyObjects[i] = designer.addSectionObject("DEPENDENCY_" + dependencyType.getDependencyName(),
					dependencyType.getDependencyType().getName());
			dependencyObjects[i].setTypeQualifier(dependencyType.getTypeQualifier());
		}

		// Create the failure flow
		SectionOutput failureOutput = designer.addSectionOutput(OUTPUT_FAILURE, Throwable.class.getName(), true);

		// Configure the HTTP Security Managed Function Source
		SectionFunctionNamespace namespace = designer.addSectionFunctionNamespace("HttpSecuritySource",
				new HttpSecurityManagedFunctionSource(this.httpSecurityConfiguration));

		// Configure the challenge handling
		SectionFunction challengeFunction = namespace.addSectionFunction(
				HttpSecurityManagedFunctionSource.FUNCTION_CHALLENGE,
				HttpSecurityManagedFunctionSource.FUNCTION_CHALLENGE);
		challengeFunction.getFunctionObject("HTTP_AUTHENTICATION_REQUIRED_EXCEPTION").flagAsParameter();
		designer.link(challengeFunction.getFunctionObject("SERVER_HTTP_CONNECTION"), serverHttpConnection);
		designer.link(challengeFunction.getFunctionObject("HTTP_SESSION"), httpSession);
		designer.link(challengeFunction.getFunctionObject("HTTP_REQUEST_STATE"), httpRequestState);
		for (SectionObject dependency : dependencyObjects) {
			designer.link(challengeFunction.getFunctionObject(dependency.getSectionObjectName()), dependency);
		}
		designer.link(challengeFunction.getFunctionFlow("FAILURE"), failureOutput, false);
		for (HttpSecurityFlowType<?> flowType : securityType.getFlowTypes()) {
			String flowName = flowType.getFlowName();
			FunctionFlow functionFlow = challengeFunction.getFunctionFlow("FLOW_" + flowName);
			SectionOutput sectionOutput;
			if (OUTPUT_FAILURE.equals(flowName)) {
				// Determine if map to existing failure output
				sectionOutput = failureOutput;
			} else {
				// Create output for the flow
				sectionOutput = designer.addSectionOutput(flowName, flowType.getArgumentType().getName(), false);
			}
			designer.link(functionFlow, sectionOutput, false);
		}

		// Configure the managed object authentication
		SectionFunction moAuthFunction = namespace.addSectionFunction(
				HttpSecurityManagedFunctionSource.FUNCTION_MANAGED_OBJECT_AUTHENTICATE,
				HttpSecurityManagedFunctionSource.FUNCTION_MANAGED_OBJECT_AUTHENTICATE);
		moAuthFunction.getFunctionObject("FUNCTION_AUTHENTICATE_CONTEXT").flagAsParameter();
		for (SectionObject dependency : dependencyObjects) {
			designer.link(moAuthFunction.getFunctionObject(dependency.getSectionObjectName()), dependency);
		}

		// Configure the managed object logout
		SectionFunction moLogoutFunction = namespace.addSectionFunction(
				HttpSecurityManagedFunctionSource.FUNCTION_MANAGED_OBJECT_LOGOUT,
				HttpSecurityManagedFunctionSource.FUNCTION_MANAGED_OBJECT_LOGOUT);
		moLogoutFunction.getFunctionObject("FUNCTION_LOGOUT_CONTEXT").flagAsParameter();
		for (SectionObject dependency : dependencyObjects) {
			designer.link(moLogoutFunction.getFunctionObject(dependency.getSectionObjectName()), dependency);
		}

		// Configure the application authentication start
		SectionFunction startAuthFunction = namespace.addSectionFunction(
				HttpSecurityManagedFunctionSource.FUNCTION_START_APPLICATION_AUTHENTICATE,
				HttpSecurityManagedFunctionSource.FUNCTION_START_APPLICATION_AUTHENTICATE);
		startAuthFunction.getFunctionObject(StartApplicationHttpAuthenticateFunction.Dependencies.CREDENTIALS.name())
				.flagAsParameter();
		designer.link(
				startAuthFunction.getFunctionObject(
						StartApplicationHttpAuthenticateFunction.Dependencies.HTTP_AUTHENTICATION.name()),
				httpAuthentication);
		designer.link(startAuthFunction.getFunctionFlow(StartApplicationHttpAuthenticateFunction.Flows.FAILURE.name()),
				failureOutput, false);

		// Configure the application authentication completion
		SectionFunction completeAuthFunction = namespace.addSectionFunction(
				HttpSecurityManagedFunctionSource.FUNCTION_COMPLETE_APPLICATION_AUTHENTICATE,
				HttpSecurityManagedFunctionSource.FUNCTION_COMPLETE_APPLICATION_AUTHENTICATE);
		designer.link(
				completeAuthFunction.getFunctionObject(
						CompleteApplicationHttpAuthenticateFunction.Dependencies.HTTP_AUTHENTICATION.name()),
				httpAuthentication);
		designer.link(
				completeAuthFunction.getFunctionObject(
						CompleteApplicationHttpAuthenticateFunction.Dependencies.SERVER_HTTP_CONNECTION.name()),
				serverHttpConnection);
		designer.link(completeAuthFunction.getFunctionObject(
				CompleteApplicationHttpAuthenticateFunction.Dependencies.HTTP_SESSION.name()), httpSession);
		designer.link(
				completeAuthFunction.getFunctionObject(
						CompleteApplicationHttpAuthenticateFunction.Dependencies.REQUEST_STATE.name()),
				httpRequestState);
		designer.link(
				completeAuthFunction.getFunctionFlow(CompleteApplicationHttpAuthenticateFunction.Flows.FAILURE.name()),
				failureOutput, false);

		// Link completion for started authentication
		designer.link(startAuthFunction, completeAuthFunction);

		// Link re-continue for completed authentication
		designer.link(completeAuthFunction, designer.addSectionOutput(OUTPUT_RECONTINUE, null, false));

		// Link inputs
		designer.link(designer.addSectionInput(INPUT_CHALLENGE, HttpAuthenticationRequiredException.class.getName()),
				challengeFunction);
		designer.link(designer.addSectionInput(INPUT_AUTHENTICATE, credentialsType.getName()), startAuthFunction);
		designer.link(
				designer.addSectionInput("ManagedObjectAuthenticate", FunctionAuthenticateContext.class.getName()),
				moAuthFunction);
		designer.link(designer.addSectionInput("ManagedObjectLogout", HttpLogoutRequest.class.getName()),
				moLogoutFunction);
	}

}