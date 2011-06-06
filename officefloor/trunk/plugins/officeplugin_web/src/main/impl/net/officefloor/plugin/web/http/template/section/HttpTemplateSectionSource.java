/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2011 Daniel Sagenschneider
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

package net.officefloor.plugin.web.http.template.section;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.officefloor.compile.spi.section.SectionDesigner;
import net.officefloor.compile.spi.section.SectionInput;
import net.officefloor.compile.spi.section.SectionManagedObject;
import net.officefloor.compile.spi.section.SectionManagedObjectSource;
import net.officefloor.compile.spi.section.SectionObject;
import net.officefloor.compile.spi.section.SectionOutput;
import net.officefloor.compile.spi.section.SectionTask;
import net.officefloor.compile.spi.section.SectionWork;
import net.officefloor.compile.spi.section.source.SectionSource;
import net.officefloor.compile.spi.section.source.SectionSourceContext;
import net.officefloor.compile.spi.section.source.impl.AbstractSectionSource;
import net.officefloor.compile.work.TaskType;
import net.officefloor.frame.api.build.OfficeFloorIssues.AssetType;
import net.officefloor.frame.api.execute.Task;
import net.officefloor.frame.impl.construct.source.SourcePropertiesImpl;
import net.officefloor.frame.internal.structure.FlowInstigationStrategyEnum;
import net.officefloor.frame.internal.structure.ManagedObjectScope;
import net.officefloor.frame.spi.PrivateSource;
import net.officefloor.frame.spi.source.UnknownPropertyError;
import net.officefloor.plugin.managedobject.clazz.ClassManagedObjectSource;
import net.officefloor.plugin.managedobject.clazz.DependencyMetaData;
import net.officefloor.plugin.section.clazz.ClassSectionSource;
import net.officefloor.plugin.section.clazz.NextTask;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.socket.server.http.ServerHttpConnection;
import net.officefloor.plugin.web.http.application.HttpSessionStateful;
import net.officefloor.plugin.web.http.session.clazz.source.HttpSessionClassManagedObjectSource;
import net.officefloor.plugin.web.http.template.HttpTemplateWorkSource;
import net.officefloor.plugin.web.http.template.RawHttpTemplateLoader;
import net.officefloor.plugin.web.http.template.parse.HttpTemplate;
import net.officefloor.plugin.web.http.template.parse.HttpTemplateSection;

/**
 * {@link SectionSource} for the HTTP template.
 * 
 * @author Daniel Sagenschneider
 */
public class HttpTemplateSectionSource extends AbstractSectionSource {

	/**
	 * Registers the {@link RawHttpTemplateLoader}.
	 * 
	 * @param rawHttpTemplateLoader
	 *            {@link RawHttpTemplateLoader}.
	 */
	public static void registerRawHttpTemplateLoader(
			RawHttpTemplateLoader rawHttpTemplateLoader) {
		HttpTemplateWorkSource
				.registerRawHttpTemplateLoader(rawHttpTemplateLoader);
	}

	/**
	 * <p>
	 * Unregisters all the {@link RawHttpTemplateLoader} instances.
	 * <p>
	 * This is typically only made available to allow resetting content for
	 * testing.
	 */
	public static void unregisterAllRawHttpTemplateLoaders() {
		HttpTemplateWorkSource.unregisterAllRawHttpTemplateLoaders();
	}

	/**
	 * Property name for the {@link Class} providing the backing logic to the
	 * template.
	 */
	public static final String PROPERTY_CLASS_NAME = ClassManagedObjectSource.CLASS_NAME_PROPERTY_NAME;

	/**
	 * Name of the {@link SectionInput} for rendering this {@link HttpTemplate}.
	 */
	public static final String RENDER_TEMPLATE_INPUT_NAME = "renderTemplate";

	/**
	 * Name of the {@link SectionOutput} for flow after completion of rending
	 * the {@link HttpTemplate}.
	 */
	public static final String ON_COMPLETION_OUTPUT_NAME = "output";

	/**
	 * Prefix on the {@link Task} name of the class {@link Method} handling the
	 * link.
	 */
	private static final String LINK_METHOD_TASK_NAME_PREFIX = "ServiceLink_";

	/**
	 * {@link TemplateBeanTask} instances by the template bean
	 * {@link SectionTask} name.
	 */
	private final Map<String, TemplateBeanTask> templateBeanTasksByName = new HashMap<String, TemplateBeanTask>();

