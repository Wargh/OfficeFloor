/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2018 Daniel Sagenschneider
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
package net.officefloor.eclipse.configurer.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.widgets.Shell;

import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import net.officefloor.eclipse.configurer.ChoiceBuilder;
import net.officefloor.eclipse.configurer.ClassBuilder;
import net.officefloor.eclipse.configurer.ConfigurationBuilder;
import net.officefloor.eclipse.configurer.DefaultImages;
import net.officefloor.eclipse.configurer.ErrorListener;
import net.officefloor.eclipse.configurer.FlagBuilder;
import net.officefloor.eclipse.configurer.ListBuilder;
import net.officefloor.eclipse.configurer.MappingBuilder;
import net.officefloor.eclipse.configurer.MultipleBuilder;
import net.officefloor.eclipse.configurer.PropertiesBuilder;
import net.officefloor.eclipse.configurer.ResourceBuilder;
import net.officefloor.eclipse.configurer.TextBuilder;
import net.officefloor.eclipse.configurer.internal.inputs.ChoiceBuilderImpl;
import net.officefloor.eclipse.configurer.internal.inputs.ClassBuilderImpl;
import net.officefloor.eclipse.configurer.internal.inputs.FlagBuilderImpl;
import net.officefloor.eclipse.configurer.internal.inputs.ListBuilderImpl;
import net.officefloor.eclipse.configurer.internal.inputs.MappingBuilderImpl;
import net.officefloor.eclipse.configurer.internal.inputs.MultipleBuilderImpl;
import net.officefloor.eclipse.configurer.internal.inputs.PropertiesBuilderImpl;
import net.officefloor.eclipse.configurer.internal.inputs.ResourceBuilderImpl;
import net.officefloor.eclipse.configurer.internal.inputs.TextBuilderImpl;

/**
 * Abstract {@link ConfigurationBuilder}.
 * 
 * @author Daniel Sagenschneider
 */
public class AbstractConfigurationBuilder<M> implements ConfigurationBuilder<M> {

	/**
	 * CSS class applied to {@link GridPane} in wide view.
	 */
	public static final String CSS_CLASS_WIDE = "wide";

	/**
	 * CSS class applied to the {@link GridPane} in narrow view.
	 */
	public static final String CSS_CLASS_NARROW = "narrow";

	/**
	 * Listing of the {@link ValueRendererFactory} instances.
	 */
	private final List<ValueRendererFactory<M, ? extends ValueInput>> rendererFactories = new ArrayList<>();

	/**
	 * {@link ErrorListener}.
	 */
	private ErrorListener errorListener = null;

	/**
	 * Overrides the default {@link ErrorListener}.
	 * 
	 * @param errorListener
	 *            {@link ErrorListener}.
	 */
	public void setErrorListener(ErrorListener errorListener) {
		this.errorListener = errorListener;
	}

	/**
	 * Obtain the list of {@link ValueRendererFactory} instances.
	 * 
	 * @return {@link ValueRendererFactory} instances.
	 */
	@SuppressWarnings("unchecked")
	public ValueRendererFactory<M, ? extends ValueInput>[] getValueRendererFactories() {
		return this.rendererFactories.toArray(new ValueRendererFactory[this.rendererFactories.size()]);
	}

	/**
	 * Loads the configuration to the parent {@link Pane}.
	 * 
	 * @param model
	 *            Model.
	 * @param parent
	 *            Parent {@link Pane}.
	 */
	public void loadConfiguration(M model, Pane parent) {

		// Load the default styling
		parent.getScene().getStylesheets().add(this.getClass().getName().replace('.', '/') + ".css");

		// Scroll both narrow and wide views
		ScrollPane scroll = new ScrollPane() {
			@Override
			public void requestFocus() {
				// avoid stealing focus
				// (work around for GEF drag/drop aborting on focus change)
			}
		};
		scroll.prefWidthProperty().bind(parent.widthProperty());
		scroll.prefHeightProperty().bind(parent.heightProperty());
		scroll.setFitToWidth(true);
		scroll.getStyleClass().add("configurer-container");
		parent.getChildren().add(scroll);

		// Provide grid to load configuration
		GridPane grid = new GridPane();
		scroll.setContent(grid);

		// Apply CSS (so Scene available to inputs)
		parent.applyCss();

		// Load the configuration to grid
		this.loadConfiguration(model, grid, this);
	}

