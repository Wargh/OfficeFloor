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
package net.officefloor.eclipse.configurer.internal.inputs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import net.officefloor.eclipse.configurer.DefaultImages;
import net.officefloor.eclipse.configurer.FlagBuilder;
import net.officefloor.eclipse.configurer.ListBuilder;
import net.officefloor.eclipse.configurer.TextBuilder;
import net.officefloor.eclipse.configurer.internal.AbstractBuilder;
import net.officefloor.eclipse.configurer.internal.CellRenderer;
import net.officefloor.eclipse.configurer.internal.ColumnRenderer;
import net.officefloor.eclipse.configurer.internal.ValueInput;
import net.officefloor.eclipse.configurer.internal.ValueInputContext;
import net.officefloor.eclipse.configurer.internal.ValueRendererContext;
import net.officefloor.eclipse.configurer.internal.resize.DragResizer;

/**
 * {@link ListBuilder} implementation.
 * 
 * @author Daniel Sagenschneider
 */
public class ListBuilderImpl<M, I> extends AbstractBuilder<M, List<I>, ValueInput, ListBuilder<M, I>>
		implements ListBuilder<M, I> {

	/**
	 * Indicates if add row (configuring {@link TableCell} for add row).
	 * 
	 * @param tableCell
	 *            {@link TableCell}.
	 * @return <code>true</code> if add row.
	 */
	public static <R> boolean isUpdateItemAddRow(TableCell<R, ?> tableCell) {

		// Determine if the add row
		TableRow<?> tableRow = tableCell.getTableRow();
		if (tableRow != null) {
			Object item = tableRow.getItem();
			if (item != null) {
				if (item instanceof ListBuilderImpl.AddRow) {
					ListBuilderImpl<?, ?>.AddRow addRow = (ListBuilderImpl<?, ?>.AddRow) item;

					// First column to contain add icon
					int columnIndex = tableCell.getTableView().getColumns().indexOf(tableCell.getTableColumn());
					switch (columnIndex) {
					case 0:
						tableCell
								.setGraphic(new ImageView(new Image(DefaultImages.ADD_IMAGE_PATH, 15, 15, true, true)));
						tableCell.alignmentProperty().set(Pos.CENTER_LEFT);

						// Click anywhere on cell to add row
						tableCell.setOnMouseClicked((event) -> addRow.addRow());
						break;
					default:
						tableCell.setGraphic(null);
						break;
					}

					// Add row
					tableCell.setText(null);
					return true;
				}
			}
		}

		// Clear the on click handling
		tableCell.setOnMouseClicked((event) -> {
		});

		// As here not an add row
		return false;
	}

	/**
	 * {@link ColumnRenderer} instances.
	 */
	private final List<ColumnRenderer<I, ?>> renderers = new ArrayList<>();

	/**
	 * {@link Supplier} for new items (rows).
	 */
	private Supplier<I> itemFactory = null;

	/**
	 * Indicates whether can delete.
	 */
	private boolean isDelete = false;

	/**
	 * Instantiate.
	 * 
	 * @param label
	 *            Label.
	 */
	public ListBuilderImpl(String label) {
		super(label);
	}

	/**
	 * Convenience method to register the {@link ColumnRenderer}.
	 * 
	 * @param builder
	 *            Builder.
	 * @return Input builder.
	 */
	private <V, B extends ColumnRenderer<I, V>> B registerBuilder(B builder) {
		this.renderers.add(builder);
		return builder;
	}

	/*
	 * ============== AbstractBuilder ===============
	 */

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ValueInput createInput(ValueInputContext<M, List<I>> context) {

		// Obtain the items
		final Property<List<I>> itemsProperty = context.getInputValue();

		// Create the rows for the table
		final ObservableList<Row> rows = FXCollections.observableArrayList();

		// Update the items
		Runnable loadRowsToItems = () -> {
			List<I> updatedItems = new ArrayList<>(rows.size());
			for (Row row : rows) {
				if (row.item != null) {
					updatedItems.add(row.item);
				}
			}
			itemsProperty.setValue(updatedItems);
		};

		// Create the table
		final TableView<Row> table = new TableView<>();
		table.setEditable(true);
		table.columnResizePolicyProperty().set(TableView.CONSTRAINED_RESIZE_POLICY);
		table.setPlaceholder(new Label("No entries"));
		table.getSelectionModel().setCellSelectionEnabled(true);

		// Load the columns to the table
		final TableColumn<Row, ?>[] columns = new TableColumn[this.renderers.size() + (this.isDelete ? 1 : 0)];
		for (int i = 0; i < this.renderers.size(); i++) {
			ColumnRenderer<I, ?> columnRenderer = this.renderers.get(i);

			// Add the column
			int columnIndex = i;
			TableColumn<Row, ?> column = columnRenderer.createTableColumn(table, (index) -> {
				Row row = table.getItems().get(index);
				return (Property) row.cells[columnIndex].getValue();
			});
			columns[i] = column;

			// Load the cell factory
			column.setCellValueFactory((row) -> {
				Property cellValue = row.getValue().cells[columnIndex].getValue();
				return cellValue;
			});
			if (columnRenderer.isEditable()) {
				// Column editable
				column.setOnEditCommit((event) -> {

					// Load the value into the cell
					Row row = table.getItems().get(event.getTablePosition().getRow());
					Property cellValue = row.cells[columnIndex].getValue();
					cellValue.setValue(event.getNewValue());

					// Update the items
					loadRowsToItems.run();
				});
				column.getStyleClass().add("configurer-column-editable");

			} else {
				// Column not editable
				column.setEditable(false);
				column.getStyleClass().add("configurer-column-read-only");
			}
		}

		// Determine if able to delete
		if (this.isDelete) {

			// Add column of delete buttons
			TableColumn<Row, DeleteRow> column = new TableColumn<>();
			column.setCellValueFactory((row) -> new SimpleObjectProperty<>(row.getValue().delete));
			column.setCellFactory((tc) -> {
				TableCell<Row, DeleteRow> cell = new TableCell<Row, DeleteRow>() {
					@Override
					protected void updateItem(DeleteRow item, boolean empty) {
						super.updateItem(item, empty);

						// Handle add row
						if (isUpdateItemAddRow(this)) {
							return;
						}

						// No text
						this.setText(null);

						// Determine if show delete button
						if ((!this.isEmpty()) && (item != null)) {

							// Click anywhere on cell to delete
							this.setOnMouseClicked((event) -> item.deleteRow());

							// Delete row icon
							this.setGraphic(
									new ImageView(new Image(DefaultImages.DELETE_IMAGE_PATH, 15, 15, true, true)));

						} else {
							// Row delete, so clear graphic
							this.setGraphic(null);
						}
					}
				};
				cell.setAlignment(Pos.CENTER);
				return cell;
			});
			columns[columns.length - 1] = column;
		}

		// Specify the columns
		table.getColumns().setAll(columns);

		// Load the rows
		List<I> items = itemsProperty.getValue();
		if (items != null) {
			for (I item : items) {
				rows.add(new Row(table, loadRowsToItems, item));
			}
		}

		// Determine if able to add rows
		if (this.itemFactory != null) {
			rows.add(new AddRow(table, loadRowsToItems));
		}

		// Load rows to the table
		table.setItems(rows);

		// Hook in typing to start edit
		table.addEventFilter(KeyEvent.KEY_PRESSED, (event) -> {

			// Obtain the selected row
			if (table.getSelectionModel().getSelectedCells().size() == 0) {
				return; // nothing selected
			}
			TablePosition<Row, ?> selectedPosition = table.getSelectionModel().getSelectedCells().get(0);
			Row row = table.getItems().get(selectedPosition.getRow());

			// Determine if add row
			if (row instanceof ListBuilderImpl.AddRow) {
				AddRow addRow = (AddRow) row;

				// Handle adding a new row
				switch (event.getCode()) {
				case ENTER:
					// Add a row
					addRow.addRow();
					break;
				default:
					break;
				}
				return;
			}

			// Determine if delete row
			if (this.isDelete) {

				// Determine if delete row
				switch (event.getCode()) {
				case BACK_SPACE:
				case DELETE:
					// Delete if not editing and able to delete
					if ((table.getEditingCell() == null) && (row.delete != null)) {
						row.delete.deleteRow();
					}
					break;
				default:
					break;
				}

				// Determine if key press on delete column
				int columnIndex = selectedPosition.getColumn();
				if (columnIndex >= this.renderers.size()) {
					// Delete column
					switch (event.getCode()) {
					case SPACE:
					case ENTER:
						if (row.delete != null) {
							row.delete.deleteRow();
						}
						break;
					default:
						break;
					}
					return;
				}
			}

			// Attempting now to edit (so ensure column is editable)
			if (!selectedPosition.getTableColumn().isEditable()) {
				return;
			}

			// Handle column based on type
			Property<?> cellProperty = row.cells[selectedPosition.getColumn()].getValue();
			if (cellProperty instanceof BooleanProperty) {
				BooleanProperty toggle = (BooleanProperty) cellProperty;

				// Toggle value on space
				switch (event.getCode()) {
				case SPACE:
				case ENTER:
					toggle.setValue(!toggle.getValue());
					break;
				default:
					break;
				}

			} else if (cellProperty instanceof StringProperty) {
				// Start editing (if not already editing)
				TablePosition<Row, ?> editingPosition = table.getEditingCell();
				if (editingPosition == null) {
					if (event.getCode().isLetterKey() || event.getCode().isDigitKey()) {
						TablePosition focusedCellPosition = table.getFocusModel().getFocusedCell();
						table.edit(focusedCellPosition.getRow(), focusedCellPosition.getTableColumn());
					}
					return;
				}
			}
		});

		// Determine if fixed number of rows
		double ROW_HEIGHT = 30;
		double HEADER_HEIGHT = ROW_HEIGHT + 5;
		if ((this.itemFactory == null) && (!this.isDelete)) {
			// Fixed number of rows, so fix on number of rows
			table.setFixedCellSize(ROW_HEIGHT);
			table.prefHeightProperty()
					.bind(Bindings.size(table.getItems()).multiply(table.getFixedCellSize()).add(HEADER_HEIGHT));

		} else {
			// Allow to adjust height (with reasonable size)
			double requiredHeight = ((table.getItems().size() + 1) * ROW_HEIGHT) + HEADER_HEIGHT;
			table.prefHeightProperty().set(Math.min(requiredHeight, 300));
			DragResizer.makeResizable(table);

			// Handle adding/removing rows
			rows.addListener((Change<?> event) -> loadRowsToItems.run());
		}

		// Return table
		return () -> table;
	}

	/*
	 * =============== ListBuilder ==================
	 */

	@Override
	public TextBuilder<I> text(String label) {
		return this.registerBuilder(new TextBuilderImpl<>(label));
	}

	@Override
	public FlagBuilder<I> flag(String label) {
		return this.registerBuilder(new FlagBuilderImpl<>(label));
	}

	@Override
	public ListBuilder<M, I> addItem(Supplier<I> itemFactory) {
		this.itemFactory = itemFactory;
		return this;
	}

	@Override
	public ListBuilder<M, I> deleteItem() {
		this.isDelete = true;
		return this;
	}

	/**
	 * Row of the {@link TableView}.
	 */
	private class Row implements ValueRendererContext<I> {

		/**
		 * Item.
		 */
		private final I item;

		/**
		 * {@link CellRenderer} instances for the {@link TableCell} instances.
		 */
		private final CellRenderer<I, ?>[] cells;

		/**
		 * {@link Runnable} to update model.
		 */
		protected final Runnable updater;

		/**
		 * {@link DeleteRow}.
		 */
		private final DeleteRow delete;

		/**
		 * Instantiate.
		 * 
		 * @param table
		 *            {@link TableView}.
		 * @param updater
		 *            {@link Runnable} to update model.
		 * @param item
		 *            Item for the {@link Row}.
		 * @param isAddRow
		 *            <code>true</code> if the add row.
		 */
		@SuppressWarnings("unchecked")
		private Row(TableView<Row> table, Runnable updater, I item) {
			this.item = item;
			this.updater = updater;

			// Create the properties for the row
			this.cells = new CellRenderer[ListBuilderImpl.this.renderers.size()];
			for (int i = 0; i < this.cells.length; i++) {
				this.cells[i] = ListBuilderImpl.this.renderers.get(i).createCellRenderer(this);

				// Trigger updating
				this.cells[i].getValue().addListener((event) -> this.updater.run());
			}

			// Provide the delete (except for add row - null item)
			this.delete = ((item != null) && ListBuilderImpl.this.isDelete) ? new DeleteRow(table, this) : null;
		}

		/*
		 * ============== ValueRendererContext ===============
		 */

		@Override
		public I getModel() {
			return this.item;
		}

		@Override
		public void refreshError() {
			// TODO Auto-generated method stub

		}
	}

	/**
	 * {@link Row} to add another {@link Row}.
	 */
	private class AddRow extends Row {

		/**
		 * {@link TableView}.
		 */
		private final TableView<Row> table;

		/**
		 * Instantiate.
		 * 
		 * @param table
		 *            {@link TableView}.
		 * @param updater
		 *            {@link Runnable} to update model.
		 */
		public AddRow(TableView<Row> table, Runnable updater) {
			super(table, updater, null);
			this.table = table;
		}

		/**
		 * Adds a {@link Row}.
		 * 
		 * @param table
		 *            {@link TableView}.
		 */
		private void addRow() {

			// Create the new row
			I newItem = ListBuilderImpl.this.itemFactory.get();
			Row newRow = new Row(this.table, this.updater, newItem);

			// Add the row (before the add row)
			ObservableList<Row> items = this.table.getItems();
			items.add(items.size() - 1, newRow);
		}
	}

	/**
	 * Object for deleting the {@link Row}.
	 */
	private class DeleteRow {

		/**
		 * {@link TableView} containing the {@link Row} instances.
		 */
		private final TableView<Row> table;

		/**
		 * Row to be deleted.
		 */
		private final Row row;

		/**
		 * Instantiate.
		 * 
		 * @param table
		 *            {@link TableView}.
		 * @param row
		 *            {@link Row}.
		 */
		public DeleteRow(TableView<Row> table, Row row) {
			this.table = table;
			this.row = row;
		}

		/**
		 * Deletes the {@link Row}.
		 */
		public void deleteRow() {
			this.table.getItems().remove(this.row);
		}
	}

}