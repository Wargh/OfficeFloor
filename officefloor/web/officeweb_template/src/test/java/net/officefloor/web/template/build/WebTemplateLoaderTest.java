/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2018 Daniel Sagenschneider
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
package net.officefloor.web.template.build;

import java.io.IOException;
import java.io.StringReader;
import java.util.function.Consumer;

import net.officefloor.compile.OfficeFloorCompiler;
import net.officefloor.compile.spi.section.SectionDesigner;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.plugin.managedfunction.clazz.FlowInterface;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.web.template.extension.WebTemplateExtension;
import net.officefloor.web.template.extension.WebTemplateExtensionContext;
import net.officefloor.web.template.section.WebTemplateLinkAnnotation;
import net.officefloor.web.template.type.WebTemplateLoader;
import net.officefloor.web.template.type.WebTemplateLoaderUtil;
import net.officefloor.web.template.type.WebTemplateType;

/**
 * Tests the {@link WebTemplateLoader}.
 * 
 * @author Daniel Sagenschneider
 */
public class WebTemplateLoaderTest extends OfficeFrameTestCase {

	/**
	 * Ensure can load with simple {@link WebTemplate}.
	 */
	public void testSimple() {
		this.doTypeTest(false, "/path", "template", null, null);
	}

	/**
	 * Ensure provides link.
	 */
	public void testTemplateLink() {
		this.doTypeTest(false, "/path", "#{link}", null, (designer) -> {
			designer.addSectionInput("link", null).addAnnotation(new WebTemplateLinkAnnotation(false, "link"));
			designer.addSectionOutput("link", null, false);
		});
	}

	/**
	 * Ensure no output if link handled by method.
	 */
	public void testHandledLink() {
		this.doTypeTest(false, "/path", "#{link}", (template) -> {
			template.setLogicClass(HandledLink.class.getName());
		}, (designer) -> {
			designer.addSectionInput("link", null).addAnnotation(new WebTemplateLinkAnnotation(false, "link"));
		});
	}

	public static class HandledLink {
		public void link() {
		}
	}

	/**
	 * Ensure provides output from logic.
	 */
	public void testLogicOutput() {
		this.doTypeTest(false, "/path", "template", (template) -> {
			template.setLogicClass(LogicOutput.class.getName());
		}, (designer) -> {
			designer.addSectionInput("getTemplate", null);
			designer.addSectionOutput("flow", null, false);
		});
	}

	@FlowInterface
	public static interface LogicOutputFlows {
		void flow();
	}

	public static class LogicOutput {
		public void getTemplate(LogicOutputFlows flows) {
		}
	}

	/**
	 * Ensure handle dynamic path.
	 */
	public void testDynamicPath() {
		this.doTypeTest(false, "/path/{param}", "template", (template) -> {
			template.setLogicClass(DynamicPath.class.getName());
			template.setRedirectValuesFunction("redirect");
		}, (designer) -> {
			designer.addSectionInput("getParam", null);
			designer.addSectionInput("redirect", null);
		});
	}

	public static class DynamicPath {
		public DynamicPath redirect() {
			return this;
		}

		public String getParam() {
			return "value";
		}
	}

	/**
	 * Ensure can extend the {@link WebTemplate}.
	 */
	public void testExtendTemplate() throws Exception {
		this.doTypeTest(false, "/extend", "#{override}", (template) -> {
			template.addExtension(MockWebTemplateExtension.class.getName()).addProperty("test", "available");
		}, (designer) -> {
			designer.addSectionInput("extend", null).addAnnotation(new WebTemplateLinkAnnotation(false, "extend"));
			designer.addSectionOutput("extend", null, false);
		});
	}

	public static class MockWebTemplateExtension implements WebTemplateExtension {
		@Override
		public void extendWebTemplate(WebTemplateExtensionContext context) throws Exception {
			assertEquals("Should obtain configured property", "available", context.getProperty("test"));
			context.setTemplateContent("#{extend}");
		}
	}

	/**
	 * Ensure handle template inheritance.
	 */
	public void testTemplateInheritance() throws Exception {

		// Create the loader
		WebTemplateLoader loader = WebTemplateArchitectEmployer
				.employWebTemplateLoader(OfficeFloorCompiler.newOfficeFloorCompiler(null));

		// Create the template
		WebTemplate template = loader.addTemplate(false, "/path", new StringReader("#{link}"));

		// Provide parent
		WebTemplate parent = loader.addTemplate(false, "/parent", new StringReader("#{link}"));
		template.setSuperTemplate(parent);

		// Load the type
		WebTemplateType type = loader.loadWebTemplateType(template);

		// Create the expected type (already supplying common)
		SectionDesigner expected = WebTemplateLoaderUtil.createSectionDesigner();
		expected.addSectionInput("link", null).addAnnotation(new WebTemplateLinkAnnotation(false, "link"));
		expected.addSectionInput("renderTemplate", null);
		expected.addSectionOutput("link", null, false);
		expected.addSectionOutput(IOException.class.getName(), IOException.class.getName(), true);
		expected.addSectionObject(ServerHttpConnection.class.getName(), ServerHttpConnection.class.getName());

		// Validate the type
		WebTemplateLoaderUtil.validateWebTemplateType(expected, type);
	}

	/**
	 * Undertakes the type test.
	 * 
	 * @param isSecure
	 *            Indicates if secure.
	 * @param path
	 *            Application Path.
	 * @param templateContent
	 *            Content for the {@link WebTemplate}.
	 * @param webTemplateDecorator
	 *            {@link WebTemplate} decorator. May be <code>null</code>.
	 * @param typeDecorator
	 *            Type decorator. May be <code>null</code>.
	 */
	public void doTypeTest(boolean isSecure, String path, String templateContent,
			Consumer<WebTemplate> webTemplateDecorator, Consumer<SectionDesigner> typeDecorator) {
		try {

			// Create the loader
			WebTemplateLoader loader = WebTemplateArchitectEmployer
					.employWebTemplateLoader(OfficeFloorCompiler.newOfficeFloorCompiler(null));

			// Load the type
			WebTemplate template = loader.addTemplate(isSecure, path, new StringReader(templateContent));
			if (webTemplateDecorator != null) {
				webTemplateDecorator.accept(template);
			}
			WebTemplateType type = loader.loadWebTemplateType(template);

			// Create the expected type (already supplying common)
			SectionDesigner expected = WebTemplateLoaderUtil.createSectionDesigner();
			if (typeDecorator != null) {
				typeDecorator.accept(expected);
			}
			expected.addSectionInput("renderTemplate", null);
			expected.addSectionOutput(IOException.class.getName(), IOException.class.getName(), true);
			expected.addSectionObject(ServerHttpConnection.class.getName(), ServerHttpConnection.class.getName());

			// Validate the type
			WebTemplateLoaderUtil.validateWebTemplateType(expected, type);

		} catch (Exception ex) {
			throw fail(ex);
		}
	}

}