	/**
	 * Loads the configuration to the {@link GridPane}.
	 * 
	 * @param model
	 *            Model.
	 * @param parent
	 *            {@link GridPane}.
	 * @param parentConfigurationBuilder
	 *            Parent {@link AbstractConfigurationBuilder}
	 */
	@SuppressWarnings("unchecked")
	public void loadConfiguration(M model, GridPane parent,
			AbstractConfigurationBuilder<?> parentConfigurationBuilder) {

		// Provide error listener
		DefaultErrorListener<M> defaultErrorListener = null;
		if (this.errorListener == null) {
			this.errorListener = parentConfigurationBuilder.errorListener;
		}
		if (this.errorListener == null) {
			// Provide default error listener
			defaultErrorListener = new DefaultErrorListener<>();
			this.errorListener = defaultErrorListener;
		}

		// Apply CSS (so Scene available to inputs)
		parent.applyCss();

		// Create the listing of renderer factories
		ValueRendererFactory<M, ? extends ValueInput>[] rendererFactories;
		ValueRendererFactory<M, ? extends ValueInput>[] configuredRendererFactories = this.getValueRendererFactories();
		if (defaultErrorListener != null) {
			// Include rendering the first error
			rendererFactories = new ValueRendererFactory[configuredRendererFactories.length + 1];
			rendererFactories[0] = defaultErrorListener;
			for (int i = 0; i < configuredRendererFactories.length; i++) {
				rendererFactories[i + 1] = configuredRendererFactories[i];
			}
		} else {
			// Errors handled externally, so only configured factories
			rendererFactories = configuredRendererFactories;
		}

		// Load the value render list
		ValueLister<M> lister = new ValueLister<>(model, parent, this.errorListener, rendererFactories);
		lister.organiseWide(1); // ensure initially organised

		// Responsive view
		final double RESPONSIVE_WIDTH = 800;
		InvalidationListener listener = (event) -> {
			if (parent.getWidth() < RESPONSIVE_WIDTH) {
				// Avoid events if already narrow
				if (lister.isWideNotNarrow) {
					lister.organiseNarrow(1);
				}
			} else {
				// Again avoid events if already wide
				if (!lister.isWideNotNarrow) {
					lister.organiseWide(1);
				}
			}
		};
		parent.widthProperty().addListener(listener);
		listener.invalidated(null); // organise initial view
	}

	/**
	 * Convenience method to register the {@link ValueRendererFactory}.
	 * 
	 * @param builder
	 *            Builder.
	 * @return Input builder.
	 */
	private <B extends ValueRendererFactory<M, ? extends ValueInput>> B registerBuilder(B builder) {
		this.rendererFactories.add(builder);
		return builder;
	}

	/*
	 * ================== ConfigurationBuilder ====================
	 */

	@Override
	public TextBuilder<M> text(String label) {
		return this.registerBuilder(new TextBuilderImpl<>(label));
	}

	@Override
	public FlagBuilder<M> flag(String label) {
		return this.registerBuilder(new FlagBuilderImpl<>(label));
	}

	@Override
	public ChoiceBuilder<M> choices(String label) {
		return this.registerBuilder(new ChoiceBuilderImpl<>(label));
	}

	@Override
	public <I> ListBuilder<M, I> list(String label, Class<I> itemType) {
		return this.registerBuilder(new ListBuilderImpl<>(label));
	}

	@Override
	public <I> MultipleBuilder<M, I> multiple(String label, Class<I> itemType) {
		return this.registerBuilder(new MultipleBuilderImpl<>(label));
	}

	@Override
	public PropertiesBuilder<M> properties(String label) {
		return this.registerBuilder(new PropertiesBuilderImpl<>(label));
	}

