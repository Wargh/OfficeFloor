/*-
 * #%L
 * OfficeFloor integration of WAR
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

package net.officefloor.spring.webflux;

import org.springframework.http.server.reactive.HttpHandler;

import net.officefloor.compile.spi.managedfunction.source.FunctionNamespaceBuilder;
import net.officefloor.compile.spi.managedfunction.source.ManagedFunctionSource;
import net.officefloor.compile.spi.managedfunction.source.ManagedFunctionSourceContext;
import net.officefloor.compile.spi.managedfunction.source.ManagedFunctionTypeBuilder;
import net.officefloor.compile.spi.managedfunction.source.impl.AbstractManagedFunctionSource;
import net.officefloor.compile.spi.section.SectionDesigner;
import net.officefloor.compile.spi.section.SectionFunction;
import net.officefloor.compile.spi.section.SectionInput;
import net.officefloor.compile.spi.section.SectionOutput;
import net.officefloor.compile.spi.section.source.SectionSource;
import net.officefloor.compile.spi.section.source.SectionSourceContext;
import net.officefloor.compile.spi.section.source.impl.AbstractSectionSource;
import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.api.function.ManagedFunctionContext;
import net.officefloor.frame.api.function.StaticManagedFunction;
import net.officefloor.server.http.HttpResponse;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.server.http.ServerHttpConnection;

/**
 * {@link SectionSource} servicing {@link ServerHttpConnection} via Web Flux.
 * 
 * @author Daniel Sagenschneider
 */
public class WebFluxSectionSource extends AbstractSectionSource {

	/**
	 * Specifies the {@link HttpHandler}.
	 * 
	 * @param httpHandler {@link HttpHandler}.
	 */
	public static void setHttpHandler(HttpHandler httpHandler) {
		WebFluxHttpHandler handler = webFluxHttpHandler.get();
		handler.function.httpHandler = httpHandler;
		handler.attemptRelease();
	}

	/**
	 * Manages {@link ThreadLocal} for {@link WebFluxFunction} setup.
	 */
	private static class WebFluxHttpHandler {

		/**
		 * {@link WebFluxFunction}.
		 */
		private final WebFluxFunction function = new WebFluxFunction();

		/**
		 * Indicates if {@link WebFluxFunction} used.
		 */
		private boolean isFunctionCreated = false;

		/**
		 * Attempts release of {@link ThreadLocal}.
		 */
		private void attemptRelease() {
			if ((function.httpHandler != null) && (this.isFunctionCreated)) {
				webFluxHttpHandler.remove();
			}
		}
	}

	/**
	 * {@link ThreadLocal} for the {@link WebFluxHttpHandler}.
	 */
	private static final ThreadLocal<WebFluxHttpHandler> webFluxHttpHandler = new ThreadLocal<WebFluxHttpHandler>() {

		@Override
		protected WebFluxHttpHandler initialValue() {
			return new WebFluxHttpHandler();
		}
	};

	/**
	 * {@link SectionInput} name for servicing the {@link ServerHttpConnection}.
	 */
	public static final String INPUT = "serviceBySpringWeb";

	/**
	 * {@link SectionOutput} for passing on to next in chain for servicing
	 * {@link ServerHttpConnection}.
	 */
	public static final String OUTPUT = "notServiced";

	/**
	 * Name of service {@link ManagedFunction}.
	 */
	private static final String FUNCTION = "service";

	/*
	 * ========================= SectionSource ==============================
	 */

	@Override
	protected void loadSpecification(SpecificationContext context) {
		// no specification
	}

	@Override
	public void sourceSection(SectionDesigner designer, SectionSourceContext context) throws Exception {

		// Configure in servicing
		SectionFunction service = designer.addSectionFunctionNamespace(FUNCTION, new WebFluxManagedFunctionSource())
				.addSectionFunction(FUNCTION, FUNCTION);

		// Link for use
		designer.link(designer.addSectionInput(INPUT, null), service);
		designer.link(service.getFunctionFlow(FlowKeys.NOT_FOUND.name()),
				designer.addSectionOutput(OUTPUT, null, false), false);

		// Provide dependencies
		designer.link(service.getFunctionObject(DependencyKeys.SERVER_HTTP_CONNECTION.name()), designer
				.addSectionObject(ServerHttpConnection.class.getSimpleName(), ServerHttpConnection.class.getName()));
	}

	/**
	 * Dependency keys.
	 */
	private static enum DependencyKeys {
		SERVER_HTTP_CONNECTION
	}

	/**
	 * Flow keys.
	 */
	private static enum FlowKeys {
		NOT_FOUND
	}

	/**
	 * {@link ManagedFunctionSource} for {@link WebFluxFunction}.
	 */
	private static class WebFluxManagedFunctionSource extends AbstractManagedFunctionSource {

		/*
		 * ===================== ManagedFunctionSource ======================
		 */

		@Override
		protected void loadSpecification(SpecificationContext context) {
			// no specification
		}

		@Override
		public void sourceManagedFunctions(FunctionNamespaceBuilder functionNamespaceTypeBuilder,
				ManagedFunctionSourceContext context) throws Exception {

			// Create the web flux function
			WebFluxHttpHandler handler = webFluxHttpHandler.get();
			if (!context.isLoadingType()) {
				handler.isFunctionCreated = true;
				handler.attemptRelease();
			}

			// Provide service function
			ManagedFunctionTypeBuilder<DependencyKeys, FlowKeys> function = functionNamespaceTypeBuilder
					.addManagedFunctionType(FUNCTION, handler.function, DependencyKeys.class, FlowKeys.class);
			function.addObject(ServerHttpConnection.class).setKey(DependencyKeys.SERVER_HTTP_CONNECTION);
			function.addFlow().setKey(FlowKeys.NOT_FOUND);
		}
	}

	/**
	 * Web Flux {@link ManagedFunction}.
	 */
	private static class WebFluxFunction extends StaticManagedFunction<DependencyKeys, FlowKeys> {

		/**
		 * {@link HttpHandler}.
		 */
		private HttpHandler httpHandler;

		/*
		 * ======================== ManagedFunction =========================
		 */

		@Override
		public void execute(ManagedFunctionContext<DependencyKeys, FlowKeys> context) throws Throwable {

			// Obtain dependencies
			ServerHttpConnection connection = (ServerHttpConnection) context
					.getObject(DependencyKeys.SERVER_HTTP_CONNECTION);

			// Undertake servicing

			// Determine if not serviced
			HttpResponse response = connection.getResponse();
			if (HttpStatus.NOT_FOUND.equals(response.getStatus())) {

				// Reset and attempt further handling in chain
				response.reset();
				context.doFlow(FlowKeys.NOT_FOUND, null, null);
			}
		}
	}

}