/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2012 Daniel Sagenschneider
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

package net.officefloor.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import net.officefloor.building.command.LocalRepositoryOfficeFloorCommandParameter;
import net.officefloor.building.command.RemoteRepositoryUrlsOfficeFloorCommandParameter;
import net.officefloor.building.command.parameters.KeyStoreOfficeFloorCommandParameterImpl;
import net.officefloor.building.command.parameters.KeyStorePasswordOfficeFloorCommandParameterImpl;
import net.officefloor.building.command.parameters.OfficeBuildingPortOfficeFloorCommandParameter;
import net.officefloor.building.command.parameters.PasswordOfficeFloorCommandParameterImpl;
import net.officefloor.building.command.parameters.RemoteRepositoryUrlsOfficeFloorCommandParameterImpl;
import net.officefloor.building.command.parameters.UsernameOfficeFloorCommandParameterImpl;
import net.officefloor.building.manager.OfficeBuildingManager;
import net.officefloor.building.process.ProcessConfiguration;
import net.officefloor.console.OfficeBuilding;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

/**
 * Maven goal to start the {@link OfficeBuilding}.
 * 
 * @goal start
 * @requiresDependencyResolution compile
 * 
 * @author Daniel Sagenschneider
 */
public class StartOfficeBuildingGoal extends AbstractGoal {

	/**
	 * Default {@link OfficeBuilding} port.
	 */
	public static final Integer DEFAULT_OFFICE_BUILDING_PORT = Integer
			.valueOf(OfficeBuildingPortOfficeFloorCommandParameter.DEFAULT_OFFICE_BUILDING_PORT);

	/**
	 * Makes the default key store file available.
	 * 
	 * @return Key store {@link File}.
	 * @throws MojoFailureException
	 *             If fails to obtian the key store {@link File}.
	 */
	public static File getKeyStoreFile() throws MojoFailureException {

		// Obtain location for the key store file
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File keyStore = new File(tempDir, "officefloorkeystore.jks");

		// Ensure the key store file exists
		if (!(keyStore.exists())) {
			try {
				// Create the file
				InputStream contents = Thread
						.currentThread()
						.getContextClassLoader()
						.getResourceAsStream(
								KeyStoreOfficeFloorCommandParameterImpl.DEFAULT_KEY_STORE_CLASSPATH_LOCATION);
				FileOutputStream output = new FileOutputStream(keyStore, false);
				for (int value = contents.read(); value != -1; value = contents
						.read()) {
					output.write(value);
				}
				output.close();
				contents.close();
			} catch (IOException ex) {
				throw new MojoFailureException(
						"Failed making default key store available: "
								+ ex.getMessage() + " ["
								+ ex.getClass().getName() + "]");
			}
		}

		// Return the key store file
		return keyStore;
	}

	/**
	 * Key store {@link File} password.
	 */
	public static final String KEY_STORE_PASSWORD = KeyStorePasswordOfficeFloorCommandParameterImpl.DEFAULT_KEY_STORE_PASSWORD;

	/**
	 * Client user name.
	 */
	public static final String USER_NAME = UsernameOfficeFloorCommandParameterImpl.DEFAULT_USER_NAME;

	/**
	 * Client password.
	 */
	public static final String PASSWORD = PasswordOfficeFloorCommandParameterImpl.DEFAULT_PASSWORD;

