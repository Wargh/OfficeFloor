package net.officefloor.extension;

import org.junit.Assert;

import net.officefloor.compile.OfficeFloorCompiler;
import net.officefloor.compile.spi.office.OfficeArchitect;
import net.officefloor.compile.spi.office.extension.OfficeExtensionContext;
import net.officefloor.compile.spi.office.extension.OfficeExtensionService;
import net.officefloor.compile.test.issues.FailTestCompilerIssues;
import net.officefloor.frame.api.manage.Office;
import net.officefloor.frame.api.manage.OfficeFloor;

/**
 * <p>
 * {@link OfficeExtensionService} to configure the {@link Office} within tests.
 * <p>
 * For this to operate within tests, the
 * <code>src/test/resources/META-INF/services/net.officefloor.compile.spi.office.extension.OfficeExtensionService</code>
 * must be configured with this class name.
 * 
 * @author Daniel Sagenschneider
 */
public class CompileOffice implements OfficeExtensionService {

	/**
	 * {@link OfficeExtensionService} logic.
	 */
	private static OfficeExtensionService extender = null;

	/**
	 * {@link OfficeFloorCompiler}.
	 */
	private final OfficeFloorCompiler compiler;

	/**
	 * Instantiate.
	 */
	public CompileOffice() {
		this(OfficeFloorCompiler.newOfficeFloorCompiler(null));
	}

	/**
	 * Instantiate.
	 * 
	 * @param officeFloorCompiler
	 *            {@link OfficeFloorCompiler} to use.
	 */
	public CompileOffice(OfficeFloorCompiler officeFloorCompiler) {
		this.compiler = officeFloorCompiler;
		this.compiler.setCompilerIssues(new FailTestCompilerIssues());
	}

	/**
	 * Obtains the {@link OfficeFloorCompiler}.
	 * 
	 * @return {@link OfficeFloorCompiler}.
	 */
	public OfficeFloorCompiler getOfficeFloorCompiler() {
		return this.compiler;
	}

	/**
	 * Compiles the {@link Office}.
	 * 
	 * @param officeConfiguration
	 *            {@link OfficeExtensionService} to configure the
	 *            {@link Office}.
	 * @return {@link OfficeFloor}.
	 * @throws Exception
	 *             If fails to compile the {@link OfficeFloor}.
	 */
	public OfficeFloor compileOffice(OfficeExtensionService officeConfiguration) throws Exception {

		// Compile the solution
		try {
			extender = officeConfiguration;

			// Compile and return the office
			return this.compiler.compile("OfficeFloor");

		} finally {
			// Ensure the extender is cleared for other tests
			extender = null;
		}

	}

	/**
	 * Compiles and opens the {@link Office}.
	 * 
	 * @param officeConfiguration
	 *            {@link OfficeExtensionService} to configure the
	 *            {@link Office}.
	 * @return {@link OfficeFloor}.
	 * @throws Exception
	 *             If fails to compile and open the {@link OfficeFloor}.
	 */
	public OfficeFloor compileAndOpenOffice(OfficeExtensionService officeConfiguration) throws Exception {

		// Compile the OfficeFloor
		OfficeFloor officeFloor = this.compileOffice(officeConfiguration);

		// Open the OfficeFloor
		Assert.assertNotNull("Should compile OfficeFloor", officeFloor);
		officeFloor.openOfficeFloor();

		// Return the OfficeFloor
		return officeFloor;
	}

	/*
	 * ====================== OfficeExtensionService =====================
	 */

	@Override
	public void extendOffice(OfficeArchitect officeArchitect, OfficeExtensionContext context) throws Exception {
		if (extender != null) {
			extender.extendOffice(officeArchitect, context);
		}
	}

}