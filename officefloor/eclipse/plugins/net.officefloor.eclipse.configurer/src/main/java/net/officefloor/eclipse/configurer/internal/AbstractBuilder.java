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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import net.officefloor.eclipse.configurer.Actioner;
import net.officefloor.eclipse.configurer.Builder;
import net.officefloor.eclipse.configurer.DefaultImages;
import net.officefloor.eclipse.configurer.ErrorListener;
import net.officefloor.eclipse.configurer.ValueLoader;
import net.officefloor.eclipse.configurer.ValueValidator;
import net.officefloor.eclipse.configurer.ValueValidator.ValueValidatorContext;

/**
 * Abstract {@link ValueRenderer}.
 * 
 * @author Daniel Sagenschneider
 */
public abstract class AbstractBuilder<M, V, I extends ValueInput, B extends Builder<M, V, B>>
		implements Builder<M, V, B>, ValueRendererFactory<M, I>, ColumnRenderer<M, V> {

	/**
	 * Label.
	 */
	private final String label;

	/**
	 * {@link ValueLoader}.
	 */
	private ValueLoader<M, V> valueLoader = null;

	/**
	 * {@link Function} to obtain the initial value from the model.
	 */
	private Function<M, V> getInitialValue = null;

	/**
	 * {@link ValueValidator}.
	 */
	private ValueValidator<V> validator = null;

	/**
	 * Instantiate.
	 * 
	 * @param label
	 *            Label.
	 */
	public AbstractBuilder(String label) {
		this.label = label;
	}

	/**
	 * Creates the input {@link ValueInput} for the {@link ObservableValue}.
	 * 
	 * @param context
	 *            {@link ValueInputContext}.
	 * @return {@link ValueInput} to configure the {@link ObservableValue}.
	 */
	protected abstract I createInput(ValueInputContext<M, V> context);

	/**
	 * Creates the label {@link Node}.
	 * 
	 * @param labelText
	 *            Label text.
	 * @param valueInput
	 *            {@link ValueInput}.
	 * @return Label {@link Node}.
	 */
	protected Node createLabel(String labelText, I valueInput) {
		return new Label(this.getLabel());
	}

	/**
	 * Creates the error feedback {@link Node}.
	 * 
	 * @param valueInput
	 *            {@link ValueInput}.
	 * @param errorProperty
	 *            Error {@link Property}.
	 * @return Error feedback {@link Node}.
	 */
	protected Node createErrorFeedback(I valueInput, Property<Throwable> errorProperty) {
		ImageView error = new ImageView(new Image(DefaultImages.ERROR_IMAGE_PATH, 15, 15, true, true));
		Tooltip errorTooltip = new Tooltip();
		errorTooltip.getStyleClass().add("error-tooltip");
		Tooltip.install(error, errorTooltip);
		InvalidationListener listener = (observableError) -> {
			Throwable cause = errorProperty.getValue();
			if (cause != null) {
				// Display the error
				errorTooltip.setText(cause.getMessage());
				error.visibleProperty().set(true);
			} else {
				// No error
				error.visibleProperty().set(false);
			}
		};
		errorProperty.addListener(listener);
		listener.invalidated(errorProperty); // Initialise
		return error;
	}

	/**
	 * Obtains the error.
	 * 
	 * @param valueInput
	 *            {@link ValueInput}.
	 * @param error
	 *            {@link Throwable} error. May be <code>null</code> if no error.
	 * @return {@link Throwable} error or <code>null</code> if no error.
	 */
	protected Throwable getError(I valueInput, ReadOnlyProperty<Throwable> error) {
		return error.getValue();
	}

	/**
	 * Creates the {@link Property} for the {@link TableCell}.
	 * 
	 * @return {@link Property} for the {@link TableCell}.
	 */
	protected Property<V> createCellProperty() {
		return new SimpleObjectProperty<>();
	}

	/**
	 * Allow overriding to configure the {@link TableColumn}.
	 * 
	 * @param table
	 *            {@link TableView} that will contain the {@link TableColumn}.
	 * @param column
	 *            {@link TableColumn}.
	 * @param callback
	 *            {@link Callback}.
	 */
	protected <R> void configureTableColumn(TableView<R> table, TableColumn<R, V> column,
			Callback<Integer, ObservableValue<V>> callback) {
	}

	/**
	 * Obtains the label.
	 * 
	 * @return Label.
	 */
	protected String getLabel() {
		return this.label == null ? "" : this.label;
	}

	/*
	 * ============ Builder ===============
	 */

	@Override
	@SuppressWarnings("unchecked")
	public B init(Function<M, V> getInitialValue) {
		this.getInitialValue = getInitialValue;
		return (B) this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public B validate(ValueValidator<V> validator) {
		this.validator = validator;
		return (B) this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public B setValue(ValueLoader<M, V> valueLoader) {
		this.valueLoader = valueLoader;
		return (B) this;
	}

	/*
	 * ============ ValueRendererFactory =================
	 */

	@Override
	public ValueRenderer<M, I> createValueRenderer(ValueRendererContext<M> context) {
		return new ValueRendererImpl(context);
	}

	/**
	 * {@link ValueRenderer} implementation.
	 */
	private class ValueRendererImpl implements ValueRenderer<M, I>, ValueValidatorContext<V>, ValueInputContext<M, V> {

		/**
		 * {@link ObservableValue}.
		 */
		private final Property<V> value = new SimpleObjectProperty<>();

		/**
		 * {@link ValueRendererContext}.
		 */
		private final ValueRendererContext<M> context;

		/**
		 * {@link ValueValidator} instances.
		 */
		private final List<ValueValidator<V>> validators = new ArrayList<>(1);

		/**
		 * Tracks if an error occurred on validation.
		 */
		private boolean isError = false;

		/**
		 * Error.
		 */
		private final Property<Throwable> error = new SimpleObjectProperty<>();

		/**
		 * Initialises the {@link ValueRenderer}.
		 */
		private ValueRendererImpl(ValueRendererContext<M> context) {
			this.context = context;

			// Obtain the model
			M model = this.context.getModel();

			// Refresh on error or value change
			this.error.addListener((event) -> this.context.refreshError());
			this.value.addListener((event) -> this.context.refreshError());

			// Load value to model (so model consistent before validation)
			if (AbstractBuilder.this.valueLoader != null) {
				this.value.addListener(
						(event) -> AbstractBuilder.this.valueLoader.loadValue(model, this.value.getValue()));
			}

			// Listen to change to run validation
			if (AbstractBuilder.this.validator != null) {
				this.validators.add(AbstractBuilder.this.validator);
			}
			this.value.addListener((event) -> this.validate());

			// Initialise to model (triggering validation)
			if (AbstractBuilder.this.getInitialValue != null) {
				V initialValue = AbstractBuilder.this.getInitialValue.apply(model);
				this.value.setValue(initialValue);
			}

			// Track model becoming dirty
			this.value.addListener((event) -> context.dirtyProperty().setValue(true));
		}

		/**
		 * Undertakes validation.
		 */
		private void validate() {

			// Track whether error in validation
			this.isError = false;
			try {
				// Undertake validation
				Iterator<ValueValidator<V>> iterator = this.validators.iterator();
				while ((!this.isError) && (iterator.hasNext())) {
					ValueValidator<V> validator = iterator.next();
					validator.validate(this);
				}
			} catch (Exception ex) {
				// Provide error message
				String message = ex.getMessage();
				if ((message == null) || (message.length() == 0)) {
					// Provide message from exception
					ex = new Exception("Thrown exception " + ex.getClass().getName(), ex);
				}

				// Flag error and override with exception
				this.isError = true;
				this.error.setValue(ex);
			}
			if (!this.isError) {
				// No error in validate, so clear error
				this.setError(null);
			}
		}

		/*
		 * ============ ValueInputContext ================
		 */

		@Override
		public M getModel() {
			return this.context.getModel();
		}

		@Override
		public Property<V> getInputValue() {
			return this.value;
		}

		@Override
		public void addValidator(ValueValidator<V> validator) {
			this.validators.add(validator);

			// Run validation immediately
			this.validate();
		}

		@Override
		public void refreshError() {
			this.context.refreshError();
		}

		@Override
		public Actioner getOptionalActioner() {
			return this.context.getOptionalActioner();
		}

		@Override
		public Property<Boolean> dirtyProperty() {
			return this.context.dirtyProperty();
		}

		@Override
		public Property<Boolean> validProperty() {
			return this.context.validProperty();
		}

		@Override
		public ErrorListener getErrorListener() {
			return this.context.getErrorListener();
		}

		/*
		 * =============== ValueRenderer =================
		 */

		@Override
		public I createInput() {
			return AbstractBuilder.this.createInput(this);
		}

		@Override
		public String getLabel(I valueInput) {
			return AbstractBuilder.this.getLabel();
		}

		@Override
		public Node createLabel(String labelText, I valueInput) {
			return AbstractBuilder.this.createLabel(labelText, valueInput);
		}

		@Override
		public Node createErrorFeedback(I valueInput) {
			return AbstractBuilder.this.createErrorFeedback(valueInput, this.error);
		}

		@Override
		public Throwable getError(I valueInput) {
			return AbstractBuilder.this.getError(valueInput, this.error);
		}

		/*
		 * ========== ValueValidatorContext =============
		 */

		@Override
		public ReadOnlyProperty<V> getValue() {
			return this.value;
		}

		@Override
		public void setError(String message) {

			// Blank string considered no exception
			if ((message != null) && (message.length() == 0)) {
				message = null;
			}

			// Update the error feedback
			this.isError = (message != null);
			this.error.setValue(this.isError ? new MessageOnlyException(message) : null);
		}
	}

	/*
	 * ============ ColumnRenderer =================
	 */

	@Override
	public <R> TableColumn<R, V> createTableColumn(TableView<R> table, Callback<Integer, ObservableValue<V>> callback) {
		TableColumn<R, V> column = new TableColumn<>(this.label);
		this.configureTableColumn(table, column, callback);
		return column;
	}

	@Override
	public boolean isEditable() {
		return (this.valueLoader != null);
	}

	@Override
	public CellRenderer<M, V> createCellRenderer(ValueRendererContext<M> context) {
		return new CellRendererImpl(context);
	}

	/**
	 * {@link CellRenderer} implementation.
	 */
	private class CellRendererImpl implements CellRenderer<M, V> {

		/**
		 * {@link ObservableValue}.
		 */
		private final Property<V> value;

		/**
		 * {@link ValueRendererContext}.
		 */
		private final ValueRendererContext<M> context;

		/**
		 * Instantiate.
		 * 
		 * @param context
		 *            {@link ValueRendererContext}.
		 */
		private CellRendererImpl(ValueRendererContext<M> context) {
			this.context = context;

			// Obtain the cell value property
			this.value = AbstractBuilder.this.createCellProperty();

			// Load initial value
			if ((AbstractBuilder.this.getInitialValue != null) && (this.context.getModel() != null)) {
				V value = AbstractBuilder.this.getInitialValue.apply(this.context.getModel());
				this.value.setValue(value);
			}

			// Always load value to model
			if (AbstractBuilder.this.valueLoader != null) {
				this.value.addListener((event) -> AbstractBuilder.this.valueLoader.loadValue(this.context.getModel(),
						this.value.getValue()));
			}
		}

		/*
		 * ============ CellRenderer ====================
		 */

		@Override
		public Property<V> getValue() {
			return this.value;
		}
	}

}