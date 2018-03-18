/*******************************************************************************
 * Copyright (c) 2016, 2017 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package net.officefloor.eclipse.editor.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.gef.common.adapt.AdapterKey;
import org.eclipse.gef.geometry.planar.Dimension;
import org.eclipse.gef.geometry.planar.Point;
import org.eclipse.gef.mvc.fx.domain.IDomain;
import org.eclipse.gef.mvc.fx.gestures.ClickDragGesture;
import org.eclipse.gef.mvc.fx.handlers.AbstractHandler;
import org.eclipse.gef.mvc.fx.handlers.IOnDragHandler;
import org.eclipse.gef.mvc.fx.models.SelectionModel;
import org.eclipse.gef.mvc.fx.operations.DeselectOperation;
import org.eclipse.gef.mvc.fx.parts.IContentPart;
import org.eclipse.gef.mvc.fx.parts.IRootPart;
import org.eclipse.gef.mvc.fx.parts.LayeredRootPart;
import org.eclipse.gef.mvc.fx.policies.CreationPolicy;
import org.eclipse.gef.mvc.fx.viewer.IViewer;
import org.eclipse.gef.mvc.fx.viewer.InfiniteCanvasViewer;

import com.google.common.collect.HashMultimap;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import net.officefloor.eclipse.editor.models.ProxyCreateAdaptedParent;
import net.officefloor.eclipse.editor.parts.AdaptedParentPart;

public class CreateAndTranslateShapeOnDragHandler extends AbstractHandler implements IOnDragHandler {

	private AdaptedParentPart<?> adaptedParentPart;
	private Map<AdapterKey<? extends IOnDragHandler>, IOnDragHandler> dragPolicies;

	@Override
	public void abortDrag() {
		if (adaptedParentPart == null) {
			return;
		}

		// forward event to bend target part
		if (dragPolicies != null) {
			for (IOnDragHandler dragPolicy : dragPolicies.values()) {
				dragPolicy.abortDrag();
			}
		}

		adaptedParentPart = null;
		dragPolicies = null;
	}

	@Override
	public void drag(MouseEvent event, Dimension delta) {
		if (adaptedParentPart == null) {
			return;
		}

		// forward drag events to bend target part
		if (dragPolicies != null) {
			for (IOnDragHandler dragPolicy : dragPolicies.values()) {
				dragPolicy.drag(event, delta);
			}
		}
	}

	@Override
	public void endDrag(MouseEvent e, Dimension delta) {
		if (adaptedParentPart == null) {
			return;
		}

		// forward event to bend target part
		if (dragPolicies != null) {
			for (IOnDragHandler dragPolicy : dragPolicies.values()) {
				dragPolicy.endDrag(e, delta);
			}
		}

		restoreRefreshVisuals(adaptedParentPart);
		adaptedParentPart = null;
		dragPolicies = null;
	}

	protected IViewer getContentViewer() {
		return getHost().getRoot().getViewer().getDomain()
				.getAdapter(AdapterKey.get(IViewer.class, IDomain.CONTENT_VIEWER_ROLE));
	}

	@Override
	public AdaptedParentPart<?> getHost() {
		return (AdaptedParentPart<?>) super.getHost();
	}

	protected Point getLocation(MouseEvent e) {
		Point2D location = ((InfiniteCanvasViewer) getHost().getRoot().getViewer()).getCanvas().getContentGroup()
				.sceneToLocal(e.getSceneX(), e.getSceneY());
		return new Point(location.getX(), location.getY());
	}

	@Override
	public void hideIndicationCursor() {
	}

	@Override
	public boolean showIndicationCursor(KeyEvent event) {
		return false;
	}

	@Override
	public boolean showIndicationCursor(MouseEvent event) {
		return false;
	}

	@Override
	public void startDrag(MouseEvent event) {
		// find model part
		IRootPart<? extends Node> contentRoot = getContentViewer().getRootPart();

		// Create proxy to create another of type
		ProxyCreateAdaptedParent<?> proxy = new ProxyCreateAdaptedParent<>(this.getHost().getContent());
		// determine coordinates of prototype's origin in model coordinates
		Point2D localToScene = getHost().getVisual().localToScene(0, 0);
		Point2D originInModel = ((LayeredRootPart) getContentViewer().getRootPart()).getContentLayer()
				.sceneToLocal(localToScene.getX(), localToScene.getY());

		// create copy of host's geometry using CreationPolicy from root part
		CreationPolicy creationPolicy = contentRoot.getAdapter(CreationPolicy.class);
		init(creationPolicy);
		adaptedParentPart = (AdaptedParentPart<?>) creationPolicy.create(proxy, contentRoot,
				HashMultimap.<IContentPart<? extends Node>, String>create());
		commit(creationPolicy);

		// disable refresh visuals for the created shape part
		storeAndDisableRefreshVisuals(adaptedParentPart);

		// build operation to deselect all but the new part
		List<IContentPart<? extends Node>> toBeDeselected = new ArrayList<>(
				getContentViewer().getAdapter(SelectionModel.class).getSelectionUnmodifiable());
		toBeDeselected.remove(adaptedParentPart);
		DeselectOperation deselectOperation = new DeselectOperation(getContentViewer(), toBeDeselected);

		// execute on stack
		try {
			getHost().getRoot().getViewer().getDomain().execute(deselectOperation, new NullProgressMonitor());
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}

		// find drag target part
		dragPolicies = adaptedParentPart.getAdapters(ClickDragGesture.ON_DRAG_POLICY_KEY);
		if (dragPolicies != null) {
			for (IOnDragHandler dragPolicy : dragPolicies.values()) {
				dragPolicy.startDrag(event);
			}
		}
	}

}
