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

package net.officefloor.plugin.web.http.template.route;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.officefloor.frame.api.build.OfficeAwareWorkFactory;
import net.officefloor.frame.api.execute.Task;
import net.officefloor.frame.api.execute.TaskContext;
import net.officefloor.frame.api.execute.Work;
import net.officefloor.frame.api.manage.InvalidParameterTypeException;
import net.officefloor.frame.api.manage.Office;
import net.officefloor.frame.api.manage.TaskManager;
import net.officefloor.frame.api.manage.UnknownTaskException;
import net.officefloor.frame.api.manage.UnknownWorkException;
import net.officefloor.frame.api.manage.WorkManager;
import net.officefloor.frame.internal.structure.Flow;
import net.officefloor.frame.util.AbstractSingleTask;
import net.officefloor.plugin.socket.server.http.HttpRequest;
import net.officefloor.plugin.socket.server.http.ServerHttpConnection;
import net.officefloor.plugin.web.http.resource.HttpResourceUtil;
import net.officefloor.plugin.web.http.resource.InvalidHttpRequestUriException;
import net.officefloor.plugin.web.http.template.HttpTemplateRequestHandlerDifferentiator;
import net.officefloor.plugin.web.http.template.HttpTemplateWorkSource;
import net.officefloor.plugin.web.http.template.parse.HttpTemplate;
import net.officefloor.plugin.web.http.template.parse.LinkHttpTemplateSectionContent;

/**
 * {@link Task} that routes to the {@link LinkHttpTemplateSectionContent}
 * handling {@link Task}.
 * 
 * @author Daniel Sagenschneider
 */
public class HttpTemplateRouteTask
		extends
		AbstractSingleTask<HttpTemplateRouteTask, HttpTemplateRouteTask.HttpTemplateRouteDependencies, HttpTemplateRouteTask.HttpTemplateRouteTaskFlows>
		implements OfficeAwareWorkFactory<HttpTemplateRouteTask> {

	/**
	 * Keys of dependencies for the {@link HttpTemplateRouteTask}.
	 */
	public static enum HttpTemplateRouteDependencies {
		SERVER_HTTP_CONNECTION
	}

	/**
	 * Keys of {@link Flow} for the {@link HttpTemplateRouteTask}.
	 */
	public static enum HttpTemplateRouteTaskFlows {
		NON_MATCHED_REQUEST
	}

	/**
	 * Cache of {@link Work} name and subsequent listing of the
	 * {@link HttpTemplate} handling {@link Task} names.
	 */
	private Map<String, String[]> templateHandlerTasks = new HashMap<String, String[]>();

	/*
	 * ================ OfficeAwareWorkFactory =======================
	 */

	@Override
	public void setOffice(Office office) throws Exception {
		// Cache the handler task names
		for (String workName : office.getWorkNames()) {
			WorkManager workManager = office.getWorkManager(workName);

			// Obtain the handler tasks
			List<String> handlerTaskNames = new LinkedList<String>();
			for (String taskName : workManager.getTaskNames()) {
				TaskManager taskManager = workManager.getTaskManager(taskName);

				// Determine if handler task
				Object differentiator = taskManager.getDifferentiator();
				if ((differentiator != null)
						&& (differentiator instanceof HttpTemplateRequestHandlerDifferentiator)) {
					// Handler task so include for work
					handlerTaskNames.add(taskName);
				}
			}

			// Include handler tasks (if any for the work)
			if (handlerTaskNames.size() > 0) {
				this.templateHandlerTasks.put(workName,
						handlerTaskNames.toArray(new String[0]));
			}
		}
	}

	/*
	 * ======================== Task =================================
	 */

	@Override
	public Object doTask(
			TaskContext<HttpTemplateRouteTask, HttpTemplateRouteDependencies, HttpTemplateRouteTaskFlows> context)
			throws InvalidHttpRequestUriException, UnknownWorkException,
			UnknownTaskException, InvalidParameterTypeException {

		// Obtain the Http Request
		ServerHttpConnection connection = (ServerHttpConnection) context
				.getObject(HttpTemplateRouteDependencies.SERVER_HTTP_CONNECTION);
		HttpRequest request = connection.getHttpRequest();

		// Obtain the canonical path from request
		String path = request.getRequestURI();
		path = HttpResourceUtil.transformToCanonicalPath(path);

		// Determine if link task request
		if (path.endsWith(HttpTemplateWorkSource.LINK_URL_EXTENSION)) {
			// Link task request, so obtain work and task names from path
			path = path.substring("/".length(), path.length()
					- HttpTemplateWorkSource.LINK_URL_EXTENSION.length());
			int workTaskPos = path.lastIndexOf('/');
			if (workTaskPos > 0) {
				// Work and Task name within URL
				String workName = path.substring(0, workTaskPos);
				String taskName = path.substring(workTaskPos + "/".length());

				// Determine if handler task
				String[] taskNames = this.templateHandlerTasks.get(workName);
				if (taskNames != null) {
					for (String handlerTaskName : taskNames) {
						if (handlerTaskName.equals(taskName)) {
							// Is a handler task, so invoke flow
							context.doFlow(workName, taskName, null);

							// Routed by invoking flow, no further processing
							return null;
						}
					}
				}
			}
		}

		// No handling task so invoke non matched flow
		context.doFlow(HttpTemplateRouteTaskFlows.NON_MATCHED_REQUEST, null);

		// Nothing returned as routing
		return null;
	}

}