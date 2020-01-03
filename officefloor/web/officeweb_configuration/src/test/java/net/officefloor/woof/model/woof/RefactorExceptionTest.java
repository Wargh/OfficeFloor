package net.officefloor.woof.model.woof;

import java.io.IOException;

import net.officefloor.model.change.Change;
import net.officefloor.woof.model.woof.WoofExceptionModel;

/**
 * Tests refactoring the {@link WoofExceptionModel}.
 * 
 * @author Daniel Sagenschneider
 */
public class RefactorExceptionTest extends AbstractWoofChangesTestCase {

	/**
	 * {@link WoofExceptionModel}.
	 */
	private WoofExceptionModel exception;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.exception = this.model.getWoofExceptions().get(0);
	}

	/**
	 * Ensure no issue if refactored to same exception.
	 */
	public void testNoChange() {

		// Change to a unique exception
		Change<WoofExceptionModel> change = this.operations.refactorException(this.exception,
				RuntimeException.class.getName());

		// Validate the change
		this.assertChange(change, this.exception, "Refactor Exception", true);
	}

	/**
	 * Ensure can change the exception.
	 */
	public void testChangeException() {

		// Change to a unique exception
		Change<WoofExceptionModel> change = this.operations.refactorException(this.exception,
				NullPointerException.class.getName());

		// Validate the change
		this.assertChange(change, this.exception, "Refactor Exception", true);
	}

	/**
	 * Ensure not able to refactor to an existing {@link Exception}.
	 */
	public void testExceptionAlreadyExists() {

		// Change to a unique exception
		Change<WoofExceptionModel> change = this.operations.refactorException(this.exception,
				IOException.class.getName());

		// Validate no change (as exception is already handled)
		this.assertChange(change, this.exception, "Refactor Exception", false,
				"Exception already exists for '" + IOException.class.getName() + "'");
	}

}