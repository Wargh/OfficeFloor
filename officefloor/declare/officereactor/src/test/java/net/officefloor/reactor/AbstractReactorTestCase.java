/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2020 Daniel Sagenschneider
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.officefloor.reactor;

import java.util.function.Consumer;

import junit.framework.AssertionFailedError;
import net.officefloor.activity.impl.procedure.ClassProcedureSource;
import net.officefloor.activity.procedure.ProcedureLoaderUtil;
import net.officefloor.activity.procedure.ProcedureType;
import net.officefloor.activity.procedure.ProcedureTypeBuilder;
import net.officefloor.activity.procedure.build.ProcedureArchitect;
import net.officefloor.activity.procedure.build.ProcedureEmployer;
import net.officefloor.compile.spi.office.OfficeArchitect;
import net.officefloor.compile.spi.office.OfficeEscalation;
import net.officefloor.compile.spi.office.OfficeSection;
import net.officefloor.compile.test.officefloor.CompileOfficeFloor;
import net.officefloor.frame.api.manage.OfficeFloor;
import net.officefloor.frame.test.OfficeFrameTestCase;
import net.officefloor.plugin.section.clazz.Parameter;

/**
 * Abstract Reactor test functionality.
 * 
 * @author Daniel Sagenschneider
 */
public class AbstractReactorTestCase extends OfficeFrameTestCase {

	/**
	 * Success.
	 */
	protected Object success;

	/**
	 * Failure.
	 */
	protected Throwable failure;

	/**
	 * Undertakes test for successful value.
	 * 
	 * @param methodName      Name of method for procedure.
	 * @param expectedSuccess Expected success.
	 * @param typeBuilder     Builds the expected type.
	 */
	protected void success(String methodName, Object expectedSuccess, Consumer<ProcedureTypeBuilder> typeBuilder) {
		this.test(methodName, typeBuilder, (officeFloor) -> {
			assertNull("Should be no failure", this.failure);
			assertEquals("Incorrect success", expectedSuccess, this.success);
		});
	}

	/**
	 * Undertake test for failure.
	 * 
	 * @param methodName       Name of method for procedure.
	 * @param exceptionHandler Handler of the exception.
	 * @param typeBuilder      Builds the expected type.
	 */
	protected void failure(String methodName, Consumer<Throwable> exceptionHandler,
			Consumer<ProcedureTypeBuilder> typeBuilder) {
		this.test(methodName, typeBuilder, (officeFloor) -> {
			assertNull("Should be no success", this.success);
			assertNotNull("Should be a failure", this.failure);
			exceptionHandler.accept(this.failure);
		});
	}

	/**
	 * Undertake tests for invalid return type.
	 * 
	 * @param methodName   Name of method for procedure.
	 * @param errorMessage Error message.
	 */
	protected void invalid(String methodName, String errorMessage) {
		ProcedureTypeBuilder builder = ProcedureLoaderUtil.createProcedureTypeBuilder(methodName, null);
		boolean isSuccessful = false;
		try {
			ProcedureLoaderUtil.validateProcedureType(builder, this.getClass().getName(), methodName);
			isSuccessful = true;
		} catch (AssertionFailedError ex) {

		}
		assertFalse("Should not be successful", isSuccessful);
	}

	/**
	 * Runs test.
	 * 
	 * @param methodName  Name of method for procedure.
	 * @param typeBuilder Builds the expected {@link ProcedureType}.
	 * @param testRunner  Confirms result of test.
	 */
	protected void test(String methodName, Consumer<ProcedureTypeBuilder> typeBuilder,
			Consumer<OfficeFloor> testRunner) {

		// Ensure correct type
		ProcedureTypeBuilder builder = ProcedureLoaderUtil.createProcedureTypeBuilder(methodName, null);
		if (typeBuilder != null) {
			typeBuilder.accept(builder);
		}
		ProcedureLoaderUtil.validateProcedureType(builder, this.getClass().getName(), methodName);

		// Ensure can invoke procedure and resolve Mono
		CompileOfficeFloor compiler = new CompileOfficeFloor();
		compiler.office((context) -> {
			OfficeArchitect officeArchitect = context.getOfficeArchitect();
			ProcedureArchitect<OfficeSection> procedureArchitect = ProcedureEmployer
					.employProcedureArchitect(officeArchitect, context.getOfficeSourceContext());

			// Create the procedure under test
			OfficeSection procedure = procedureArchitect.addProcedure(methodName, this.getClass().getName(),
					ClassProcedureSource.SOURCE_NAME, methodName, true, null);

			// Capture success
			OfficeSection capture = procedureArchitect.addProcedure("capture", this.getClass().getName(), "capture",
					ClassProcedureSource.SOURCE_NAME, false, null);
			officeArchitect.link(procedure.getOfficeSectionOutput(ProcedureArchitect.NEXT_OUTPUT_NAME),
					capture.getOfficeSectionInput(ProcedureArchitect.INPUT_NAME));

			// Handle failure
			OfficeEscalation escalation = officeArchitect.addOfficeEscalation(Throwable.class.getName());
			OfficeSection handle = procedureArchitect.addProcedure("handle", this.getClass().getName(),
					ClassProcedureSource.SOURCE_NAME, "handle", false, null);
			officeArchitect.link(escalation, handle.getOfficeSectionInput(ProcedureArchitect.INPUT_NAME));
		});
		try (OfficeFloor officeFloor = compiler.compileAndOpenOfficeFloor()) {
			this.success = null;
			this.failure = null;
			CompileOfficeFloor.invokeProcess(officeFloor, methodName + ".procedure", null);
			testRunner.accept(officeFloor);
		} catch (Throwable ex) {
			throw fail(ex);
		}
	}

	public void capture(@Parameter Object parameter) {
		this.success = parameter;
	}

	public void handle(@Parameter Throwable parameter) {
		this.failure = parameter;
	}

}