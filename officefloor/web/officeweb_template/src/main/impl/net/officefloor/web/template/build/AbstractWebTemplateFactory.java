/*-
 * #%L
 * Web Template
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

package net.officefloor.web.template.build;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Properties;

import net.officefloor.compile.issues.SourceIssues;
import net.officefloor.compile.properties.PropertyList;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.web.template.section.WebTemplateSectionSource;

/**
 * Abstract {@link WebTemplateFactory}.
 * 
 * @author Daniel Sagenschneider
 */
public abstract class AbstractWebTemplateFactory implements WebTemplateFactory {

	/**
	 * Creates a {@link PropertyList}.
	 * 
	 * @return New {@link PropertyList}.
	 */
	protected abstract PropertyList createPropertyList();

	/**
	 * Obtains the {@link SourceIssues}.
	 * 
	 * @return {@link SourceIssues}.
	 */
	protected abstract SourceIssues getSourceIssues();

	/**
	 * Indicates if path parameters.
	 * 
	 * @param applicationPath
	 *            Application path.
	 * @return <code>true</code> if path parameters.
	 */
	protected abstract boolean isPathParameters(String applicationPath);

	/**
	 * Adds the {@link WebTemplate}.
	 * 
	 * @param isSecure
	 *            Indicates if requires secure {@link ServerHttpConnection} to
	 *            render the {@link WebTemplate}.
	 * @param applicationPath
	 *            Application path to the {@link WebTemplate}. May contain path
	 *            parameters.
	 * @param properties
	 *            Initial {@link Properties} for the {@link WebTemplate} that should
	 *            be further configured by the returned {@link WebTemplate}
	 *            implementation.
	 * @return {@link WebTemplate}.
	 */
	protected abstract WebTemplate addTemplate(boolean isSecure, String applicationPath, PropertyList properties);

	/**
	 * Creates the initial {@link PropertyList}.
	 * 
	 * @param isSecure
	 *            Indicates if secure.
	 * @param applicationPath
	 *            Application path.
	 */
	private PropertyList createInitalPropertyList(boolean isSecure, String applicationPath) {
		PropertyList properties = this.createPropertyList();
		properties.addProperty(WebTemplateSectionSource.PROPERTY_IS_PATH_PARAMETERS)
				.setValue(String.valueOf(this.isPathParameters(applicationPath)));
		return properties;
	}

	/*
	 * =================== WebTemplateFactory ===================
	 */

	@Override
	public WebTemplate addTemplate(boolean isSecure, String applicationPath, Reader templateContent) {

		// Read in the template
		StringWriter content = new StringWriter();
		try {
			for (int character = templateContent.read(); character != -1; character = templateContent.read()) {
				content.write(character);
			}
		} catch (IOException ex) {
			throw this.getSourceIssues().addIssue("Failed to read in template content for " + applicationPath, ex);
		}

		// Create the properties
		PropertyList properties = this.createInitalPropertyList(isSecure, applicationPath);
		properties.addProperty(WebTemplateSectionSource.PROPERTY_TEMPLATE_CONTENT).setValue(content.toString());

		// Add the template
		return this.addTemplate(isSecure, applicationPath, properties);
	}

	@Override
	public WebTemplate addTemplate(boolean isSecure, String applicationPath, String locationOfTemplate) {

		// Create the properties
		PropertyList properties = this.createInitalPropertyList(isSecure, applicationPath);
		properties.addProperty(WebTemplateSectionSource.PROPERTY_TEMPLATE_LOCATION).setValue(locationOfTemplate);

		// Add the template
		return this.addTemplate(isSecure, applicationPath, properties);
	}

}
