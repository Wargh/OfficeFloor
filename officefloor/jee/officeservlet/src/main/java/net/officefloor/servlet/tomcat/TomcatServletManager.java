/*-
 * #%L
 * Servlet
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

package net.officefloor.servlet.tomcat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.coyote.InputBuffer;
import org.apache.coyote.OutputBuffer;
import org.apache.coyote.Request;
import org.apache.coyote.Response;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.net.ApplicationBufferHandler;

import net.officefloor.frame.api.function.AsynchronousFlow;
import net.officefloor.frame.api.function.AsynchronousFlowCompletion;
import net.officefloor.server.http.HttpHeader;
import net.officefloor.server.http.HttpRequest;
import net.officefloor.server.http.HttpResponse;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.servlet.FilterServicer;
import net.officefloor.servlet.ServletManager;
import net.officefloor.servlet.ServletServicer;
import net.officefloor.servlet.inject.InjectContext;
import net.officefloor.servlet.inject.InjectContextFactory;
import net.officefloor.servlet.inject.InjectionRegistry;
import net.officefloor.servlet.supply.ServletSupplierSource;

/**
 * {@link Tomcat} {@link ServletServicer}.
 * 
 * @author Daniel Sagenschneider
 */
public class TomcatServletManager implements ServletManager, ServletServicer {

	/**
	 * Operation to run.
	 */
	@FunctionalInterface
	public static interface Operation<R, T extends Throwable> {

		/**
		 * Logic of operation.
		 * 
		 * @return Result.
		 * @throws T Possible failure.
		 */
		R run() throws T;
	}

	/**
	 * Indicates if within Maven <code>war</code> project.
	 */
	private static final ThreadLocal<Boolean> isWithinMavenWarProject = new ThreadLocal<>();

	/**
	 * Runs the {@link Operation} assuming within Maven <code>war</code> project.
	 * 
	 * @param <R>       Return type.
	 * @param <T>       Possible exception type.
	 * @param operation {@link Operation}.
	 * @return Result.
	 * @throws T Possible failure.
	 */
	public static <R, T extends Throwable> R runInMavenWarProject(Operation<R, T> operation) throws T {
		Boolean original = isWithinMavenWarProject.get();
		try {
			// Flag within war project
			isWithinMavenWarProject.set(Boolean.TRUE);

			// Undertake operation
			return operation.run();

		} finally {
			// Determine if clear (as specified)
			if (original == null) {
				isWithinMavenWarProject.remove();
			}
		}
	}

	/**
	 * {@link ThreadLocal} for this {@link TomcatServletManager}.
	 */
	private static final ThreadLocal<TomcatServletManager> tomcatServletManager = new ThreadLocal<>();

	/**
	 * {@link Tomcat} for embedded {@link Servlet} container.
	 */
	private final Tomcat tomcat;

	/**
	 * {@link Connector}.
	 */
	private final Connector connector;

	/**
	 * {@link Context}.
	 */
	private final Context context;

	/**
	 * {@link InjectionRegistry}.
	 */
	private final InjectionRegistry injectionRegistry;

	/**
	 * {@link ClassLoader}.
	 */
	private final ClassLoader classLoader;

	/**
	 * {@link OfficeFloorProtocol}.
	 */
	private final OfficeFloorProtocol protocol;

	/**
	 * Registered {@link Servlet} instances.
	 */
	private final Map<String, ServletServicer> registeredServlets = new HashMap<>();

	/**
	 * Registered {@link Filter} instances.
	 */
	private final Map<String, FilterServicer> registeredFilters = new HashMap<>();

	/**
	 * Indicates if to chain in this {@link ServletManager}.
	 */
	private boolean isChainInServletManager = false;

	/**
	 * Indicates if chain decision made, so no longer able to flag.
	 */
	private boolean isChainDecisionMade = false;

	/**
	 * {@link InjectContextFactory}.
	 */
	private InjectContextFactory injectContextFactory;

