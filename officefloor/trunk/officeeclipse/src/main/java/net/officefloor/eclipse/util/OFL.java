/*
 *  Office Floor, Application Server
 *  Copyright (C) 2006 Daniel Sagenschneider
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms 
 *  of the GNU General Public License as published by the Free Software Foundation; either 
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program; 
 *  if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
 *  MA 02111-1307 USA
 */
package net.officefloor.eclipse.util;

/**
 * Office Floor Logger (OFL), used mainly for debugging.
 * 
 * @author Daniel
 */
public class OFL {

	/**
	 * Log levels.
	 */
	private enum LogLevel {
		NONE, DEBUG, ERROR
	}

	/**
	 * Log level.
	 */
	private static final LogLevel logLevel;

	/**
	 * Initiates the log level from system properties.
	 */
	static {
		String levelName = System.getProperty("office.floor.eclipse.log.level");
		if ("debug".equalsIgnoreCase(levelName)) {
			logLevel = LogLevel.DEBUG;
		} else {
			logLevel = LogLevel.NONE;
		}
	}

	/**
	 * Debug level log.
	 * 
	 * @param messageParts
	 *            Message parts to be concatenated to construct the log message.
	 */
	public static void debug(Object... messageParts) {
		switch (logLevel) {
		case DEBUG:
			logMessage(LogLevel.DEBUG, messageParts);
		}
	}

	/**
	 * Logs the message.
	 * 
	 * @param level
	 *            {@link LogLevel}.
	 * @param messageParts
	 *            Parts of the message.
	 */
	private static void logMessage(LogLevel level, Object... messageParts) {

		// Create the message
		String message = constructLogMessage(messageParts);

		// Obtain location called from
		Throwable stackDetail = new Throwable();
		StackTraceElement[] stackTrace = stackDetail.getStackTrace();
		String location = "";
		if (stackTrace.length > 2) {
			// This method, debug method, caller (2)
			StackTraceElement caller = stackTrace[2];
			location = "[" + caller.getClassName() + "#"
					+ caller.getMethodName() + " (" + caller.getFileName()
					+ ":" + caller.getLineNumber() + ")]";
		}

		// Log the message
		logMessage(level, message + "       " + location);
	}

	/**
	 * Logs the message.
	 * 
	 * @param message
	 *            Message.
	 */
	private static void logMessage(LogLevel level, String message) {
		// TODO consider better way to log message
		switch (level) {
		case DEBUG:
			System.out.println(message);
			break;
		case ERROR:
			System.err.print(message);
			break;
		}
	}

	/**
	 * Constructs the message from the input parts.
	 * 
	 * @param messageParts
	 *            Message parts.
	 * @return Message.
	 */
	private static String constructLogMessage(Object... messageParts) {
		StringBuilder message = new StringBuilder();
		for (Object part : messageParts) {
			message.append(part);
		}
		return message.toString();
	}

	/**
	 * All access via static methods.
	 */
	private OFL() {
	}

}
