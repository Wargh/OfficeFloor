/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2019 Daniel Sagenschneider
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
package net.officefloor.maven.woof;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import net.officefloor.compile.impl.util.CompileUtil;
import net.officefloor.woof.WoofLoaderExtensionService;

/**
 * Enables viewing WoOF configurations.
 * 
 * @author Daniel Sagenschneider
 */
@Mojo(name = "view", requiresProject = false, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ViewWoofMojo extends AbstractMojo {

	/**
	 * {@link PluginDescriptor} for {@link ViewWoofMojo}.
	 */
	@Parameter(defaultValue = "${plugin}", readonly = true)
	private PluginDescriptor plugin;

	/**
	 * Possible configured {@link Artifact} to be resolved.
	 */
	@Parameter(property = "artifact", required = false, defaultValue = "")
	private String artifact;

	@Component
	private RepositorySystem repositorySystem;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession repositorySystemSession;

	@Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true)
	private List<RemoteRepository> remoteRepositories;

	/**
	 * {@link MavenProject}.
	 */
	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	/**
	 * Path to configuration within the {@link MavenProject} / {@link Artifact}.
	 */
	@Parameter(property = "path", required = false, defaultValue = WoofLoaderExtensionService.APPLICATION_WOOF)
	private String path = WoofLoaderExtensionService.APPLICATION_WOOF;

	/*
	 * =================== AbstractMojo =================
	 */

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Obtain the URLs
		List<URL> classPathUrls = new ArrayList<>();

		// Determine if specified the artifact
		if (!CompileUtil.isBlank(this.artifact)) {

			// Attempt to resolve the artifact
			ArtifactResult result;
			try {
				ArtifactRequest request = new ArtifactRequest();
				request.setArtifact(new DefaultArtifact(this.artifact));
				request.setRepositories(this.remoteRepositories);
				result = this.repositorySystem.resolveArtifact(this.repositorySystemSession, request);
			} catch (ArtifactResolutionException ex) {
				throw new MojoExecutionException("Failed to resolve artifact " + this.artifact, ex);
			}

			// Determine if error
			for (Exception ex : result.getExceptions()) {
				throw new MojoExecutionException("Failed to resolve artifact " + this.artifact, ex);
			}

			// Ensure have artifact
			if (result.isMissing()) {
				throw new MojoExecutionException("Did not find artifact " + this.artifact);
			}

			// Obtain the artifact
			File artifactFile = result.getArtifact().getFile();

			// Load the artifact URL
			URL artifactUrl;
			try {
				artifactUrl = artifactFile.toURI().toURL();
			} catch (MalformedURLException ex) {
				throw new MojoExecutionException(
						"Failed to obtain class path from artifact: " + this.artifact + " (" + artifactFile + ")", ex);
			}

			// TODO REMOVE
			this.getLog().info("TODO artifact entry: " + artifactUrl.toString());

			// Include the URL
			classPathUrls.add(artifactUrl);
		}

		// Determine if load from current project (no artifact specified)
		if (classPathUrls.size() == 0) {

			// Ensure have current project
			if (this.project.getFile() == null) {
				throw new MojoFailureException("Must specify artifact or execute within project");
			}

			// Within project, so find on class path
			try {
				for (String classPathEntry : this.project.getRuntimeClasspathElements()) {
					classPathUrls.add(new File(classPathEntry).toURI().toURL());
				}
			} catch (DependencyResolutionRequiredException | MalformedURLException ex) {
				throw new MojoExecutionException("Failed to obtain class path from project", ex);
			}
		}

		// Load the plugin dependencies
		for (URL entryUrl : this.plugin.getClassRealm().getURLs()) {

			// TODO REMOVE
			this.getLog().info("TODO plugin entry: " + entryUrl.toString());

			classPathUrls.add(entryUrl);
		}

		// Load the JavaFX class path entries
		try {
			for (URL entryUrl : JavaFxFacet.getClassPathEntries()) {

				// TODO REMOVE
				this.getLog().info("TODO javafx entry: " + entryUrl.toString());

				classPathUrls.add(entryUrl);
			}
		} catch (Exception ex) {
			throw new MojoExecutionException("Failed to load JavaFX libraries", ex);
		}

		// Create the class loader
		try (URLClassLoader classLoader = new URLClassLoader(classPathUrls.toArray(new URL[classPathUrls.size()]),
				null)) {

			// Load the viewer
			Class<?> viewerClass;
			try {
				viewerClass = classLoader.loadClass(Viewer.class.getName());
			} catch (ClassNotFoundException ex) {
				throw new MojoExecutionException(
						"Failed to find class " + Viewer.class.getName() + " in constructed class path", ex);
			}

			// Obtain run method to Viewer
			final String methodName = "main";
			Method run;
			try {
				run = viewerClass.getMethod(methodName, String[].class);
			} catch (NoSuchMethodException | SecurityException ex) {
				throw new MojoExecutionException(
						"Unable to extract " + methodName + " method from " + Viewer.class.getName(), ex);
			}

			// Ensure context class path configured for JavaFX application launch
			Thread.currentThread().setContextClassLoader(classLoader);

			// Run the Viewer
			try {
				run.invoke(null, (Object) new String[] { this.path });
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
				throw new MojoExecutionException("Unable to start " + Viewer.class.getName(), ex);
			}

		} catch (IOException ex) {
			throw new MojoExecutionException("Failed to close class loader", ex);
		}
	}

}