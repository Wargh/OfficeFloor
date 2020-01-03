package net.officefloor.gef.configurer;

import javafx.application.Application;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import net.officefloor.frame.api.manage.OfficeFloor;
import net.officefloor.gef.bridge.EnvironmentBridge;
import net.officefloor.gef.configurer.internal.AbstractConfigurationBuilder;
import net.officefloor.model.Model;

/**
 * {@link OfficeFloor} configurer that uses JavaFx.
 * 
 * @author Daniel Sagenschneider
 */
public class Configurer<M> extends AbstractConfigurationBuilder<M> {

	/**
	 * Instantiate.
	 * 
	 * @param envBridge {@link EnvironmentBridge}.
	 */
	public Configurer(EnvironmentBridge envBridge) {
		super(envBridge);
	}

	/**
	 * Loads the configuration, loading the parent {@link Pane} into the
	 * {@link Property}.
	 * 
	 * @param model        {@link Model}.
	 * @param nodeProperty {@link Property} to receive the parent {@link Pane}.
	 * @return {@link Configuration}.
	 */
	public Configuration loadConfiguration(M model, Property<Node> nodeProperty) {

		// Create pane for configuration components
		Pane pane = new Pane();
		nodeProperty.setValue(pane);

		// Load the configuration
		return this.loadConfiguration(model, pane);
	}

	/**
	 * Loads the input {@link Pane} with the configuration.
	 * 
	 * @param model  {@link Model}.
	 * @param parent Parent {@link Pane} to contain the configuration.
	 * @return {@link Configuration}.
	 */
	public Configuration loadConfiguration(M model, Pane parent) {
		return super.loadConfiguration(model, parent);
	}

	/**
	 * Initialise application.
	 */
	public static class InitApplication extends Application {

		@Override
		public void start(Stage primaryStage) throws Exception {
		}
	}

}