	@Override
	public MappingBuilder<M> map(String label, Function<M, ObservableList<String>> getSources,
			Function<M, ObservableList<String>> getTargets) {
		return this.registerBuilder(new MappingBuilderImpl<>(label, getSources, getTargets));
	}

	@Override
	public ClassBuilder<M> clazz(String label, IJavaProject javaProject, Shell shell) {
		return this.registerBuilder(new ClassBuilderImpl<>(label, javaProject, shell));
	}

	@Override
	public ResourceBuilder<M> resource(String label, IJavaProject javaProject, Shell shell) {
		return this.registerBuilder(new ResourceBuilderImpl<>(label, javaProject, shell));
	}

	@Override
	public void apply(Consumer<M> applier) {
		// TODO Auto-generated method stub

	}

	/**
	 * Lists some of the {@link ValueRenderer} instances.
	 */
	private static class ValueLister<M> implements ValueRendererContext<M> {

		/**
		 * Model.
		 */
		private final M model;

		/**
		 * {@link GridPane}.
		 */
		private final GridPane grid;

		/**
		 * {@link ErrorListener}.
		 */
		private final ErrorListener errorListener;

		/**
		 * {@link Input} instances for this {@link ValueLister}.
		 */
		private final List<Input<M, ? extends ValueInput>> inputs = new LinkedList<>();

		/**
		 * Indicates if organised for wide not narrow view.
		 */
		private boolean isWideNotNarrow = false;

		/**
		 * Row index for organising this list.
		 */
		private int rowIndex;

		/**
		 * Next {@link ValueLister}.
		 */
		private ValueLister<M> nextLister = null;

		/**
		 * Instantiate.
		 * 
		 * @param model
		 *            Model.
		 * @param grid
		 *            {@link GridPane}.
		 * @param errorListener
		 *            {@link ErrorListener}.
		 * @param rowIndex
		 *            Row index within wide view {@link GridPane} to continue rendering
		 *            inputs.
		 * @param rendererFactories
		 *            {@link ValueRendererFactory} instances.
		 */
		@SuppressWarnings("unchecked")
		public ValueLister(M model, GridPane grid, ErrorListener errorListener,
				ValueRendererFactory<M, ? extends ValueInput>[] rendererFactories) {
			this.model = model;
			this.grid = grid;
			this.errorListener = errorListener;

			// Ensure activate the inputs (once added)
			List<ValueInput> inputsToActivate = new ArrayList<>(rendererFactories.length * 2);
			try {

				// Render in the items
				for (int i = 0; i < rendererFactories.length; i++) {
					ValueRendererFactory<M, ? extends ValueInput> rendererFactory = rendererFactories[i];

					// Create the renderer
					ValueRenderer<M, ValueInput> renderer = (ValueRenderer<M, ValueInput>) rendererFactory
							.createValueRenderer(this);

					// Obtain the value input
					ValueInput valueInput = renderer.createInput();
					inputsToActivate.add(valueInput);

					// Add the input
					Node valueInputNode = valueInput.getNode();
					if (valueInputNode != null) {
						grid.getChildren().add(valueInputNode);
					}

					// Add the label (if provided)
					String labelText = renderer.getLabel(valueInput);
					Node label = null;
					if ((labelText == null) || (labelText.trim().length() == 0)) {
						labelText = null; // no label
					} else {
						// Create the label
						label = renderer.createLabel(labelText, valueInput);
						if (label != null) {
							grid.getChildren().add(label);
						}
					}

					// Add the error feedback (if provided)
					Node error = renderer.createErrorFeedback(valueInput);
					if (error != null) {
						grid.getChildren().add(error);
					}

					// Register the input
					Input<M, ? extends ValueInput> input = new Input<>(labelText, label, error, valueInputNode,
							valueInput, renderer);
					this.inputs.add(input);
					grid.getChildren().add(input.spacing);

					// Determine if choice value renderer
					if (valueInput instanceof ChoiceValueInput) {
						ChoiceValueInput<M> choiceRenderer = (ChoiceValueInput<M>) valueInput;

						// Load choice
						ValueRendererFactory<M, ? extends ValueInput>[] splitRenderers = Arrays
								.copyOfRange(rendererFactories, i + 1, rendererFactories.length);
						Runnable loadChoice = () -> {

							// Obtain the choice
							Integer choice = choiceRenderer.getChoiceIndex().getValue();
							if (choice == null) {
								// No choice selected, so carry on with renderers
								this.nextLister = new ValueLister<>(this.model, grid, this.errorListener,
										splitRenderers);

							} else {
								// Have choice, so create concat list of remaining
								ValueRendererFactory<M, ? extends ValueInput>[] choiceRenderers = choiceRenderer
										.getChoiceValueRendererFactories()[choice].get();
								ValueRendererFactory<M, ? extends ValueInput>[] remainingRenderers = new ValueRendererFactory[choiceRenderers.length
										+ splitRenderers.length];
								for (int c = 0; c < choiceRenderers.length; c++) {
									remainingRenderers[c] = choiceRenderers[c];
								}
								for (int s = 0; s < splitRenderers.length; s++) {
									remainingRenderers[choiceRenderers.length + s] = splitRenderers[s];
								}
								this.nextLister = new ValueLister<>(this.model, grid, this.errorListener,
										remainingRenderers);
							}

							// Organise choice changes
							if (this.isWideNotNarrow) {
								this.organiseWide(this.rowIndex);
							} else {
								this.organiseNarrow(this.rowIndex);
							}
						};

						// Listen for changes in choice
						choiceRenderer.getChoiceIndex().addListener((event) -> {

							// Clear next listing (as change in choice)
							if (this.nextLister != null) {
								this.nextLister.removeControls();
								this.nextLister = null;
							}

							// Load new choice
							loadChoice.run();
						});

						// Load current choice
						loadChoice.run();

						// Stop loading renderers (as next listing will render)
						return;
					}
				}

			} finally {
				// Activate the inputs
				for (ValueInput input : inputsToActivate) {
					input.activate();
				}
			}
		}