	/**
	 * {@link Boolean} by template {@link SectionTask} name indicating if
	 * requires bean.
	 */
	private final Map<String, Boolean> isRequireBeanTemplates = new HashMap<String, Boolean>();

	/**
	 * {@link Task} link names.
	 */
	private final Set<String> taskLinkNames = new HashSet<String>();

	/**
	 * Indicates if the {@link SectionTask} by the name is to provide a template
	 * bean.
	 * 
	 * @param taskName
	 *            Name of the {@link SectionTask}.
	 * @return <code>true</code> if is a template bean {@link SectionTask}.
	 */
	private boolean isTemplateBeanTask(String taskName) {

		// Determine if template bean method
		if (taskName.startsWith("get")) {
			String templateName = taskName.substring("get".length());
			Boolean isRequireBean = this.isRequireBeanTemplates
					.get(templateName.toUpperCase());
			if (isRequireBean != null) {
				// Is template bean method if require bean
				return isRequireBean.booleanValue();
			}
		}

		// As here, not a template bean task
		return false;
	}

	/*
	 * ===================== SectionSource =========================
	 */

	@Override
	protected void loadSpecification(SpecificationContext context) {
		context.addProperty(PROPERTY_CLASS_NAME, "Class");
	}

	@Override
	public void sourceSection(SectionDesigner designer,
			SectionSourceContext context) throws Exception {

		// Obtain the class loader
		ClassLoader classLoader = context.getClassLoader();

		// Obtain the template location
		String templateLocation = context.getSectionLocation();

		// Obtain the HTTP template content
		SourcePropertiesImpl templateProperties = new SourcePropertiesImpl(
				context);
		templateProperties
				.addProperty(HttpTemplateWorkSource.PROPERTY_TEMPLATE_FILE,
						templateLocation);
		Reader templateContentReader = HttpTemplateWorkSource
				.getHttpTemplateContent(templateProperties, classLoader);
		StringBuilder templateContentBuffer = new StringBuilder();
		for (int character = templateContentReader.read(); character != -1; character = templateContentReader
				.read()) {
			templateContentBuffer.append((char) character);
		}
		String templateContent = templateContentBuffer.toString();

		// Obtain the section class
		String sectionClassName = context.getProperty(PROPERTY_CLASS_NAME);
		Class<?> sectionClass = classLoader.loadClass(sectionClassName);

		// Create the HTTP Template Class Section Source
		HttpTemplateClassSectionSource classSource = new HttpTemplateClassSectionSource(
				sectionClass, designer, context);

		// Obtain the Section Managed Object
		SectionManagedObject sectionClassObject = classSource
				.getClassManagedObject();

		// Extend the template as necessary
		final String EXTENSION_PREFIX = "extension.";
		int extensionIndex = 1;
		String extensionClassName = context.getProperty(EXTENSION_PREFIX
				+ extensionIndex, null);
		while (extensionClassName != null) {

			// Create an instance of the extension class
			HttpTemplateSectionExtension extension = (HttpTemplateSectionExtension) classLoader
					.loadClass(extensionClassName).newInstance();

			// Extend the template
			String extensionPropertyPrefix = EXTENSION_PREFIX + extensionIndex
					+ ".";
			HttpTemplateSectionExtensionContext extensionContext = new HttpTemplateSectionExtensionContextImpl(
					templateContent, sectionClass, extensionPropertyPrefix,
					designer, context, sectionClassObject, classSource);
			extension.extendTemplate(extensionContext);

			// Override template details
			templateContent = extensionContext.getTemplateContent();
			sectionClass = extensionContext.getTemplateClass();

			// Initiate for next extension
			extensionIndex++;
			extensionClassName = context.getProperty(EXTENSION_PREFIX
					+ extensionIndex, null);
		}

		// Obtain the HTTP template
		HttpTemplate template = HttpTemplateWorkSource
				.getHttpTemplate(new StringReader(templateContent));

		// Obtain the listing of task link names
		String[] linkNames = HttpTemplateWorkSource
				.getHttpTemplateLinkNames(template);
		this.taskLinkNames.addAll(Arrays.asList(linkNames));

		// Register the HTTP template sections requiring a bean
		for (HttpTemplateSection templateSection : template.getSections()) {
			String templateSectionName = templateSection.getSectionName();
			boolean isRequireBean = HttpTemplateWorkSource
					.isHttpTemplateSectionRequireBean(templateSection);
			this.isRequireBeanTemplates.put(templateSectionName.toUpperCase(),
					new Boolean(isRequireBean));
		}

		// Create the input to the section
		SectionInput sectionInput = designer.addSectionInput(
				RENDER_TEMPLATE_INPUT_NAME, null);

		// Name of work is exposed on URL for links.
		// Result is: /<section>.links-<link>.task
		final String TEMPLATE_WORK_NANE = "links";

		// Load the HTTP template
		SectionWork templateWork = designer.addSectionWork(TEMPLATE_WORK_NANE,
				HttpTemplateWorkSource.class.getName());
		templateWork.addProperty(
				HttpTemplateWorkSource.PROPERTY_TEMPLATE_CONTENT,
				templateContent);

		// Create the template tasks and ensure registered for logic flows
		Map<String, SectionTask> templateTasks = new HashMap<String, SectionTask>();
		for (HttpTemplateSection templateSection : template.getSections()) {

			// Obtain the template task name
			String templateTaskName = templateSection.getSectionName();

			// Add the template task
			SectionTask templateTask = templateWork.addSectionTask(
					templateTaskName, templateTaskName);

			// Register the template task
			templateTasks.put(templateTaskName, templateTask);

			// Make template task available to logic flow
			classSource.registerTaskByTypeName(templateTaskName, templateTask);
		}

		// Load the section class (with ability to link in template tasks)
		classSource.sourceSection(designer, context);

		// Keep track of template bean task keys
		Set<String> templateBeanTaskKeys = new HashSet<String>();

		// Load the HTTP template tasks
		SectionTask firstTemplateTask = null;
		SectionTask previousTemplateTask = null;
		for (HttpTemplateSection templateSection : template.getSections()) {

			// Obtain the template task
			String templateTaskName = templateSection.getSectionName();
			SectionTask templateTask = templateTasks.get(templateTaskName);

			// Determine if template section requires a bean
			boolean isRequireBean = HttpTemplateWorkSource
					.isHttpTemplateSectionRequireBean(templateSection);

			// Link the Server HTTP Connection dependency
			SectionObject connectionObject = classSource
					.getOrCreateObject(ServerHttpConnection.class.getName());
			designer.link(templateTask.getTaskObject("SERVER_HTTP_CONNECTION"),
					connectionObject);

			// Flag bean as parameter if requires a bean
			if (isRequireBean) {
				templateTask.getTaskObject("OBJECT").flagAsParameter();
			}

			// Link the I/O escalation
			SectionOutput ioEscalation = classSource.getOrCreateOutput(
					IOException.class.getName(), IOException.class.getName(),
					true);
			designer.link(
					templateTask.getTaskEscalation(IOException.class.getName()),
					ioEscalation, FlowInstigationStrategyEnum.SEQUENTIAL);

			// Obtain the bean task method
			String beanTaskName = "get" + templateTaskName;
			String beanTaskKey = beanTaskName.toUpperCase();
			TemplateBeanTask beanTask = this.templateBeanTasksByName
					.get(beanTaskKey);

			// Keep track of bean task keys
			templateBeanTaskKeys.add(beanTaskKey);

			// Ensure correct configuration, if template section requires bean
			if (isRequireBean) {

				// Must have template bean task
				if (beanTask == null) {
					designer.addIssue("Missing method '" + beanTaskName
							+ "' on class " + sectionClass.getName()
							+ " to provide bean for template "
							+ templateLocation, AssetType.WORK,
							TEMPLATE_WORK_NANE);

				} else {
					// Ensure bean task does not have a parameter
					if (beanTask.parameter != null) {
						designer.addIssue("Template bean method '"
								+ beanTaskName + "' must not have a "
								+ Parameter.class.getSimpleName()
								+ " annotation", AssetType.TASK, beanTaskName);
					}

					// Obtain the argument type for the template
					Class<?> argumentType = beanTask.type.getReturnType();
					if ((argumentType == null)
							|| (Void.class.equals(argumentType))) {
						// Must provide argument from bean task
						designer.addIssue("Bean method '" + beanTaskName
								+ "' must have return value", AssetType.TASK,
								beanTaskName);

					} else {
						// Determine bean type and whether an array
						Class<?> beanType = argumentType;
						boolean isArray = argumentType.isArray();
						if (isArray) {
							beanType = argumentType.getComponentType();
						}

						// Inform template of bean type
						templateWork.addProperty(
								HttpTemplateWorkSource.PROPERTY_BEAN_PREFIX
										+ templateTaskName, beanType.getName());

						// Handle iterating over array of beans
						if (isArray) {
							// Provide iterator task if array
							SectionWork arrayIteratorWork = designer
									.addSectionWork(
											templateTaskName + "ArrayIterator",
											HttpTemplateArrayIteratorWorkSource.class
													.getName());
							arrayIteratorWork
									.addProperty(
											HttpTemplateArrayIteratorWorkSource.PROPERTY_COMPONENT_TYPE_NAME,
											beanType.getName());
							SectionTask arrayIteratorTask = arrayIteratorWork
									.addSectionTask(
											templateTaskName + "ArrayIterator",
											HttpTemplateArrayIteratorWorkSource.TASK_NAME);
							arrayIteratorTask
									.getTaskObject(
											HttpTemplateArrayIteratorWorkSource.OBJECT_NAME)
									.flagAsParameter();

							// Link iteration of array to rendering
							designer.link(
									arrayIteratorTask
											.getTaskFlow(HttpTemplateArrayIteratorWorkSource.FLOW_NAME),
									templateTask,
									FlowInstigationStrategyEnum.PARALLEL);

							// Iterator is now controller for template
							templateTask = arrayIteratorTask;
						}
					}
				}
			}

			// Determine if first template task
			if (firstTemplateTask == null) {
				// First template task so link to input
				if (beanTask != null) {
					// First template task is bean task
					firstTemplateTask = beanTask.task;

					// Link input to bean then template
					designer.link(sectionInput, beanTask.task);
					designer.link(beanTask.task, templateTask);
				} else {
					// First template task is section rendering
					firstTemplateTask = templateTask;

					// Link input to just template
					designer.link(sectionInput, templateTask);
				}

			} else {
				// Subsequent template tasks so link to previous task
				if (beanTask != null) {
					// Link with bean task then template
					designer.link(previousTemplateTask, beanTask.task);
					designer.link(beanTask.task, templateTask);
				} else {
					// No bean task so link to template
					designer.link(previousTemplateTask, templateTask);
				}
			}

			// Template task is always previous task
			previousTemplateTask = templateTask;
		}

		// Register the #{link} tasks
		for (String linkTaskName : linkNames) {

			// Add the task for handling the link
			SectionTask linkTask = templateWork.addSectionTask(linkTaskName,
					linkTaskName);

			// Obtain the link method task
			String linkMethodTaskName = LINK_METHOD_TASK_NAME_PREFIX
					+ linkTaskName;
			String linkMethodTaskKey = linkMethodTaskName.toUpperCase();
			TemplateBeanTask methodTask = this.templateBeanTasksByName
					.get(linkMethodTaskKey);
			if (methodTask == null) {
				designer.addIssue("No backing method for link '" + linkTaskName
						+ "'", AssetType.TASK, linkTaskName);
				continue; // must have link method
			}

			// Link handling of request to method
			designer.link(linkTask, methodTask.task);
		}

		// Link bean tasks to re-render template by default
		List<String> beanTaskNames = new ArrayList<String>(
				this.templateBeanTasksByName.keySet());
		Collections.sort(beanTaskNames);
		for (String beanTaskKey : this.templateBeanTasksByName.keySet()) {

			// Ignore template bean methods
			if (templateBeanTaskKeys.contains(beanTaskKey)) {
				continue;
			}

			// Obtain the bean method
			TemplateBeanTask methodTask = this.templateBeanTasksByName
					.get(beanTaskKey);

			// Determine if method already indicating next task
			if (!(methodTask.method.isAnnotationPresent(NextTask.class))) {
				// Next task not linked, so link to render template
				designer.link(methodTask.task, firstTemplateTask);
			}
		}

		// Link last template task to output
		SectionOutput output = classSource.getOrCreateOutput(
				ON_COMPLETION_OUTPUT_NAME, null, false);
		designer.link(previousTemplateTask, output);
	}

