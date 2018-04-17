/*******************************************************************************
 * Copyright (c) 2016 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package net.officefloor.eclipse.editor.internal.views;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.fx.nodes.InfiniteCanvas;
import org.eclipse.gef.mvc.fx.viewer.IViewer;
import org.eclipse.gef.mvc.fx.viewer.InfiniteCanvasViewer;

import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.officefloor.eclipse.editor.AdaptedEditorModule;
import net.officefloor.eclipse.editor.AdaptedErrorHandler;
import net.officefloor.eclipse.editor.AdaptedParent;
import net.officefloor.eclipse.editor.internal.behaviors.PaletteFocusBehavior;

/**
 * Composite of the viewers.
 *
 * @author Daniel Sagenschneider
 */
public class ViewersComposite implements AdaptedErrorHandler {

	/**
	 * Palette indicator width.
	 */
	private static final double PALETTE_INDICATOR_WIDTH = 10.0;

	/**
	 * {@link IViewer} for the content.
	 */
	private IViewer contentViewer;

	/**
	 * {@link IViewer} for the palette.
	 */
	private IViewer paletteViewer;

	/**
	 * Composite for the view.
	 */
	private final VBox composite = new VBox();

	/**
	 * Header for the error.
	 */
	private final GridPane errorHeader = new GridPane();

	/**
	 * Label for the error.
	 */
	private final Label errorLabel = new Label();

	/**
	 * Dismisses the error.
	 */
	private final Hyperlink dismissError = new Hyperlink("dismiss");

	/**
	 * Editor with stack trace.
	 */
	private final SplitPane editorWithStackTrace = new SplitPane();

	/**
	 * Toggle for showing the stack trace.
	 */
	private final Hyperlink stackTraceToggle = new Hyperlink();

	/**
	 * Indicate if showing stack trace.
	 */
	private boolean isShowingStackTrace = false;

	/**
	 * {@link TextArea} to display the stack trace.
	 */
	private final TextArea stackTrace = new TextArea();

	/**
	 * Instantiate.
	 * 
	 * @param contentViewer
	 *            {@link IViewer} for the editor.
	 * @param paletteViewer
	 *            {@link IViewer} for the palette.
	 */
	public ViewersComposite(IViewer contentViewer, IViewer paletteViewer) {
		this.contentViewer = contentViewer;
		this.paletteViewer = paletteViewer;
	}

