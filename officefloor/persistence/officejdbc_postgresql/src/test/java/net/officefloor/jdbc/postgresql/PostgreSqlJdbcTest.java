/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2018 Daniel Sagenschneider
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
package net.officefloor.jdbc.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.postgresql.ds.PGSimpleDataSource;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import com.zaxxer.hikari.HikariDataSource;

import net.officefloor.compile.properties.PropertyConfigurable;
import net.officefloor.jdbc.ConnectionManagedObjectSource;
import net.officefloor.jdbc.ReadOnlyConnectionManagedObjectSource;
import net.officefloor.jdbc.datasource.DefaultDataSourceFactory;
import net.officefloor.jdbc.test.AbstractJdbcTestCase;

/**
 * Tests the {@link PostgreSqlConnectionManagedObjectSource}.
 * 
 * @author Daniel Sagenschneider
 */
public class PostgreSqlJdbcTest extends AbstractJdbcTestCase {

	/**
	 * Pulled docker images.
	 */
	private static final Set<String> pulledDockerImages = new HashSet<>();

	/**
	 * Pulls the docker image.
	 * 
	 * @param imageName Docker image name.
	 * @param client    {@link DockerClient}.
	 */
	public static void pullDockerImage(String imageName, DockerClient client) throws Exception {

		// Determine if already pulled
		if (pulledDockerImages.contains(imageName)) {
			return;
		}

		// Pull the docker image
		Consumer<String> print = (message) -> {
			System.out.print(message == null ? "" : " " + message);
			System.out.flush();
		};
		client.pull(imageName, (message) -> {
			print.accept(message.progress());
			print.accept(message.status());
			print.accept(message.id());
			System.out.println();
		});

		// Flag that pulled image
		pulledDockerImages.add(imageName);
	}

	private DockerClient docker;

	private String postgresContainerId;

	@Override
	protected void setUp() throws Exception {

		final String IMAGE_NAME = "postgres:latest";
		final String CONTAINER_NAME = "officefloor_postgres";

		// Create the docker client
		this.docker = DefaultDockerClient.fromEnv().build();

		// Determine if container already running
		for (Container container : this.docker.listContainers()) {
			for (String name : container.names()) {
				if (name.equals("/" + CONTAINER_NAME)) {
					this.postgresContainerId = container.id();
				}
			}
		}

		// Start PostgreSQL (if not running)
		if (this.postgresContainerId == null) {
			System.out.println();
			System.out.println("Starting PostgreSQL");
			pullDockerImage(IMAGE_NAME, this.docker);

			// Bind container port to host port
			final String[] ports = { "5432" };
			final Map<String, List<PortBinding>> portBindings = new HashMap<>();
			for (String port : ports) {
				portBindings.put(port, Arrays.asList(PortBinding.of("0.0.0.0", port)));
			}
			final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();
			final ContainerConfig containerConfig = ContainerConfig.builder().hostConfig(hostConfig).image(IMAGE_NAME)
					.exposedPorts(ports).env("POSTGRES_USER=testuser", "POSTGRES_PASSWORD=testpassword").build();

			// Start the container
			final ContainerCreation creation = docker.createContainer(containerConfig, CONTAINER_NAME);
			this.postgresContainerId = creation.id();
			this.docker.startContainer(this.postgresContainerId);
		}

		// Run setup (now database available to connect)
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {

		// Stop PostgresSQL
		System.out.println("Stopping PostgreSQL");
		this.docker.killContainer(this.postgresContainerId);
		this.docker.removeContainer(this.postgresContainerId);
		this.docker.close();

		// Complete tear down
		super.tearDown();
	}

	@Override
	protected Class<? extends ConnectionManagedObjectSource> getConnectionManagedObjectSourceClass() {
		return PostgreSqlConnectionManagedObjectSource.class;
	}

	@Override
	protected Class<? extends ReadOnlyConnectionManagedObjectSource> getReadOnlyConnectionManagedObjectSourceClass() {
		return PostgreSqlReadOnlyConnectionManagedObjectSource.class;
	}

	@Override
	protected void loadConnectionProperties(PropertyConfigurable mos) {
		mos.addProperty(PostgreSqlConnectionManagedObjectSource.PROPERTY_SERVER_NAME, "localhost");
		mos.addProperty(PostgreSqlConnectionManagedObjectSource.PROPERTY_PORT, "5432");
		mos.addProperty(PostgreSqlConnectionManagedObjectSource.PROPERTY_USER, "testuser");
		mos.addProperty(PostgreSqlConnectionManagedObjectSource.PROPERTY_PASSWORD, "testpassword");
	}

	@Override
	protected void loadOptionalConnectionSpecification(Properties properties) {
		properties.setProperty(PostgreSqlConnectionManagedObjectSource.PROPERTY_PORT, "5432");
	}

	@Override
	protected void loadDataSourceProperties(PropertyConfigurable mos) {
		mos.addProperty(DefaultDataSourceFactory.PROPERTY_DATA_SOURCE_CLASS_NAME, HikariDataSource.class.getName());
		mos.addProperty("dataSourceClassName", PGSimpleDataSource.class.getName());
		mos.addProperty("username", "testuser");
		mos.addProperty("password", "testpassword");
	}

	@Override
	protected void cleanDatabase(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeQuery("SELECT * FROM information_schema.tables");
			statement.executeUpdate("DROP TABLE IF EXISTS OFFICE_FLOOR_JDBC_TEST");
		}
	}

}