	/**
	 * {@link SectionTask} for the template bean.
	 */
	private static class TemplateBeanTask {

		/**
		 * {@link SectionTask}.
		 */
		public final SectionTask task;

		/**
		 * {@link TaskType}.
		 */
		public final TaskType<?, ?, ?> type;

		/**
		 * {@link Method} for the {@link SectionTask}.
		 */
		public final Method method;

		/**
		 * Type of parameter for {@link SectionTask}. <code>null</code>
		 * indicates no parameter.
		 */
		public final Class<?> parameter;

		/**
		 * Initiate.
		 * 
		 * @param task
		 *            {@link SectionTask}.
		 * @param type
		 *            {@link TaskType}.
		 * @param method
		 *            {@link Method} for the {@link SectionTask}.
		 * @param parameter
		 *            Type of parameter for {@link SectionTask}.
		 *            <code>null</code> indicates no parameter.
		 */
		public TemplateBeanTask(SectionTask task, TaskType<?, ?, ?> type,
				Method method, Class<?> parameter) {
			this.task = task;
			this.type = type;
			this.method = method;
			this.parameter = parameter;
		}
	}

	/**
	 * {@link HttpTemplateSectionExtensionContext} implementation.
	 */
	private class HttpTemplateSectionExtensionContextImpl implements
			HttpTemplateSectionExtensionContext {

		/**
		 * Raw {@link HttpTemplate} content.
		 */
		private String templateContent;

		/**
		 * {@link HttpTemplate} logic class.
		 */
		private final Class<?> templateClass;

		/**
		 * Prefix for a property of this extension.
		 */
		private final String extensionPropertyPrefix;

		/**
		 * {@link SectionDesigner}.
		 */
		private final SectionDesigner sectionDesigner;

		/**
		 * {@link SectionSourceContext}.
		 */
		private final SectionSourceContext sectionSourceContext;

		/**
		 * {@link SectionManagedObject} for the template logic object.
		 */
		private final SectionManagedObject templateLogicObject;

		/**
		 * {@link HttpTemplateClassSectionSource}.
		 */
		private final HttpTemplateClassSectionSource classSectionSource;

		/**
		 * Initiate.
		 * 
		 * @param templateContent
		 *            Raw {@link HttpTemplate} content.
		 * @param templateClass
		 *            {@link HttpTemplate} logic class. May be <code>null</code>
		 *            if not overridden.
		 * @param extensionPropertyPrefix
		 *            Prefix for a property of this extension.
		 * @param sectionDesigner
		 *            {@link SectionDesigner}.
		 * @param sectionSourceContext
		 *            {@link SectionSourceContext}.
		 * @param templateLogicObject
		 *            {@link SectionManagedObject} for the template logic
		 *            object.
		 * @param classSectionSource
		 *            {@link HttpTemplateClassSectionSource}.
		 */
		public HttpTemplateSectionExtensionContextImpl(String templateContent,
				Class<?> templateClass, String extensionPropertyPrefix,
				SectionDesigner sectionDesigner,
				SectionSourceContext sectionSourceContext,
				SectionManagedObject templateLogicObject,
				HttpTemplateClassSectionSource classSectionSource) {
			this.templateContent = templateContent;
			this.templateClass = templateClass;
			this.extensionPropertyPrefix = extensionPropertyPrefix;
			this.sectionDesigner = sectionDesigner;
			this.sectionSourceContext = sectionSourceContext;
			this.templateLogicObject = templateLogicObject;
			this.classSectionSource = classSectionSource;
		}

		/*
		 * ============== HttpTemplateSectionExtensionContext ================
		 */

		@Override
		public String getTemplateContent() {
			return this.templateContent;
		}

		@Override
		public void setTemplateContent(String templateContent) {
			this.templateContent = templateContent;
		}

		@Override
		public Class<?> getTemplateClass() {
			return this.templateClass;
		}

		@Override
		public String[] getPropertyNames() {

			// Obtain all the property names
			String[] contextNames = this.sectionSourceContext
					.getPropertyNames();

			// Filter to just this extension's properties
			List<String> extensionNames = new LinkedList<String>();
			for (String contextName : contextNames) {
				if (contextName.startsWith(this.extensionPropertyPrefix)) {
					// Add the extension property name
					String extensionName = contextName
							.substring(this.extensionPropertyPrefix.length());
					extensionNames.add(extensionName);
				}
			}

			// Return the extension names
			return extensionNames.toArray(new String[extensionNames.size()]);
		}

		@Override
		public String getProperty(String name) throws UnknownPropertyError {
			// Obtain the extension property value
			return this.sectionSourceContext
					.getProperty(this.extensionPropertyPrefix + name);
		}

		@Override
		public String getProperty(String name, String defaultValue) {
			// Obtain the extension property value
			return this.sectionSourceContext.getProperty(
					this.extensionPropertyPrefix + name, defaultValue);
		}

		@Override
		public Properties getProperties() {

			// Obtain all the properties
			Properties properties = new Properties();

			// Filter to just this extension's properties
			String[] contextNames = this.sectionSourceContext
					.getPropertyNames();
			for (String contextName : contextNames) {
				if (contextName.startsWith(this.extensionPropertyPrefix)) {
					// Add the extension property name
					String extensionName = contextName
							.substring(this.extensionPropertyPrefix.length());
					String value = this.sectionSourceContext
							.getProperty(contextName);
					properties.setProperty(extensionName, value);
				}
			}

			// Return the properties
			return properties;
		}

		@Override
		public SectionSourceContext getSectionSourceContext() {
			return this.sectionSourceContext;
		}

		@Override
		public SectionDesigner getSectionDesigner() {
			return this.sectionDesigner;
		}

		@Override
		public SectionManagedObject getTemplateLogicObject() {
			return this.templateLogicObject;
		}

		@Override
		public SectionTask getTask(String taskName) {
			return this.classSectionSource.getTaskByName(taskName);
		}

		@Override
		public SectionObject getOrCreateSectionObject(String typeName) {
			return this.classSectionSource.getOrCreateObject(typeName);
		}

		@Override
		public SectionOutput getOrCreateSectionOutput(String name,
				String argumentType, boolean isEscalationOnly) {
			return this.classSectionSource.getOrCreateOutput(name,
					argumentType, isEscalationOnly);
		}
	}