		/**
		 * Organises for wide view.
		 * 
		 * @param rowIndex
		 *            Row index to start organising inputs.
		 */
		private void organiseWide(int rowIndex) {
			this.rowIndex = rowIndex;

			// Organise the inputs
			for (Input<M, ? extends ValueInput> input : this.inputs) {
				if (input.label != null) {
					GridPane.setConstraints(input.label, 1, rowIndex, 2, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS,
							Priority.ALWAYS);
				}
				if (input.errorFeedback != null) {
					GridPane.setConstraints(input.errorFeedback, 3, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER,
							Priority.SOMETIMES, Priority.ALWAYS);
				}
				if (input.input != null) {
					GridPane.setConstraints(input.input, 4, rowIndex, 1, 1, HPos.LEFT, VPos.TOP, Priority.ALWAYS,
							Priority.ALWAYS);
				}
				GridPane.setConstraints(input.spacing, 5, rowIndex, 1, 1, HPos.LEFT, VPos.TOP, Priority.NEVER,
						Priority.NEVER);

				// Increment for next row
				rowIndex += 2;
			}

			// Ensure next lister is also organised
			if (this.nextLister != null) {
				this.nextLister.organiseWide(rowIndex);
			}

			// Indicate now wide view
			this.grid.getStyleClass().remove(CSS_CLASS_NARROW);
			this.grid.getStyleClass().add(CSS_CLASS_WIDE);
			this.isWideNotNarrow = true;
		}

