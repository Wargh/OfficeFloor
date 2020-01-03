package net.officefloor.gef.configurer.internal.inputs;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.util.Callback;
import net.officefloor.gef.configurer.FlagBuilder;
import net.officefloor.gef.configurer.TextBuilder;
import net.officefloor.gef.configurer.internal.AbstractBuilder;
import net.officefloor.gef.configurer.internal.ValueInput;
import net.officefloor.gef.configurer.internal.ValueInputContext;

/**
 * {@link TextBuilder} implementation.
 * 
 * @author Daniel Sagenschneider
 */
public class FlagBuilderImpl<M> extends AbstractBuilder<M, Boolean, ValueInput, FlagBuilder<M>>
		implements FlagBuilder<M> {

	/**
	 * Instantiate.
	 * 
	 * @param label
	 *            Label.
	 */
	public FlagBuilderImpl(String label) {
		super(label);
	}

	/*
	 * ============= AbstractBuilder ==============
	 */

	@Override
	protected ValueInput createInput(ValueInputContext<M, Boolean> context) {
		CheckBox checkBox = new CheckBox();
		if (this.isEditable()) {
			checkBox.selectedProperty().bindBidirectional(context.getInputValue());
		} else {
			checkBox.setDisable(true);
			checkBox.selectedProperty().bind(context.getInputValue());
		}
		checkBox.getStyleClass().add("configurer-input-checkbox");
		return () -> checkBox;
	}

	@Override
	protected Property<Boolean> createCellProperty() {
		return new SimpleBooleanProperty();
	}

	@Override
	protected <R> void configureTableColumn(TableView<R> table, TableColumn<R, Boolean> column,
			Callback<Integer, ObservableValue<Boolean>> callback) {
		column.setCellFactory((tc) -> new CheckBoxTableCell<R, Boolean>(callback, null) {

			@Override
			public void updateItem(Boolean item, boolean empty) {
				super.updateItem(item, empty);

				// Handle add row
				if (ListBuilderImpl.isUpdateItemAddRow(this)) {
					return;
				}
			}
		});
	}

}