	/**
	 * {@link ClassSectionSource} specific to the HTTP template.
	 */
	@PrivateSource
	public class HttpTemplateClassSectionSource extends ClassSectionSource {

		/**
		 * Section class.
		 */
		private final Class<?> sectionClass;

		/**
		 * {@link SectionDesigner}.
		 */
		private final SectionDesigner designer;

		/**
		 * {@link SectionSourceContext}.
		 */
		private final SectionSourceContext sourceContext;

		/**
		 * {@link SectionManagedObject} for the section object.
		 */
		private SectionManagedObject sectionClassManagedObject = null;

		/**
		 * Initiate.
		 * 
		 * @param sectionClass
		 *            Section class.
		 * @param designer
		 *            {@link SectionDesigner}.
		 * @param sourceContext
		 *            {@link SectionSourceContext}.
		 */
		public HttpTemplateClassSectionSource(Class<?> sectionClass,
				SectionDesigner designer, SectionSourceContext sourceContext) {
			this.sectionClass = sectionClass;
			this.designer = designer;
			this.sourceContext = sourceContext;
		}

		/**
		 * Obtains the {@link SectionManagedObject} for the section class.
		 * 
		 * @return {@link SectionManagedObject} for the section class.
		 * @throws Exception
		 *             If fails to obtain the {@link SectionManagedObject}.
		 */
		public SectionManagedObject getClassManagedObject() throws Exception {
			return this.createClassManagedObject(CLASS_OBJECT_NAME,
					this.getSectionClass(this.getSectionClassName()));
		}