	/**
	 * Initialises.
	 * 
	 * @param isCreateParents
	 *            Flag indicate whether able to create {@link AdaptedParent}
	 *            instances.
	 */
	public void init(boolean isCreateParents) {

		// Obtain the content root
		Parent contentRootNode = contentViewer.getCanvas();
		InfiniteCanvas paletteRootNode = ((InfiniteCanvasViewer) paletteViewer).getCanvas();

		// Arrange viewers above each other
		AnchorPane viewersPane = new AnchorPane();
		viewersPane.getChildren().addAll(contentRootNode, paletteRootNode);

		// Ensure viewers fill the space
		HBox.setHgrow(viewersPane, Priority.ALWAYS);
		viewersPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		AnchorPane.setBottomAnchor(contentRootNode, 0.0);
		AnchorPane.setLeftAnchor(contentRootNode, 0.0);
		AnchorPane.setRightAnchor(contentRootNode, 0.0);
		AnchorPane.setTopAnchor(contentRootNode, 0.0);
		AnchorPane.setBottomAnchor(paletteRootNode, 0.0);
		AnchorPane.setLeftAnchor(paletteRootNode, 0.0);
		AnchorPane.setTopAnchor(paletteRootNode, 0.0);

		// Configure palette
		paletteRootNode.setZoomGrid(false);
		paletteRootNode.setShowGrid(false);
		paletteRootNode.setHorizontalScrollBarPolicy(ScrollBarPolicy.NEVER);
		paletteRootNode.setStyle(PaletteFocusBehavior.DEFAULT_STYLE);
		if (!isCreateParents) {
			paletteRootNode.setVisible(false);
		}

		// Create palette indicator
		List<Pane> panes = new ArrayList<>(2);
		if (isCreateParents) {

			// Able to create parents, so provide palette
			Pane paletteIndicator = new Pane();
			paletteIndicator.setStyle("-fx-background-color: rgba(128,128,128,1);");
			paletteIndicator.setMaxSize(PALETTE_INDICATOR_WIDTH, Double.MAX_VALUE);
			paletteIndicator.setMinSize(PALETTE_INDICATOR_WIDTH, 0.0);
			panes.add(paletteIndicator);

			// Register listeners to show/hide palette
			paletteIndicator.setOnMouseEntered((event) -> paletteRootNode.setVisible(true));
			paletteRootNode.setOnMouseExited((event) -> paletteRootNode.setVisible(false));

			// Register listeners to update the palette width
			paletteRootNode.getContentGroup().layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
				double scrollBarWidth = paletteRootNode.getVerticalScrollBar().isVisible()
						? paletteRootNode.getVerticalScrollBar().getLayoutBounds().getWidth()
						: 0;
				paletteRootNode.setPrefWidth(newValue.getWidth() + scrollBarWidth);
			});
			paletteRootNode.getVerticalScrollBar().visibleProperty().addListener((observable, oldValue, newValue) -> {
				double contentWidth = paletteRootNode.getContentGroup().getLayoutBounds().getWidth();
				double scrollBarWidth = newValue ? paletteRootNode.getVerticalScrollBar().getLayoutBounds().getWidth()
						: 0;
				paletteRootNode.setPrefWidth(contentWidth + scrollBarWidth);
			});

