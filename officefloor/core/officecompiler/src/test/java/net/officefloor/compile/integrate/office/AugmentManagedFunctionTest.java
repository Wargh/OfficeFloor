/*-
 * #%L
 * OfficeCompiler
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

package net.officefloor.compile.integrate.office;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import net.officefloor.compile.impl.structure.OfficeNodeImpl;
import net.officefloor.compile.issues.CompilerIssue;
import net.officefloor.compile.managedfunction.ManagedFunctionObjectType;
import net.officefloor.compile.spi.office.AugmentedFunctionObject;
import net.officefloor.compile.spi.office.OfficeAdministration;
import net.officefloor.compile.spi.office.OfficeArchitect;
import net.officefloor.compile.spi.office.OfficeManagedObject;
import net.officefloor.compile.spi.office.OfficeSubSection;
import net.officefloor.compile.spi.section.SectionDesigner;
import net.officefloor.compile.spi.section.SectionFunctionNamespace;
import net.officefloor.compile.spi.section.source.SectionSourceContext;
import net.officefloor.compile.spi.section.source.impl.AbstractSectionSource;
import net.officefloor.compile.test.issues.MockCompilerIssues;
import net.officefloor.compile.test.officefloor.CompileOfficeFloor;
import net.officefloor.frame.api.administration.Administration;
import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.api.manage.FunctionManager;
import net.officefloor.frame.api.manage.OfficeFloor;
import net.officefloor.frame.internal.structure.ManagedObjectScope;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.plugin.administration.clazz.ClassAdministrationSource;
import net.officefloor.plugin.managedfunction.clazz.ClassManagedFunctionSource;
import net.officefloor.plugin.managedobject.singleton.Singleton;

/**
 * Ensure able to augment {@link ManagedFunction} instances.
 * 
 * @author Daniel Sagenschneider
 */
public class AugmentManagedFunctionTest extends OfficeFrameTestCase {

	/**
	 * {@link MockObject}.
	 */
	private static MockObject object = null;

