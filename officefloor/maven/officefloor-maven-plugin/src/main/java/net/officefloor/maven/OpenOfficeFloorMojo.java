/*-
 * #%L
 * Maven OfficeFloor Plugin
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

package net.officefloor.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import net.officefloor.OfficeFloorMain;
import net.officefloor.frame.api.manage.OfficeFloor;

/**
 * Open {@link OfficeFloor} {@link Mojo}.
 * 
 * @author Daniel Sagenschneider
 */
@Mojo(name = "open", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class OpenOfficeFloorMojo extends AbstractMojo {

	/**
	 * Default JMX port.
	 */
	public static final int DEFAULT_JMX_PORT = 7777;

	/**
	 * Default time out in seconds to start {@link OfficeFloor}.
	 */
	private static final int DEFAULT_TIMEOUT = 60;

	/**
	 * {@link MavenProject}.
	 */
	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	/**
	 * System properties provided to JVM.
	 */
	@Parameter(required = false)
	private Map<String, String> systemProperties;

	/**
	 * JMX port.
	 */
	@Parameter(required = false, defaultValue = "" + DEFAULT_JMX_PORT)
	private int jmxPort = DEFAULT_JMX_PORT;

	/**
	 * Time out in seconds for starting {@link OfficeFloor}.
	 */
	@Parameter(required = false, defaultValue = "" + DEFAULT_TIMEOUT)
	private int timeout = DEFAULT_TIMEOUT;

	/**
	 * {@link Process} running {@link OfficeFloor}.
	 */
	private Process process = null;

	/**
	 * Obtains the {@link Process} running {@link OfficeFloor}.
	 * 
	 * @return {@link Process} running {@link OfficeFloor}. May be
	 *         <code>null</code>.
	 */
	public Process getProcess() {
		return this.process;
	}

	/*
	 * =================== AbstractMojo =================
	 */

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Should be able to start
		try {

			// Obtain details to run
			String javaHome = System.getProperty("java.home");
			String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

			// Generate the class path
			StringBuilder classPath = new StringBuilder();
			boolean isFirst = true;
			for (String classPathEntry : this.project.getRuntimeClasspathElements()) {
				if (!isFirst) {
					classPath.append(File.pathSeparator);
				}
				isFirst = false;
				classPath.append(classPathEntry);
			}

			// Create the command line
			List<String> commandLine = new LinkedList<>();
			commandLine.add(javaBin);
			commandLine.add("-cp");
			commandLine.add(classPath.toString());
			commandLine.add("-Dcom.sun.management.jmxremote.port=" + this.jmxPort);
			commandLine.add("-Dcom.sun.management.jmxremote.authenticate=false");
			commandLine.add("-Dcom.sun.management.jmxremote.ssl=false");
			if (this.systemProperties != null) {
				for (String name : this.systemProperties.keySet()) {

					// Ensure system property is valid
					if (name.startsWith("com.sun.management.jmxremote.")) {
						throw new MojoExecutionException(
								"JMX configuration managed by officefloor-maven-plugin.  Can not configure property "
										+ name);
					}
					String value = this.systemProperties.get(name);

					// Add the system property
					commandLine.add("-D" + name.trim() + "=" + value.trim());
				}
			}
			commandLine.add(OfficeFloorMain.class.getName());

			// Log the command line
			this.getLog().debug("Running OfficeFloor with: " + String.join(" ", commandLine));

			// Build and start the process
			ProcessBuilder builder = new ProcessBuilder(commandLine.toArray(new String[commandLine.size()]));
			this.process = builder.start();

			// Gobble streams
			StreamLock lock = new StreamLock();
			StdOutStreamGobbler stdout = new StdOutStreamGobbler(this.process.getInputStream(), lock);
			StdErrStreamGobbler stderr = new StdErrStreamGobbler(this.process.getErrorStream(), lock);

			// Wait on OfficeFloor to open (or fail in opening)
			long startTime = System.currentTimeMillis();
			String errorPrefix = "OfficeFloor failed to open. ";
			synchronized (lock) {
				for (;;) {

					// Determine if error
					if (stdout.failure != null) {
						lock.stopCapture();
						throw new MojoExecutionException("Failed to open " + OfficeFloor.class.getSimpleName(),
								stdout.failure);
					}
					if (stderr.failure != null) {
						lock.stopCapture();
						throw new MojoExecutionException("Failed to open " + OfficeFloor.class.getSimpleName(),
								stderr.failure);
					}

					// Determine if error in opening
					if ((stderr.isComplete) && (stderr.errorContent != null)) {
						lock.stopCapture();
						throw new MojoExecutionException(errorPrefix + stderr.errorContent.toString());
					}

					// Determine if opened
					if (stdout.isOpen) {
						lock.stopCapture();
						return; // successfully opened
					}

					// Determine timed out waiting on open
					if ((startTime + (OpenOfficeFloorMojo.this.timeout * 1000)) < System.currentTimeMillis()) {

						// Stop the capture
						lock.stopCapture();

						// Provide error details if available
						String errorMessage = (stderr.errorContent != null) ? stderr.errorContent.toString() : "";
						if (errorMessage.trim().length() > 0) {
							throw new MojoExecutionException(errorPrefix + errorMessage);
						}

						// Indicate timed out waiting to open (providing logs to help determine why)
						String stdoutText = stdout.capture.toString();
						String stderrText = stderr.capture.toString();
						throw new MojoFailureException("Timed out waiting on " + OfficeFloor.class.getSimpleName()
								+ " to open after " + OpenOfficeFloorMojo.this.timeout + " seconds\n\nstdout:\n"
								+ stdoutText + "\n\nstderr:\n" + stderrText);
					}

					// Wait some time
					try {
						lock.wait(10);
					} catch (InterruptedException ex) {
						throw new MojoExecutionException(
								"Wait on " + OfficeFloor.class.getSimpleName() + " interrupted", ex);
					}
				}
			}

		} catch (Exception ex) {
			if (ex instanceof MojoExecutionException) {
				throw (MojoExecutionException) ex;
			} else if (ex instanceof MojoFailureException) {
				throw (MojoFailureException) ex;
			}
			throw new MojoFailureException("Failed to open " + OfficeFloor.class.getSimpleName(), ex);
		}
	}

	/**
	 * State of the stream.
	 */
	private static class StreamLock {

		/**
		 * Flags whether to continue capturing the logs.
		 */
		private boolean isCapture = true;

		/**
		 * Flags to stop capture of logs.
		 */
		protected void stopCapture() {
			this.isCapture = false;
		}
	}

	/**
	 * Gobbles the Stream.
	 */
	private abstract class StreamGobbler extends Thread {

		/**
		 * {@link InputStream} to gobble.
		 */
		private final BufferedReader input;

		/**
		 * {@link StreamLock}.
		 */
		protected final StreamLock lock;

		/**
		 * Indicates if error.
		 */
		private final boolean isStdErr;

		/**
		 * {@link Log} to log output.
		 */
		private final Log logger;

		/**
		 * Capture of stream.
		 */
		protected StringWriter capture = new StringWriter();

		/**
		 * Possible failure in running process.
		 */
		protected Throwable failure = null;

		/**
		 * Indicates if complete.
		 */
		protected boolean isComplete = false;

		/**
		 * Initiate.
		 * 
		 * @param input    {@link InputStream} to gobble.
		 * @param lock     {@link StreamLock}.
		 * @param isStdErr Indicates if <code>stderr</code>.
		 */
		private StreamGobbler(InputStream input, StreamLock lock, boolean isStdErr) {
			this.input = new BufferedReader(new InputStreamReader(input));
			this.lock = lock;
			this.isStdErr = isStdErr;

			// Obtain the logger
			this.logger = OpenOfficeFloorMojo.this.getLog();

			// Flag as deamon (should not stop process finishing)
			this.setDaemon(true);

			// Start gobbling
			this.start();
		}

		/**
		 * Handles the output line.
		 * 
		 * @param outputLine Output line.
		 */
		protected abstract void handleOutputLine(String outputLine);

		/**
		 * Logs the output line.
		 * 
		 * @param outputLine Output line.
		 */
		protected void logOutputLine(String outputLine) {
			if (this.isStdErr) {
				this.logger.error(outputLine);
			} else {
				this.logger.info(outputLine);
			}
		}

		/*
		 * ================= Thread ======================
		 */

		@Override
		public void run() {
			try {

				// Consume from stream until EOF
				String line;
				while ((line = this.input.readLine()) != null) {

					// Capture logs
					synchronized (this.lock) {
						if (this.lock.isCapture) {
							this.capture.write(line + "\n");
						}
					}

					// Handle only non-blank output lines
					if (line.trim().length() > 0) {
						this.handleOutputLine(line);
					}
				}

			} catch (Throwable ex) {
				// Propagate failure in stream
				synchronized (this.lock) {
					this.failure = ex;
					this.lock.notify();
				}

			} finally {
				// Flag complete
				synchronized (this.lock) {
					this.isComplete = true;
					this.lock.notify();
				}
			}
		}
	}

	/**
	 * Waits for {@link OfficeFloorMain} to be running.
	 */
	private class StdOutStreamGobbler extends StreamGobbler {

		/**
		 * Indicates if {@link OfficeFloor} has opened.
		 */
		private boolean isOpen = false;

		/**
		 * Instantiate.
		 * 
		 * @param input {@link InputStream} to stdout to gobble.
		 * @param lock  {@link StreamLock}.
		 */
		private StdOutStreamGobbler(InputStream input, StreamLock lock) {
			super(input, lock, false);
		}

		/*
		 * ============== StreamGobbler ==============
		 */

		@Override
		protected void handleOutputLine(String outputLine) {

			// Determine if open
			if (OfficeFloorMain.STD_OUT_RUNNING_LINE.equals(outputLine)) {
				synchronized (this.lock) {
					this.isOpen = true;
					this.lock.notify();
				}
			}

			// Log the line
			this.logOutputLine(outputLine);
		}
	}

	/**
	 * Waits for possible {@link OfficeFloorMain} to fail.
	 */
	private class StdErrStreamGobbler extends StreamGobbler {

		/**
		 * Captures the error content.
		 */
		private StringWriter errorContent = null;

		/**
		 * Instantiate.
		 * 
		 * @param input {@link InputStream} to stderr to gobble.
		 * @param lock  {@link StreamLock}.
		 */
		private StdErrStreamGobbler(InputStream input, StreamLock lock) {
			super(input, lock, true);
		}

		/*
		 * ============== StreamGobbler ==============
		 */

		@Override
		protected void handleOutputLine(String outputLine) {

			// Determine if fail open, so start capture error details
			if ((this.errorContent == null) && (OfficeFloorMain.STD_ERR_FAIL_LINE.equals(outputLine))) {
				this.errorContent = new StringWriter();
				return;
			}

			// Handle error content
			if (this.errorContent != null) {
				// Include error details for open failure
				this.errorContent.write(outputLine + "\n");

			} else {
				// Log the line
				this.logOutputLine(outputLine);
			}
		}
	}

}