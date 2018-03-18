/*******************************************************************************
 * Copyright (c) 2014, 2017 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package net.officefloor.eclipse.editor.parts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.mvc.fx.parts.IVisualPart;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import net.officefloor.eclipse.editor.AdaptedChild;
import net.officefloor.model.Model;

public class AdaptedChildPart<M extends Model, A extends AdaptedChild<M>> extends AbstractAdaptedPart<M, A, Pane> {

	@Override
	protected SetMultimap<? extends Object, String> doGetContentAnchorages() {
		return HashMultimap.create();
	}

	@Override
	protected List<Object> doGetContentChildren() {
		return this.getContent().getChildren();
	}

	@Override
	protected void doAddChildVisual(IVisualPart<? extends Node> child, int index) {
		// Should only be children groups (already added)
	}

	@Override
	protected Pane doCreateVisual() {

		// Create the visual
		Pane pane = this.getContent().createVisual();

		// Provide model as class for CSS
		pane.getStyleClass().add(this.getContent().getModel().getClass().getSimpleName());

		// Return the visual
		return pane;
	}

	@Override
	protected void doRefreshVisual(Pane visual) {
	}

}