	/**
	 * Ensure can augment the {@link ManagedFunction}.
	 */
	public void testAugmentManagedFunction() throws Exception {

		// Create the managed object
		MockObject mockObject = new MockObject();

		// Compile the OfficeFloor with augmented managed function
		CompileOfficeFloor compile = new CompileOfficeFloor();
		compile.office((context) -> {
			OfficeArchitect architect = context.getOfficeArchitect();

			// Add the managed object
			OfficeManagedObject managedObject = architect
					.addOfficeManagedObjectSource("OBJECT", new Singleton(mockObject))
					.addOfficeManagedObject("OBJECT", ManagedObjectScope.PROCESS);

			// Augment the function object
			context.getOfficeArchitect().addManagedFunctionAugmentor((augment) -> {

				// Ensure have managed function name (identify function)
				assertEquals("Incorrect managed function name", "SECTION.function", augment.getManagedFunctionName());

				// Validate can add objects for function parameters
				for (ManagedFunctionObjectType<?> type : augment.getManagedFunctionType().getObjectTypes()) {
					Class<?> objectType = type.getObjectType();
					if (objectType.isAnnotationPresent(MockAnnotation.class)) {

						// Obtain the function object
						AugmentedFunctionObject object = augment.getFunctionObject(type.getObjectName());
						assertFalse("Should not be linked", object.isLinked());

						// Link managed object
						augment.link(object, managedObject);
					}
				}
			});
		});
		compile.section((context) -> {
			SectionFunctionNamespace namespace = context.getSectionDesigner().addSectionFunctionNamespace("NAMESPACE",
					ClassManagedFunctionSource.class.getName());
			namespace.addProperty(ClassManagedFunctionSource.CLASS_NAME_PROPERTY_NAME, MockFunction.class.getName());
			namespace.addSectionFunction("function", "function");
		});
		OfficeFloor officeFloor = compile.compileAndOpenOfficeFloor();

		// Reset for test
		object = null;

		// Execute the method (with augmented object)
		FunctionManager function = officeFloor.getOffice("OFFICE").getFunctionManager("SECTION.function");
		function.invokeProcess(null, null);

		// Should have loaded the augmented object
		assertSame("Should load augmented object", mockObject, object);
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface MockAnnotation {
	}

	@MockAnnotation
	public static class MockObject {
	}

	public static class MockFunction {
		public void function(MockObject object) {
			AugmentManagedFunctionTest.object = object;
		}
	}

	/**
	 * Ensure can augment the {@link ManagedFunction} within an
	 * {@link OfficeSubSection}.
	 */
	public void testAugmentSubSectionManagedFunction() throws Exception {

		// Create the managed object
		MockObject mockObject = new MockObject();

		// Compile the OfficeFloor with augmented managed function
		CompileOfficeFloor compile = new CompileOfficeFloor();
		compile.office((context) -> {
			OfficeArchitect architect = context.getOfficeArchitect();

			// Add the managed object
			OfficeManagedObject managedObject = architect
					.addOfficeManagedObjectSource("OBJECT", new Singleton(mockObject))
					.addOfficeManagedObject("OBJECT", ManagedObjectScope.PROCESS);

			// Augment the function object
			context.getOfficeArchitect().addManagedFunctionAugmentor((augment) -> {

				// Ensure have managed function name (identify function)
				assertEquals("Incorrect managed function name", "SECTION.SUB_SECTION.function",
						augment.getManagedFunctionName());

				// Validate can add objects for function parameters
				for (ManagedFunctionObjectType<?> type : augment.getManagedFunctionType().getObjectTypes()) {
					Class<?> objectType = type.getObjectType();
					if (objectType.isAnnotationPresent(MockAnnotation.class)) {

						// Obtain the function object
						AugmentedFunctionObject object = augment.getFunctionObject(type.getObjectName());
						assertFalse("Should not be linked", object.isLinked());

						// Link managed object
						augment.link(object, managedObject);
					}
				}
			});
		});
		compile.section((context) -> {
			context.getSectionDesigner().addSubSection("SUB_SECTION", new AbstractSectionSource() {
				@Override
				protected void loadSpecification(SpecificationContext context) {
				}

				@Override
				public void sourceSection(SectionDesigner designer, SectionSourceContext context) throws Exception {
					SectionFunctionNamespace namespace = designer.addSectionFunctionNamespace("NAMESPACE",
							ClassManagedFunctionSource.class.getName());
					namespace.addProperty(ClassManagedFunctionSource.CLASS_NAME_PROPERTY_NAME,
							MockFunction.class.getName());
					namespace.addSectionFunction("function", "function");
				}
			}, null);
		});
		OfficeFloor officeFloor = compile.compileAndOpenOfficeFloor();

		// Reset for test
		object = null;

		// Execute the method (with augmented object)
		FunctionManager function = officeFloor.getOffice("OFFICE").getFunctionManager("SECTION.SUB_SECTION.function");
		function.invokeProcess(null, null);

		// Should have loaded the augmented object
		assertSame("Should load augmented object", mockObject, object);
	}

	/**
	 * Ensure can augment {@link ManagedFunction} by adding
	 * {@link Administration}.
	 */
	public void testAugmentManagedFunctionWithAdministration() throws Exception {

		StringBuilder content = new StringBuilder();

		// Compile the OfficeFloor with augmented managed function
		CompileOfficeFloor compile = new CompileOfficeFloor();
		compile.office((context) -> {
			OfficeArchitect architect = context.getOfficeArchitect();

			// Register the content
			OfficeManagedObject managedObject = architect.addOfficeManagedObjectSource("OBJECT", new Singleton(content))
					.addOfficeManagedObject("OBJECT", ManagedObjectScope.PROCESS);

			// Create the administration
			OfficeAdministration administration = architect.addOfficeAdministration("ADMIN",
					ClassAdministrationSource.class.getName());
			administration.addProperty(ClassAdministrationSource.CLASS_NAME_PROPERTY_NAME,
					MockAdministration.class.getName());
			administration.administerManagedObject(managedObject);

			// Augment the function object
			context.getOfficeArchitect().addManagedFunctionAugmentor((augment) -> {

				// Ensure have managed function name (identify function)
				assertEquals("Incorrect managed function name", "SECTION.function", augment.getManagedFunctionName());

				// Add administration
				augment.addPreAdministration(administration);
				augment.addPostAdministration(administration);

				// Add the managed object
				for (ManagedFunctionObjectType<?> type : augment.getManagedFunctionType().getObjectTypes()) {
					AugmentedFunctionObject object = augment.getFunctionObject(type.getObjectName());
					augment.link(object, managedObject);
				}

			});
		});
		compile.section((context) -> {
			SectionFunctionNamespace namespace = context.getSectionDesigner().addSectionFunctionNamespace("NAMESPACE",
					ClassManagedFunctionSource.class.getName());
			namespace.addProperty(ClassManagedFunctionSource.CLASS_NAME_PROPERTY_NAME,
					MockAdministeredFunction.class.getName());
			namespace.addSectionFunction("function", "function");
		});
		OfficeFloor officeFloor = compile.compileAndOpenOfficeFloor();

		// Execute the method (with augmented administration)
		FunctionManager function = officeFloor.getOffice("OFFICE").getFunctionManager("SECTION.function");
		function.invokeProcess(null, null);

		// Should run both pre/post administration
		assertEquals("Should run augmented administration", "ADMIN FUNCTION ADMIN ", content.toString());
	}

	public static class MockAdministration {
		public void admin(Appendable[] writers) throws IOException {
			for (Appendable writer : writers) {
				writer.append("ADMIN ");
			}
		}
	}

	public static class MockAdministeredFunction {
		public void function(StringBuilder writer) {
			writer.append("FUNCTION ");
		}
	}

	/**
	 * Ensure can provide {@link CompilerIssue} in augmenting
	 * {@link ManagedFunction}.
	 */
	public void testIssueInAugmentManagedFunction() throws Exception {

		final MockCompilerIssues issues = new MockCompilerIssues(this);
		final Exception failure = new Exception("TEST");

		// Record issue
		issues.recordIssue("OFFICE", OfficeNodeImpl.class, "ISSUE");
		issues.recordIssue("OFFICE", OfficeNodeImpl.class, "ANOTHER", failure);

		// Test
		this.replayMockObjects();

		// Compile the OfficeFloor with augmented managed function
		CompileOfficeFloor compile = new CompileOfficeFloor();
		compile.getOfficeFloorCompiler().setCompilerIssues(issues);
		compile.office((context) -> {

			// Augment the function object
			context.getOfficeArchitect().addManagedFunctionAugmentor((augment) -> {
				augment.addIssue("ISSUE");
				throw augment.addIssue("ANOTHER", failure);
			});
		});
		compile.section((context) -> {
			SectionFunctionNamespace namespace = context.getSectionDesigner().addSectionFunctionNamespace("NAMESPACE",
					ClassManagedFunctionSource.class.getName());
			namespace.addProperty(ClassManagedFunctionSource.CLASS_NAME_PROPERTY_NAME,
					MockIssueFunction.class.getName());
			namespace.addSectionFunction("function", "function");
		});
		OfficeFloor officeFloor = compile.compileOfficeFloor();
		assertNull("Should not create OfficeFloor due to issue", officeFloor);

		// Ensure issue
		this.verifyMockObjects();
	}

	public static class MockIssueFunction {
		public void function() {
			fail("Should not be invoked");
		}
	}

}