			// Hide palette when a palette element is pressed
			paletteRootNode.addEventHandler(MouseEvent.MOUSE_PRESSED, (event) -> {
				if (event.getTarget() != paletteRootNode) {
					paletteRootNode.setVisible(false);
				}
			});
		}
		panes.add(viewersPane);

		// Provide composite
		HBox editor = new HBox();
		editor.setStyle("-fx-background-color: transparent;");
		editor.getChildren().addAll(panes);

		// Ensure composite fills the whole space
		editor.setMinSize(0, 0);
		editor.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		editor.setFillHeight(true);
		editor.setSpacing(0.0); // no spacing between palette and content

		// Load the editor into the split pane (allow viewing stack trace)
		this.editorWithStackTrace.setOrientation(Orientation.HORIZONTAL);
		this.editorWithStackTrace.getItems().add(editor);

		// Configure the error details
		HBox errorDetails = new HBox(10.0);
		errorDetails.getStyleClass().setAll("header-panel");
		final String errorImagePath = AdaptedEditorModule.class.getPackage().getName().replace('.', '/') + "/error.png";
		final ImageView errorImage = new ImageView(new Image(errorImagePath, 15, 15, true, true));
		this.errorLabel.alignmentProperty().setValue(Pos.CENTER_LEFT);
		this.dismissError.getStyleClass().add("dismiss-error");
		this.dismissError.setOnAction((event) -> this.showError((String) null));
		errorDetails.getChildren().setAll(errorImage, this.errorLabel, this.dismissError);

		// Provide stack trace toggle
		this.stackTraceToggle.setText("Show Stack Trace");
		this.stackTraceToggle.getStyleClass().setAll("details-button", "more");
		this.stackTraceToggle.setVisible(this.isShowingStackTrace);
		this.stackTraceToggle.setOnAction((event) -> {
			if (this.isShowingStackTrace) {
				// Hide stack trace
				this.editorWithStackTrace.getItems().remove(this.stackTrace);
				this.stackTraceToggle.setText("Show Stack Trace");
				this.stackTraceToggle.getStyleClass().setAll("details-button", "more");
				this.isShowingStackTrace = false;
			} else {
				// Show stack trace
				if (!this.editorWithStackTrace.getItems().contains(this.stackTrace)) {
					this.editorWithStackTrace.getItems().add(this.stackTrace);
				}
				this.stackTraceToggle.setText("Hide Stack Trace");
				this.stackTraceToggle.getStyleClass().setAll("details-button", "less");
				this.isShowingStackTrace = true;
			}
		});
		HBox stackTraceToggleContainer = new HBox();
		stackTraceToggleContainer.getChildren().add(this.stackTraceToggle);
		stackTraceToggleContainer.getStyleClass().setAll("container");
		stackTraceToggleContainer.alignmentProperty().setValue(Pos.CENTER_RIGHT);
		HBox stackTraceToggleButtonBar = new HBox();
		stackTraceToggleButtonBar.getStyleClass().setAll("header-panel", "button-bar");
		stackTraceToggleButtonBar.getChildren().setAll(stackTraceToggleContainer);

		// Configurer the error header
		this.errorHeader.getStyleClass().setAll("dialog-pane", "error-header");
		this.errorHeader.pseudoClassStateChanged(PseudoClass.getPseudoClass("header"), true);
		this.errorHeader.add(errorDetails, 0, 0);
		this.errorHeader.add(stackTraceToggleButtonBar, 1, 0);
		GridPane.setHgrow(errorDetails, Priority.ALWAYS);
		GridPane.setHgrow(stackTraceToggleButtonBar, Priority.SOMETIMES);

		// Configure the stack trace
		this.stackTrace.setEditable(false);
		this.stackTrace.prefHeightProperty().bind(editorWithStackTrace.heightProperty());
		this.stackTrace.prefWidthProperty().bind(Bindings.divide(this.composite.widthProperty(), 2));

		// Configure the composite (initially only error)
		this.composite.getChildren().add(editorWithStackTrace);
		VBox.setVgrow(editorWithStackTrace, Priority.ALWAYS);
	}

	/**
	 * Obtains the {@link Pane} containing the view.
	 * 
	 * @return {@link Pane} containing the view.
	 */
	public Pane getComposite() {
		return this.composite;
	}

	/*
	 * ================= AdaptedErrorHandler ====================
	 */

	@Override
	public void showError(String message) {
		this.showError((message == null) || (message.trim().length() == 0) ? null : new MessageOnlyException(message));
	}

	@Override
	public void showError(Throwable error) {

		// Determine if error
		String errorText = null;
		String stackTraceText = null;
		if (error != null) {

			// Provide error text
			errorText = error.getMessage();
			if ((errorText == null) || (errorText.trim().length() == 0)) {
				errorText = error.getClass().getSimpleName() + " thrown";
			}

			// Determine if stack trace
			if (!(error instanceof MessageOnlyException)) {
				StringWriter buffer = new StringWriter();
				error.printStackTrace(new PrintWriter(buffer));
				stackTraceText = buffer.toString();
			}
		}

		// Handle error message
		if (errorText == null) {
			// No error message
			this.composite.getChildren().remove(this.errorHeader);

		} else {
			// Show error message
			this.errorLabel.setText(errorText);
			this.dismissError.setVisited(false);
			if (!this.composite.getChildren().contains(this.errorHeader)) {
				this.composite.getChildren().add(0, this.errorHeader);
			}
		}

		// Handle stack trace
		if (stackTraceText == null) {
			// No stack trace
			this.stackTraceToggle.setVisible(false);
			this.editorWithStackTrace.getItems().remove(this.stackTrace);

		} else {
			// Show stack trace
			this.stackTrace.setText(stackTraceText);
			this.stackTraceToggle.setVisible(true);
		}
	}

	@Override
	public boolean isError(UncertainOperation operation) {
		try {

			// Undertake operation
			operation.run();

			// No error
			return false;

		} catch (Throwable ex) {

			// Show the error
			this.showError(ex);

			// An error
			return true;
		}
	}

	/**
	 * Message only {@link Exception}.
	 */
	private static class MessageOnlyException extends Exception {

		/**
		 * Instantiate.
		 * 
		 * @param message
		 *            Message.
		 */
		public MessageOnlyException(String message) {
			super(message);
		}
	}

}