		/**
		 * Organises for narrow view.
		 * 
		 * @param rowIndex
		 *            Row index to start organising inputs.
		 */
		private void organiseNarrow(int rowIndex) {
			this.rowIndex = rowIndex;

			// Organise the inputs
			for (Input<M, ? extends ValueInput> input : this.inputs) {
				if (input.label != null) {
					GridPane.setConstraints(input.label, 1, rowIndex, 1, 1, HPos.LEFT, VPos.TOP, Priority.ALWAYS,
							Priority.ALWAYS);
				}
				if ((input.errorFeedback != null) && (input.input != null)) {
					rowIndex++;
					if (input.errorFeedback != null) {
						GridPane.setConstraints(input.errorFeedback, 0, rowIndex, 1, 1, HPos.RIGHT, VPos.CENTER,
								Priority.SOMETIMES, Priority.ALWAYS);
					}
					if (input.input != null) {
						// Provide input on next row
						GridPane.setConstraints(input.input, 1, rowIndex, 2, 1, HPos.LEFT, VPos.TOP, Priority.ALWAYS,
								Priority.ALWAYS);
					}
				}

				// Increment for next row
				rowIndex++;
				GridPane.setConstraints(input.spacing, 1, rowIndex, 2, 1, HPos.LEFT, VPos.TOP, Priority.NEVER,
						Priority.ALWAYS);
				rowIndex++;
			}

			// Ensure next lister is also organised
			if (this.nextLister != null) {
				this.nextLister.organiseNarrow(rowIndex);
			}

			// Indicate now narrow view
			this.grid.getStyleClass().remove(CSS_CLASS_WIDE);
			this.grid.getStyleClass().add(CSS_CLASS_NARROW);
			this.isWideNotNarrow = false;
		}

		/**
		 * Removes the controls from view.
		 */
		private void removeControls() {
			for (Input<M, ? extends ValueInput> input : this.inputs) {
				if (input.label != null) {
					this.grid.getChildren().remove(input.label);
				}
				if (input.errorFeedback != null) {
					this.grid.getChildren().remove(input.errorFeedback);
				}
				if (input.input != null) {
					this.grid.getChildren().remove(input.input);
				}
				this.grid.getChildren().remove(input.spacing);
			}
		}

		/*
		 * ================== ValueRendererContext ====================
		 */

		@Override
		public M getModel() {
			return this.model;
		}

		@Override
		public void refreshError() {

			// Search for first error
			InputError<M, ? extends ValueInput> errorInput = this.getFirstError();

			// Determine if error
			if (errorInput == null) {
				this.errorListener.valid();
				return; // no error
			}

			// Provide appropriate error
			if (errorInput.error instanceof MessageOnlyException) {
				this.errorListener.error(errorInput.input.labelText, errorInput.error.getMessage());
			} else {
				this.errorListener.error(errorInput.input.labelText, errorInput.error);
			}
		}

		/**
		 * Obtains the first {@link ValueRenderer} error.
		 * 
		 * @return First {@link ValueRenderer} error or <code>null</code>
		 */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		private InputError<M, ? extends ValueInput> getFirstError() {

			// Search through for the first error
			for (Input input : this.inputs) {

				// Determine if first error
				Throwable error = input.renderer.getError(input.valueInput);
				if (error != null) {
					return new InputError<>(input, error);
				}
			}

			// No error, so determine error in next list
			return (this.nextLister != null) ? this.nextLister.getFirstError() : null;
		}
	}

	/**
	 * Input.
	 */
	private static class Input<M, I extends ValueInput> {

		/**
		 * Label text for the input.
		 */
		private final String labelText;

		/**
		 * Label for the input.
		 */
		private final Node label;

		/**
		 * Error feedback for the input.
		 */
		private final Node errorFeedback;

		/**
		 * {@link Node} to capture the input.
		 */
		private final Node input;

		/**
		 * Used for spacing.
		 */
		private final Pane spacing = new Pane();

		/**
		 * {@link ValueInput}.
		 */
		private final I valueInput;

		/**
		 * {@link ValueRenderer}. As bindings are weak references, need strong reference
		 * to {@link ValueRenderer} to keep bindings active.
		 */
		private final ValueRenderer<M, I> renderer;

