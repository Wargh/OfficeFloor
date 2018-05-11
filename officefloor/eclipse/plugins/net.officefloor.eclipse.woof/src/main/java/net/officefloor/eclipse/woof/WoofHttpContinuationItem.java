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
package net.officefloor.eclipse.woof;

import java.util.List;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import net.officefloor.eclipse.configurer.ValueValidator;
import net.officefloor.eclipse.editor.AdaptedModelVisualFactoryContext;
import net.officefloor.eclipse.editor.DefaultConnectors;
import net.officefloor.eclipse.ide.editor.AbstractConfigurableItem;
import net.officefloor.model.ConnectionModel;
import net.officefloor.woof.model.woof.WoofChanges;
import net.officefloor.woof.model.woof.WoofHttpContinuationModel;
import net.officefloor.woof.model.woof.WoofHttpContinuationToWoofHttpContinuationModel;
import net.officefloor.woof.model.woof.WoofHttpContinuationModel.WoofHttpContinuationEvent;
import net.officefloor.woof.model.woof.WoofHttpContinuationToWoofResourceModel;
import net.officefloor.woof.model.woof.WoofHttpContinuationToWoofSectionInputModel;
import net.officefloor.woof.model.woof.WoofHttpContinuationToWoofSecurityModel;
import net.officefloor.woof.model.woof.WoofHttpContinuationToWoofTemplateModel;
import net.officefloor.woof.model.woof.WoofHttpInputToWoofHttpContinuationModel;
import net.officefloor.woof.model.woof.WoofModel;
import net.officefloor.woof.model.woof.WoofModel.WoofEvent;
import net.officefloor.woof.model.woof.WoofResourceModel;
import net.officefloor.woof.model.woof.WoofResourceModel.WoofResourceEvent;
import net.officefloor.woof.model.woof.WoofSectionInputModel;
import net.officefloor.woof.model.woof.WoofTemplateModel;
import net.officefloor.woof.model.woof.WoofTemplateOutputToWoofHttpContinuationModel;
import net.officefloor.woof.model.woof.WoofTemplateModel.WoofTemplateEvent;
import net.officefloor.woof.model.woof.WoofSectionInputModel.WoofSectionInputEvent;
import net.officefloor.woof.model.woof.WoofSecurityModel;
import net.officefloor.woof.model.woof.WoofSecurityModel.WoofSecurityEvent;

/**
 * Configuration for the {@link WoofHttpContinuationModel}.
 * 
 * @author Daniel Sagenschneider
 */
