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

import org.eclipse.gef.geometry.convert.fx.FX2Geometry;
import org.eclipse.gef.geometry.convert.fx.Geometry2FX;
import org.eclipse.gef.geometry.planar.AffineTransform;
import org.eclipse.gef.mvc.fx.parts.ITransformableContentPart;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import javafx.scene.layout.Pane;
import javafx.scene.transform.Affine;
import net.officefloor.eclipse.editor.AdaptedParent;
import net.officefloor.model.Model;

public class AdaptedParentPart<M extends Model> extends AdaptedChildPart<M, AdaptedParent<M>>
		implements ITransformableContentPart<Pane> {

	/**
	 * {@link AffineTransform} for location of the {@link AdaptedParent}.
	 */
	private AffineTransform contentTransform = null;

	@Override
	public void init() {
		// Capture the initial location
		M model = this.getContent().getModel();
		this.contentTransform = new AffineTransform(1, 0, 0, 1, model.getX(), model.getY());
	}

	@Override
	protected SetMultimap<? extends Object, String> doGetContentAnchorages() {
		return HashMultimap.create();
	}

	@Override
	protected Pane doCreateVisual() {

		// Obtain the visual
		Pane container = super.doCreateVisual();

		// Specify the initial location
		container.setLayoutX(this.getContent().getModel().getX());
		container.setLayoutY(this.getContent().getModel().getY());

		// Return the pane
		return container;
	}

	@Override
	public Affine getContentTransform() {
		return Geometry2FX.toFXAffine(this.contentTransform);
	}

	@Override
	public void setContentTransform(Affine totalTransform) {
		this.contentTransform = FX2Geometry.toAffineTransform(totalTransform);

		// Update the location for the model
		this.getContent().changeLocation((int) this.contentTransform.getTranslateX(),
				(int) this.contentTransform.getTranslateY());
	}

}