		/**
		 * Determine if the section class is stateful - annotated with
		 * {@link HttpSessionStateful}.
		 * 
		 * @param sectionClass
		 *            Section class.
		 * @return <code>true</code> if stateful.
		 */
		private boolean isHttpSessionStateful(Class<?> sectionClass) {

			// Determine if stateful
			boolean isStateful = sectionClass
					.isAnnotationPresent(HttpSessionStateful.class);

			// Return indicating if stateful
			return isStateful;
		}

		/*
		 * =================== ClassSectionSource ==========================
		 */

		@Override
		protected SectionDesigner getDesigner() {
			return this.designer;
		}

		@Override
		protected SectionSourceContext getContext() {
			return this.sourceContext;
		}

		@Override
		protected String getSectionClassName() {
			return this.sectionClass.getName();
		}

		@Override
		protected Class<?> getSectionClass(String sectionClassName)
				throws Exception {
			return this.sectionClass;
		}

		@Override
		protected SectionManagedObject createClassManagedObject(
				String objectName, Class<?> sectionClass) {

			// Determine if already loaded the Section Managed Object
			if (this.sectionClassManagedObject != null) {
				return this.sectionClassManagedObject; // instance
			}

			// Determine if stateful
			boolean isStateful = this.isHttpSessionStateful(sectionClass);

			// Default behaviour if not stateful
			if (!isStateful) {
				// Defer to default behaviour
				this.sectionClassManagedObject = super
						.createClassManagedObject(objectName, sectionClass);

			} else {
				// As stateful, the class must be serialisable
				if (!(Serializable.class.isAssignableFrom(sectionClass))) {
					this.getDesigner().addIssue(
							"Template logic class " + sectionClass.getName()
									+ " is annotated with "
									+ HttpSessionStateful.class.getSimpleName()
									+ " but is not "
									+ Serializable.class.getSimpleName(),
							AssetType.MANAGED_OBJECT, objectName);
				}

				// Create the managed object for the stateful template logic
				SectionManagedObjectSource managedObjectSource = this
						.getDesigner().addSectionManagedObjectSource(
								objectName,
								HttpSessionClassManagedObjectSource.class
										.getName());
				managedObjectSource
						.addProperty(
								HttpSessionClassManagedObjectSource.PROPERTY_CLASS_NAME,
								sectionClass.getName());

				// Create the managed object
				this.sectionClassManagedObject = managedObjectSource
						.addSectionManagedObject(objectName,
								ManagedObjectScope.PROCESS);
			}

			// Return the managed object
			return this.sectionClassManagedObject;
		}