public class WoofHttpContinuationItem extends
		AbstractConfigurableItem<WoofModel, WoofEvent, WoofChanges, WoofHttpContinuationModel, WoofHttpContinuationEvent, WoofHttpContinuationItem> {

	/**
	 * Test configuration.
	 */
	public static void main(String[] args) {
		WoofEditor.launchConfigurer(new WoofHttpContinuationItem(), (model) -> {
			model.setApplicationPath("/path");
			model.setIsSecure(true);
		});
	}

	/**
	 * Application path.
	 */
	private String applicationPath;

	/**
	 * Indicates if HTTPS.
	 */
	private boolean isHttps = false;

	/*
	 * ================= AbstractConfigurableItem ==================
	 */

	@Override
	public WoofHttpContinuationModel prototype() {
		return new WoofHttpContinuationModel(false, "HTTP Continuation");
	}

	@Override
	public IdeExtractor extract() {
		return new IdeExtractor((parent) -> parent.getWoofHttpContinuations(), WoofEvent.ADD_WOOF_HTTP_CONTINUATION,
				WoofEvent.REMOVE_WOOF_HTTP_CONTINUATION);
	}

	@Override
	public Pane visual(WoofHttpContinuationModel model,
			AdaptedModelVisualFactoryContext<WoofHttpContinuationModel> context) {
		HBox container = new HBox();
		context.addNode(container,
				context.connector(DefaultConnectors.FLOW)
						.target(WoofHttpContinuationToWoofHttpContinuationModel.class,
								WoofHttpInputToWoofHttpContinuationModel.class,
								WoofTemplateOutputToWoofHttpContinuationModel.class)
						.getNode());
		context.label(container);
		context.addNode(container,
				context.connector(DefaultConnectors.FLOW, WoofHttpContinuationToWoofSectionInputModel.class,
						WoofHttpContinuationToWoofTemplateModel.class, WoofHttpContinuationToWoofResourceModel.class,
						WoofHttpContinuationToWoofSecurityModel.class)
						.source(WoofHttpContinuationToWoofHttpContinuationModel.class).getNode());
		return container;
	}

	@Override
	public IdeLabeller label() {
		return new IdeLabeller((model) -> model.getApplicationPath(),
				WoofHttpContinuationEvent.CHANGE_APPLICATION_PATH);
	}

	@Override
	public void loadToParent(WoofModel parentModel, WoofHttpContinuationModel itemModel) {
		parentModel.addWoofHttpContinuation(itemModel);
	}

	@Override
	protected WoofHttpContinuationItem item(WoofHttpContinuationModel model) {
		WoofHttpContinuationItem item = new WoofHttpContinuationItem();
		if (model != null) {
			item.applicationPath = model.getApplicationPath();
			item.isHttps = model.getIsSecure();
		}
		return item;
	}

	@Override
	protected void connections(List<IdeConnectionTarget<? extends ConnectionModel, ?, ?>> connections) {

		// Section Input
		connections.add(new IdeConnection<>(WoofHttpContinuationToWoofSectionInputModel.class)
				.connectOne(s -> s.getWoofSectionInput(), c -> c.getWoofHttpContinuation(),
						WoofHttpContinuationEvent.CHANGE_WOOF_SECTION_INPUT)
				.to(WoofSectionInputModel.class)
				.many(t -> t.getWoofHttpContinuations(), c -> c.getWoofSectionInput(),
						WoofSectionInputEvent.ADD_WOOF_HTTP_CONTINUATION,
						WoofSectionInputEvent.REMOVE_WOOF_HTTP_CONTINUATION)
				.create((s, t, ctx) -> {
					ctx.getChangeExecutor().execute(ctx.getOperations().linkHttpContinuationToSectionInput(s, t));
				}).delete((ctx) -> {
					ctx.getChangeExecutor()
							.execute(ctx.getOperations().removeHttpContinuationToSectionInput(ctx.getModel()));
				}));

		// Template
		connections.add(new IdeConnection<>(WoofHttpContinuationToWoofTemplateModel.class)
				.connectOne(s -> s.getWoofTemplate(), c -> c.getWoofHttpContinuation(),
						WoofHttpContinuationEvent.CHANGE_WOOF_TEMPLATE)
				.to(WoofTemplateModel.class)
				.many(t -> t.getWoofHttpContinuations(), c -> c.getWoofTemplate(),
						WoofTemplateEvent.ADD_WOOF_HTTP_CONTINUATION, WoofTemplateEvent.REMOVE_WOOF_HTTP_CONTINUATION)
				.create((s, t, ctx) -> {
					ctx.getChangeExecutor().execute(ctx.getOperations().linkHttpContinuationToTemplate(s, t));
				}).delete((ctx) -> {
					ctx.getChangeExecutor()
							.execute(ctx.getOperations().removeHttpContinuationToTemplate(ctx.getModel()));
				}));

		// Resource
		connections.add(new IdeConnection<>(WoofHttpContinuationToWoofResourceModel.class)
				.connectOne(s -> s.getWoofResource(), c -> c.getWoofHttpContinuation(),
						WoofHttpContinuationEvent.CHANGE_WOOF_RESOURCE)
				.to(WoofResourceModel.class)
				.many(t -> t.getWoofHttpContinuations(), c -> c.getWoofResource(),
						WoofResourceEvent.ADD_WOOF_HTTP_CONTINUATION, WoofResourceEvent.REMOVE_WOOF_HTTP_CONTINUATION)
				.create((s, t, ctx) -> {
					ctx.getChangeExecutor().execute(ctx.getOperations().linkHttpContinuationToResource(s, t));
				}).delete((ctx) -> {
					ctx.getChangeExecutor()
							.execute(ctx.getOperations().removeHttpContinuationToResource(ctx.getModel()));
				}));

		// Security
		connections.add(new IdeConnection<>(WoofHttpContinuationToWoofSecurityModel.class)
				.connectOne(s -> s.getWoofSecurity(), c -> c.getWoofHttpContinuation(),
						WoofHttpContinuationEvent.CHANGE_WOOF_SECURITY)
				.to(WoofSecurityModel.class)
				.many(t -> t.getWoofHttpContinuations(), c -> c.getWoofSecurity(),
						WoofSecurityEvent.ADD_WOOF_HTTP_CONTINUATION, WoofSecurityEvent.REMOVE_WOOF_HTTP_CONTINUATION)
				.create((s, t, ctx) -> {
					ctx.getChangeExecutor().execute(ctx.getOperations().linkHttpContinuationToSecurity(s, t));
				}).delete((ctx) -> {
					ctx.getChangeExecutor()
							.execute(ctx.getOperations().removeHttpContinuationToSecurity(ctx.getModel()));
				}));

		// HTTP Continuation
		connections.add(new IdeConnection<>(WoofHttpContinuationToWoofHttpContinuationModel.class)
				.connectOne(s -> s.getWoofRedirect(), c -> c.getWoofHttpContinuation(),
						WoofHttpContinuationEvent.CHANGE_WOOF_REDIRECT)
				.to(WoofHttpContinuationModel.class)
				.many(t -> t.getWoofHttpContinuations(), c -> c.getWoofRedirect(),
						WoofHttpContinuationEvent.ADD_WOOF_HTTP_CONTINUATION,
						WoofHttpContinuationEvent.REMOVE_WOOF_HTTP_CONTINUATION)
				.create((s, t, ctx) -> {
					ctx.getChangeExecutor().execute(ctx.getOperations().linkHttpContinuationToHttpContinuation(s, t));
				}).delete((ctx) -> {
					ctx.getChangeExecutor()
							.execute(ctx.getOperations().removeHttpContinuationToHttpContinuation(ctx.getModel()));
				}));
	}

	@Override
	public IdeConfigurer configure() {
		return new IdeConfigurer().addAndRefactor((builder, context) -> {
			builder.title("HTTP Continuation");
			builder.text("Path").init((item) -> item.applicationPath)
					.validate(ValueValidator.notEmptyString("Must specify application path"))
					.setValue((item, value) -> item.applicationPath = value);
			builder.flag("https").init((item) -> item.isHttps).setValue((item, value) -> item.isHttps = value);

		}).add((builder, context) -> {
			builder.apply("Add", (item) -> {
				context.execute(context.getOperations().addHttpContinuation(item.applicationPath, item.isHttps));
			});

		}).refactor((builder, context) -> {
			builder.apply("Refactor", (item) -> {
				context.execute(context.getOperations().refactorHttpContinuation(context.getModel(),
						item.applicationPath, item.isHttps));
			});

		}).delete((context) -> {
			context.execute(context.getOperations().removeHttpContinuation(context.getModel()));
		});
	}

}