	/**
	 * Ensures the {@link OfficeBuilding} is running on the
	 * {@link #DEFAULT_OFFICE_BUILDING_PORT}.
	 * 
	 * @param project
	 *            {@link MavenProject}.
	 * @param pluginDependencies
	 *            Plug-in dependencies.
	 * @param localRepository
	 *            Local repository.
	 * @param wagonProvider
	 *            {@link WagonProvider}.
	 * @param log
	 *            {@link Log}.
	 * @throws MojoExecutionException
	 *             As per {@link Mojo} API.
	 * @throws MojoFailureException
	 *             As per {@link Mojo} API.
	 */
	public static void ensureDefaultOfficeBuildingAvailable(
			MavenProject project, List<Artifact> pluginDependencies,
			ArtifactRepository localRepository, WagonProvider wagonProvider,
			Log log) throws MojoExecutionException, MojoFailureException {

		// Ensure the OfficeBuilding is available
		if (!OfficeBuildingManager.isOfficeBuildingAvailable(null,
				DEFAULT_OFFICE_BUILDING_PORT.intValue(), getKeyStoreFile(),
				KEY_STORE_PASSWORD, USER_NAME, PASSWORD)) {

			// OfficeBuilding not available, so start it
			StartOfficeBuildingGoal.createStartOfficeBuildingGoal(project,
					pluginDependencies, localRepository, wagonProvider, log)
					.execute();
		}
	}

	/**
	 * Creates the {@link StartOfficeBuildingGoal} with the required parameters.
	 * 
	 * @param project
	 *            {@link MavenProject}.
	 * @param pluginDependencies
	 *            Plug-in dependencies.
	 * @param localRepository
	 *            Local repository.
	 * @param wagonProvider
	 *            {@link WagonProvider}.
	 * @param log
	 *            {@link Log}.
	 * @return {@link StartOfficeBuildingGoal}.
	 */
	public static StartOfficeBuildingGoal createStartOfficeBuildingGoal(
			MavenProject project, List<Artifact> pluginDependencies,
			ArtifactRepository localRepository, WagonProvider wagonProvider,
			Log log) {
		StartOfficeBuildingGoal goal = new StartOfficeBuildingGoal();
		goal.project = project;
		goal.wagonProvider = wagonProvider;
		goal.pluginDependencies = pluginDependencies;
		goal.localRepository = localRepository;
		goal.setLog(log);
		return goal;
	}

	/**
	 * {@link MavenProject}.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 */
	private MavenProject project;

	/**
	 * {@link WagonProvider}.
	 * 
	 * @component
	 */
	private WagonProvider wagonProvider;

	/**
	 * Plug-in dependencies.
	 * 
	 * @parameter expression="${plugin.artifacts}"
	 * @required
	 */
	private List<Artifact> pluginDependencies;

	/**
	 * Local repository.
	 * 
	 * @parameter expression="${localRepository}"
	 * @required
	 */
	private ArtifactRepository localRepository;

	/**
	 * Port to run the {@link OfficeBuilding} on.
	 * 
	 * @parameter
	 */
	private Integer port = DEFAULT_OFFICE_BUILDING_PORT;

	/*
	 * ======================== Mojo ==========================
	 */

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Ensure have configured values
		assertNotNull("Must have project", this.project);
		assertNotNull("Must have plug-in dependencies", this.pluginDependencies);
		assertNotNull("Must have local repository", this.localRepository);
		assertNotNull("Must have wagon provider", this.wagonProvider);
		assertNotNull(
				"Port not configured for the "
						+ OfficeBuilding.class.getSimpleName(), this.port);

		// Obtain the remote repository URLs
		String[] remoteRepositoryURLs;
		try {
			List<String> urls = new LinkedList<String>();
			for (Object object : this.project.getRemoteArtifactRepositories()) {
				ArtifactRepository repository = (ArtifactRepository) object;
				urls.add(repository.getUrl());
			}
			remoteRepositoryURLs = urls.toArray(new String[0]);
		} catch (Throwable ex) {
			throw this.newMojoExecutionException(
					"Failed obtaining Remote Repository URLs", ex);
		}

		// Create the environment properties
		Properties environment = new Properties();
		environment.putAll(this.project.getProperties());
		environment
				.put(LocalRepositoryOfficeFloorCommandParameter.PARAMETER_LOCAL_REPOSITORY,
						this.localRepository.getBasedir());
		environment
				.put(RemoteRepositoryUrlsOfficeFloorCommandParameter.PARAMETER_REMOTE_REPOSITORY_URLS,
						RemoteRepositoryUrlsOfficeFloorCommandParameterImpl
								.transformForParameterValue(remoteRepositoryURLs));