		@Override
		protected DependencyMetaData[] extractClassManagedObjectDependencies(
				String objectName, Class<?> sectionClass) throws Exception {

			// Extract the dependency meta-data for default behaviour
			DependencyMetaData[] metaData = super
					.extractClassManagedObjectDependencies(objectName,
							sectionClass);

			// Determine if stateful
			boolean isStateful = this.isHttpSessionStateful(sectionClass);

			// If not stateful, return meta-data for default behaviour
			if (!isStateful) {
				return metaData;
			}

			// As stateful, must not have any dependencies into object
			if (metaData.length > 0) {
				this.getDesigner()
						.addIssue(
								"Template logic class "
										+ sectionClass.getName()
										+ " is annotated with "
										+ HttpSessionStateful.class
												.getSimpleName()
										+ " and therefore can not have dependencies injected into the object (only its methods)",
								AssetType.MANAGED_OBJECT, objectName);
			}

			// Return the dependency meta-data for stateful template logic
			return new DependencyMetaData[] { new StatefulDependencyMetaData() };
		}

		@Override
		protected String getTaskName(TaskType<?, ?, ?> taskType) {

			// Obtain the task type name
			String taskTypeName = taskType.getTaskName();

			// Determine if backing method to link task
			boolean isLinkMethod = HttpTemplateSectionSource.this.taskLinkNames
					.contains(taskTypeName);

			// Return prefix on link method task
			return (isLinkMethod ? LINK_METHOD_TASK_NAME_PREFIX + taskTypeName
					: taskTypeName);
		}

		@Override
		protected void enrichTask(SectionTask task, TaskType<?, ?, ?> taskType,
				Method method, Class<?> parameterType) {

			// Determine name of task
			String taskName = task.getSectionTaskName();

			// Register the template bean task (case insensitive)
			HttpTemplateSectionSource.this.templateBeanTasksByName.put(taskName
					.toUpperCase(), new TemplateBeanTask(task, taskType,
					method, parameterType));
		}

		@Override
		protected void linkNextTask(SectionTask task,
				TaskType<?, ?, ?> taskType, Method taskMethod,
				Class<?> argumentType, NextTask nextTaskAnnotation) {

			// Determine if template bean task
			String taskName = taskType.getTaskName();
			if (HttpTemplateSectionSource.this.isTemplateBeanTask(taskName)) {
				// Can not have next task annotation for template bean task
				this.getDesigner().addIssue(
						"Template bean method '" + taskName
								+ "' must not be annotated with "
								+ NextTask.class.getSimpleName(),
						AssetType.TASK, taskName);
				return; // do not link next task
			}

			// Not template bean task, so link next task
			super.linkNextTask(task, taskType, taskMethod, argumentType,
					nextTaskAnnotation);
		}
	}

}