		/**
		 * Instantiate.
		 * 
		 * @param labelText
		 *            Label text for the input.
		 * @param label
		 *            Label for the input.
		 * @param errorFeedback
		 *            Error feedback for the input.
		 * @param input
		 *            {@link Node} to capture the input.
		 * @param valueInput
		 *            {@link ValueInput}.
		 * @param renderer
		 *            {@link ValueRenderer}.
		 */
		private Input(String labelText, Node label, Node errorFeedback, Node input, I valueInput,
				ValueRenderer<M, I> renderer) {
			this.labelText = labelText;
			this.label = label;
			this.errorFeedback = errorFeedback;
			this.input = input;
			this.valueInput = valueInput;
			this.renderer = renderer;
		}
	}

	/**
	 * Input error.
	 */
	private static class InputError<M, I extends ValueInput> {

		/**
		 * {@link Input}.
		 */
		private final Input<M, I> input;

		/**
		 * {@link Throwable} error.
		 */
		private final Throwable error;

		/**
		 * Instantiate.
		 * 
		 * @param input
		 *            {@link Input}.
		 * @param error
		 *            {@link Throwable} error.
		 */
		private InputError(Input<M, I> input, Throwable error) {
			this.input = input;
			this.error = error;
		}
	}

	/**
	 * Default {@link ErrorListener}.
	 */
	private static class DefaultErrorListener<M> implements ErrorListener, ValueInput,
			ValueRenderer<M, DefaultErrorListener<M>>, ValueRendererFactory<M, DefaultErrorListener<M>> {

		/**
		 * Error {@link ImageView};
		 */
		private final ImageView errorImage;

		/**
		 * Error {@link Tooltip}.
		 */
		private final Tooltip errorTooltip;

		/**
		 * Error {@link Label}.
		 */
		private final Label errorInput;

		/**
		 * Instantiate.
		 */
		private DefaultErrorListener() {
			this.errorInput = new Label();
			this.errorInput.setVisible(false);

			// Provide the error image
			this.errorImage = new ImageView(new Image(DefaultImages.ERROR_IMAGE_PATH, 15, 15, true, true));
			this.errorTooltip = new Tooltip();
			errorTooltip.getStyleClass().add("error-tooltip");
			Tooltip.install(this.errorImage, this.errorTooltip);
			this.errorImage.setVisible(false);
		}

		/*
		 * ============= ErrorListener ====================
		 */

		@Override
		public void error(String inputLabel, String message) {
			this.errorInput.setText((inputLabel == null ? "" : inputLabel + ": ") + message);
			this.errorInput.setVisible(true);
			this.errorTooltip.setText(message);
			this.errorImage.setVisible(true);
		}

		@Override
		public void error(String inputLabel, Throwable error) {
			this.errorInput.setText((inputLabel == null ? "" : inputLabel + ": ") + error.getMessage());
			this.errorInput.setVisible(true);

			// Provide stack trace on tool tip
			StringWriter buffer = new StringWriter();
			error.printStackTrace(new PrintWriter(buffer));
			this.errorTooltip.setText(buffer.toString());
			this.errorImage.setVisible(true);
		}

		@Override
		public void valid() {
			this.errorInput.setVisible(false);
			this.errorImage.setVisible(false);
		}

		/*
		 * ============= ValueRendererFactory ===========
		 */

		@Override
		public ValueRenderer<M, DefaultErrorListener<M>> createValueRenderer(ValueRendererContext<M> context) {
			return this;
		}

		/*
		 * ============= ValueInput =====================
		 */

		@Override
		public Node getNode() {
			return this.errorInput;
		}

		/*
		 * ============= ValueRenderer =====================
		 */

		@Override
		public DefaultErrorListener<M> createInput() {
			return this;
		}

		@Override
		public String getLabel(DefaultErrorListener<M> valueInput) {
			return null;
		}

		@Override
		public Node createLabel(String labelText, DefaultErrorListener<M> valueInput) {
			return null;
		}

		@Override
		public Node createErrorFeedback(DefaultErrorListener<M> valueInput) {
			return this.errorImage;
		}

		@Override
		public Throwable getError(DefaultErrorListener<M> valueInput) {
			return null; // never error
		}
	}

}