		// Obtain the OfficeBuilding Artifact
		Artifact officeBuildingArtifact = null;
		for (Artifact dependency : this.pluginDependencies) {
			if (OfficeBuildingManager.OFFICE_BUIDLING_ARTIFACT_ID
					.equals(dependency.getArtifactId())) {
				// Found the OfficeBuilding Artifact
				officeBuildingArtifact = dependency;
			}
		}
		if (officeBuildingArtifact == null) {
			// Must have OfficeBuilding Artifact
			throw this
					.newMojoExecutionException(
							"Failed to obtain plug-in dependency "
									+ OfficeBuildingManager.OFFICE_BUIDLING_ARTIFACT_ID,
							null);
		}

		// Obtain the class path for OfficeBuilding
		String classPath = null;
		try {

			// Obtain the repository system
			DefaultServiceLocator locator = new DefaultServiceLocator();
			locator.setServices(WagonProvider.class, this.wagonProvider);
			locator.addService(RepositoryConnectorFactory.class,
					WagonRepositoryConnectorFactory.class);
			RepositorySystem repoSystem = locator
					.getService(RepositorySystem.class);

			// Obtain the repository session
			MavenRepositorySystemSession repoSession = new MavenRepositorySystemSession();
			LocalRepository localRepo = new LocalRepository(
					this.localRepository.getBasedir());
			repoSession.setLocalRepositoryManager(repoSystem
					.newLocalRepositoryManager(localRepo));

			// Create the OfficeBuilding dependency
			Dependency dependency = new Dependency(new DefaultArtifact(
					officeBuildingArtifact.getGroupId(),
					officeBuildingArtifact.getArtifactId(),
					officeBuildingArtifact.getType(),
					officeBuildingArtifact.getVersion()), "compile");

			// Create the Collect Request for OfficeBuilding dependencies
			CollectRequest collectRequest = new CollectRequest();
			collectRequest.setRoot(dependency);
			for (Object object : this.project.getRemoteArtifactRepositories()) {
				ArtifactRepository repository = (ArtifactRepository) object;
				RemoteRepository remoteRepository = new RemoteRepository(
						repository.getId(), repository.getLayout().getId(),
						repository.getUrl());
				collectRequest.addRepository(remoteRepository);
			}

			// Collect the dependencies and generate the Class Path
			DependencyNode node = repoSystem.collectDependencies(repoSession,
					collectRequest).getRoot();
			repoSystem.resolveDependencies(repoSession, node, null);
			PreorderNodeListGenerator generator = new PreorderNodeListGenerator();
			node.accept(generator);
			classPath = generator.getClassPath();

			// Indicate the class path
			this.getLog().debug("OfficeBuilding class path: " + classPath);

		} catch (Exception ex) {
			throw this
					.newMojoExecutionException(
							"Failed obtaining dependencies for launching OfficeBuilding",
							ex);
		}

		// Create the process configuration
		ProcessConfiguration configuration = new ProcessConfiguration();
		configuration.setAdditionalClassPath(classPath);

		// Start the OfficeBuilding
		try {
			OfficeBuildingManager.spawnOfficeBuilding(this.port.intValue(),
					getKeyStoreFile(), KEY_STORE_PASSWORD, USER_NAME, PASSWORD,
					environment, configuration);
		} catch (Throwable ex) {
			// Provide details of the failure
			final String MESSAGE = "Failed starting the "
					+ OfficeBuilding.class.getSimpleName();
			this.getLog().error(
					MESSAGE + ": " + ex.getMessage() + " ["
							+ ex.getClass().getSimpleName() + "]");
			this.getLog().error("DIAGNOSIS INFORMATION:");
			this.getLog().error(
					"   classpath='" + System.getProperty("java.class.path")
							+ "'");
			this.getLog().error("   additional classpath='" + classPath + "'");

			// Propagate the failure
			throw this.newMojoExecutionException(MESSAGE, ex);
		}

		// Log started OfficeBuilding
		this.getLog().info(
				"Started " + OfficeBuilding.class.getSimpleName() + " on port "
						+ this.port.intValue());
	}

}