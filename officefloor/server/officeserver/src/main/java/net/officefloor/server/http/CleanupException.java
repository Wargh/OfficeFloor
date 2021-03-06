/*-
 * #%L
 * HTTP Server
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

package net.officefloor.server.http;

import java.io.PrintStream;
import java.io.PrintWriter;

import net.officefloor.frame.api.managedobject.recycle.CleanupEscalation;

/**
 * {@link Exception} wrapping the {@link CleanupEscalation} instances.
 * 
 * @author Daniel Sagenschneider
 */
public class CleanupException extends Exception {

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Print adapter.
	 */
	private static interface PrintAdapter<P> {

		/**
		 * Prints the text.
		 * 
		 * @param text    Text.
		 * @param printer Printer.
		 */
		void print(String text, P printer);

		/**
		 * Prints the text and a new line.
		 * 
		 * @param text    Text.
		 * @param printer Printer.
		 */
		void println(String text, P printer);

		/**
		 * Prints the {@link Throwable} stack trace.
		 * 
		 * @param failure Failure.
		 * @param printer Printer.
		 */
		void print(Throwable failure, P printer);
	}

	/**
	 * {@link PrintStream} {@link PrintAdapter}.
	 */
	private static PrintAdapter<PrintStream> PRINT_STREAM_ADAPTER = new PrintAdapter<PrintStream>() {

		@Override
		public void print(String text, PrintStream printer) {
			printer.print(text);
		}

		@Override
		public void println(String text, PrintStream printer) {
			printer.println(text);
		}

		@Override
		public void print(Throwable failure, PrintStream printer) {
			failure.printStackTrace(printer);
		}
	};

	/**
	 * {@link PrintWriter} {@link PrintAdapter}.
	 */
	private static PrintAdapter<PrintWriter> PRINT_WRITER_ADAPTER = new PrintAdapter<PrintWriter>() {

		@Override
		public void print(String text, PrintWriter printer) {
			printer.print(text);
		}

		@Override
		public void println(String text, PrintWriter printer) {
			printer.println(text);
		}

		@Override
		public void print(Throwable failure, PrintWriter printer) {
			failure.printStackTrace(printer);
		}
	};

	/**
	 * {@link CleanupEscalation} instances.
	 */
	private final CleanupEscalation[] cleanupEscalations;

	/**
	 * Instantiate.
	 * 
	 * @param cleanupEscalations {@link CleanupEscalation} instances.
	 */
	public CleanupException(CleanupEscalation[] cleanupEscalations) {
		this.cleanupEscalations = cleanupEscalations;
	}

	/**
	 * Obtains the {@link CleanupEscalation} instances.
	 * 
	 * @return {@link CleanupEscalation} instances.
	 */
	public CleanupEscalation[] getCleanupEscalations() {
		return this.cleanupEscalations;
	}

	/*
	 * ================= Exception =================
	 */

	@Override
	public void printStackTrace(PrintStream stream) {
		this.printStackTrace(stream, PRINT_STREAM_ADAPTER);
	}

	@Override
	public void printStackTrace(PrintWriter writer) {
		this.printStackTrace(writer, PRINT_WRITER_ADAPTER);
	}

	/**
	 * Prints the stack trace.
	 * 
	 * @param printer Printer.
	 * @param adapter {@link PrintAdapter}.
	 */
	private <P> void printStackTrace(P printer, PrintAdapter<P> adapter) {

		// Print the clean up escalation instances
		for (int i = 0; i < cleanupEscalations.length; i++) {
			CleanupEscalation cleanupEscalation = cleanupEscalations[i];

			// Write the clean up escalation
			adapter.print("Clean up failure with object of type ", printer);
			adapter.println(cleanupEscalation.getObjectType().getName(), printer);
			adapter.print(cleanupEscalation.getEscalation(), printer);
			adapter.println("", printer);
			adapter.println("", printer);
		}
	}

}
