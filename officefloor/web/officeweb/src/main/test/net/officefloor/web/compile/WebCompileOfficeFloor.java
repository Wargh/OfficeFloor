package net.officefloor.web.compile;

import java.util.function.Consumer;

import net.officefloor.compile.spi.office.OfficeArchitect;
import net.officefloor.compile.spi.office.OfficeManagedObject;
import net.officefloor.compile.spi.office.OfficeSection;
import net.officefloor.compile.spi.office.source.OfficeSourceContext;
import net.officefloor.compile.spi.section.source.SectionSource;
import net.officefloor.compile.test.officefloor.CompileOfficeContext;
import net.officefloor.compile.test.officefloor.CompileOfficeFloor;
import net.officefloor.frame.internal.structure.ManagedObjectScope;
import net.officefloor.plugin.variable.Var;
import net.officefloor.server.http.mock.MockHttpServer;
import net.officefloor.web.WebArchitectEmployer;
import net.officefloor.web.build.HttpInput;
import net.officefloor.web.build.HttpUrlContinuation;
import net.officefloor.web.build.WebArchitect;

/**
 * Provides {@link WebArchitect} and server configuration for testing web
 * applications.
 * 
 * @author Daniel Sagenschneider
 */
public class WebCompileOfficeFloor extends CompileOfficeFloor {

	/**
	 * Context path. May be <code>null</code>.
	 */
	private final String contextPath;

	/**
	 * Instantiate with no context path.
	 */
	public WebCompileOfficeFloor() {
		this(null);
	}

	/**
	 * Instantiate with context path.
	 * 
	 * @param contextPath Context path.
	 */
	public WebCompileOfficeFloor(String contextPath) {
		this.contextPath = contextPath;
	}

	/**
	 * Adds a {@link CompileWebExtension}.
	 * 
	 * @param extension {@link CompileWebExtension}.
	 */
	public void web(CompileWebExtension extension) {
		// Wrap web extension into office extension
		this.office((context) -> {
			CompileWebContextImpl web = new CompileWebContextImpl(context);
			if (extension != null) {
				// Allow no configuration except default web
				extension.extend(web);
			}
			web.webArchitect.informOfficeArchitect();
		});
	}

	/**
	 * Loads {@link MockHttpServer}.
	 * 
	 * @param consumeMockhttpServer Receives the {@link MockHttpServer}.
	 */
	public void mockHttpServer(Consumer<MockHttpServer> consumeMockhttpServer) {
		this.officeFloor((context) -> {
			MockHttpServer server = MockHttpServer.configureMockHttpServer(context.getDeployedOffice()
					.getDeployedOfficeInput(WebArchitect.HANDLER_SECTION_NAME, WebArchitect.HANDLER_INPUT_NAME));
			if (consumeMockhttpServer != null) {
				consumeMockhttpServer.accept(server);
			}
		});
	}

	/**
	 * {@link CompileWebContext} implementation.
	 */
	private class CompileWebContextImpl implements CompileWebContext {

		/**
		 * {@link CompileOfficeContext}.
		 */
		private final CompileOfficeContext officeContext;

		/**
		 * {@link OfficeArchitect}.
		 */
		private final OfficeArchitect officeArchitect;

		/**
		 * {@link WebArchitect}.
		 */
		private final WebArchitect webArchitect;

		/**
		 * Instantiate.
		 * 
		 * @param officeContext {@link CompileOfficeContext}.
		 */
		public CompileWebContextImpl(CompileOfficeContext officeContext) {
			this.officeContext = officeContext;
			this.officeArchitect = this.officeContext.getOfficeArchitect();

			// Always employ the web architect
			this.webArchitect = WebArchitectEmployer.employWebArchitect(WebCompileOfficeFloor.this.contextPath,
					this.officeArchitect, this.officeContext.getOfficeSourceContext());
		}

		/*
		 * ================== CompileWebContext ==================
		 */

		@Override
		public WebArchitect getWebArchitect() {
			return this.webArchitect;
		}

		/*
		 * ================== CompileOfficeContext ==================
		 */

		@Override
		public OfficeArchitect getOfficeArchitect() {
			return this.officeContext.getOfficeArchitect();
		}

		@Override
		public OfficeSourceContext getOfficeSourceContext() {
			return this.officeContext.getOfficeSourceContext();
		}

		@Override
		public OfficeManagedObject addManagedObject(String managedObjectName, Class<?> managedObjectClass,
				ManagedObjectScope scope) {
			return this.officeContext.addManagedObject(managedObjectName, managedObjectClass, scope);
		}

		@Override
		public OfficeSection addSection(String sectionName, Class<?> sectionClass) {
			return this.officeContext.addSection(sectionName, sectionClass);
		}

		@Override
		public <T> void variable(String qualifier, Class<T> type, Consumer<Var<T>> compileVar) {
			this.officeContext.variable(qualifier, type, compileVar);
		}

		@Override
		public OfficeSection getOfficeSection() {
			return this.officeContext.getOfficeSection();
		}

		@Override
		public OfficeSection overrideSection(Class<? extends SectionSource> sectionSourceClass,
				String sectionLocation) {
			return this.officeContext.overrideSection(sectionSourceClass, sectionLocation);
		}

		@Override
		public HttpInput link(boolean isSecure, String httpMethodName, String applicationPath, Class<?> sectionClass) {

			// Add the section
			OfficeSection section = this.addSection(httpMethodName + "_" + applicationPath, sectionClass);

			// Create the link to the section service method
			HttpInput input = this.webArchitect.getHttpInput(isSecure, httpMethodName, applicationPath);
			this.officeArchitect.link(input.getInput(), section.getOfficeSectionInput("service"));
			return input;
		}

		@Override
		public HttpUrlContinuation link(boolean isSecure, String applicationPath, Class<?> sectionClass) {

			// Add the section
			OfficeSection section = this.addSection("GET_" + applicationPath, sectionClass);

			// Return the link to the section service method
			HttpUrlContinuation continuation = this.webArchitect.getHttpInput(isSecure, applicationPath);
			this.officeArchitect.link(continuation.getInput(), section.getOfficeSectionInput("service"));
			return continuation;
		}
	}

}