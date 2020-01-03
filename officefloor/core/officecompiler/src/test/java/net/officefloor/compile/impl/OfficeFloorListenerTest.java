package net.officefloor.compile.impl;

import net.officefloor.compile.OfficeFloorCompiler;
import net.officefloor.compile.spi.officefloor.extension.OfficeFloorExtensionService;
import net.officefloor.compile.test.officefloor.CompileOfficeFloor;
import net.officefloor.frame.api.build.OfficeFloorEvent;
import net.officefloor.frame.api.build.OfficeFloorListener;
import net.officefloor.frame.api.manage.OfficeFloor;
import net.officefloor.frame.test.OfficeFrameTestCase;

/**
 * Ensure able to listen in on open/close of {@link OfficeFloor}.
 * 
 * @author Daniel Sagenschneider
 */
public class OfficeFloorListenerTest extends OfficeFrameTestCase {

	/**
	 * Open {@link OfficeFloor} event.
	 */
	private OfficeFloorEvent openEvent = null;

	/**
	 * Close {@link OfficeFloor} event.
	 */
	private OfficeFloorEvent closeEvent = null;

	/**
	 * {@link OfficeFloorListener}.
	 */
	private final OfficeFloorListener listener = new OfficeFloorListener() {

		@Override
		public void officeFloorOpened(OfficeFloorEvent event) throws Exception {
			OfficeFloorListenerTest.this.openEvent = event;
		}

		@Override
		public void officeFloorClosed(OfficeFloorEvent event) throws Exception {
			OfficeFloorListenerTest.this.closeEvent = event;
		}
	};

	/**
	 * Ensure can listen to the open/close of the {@link OfficeFloor}.
	 */
	public void testListenToOpenCloseOfOfficeFloor() throws Exception {

		// Compile the OfficeFloor
		OfficeFloorCompiler compiler = OfficeFloorCompiler.newOfficeFloorCompiler(null);
		compiler.addOfficeFloorListener(this.listener);
		OfficeFloor officeFloor = compiler.compile("OfficeFloor");

		// Validate
		this.validateOfficeFloorListener(officeFloor);
	}

	/**
	 * Ensure can {@link OfficeFloorExtensionService} can add an
	 * {@link OfficeFloorListener}.
	 */
	public void testOfficeFloorExtensionListenToOpenCloseOfficeFloor() throws Exception {

		// Compile the OfficeFloor
		CompileOfficeFloor compile = new CompileOfficeFloor();
		compile.officeFloor((context) -> {
			context.getOfficeFloorDeployer().addOfficeFloorListener(this.listener);
		});
		OfficeFloor officeFloor = compile.compileOfficeFloor();

		// Validate
		this.validateOfficeFloorListener(officeFloor);
	}

	/**
	 * Validates the {@link OfficeFloorListener}.
	 * 
	 * @param officeFloor
	 *            {@link OfficeFloor} with configured
	 *            {@link OfficeFloorListener}.
	 */
	private void validateOfficeFloorListener(OfficeFloor officeFloor) throws Exception {

		// Ensure not open
		assertNull("Initially should not be open", this.openEvent);
		assertNull("Initially should not be closed", this.closeEvent);

		// Open the OfficeFloor
		officeFloor.openOfficeFloor();
		assertNotNull("Should now be open", this.openEvent);
		assertSame("Incorrect open OfficeFloor", officeFloor, this.openEvent.getOfficeFloor());
		assertNull("Should not yet be closed", this.closeEvent);

		// Close the OfficeFloor
		officeFloor.closeOfficeFloor();
		assertNotNull("Should now be closed", this.closeEvent);
		assertSame("Incorrect close OfficeFloor", officeFloor, this.closeEvent.getOfficeFloor());
	}

}