	/**
	 * Instantiate.
	 * 
	 * @param contextPath       Context path.
	 * @param injectionRegistry {@link InjectionRegistry}.
	 * @param classLoader       {@link ClassLoader}.
	 * @param webAppPath        Path to web application (WAR). May be
	 *                          <code>null</code>.
	 * @throws IOException If fails to setup container.
	 */
	public TomcatServletManager(String contextPath, InjectionRegistry injectionRegistry, ClassLoader classLoader,
			String webAppPath) throws IOException {
		this.injectionRegistry = injectionRegistry;
		this.classLoader = classLoader;

		// Create OfficeFloor connector
		this.connector = new Connector(OfficeFloorProtocol.class.getName());
		this.connector.setPort(1);
		this.connector.setThrowOnFailure(true);

		// Obtain the username
		String username = System.getProperty("user.name");

		// Create the base directory (and directory for expanding)
		Path baseDir = Files.createTempDirectory(username + "_tomcat_base");
		Path webAppsDir = Path.of(baseDir.toAbsolutePath().toString(), "webapps");
		Files.createDirectories(webAppsDir);

		// Setup tomcat
		this.tomcat = new Tomcat();
		this.tomcat.setBaseDir(baseDir.toAbsolutePath().toString());
		this.tomcat.setConnector(this.connector);
		this.tomcat.getHost().setAutoDeploy(false);

		// Configure webapp directory
		if (webAppPath == null) {
			Path tempWebApp = Files.createTempDirectory(username + "_webapp");
			webAppPath = tempWebApp.toAbsolutePath().toString();
		}

		// Create the context
		String contextName = ((contextPath == null) || (contextPath.equals("/"))) ? "" : contextPath;
		this.context = this.tomcat.addWebapp(contextName, webAppPath);

		// Obtain OfficeFloor protocol to input request
		this.protocol = (OfficeFloorProtocol) this.connector.getProtocolHandler();

		// Listen for setup
		tomcatServletManager.set(this);
		this.context.addApplicationListener(SetupApplicationListener.class.getName());

		// Determine if load for running in Maven war project
		if (isWithinMavenWarProject.get() != null) {
			WebResourceRoot resources = new StandardRoot(this.context);
			resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
					new File("target/test-classes").getAbsolutePath(), "/"));
			resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
					new File("target/classes").getAbsolutePath(), "/"));
			this.context.setResources(resources);
		}
	}

	public static class SetupApplicationListener implements ServletContextListener {
		@Override
		public void contextInitialized(ServletContextEvent sce) {
			ServletContext servletContext = sce.getServletContext();

			// Register servlets
			servletContext.getServletRegistrations().forEach((name, registration) -> {
				ServletSupplierSource.registerForInjection(registration.getClassName());
			});

			// Register filters
			servletContext.getFilterRegistrations().forEach((name, registration) -> {
				ServletSupplierSource.registerForInjection(registration.getClassName());
			});

			// Load the instance manager
			TomcatServletManager servletManager = tomcatServletManager.get();
			InjectContextFactory factory = servletManager.injectionRegistry.createInjectContextFactory();
			servletManager.context
					.setInstanceManager(new OfficeFloorInstanceManager(factory, servletManager.classLoader));
			tomcatServletManager.remove();
		}
	}

	/**
	 * Indicates if chain in the {@link ServletManager}.
	 * 
	 * @return <code>true</code> to chain in the {@link ServletManager}.
	 */
	public boolean isChainServletManager() {

		// Flag that decision made
		this.isChainDecisionMade = true;

		// Indicates if chain in servlet manager
		return this.isChainInServletManager;
	}

	/**
	 * Starts the {@link Servlet} container.
	 * 
	 * @throws Exception If fails to start.
	 */
	public void start() throws Exception {

		// Start tomcat
		this.tomcat.start();

		// Instantiate context factory
		this.injectContextFactory = this.injectionRegistry.createInjectContextFactory();
	}

	/**
	 * Stops the {@link Servlet} container.
	 * 
	 * @throws Exception If fails to stop.
	 */
	public void stop() throws Exception {
		this.tomcat.stop();
		this.tomcat.destroy();
	}

	/*
	 * ===================== ServletServicer =======================
	 */

	@Override
	public void service(ServerHttpConnection connection, Executor executor, AsynchronousFlow asynchronousFlow,
			AsynchronousFlowCompletion asynchronousFlowCompletion, Map<String, ? extends Object> attributes)
			throws Exception {
		this.service(connection, executor, asynchronousFlow, asynchronousFlowCompletion, attributes, null,
				this.protocol.getAdapter()::service);
	}

	/*
	 * ===================== ServletManager ========================
	 */

	@Override
	public Context getContext() {
		return this.context;
	}

	@Override
	public ServletServicer addServlet(String name, Class<? extends Servlet> servletClass, Consumer<Wrapper> decorator) {

		// Determine if already registered
		ServletServicer servletServicer = this.registeredServlets.get(name);
		if (servletServicer != null) {
			return servletServicer;
		}

		// Add the servlet
		Wrapper wrapper = Tomcat.addServlet(this.context, name, servletClass.getName());

		// Decorate the servlet
		if (decorator != null) {
			decorator.accept(wrapper);
		}

		// Ensure not override name and servlet
		wrapper.setName(name);
		wrapper.setServlet(null);
		wrapper.setServletClass(servletClass.getName());

		// Always support async
		wrapper.setAsyncSupported(true);

		// Provide servicer
		ContainerAdapter adapter = new ContainerAdapter(wrapper, this.connector, this.classLoader);
		servletServicer = (connection, executor, asynchronousFlow, asynchronousFlowCompletion, attributes) -> this
				.service(connection, executor, asynchronousFlow, asynchronousFlowCompletion, attributes, null,
						adapter::service);

		// Register and return servicer
		this.registeredServlets.put(name, servletServicer);
		return servletServicer;
	}

	@Override
	public FilterServicer addFilter(String name, Class<? extends Filter> filterClass, Consumer<FilterDef> decorator) {

		// Determine if already registered
		FilterServicer filterServicer = this.registeredFilters.get(name);
		if (filterServicer != null) {
			return filterServicer;
		}

		// Add the filter
		FilterDef filterDef = new FilterDef();
		if (decorator != null) {
			decorator.accept(filterDef);
		}
		filterDef.setFilterName(name);
		filterDef.setFilterClass(filterClass.getName());
		filterDef.setAsyncSupported("true");
		this.context.addFilterDef(filterDef);

		// Add the filter chain servlet
		Wrapper wrapper = Tomcat.addServlet(this.context, name, FilterChainHttpServlet.class.getName());

		// Configure filter on servlet
		FilterMap filterMap = new FilterMap();
		filterMap.setFilterName(name);
		filterMap.addServletName(name);
		this.context.addFilterMap(filterMap);

		// Provide servicer
		ContainerAdapter adapter = new ContainerAdapter(wrapper, this.connector, this.classLoader);
		filterServicer = (connection, executor, asynchronousFlow, asynchronousFlowCompletion, chain) -> this.service(
				connection, executor, asynchronousFlow, asynchronousFlowCompletion, null, chain, adapter::service);

		// Register and return servicer
		this.registeredFilters.put(name, filterServicer);
		return filterServicer;
	}

	@Override
	public void chainInServletManager() {

		// Determine if decision made
		if (this.isChainDecisionMade) {
			throw new IllegalStateException(
					ServletManager.class.getSimpleName() + " chain configuration already completed");
		}

		// Flag chain in the servlet manager
		this.isChainInServletManager = true;
	}

	/**
	 * {@link HttpServlet} to handle {@link FilterChain}.
	 */
	public static class FilterChainHttpServlet extends HttpServlet {

		/**
		 * Serialise version.
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * Attribute name for the {@link FilterChain}.
		 */
		public static final String ATTRIBUTE_NAME_FILTER_CHAIN = "#filter-chain#";

		/*
		 * ======================= HttpServlet ===========================
		 */

		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

			// As here, execute the filter chain
			FilterChain filterChain = (FilterChain) req.getAttribute(ATTRIBUTE_NAME_FILTER_CHAIN);
			filterChain.doFilter(req, resp);
		}
	}

	/**
	 * Servicer.
	 */
	@FunctionalInterface
	private static interface Servicer {

		/**
		 * Undertakes servicing.
		 * 
		 * @param request  {@link Request}.
		 * @param response {@link Response}.
		 * @throws Exception If fails servicing.
		 */
		void service(Request request, Response response) throws Exception;
	}

	/**
	 * Services the {@link ServerHttpConnection} via {@link Servicer}.
	 * 
	 * @param connection                 {@link ServerHttpConnection}.
	 * @param executor                   {@link Executor}.
	 * @param asynchronousFlow           {@link AsynchronousFlow}.
	 * @param asynchronousFlowCompletion {@link AsynchronousFlowCompletion}.
	 * @param attributes                 Attributes for the
	 *                                   {@link HttpServletRequest}. May be
	 *                                   <code>null</code>.
	 * @param filterChain                {@link FilterChain}. Will be ignored for
	 *                                   {@link Servlet}.
	 * @param servicer                   {@link Servicer}.
	 * @throws Exception If fails servicing.
	 */
	private void service(ServerHttpConnection connection, Executor executor, AsynchronousFlow asynchronousFlow,
			AsynchronousFlowCompletion asynchronousFlowCompletion, Map<String, ? extends Object> attributes,
			FilterChain filterChain, Servicer servicer) throws Exception {

		// Parse out the URL
		HttpRequest httpRequest = connection.getRequest();
		String requestUri = httpRequest.getUri();
		String[] parts = requestUri.split("\\?");
		requestUri = parts[0];
		String queryString;
		if (parts.length > 1) {
			String[] queryParts = new String[parts.length - 1];
			System.arraycopy(parts, 1, queryParts, 0, queryParts.length);
			queryString = String.join("?", queryParts);
		} else {
			queryString = "";
		}

		// Create the request
		Request request = new Request();
		request.scheme().setString(connection.isSecure() ? "https" : "http");
		request.method().setString(httpRequest.getMethod().getName());
		request.requestURI().setString(requestUri);
		request.decodedURI().setString(requestUri);
		request.queryString().setString(queryString);
		request.protocol().setString(httpRequest.getVersion().getName());
		MimeHeaders headers = request.getMimeHeaders();
		for (HttpHeader header : httpRequest.getHeaders()) {
			headers.addValue(header.getName()).setString(header.getValue());
		}
		if (attributes != null) {
			attributes.forEach((name, value) -> request.setAttribute(name, value));
		}
		request.setInputBuffer(new OfficeFloorInputBuffer(httpRequest));

		// Provide injection of context
		InjectContext injectContext = this.injectContextFactory.createInjectContext();
		injectContext.activate();
		request.setAttribute(InjectContext.REQUEST_ATTRIBUTE_NAME, injectContext);

		// Hook in potential filter chain
		if (filterChain != null) {
			request.setAttribute(FilterChainHttpServlet.ATTRIBUTE_NAME_FILTER_CHAIN, filterChain);
		}

		// Create the response
		Response response = new Response();
		HttpResponse httpResponse = connection.getResponse();
		response.setOutputBuffer(new OfficeFloorOutputBuffer(httpResponse));

		// Create processor for request
		new OfficeFloorProcessor(this.protocol, request, response, connection, executor, asynchronousFlow,
				asynchronousFlowCompletion);

		// Undertake servicing
		servicer.service(request, response);
	}

	/**
	 * {@link InputBuffer} for {@link ServerHttpConnection}.
	 */
	private static class OfficeFloorInputBuffer implements InputBuffer {

		/**
		 * {@link InputStream} to {@link HttpRequest} entity.
		 */
		private final InputStream entity;

		/**
		 * Instantiate.
		 * 
		 * @param httpRequest {@link HttpRequest}.
		 */
		private OfficeFloorInputBuffer(HttpRequest httpRequest) {
			this.entity = httpRequest.getEntity();
		}

		/*
		 * ================ InputBuffer =====================
		 */

		@Override
		public int doRead(ApplicationBufferHandler handler) throws IOException {

			// Initiate the buffer
			ByteBuffer buffer = handler.getByteBuffer();
			buffer.limit(buffer.capacity());

			// Write content to buffer
			int bytesRead = 0;
			int value;
			while ((value = this.entity.read()) != -1) {

				// Load the byte
				buffer.put(bytesRead, (byte) value);
				bytesRead++;

				// Determine if buffer full
				if (bytesRead == buffer.capacity()) {
					buffer.limit(bytesRead);
					return bytesRead; // buffer full
				}
			}

			// Finished writing
			if (bytesRead == 0) {
				buffer.limit(0);
				return -1; // end of entity
			} else {
				// Provide last entity
				buffer.limit(bytesRead);
				return bytesRead;
			}
		}
	}

	/**
	 * {@link OutputBuffer} for {@link ServerHttpConnection}.
	 */
	private static class OfficeFloorOutputBuffer implements OutputBuffer {

		/**
		 * {@link HttpResponse}.
		 */
		private final HttpResponse httpResponse;

		/**
		 * Bytes written.
		 */
		private long bytesWritten = 0;

		/**
		 * Instantiate.
		 * 
		 * @param httpResponse {@link HttpResponse}.
		 */
		private OfficeFloorOutputBuffer(HttpResponse httpResponse) {
			this.httpResponse = httpResponse;
		}

		/*
		 * ================= OutputBuffer ======================
		 */

		@Override
		public int doWrite(ByteBuffer chunk) throws IOException {
			int size = chunk.remaining();
			this.httpResponse.getEntity().write(chunk);
			this.bytesWritten += size;
			return size;
		}

		@Override
		public long getBytesWritten() {
			return this.bytesWritten;
		}
	}

}