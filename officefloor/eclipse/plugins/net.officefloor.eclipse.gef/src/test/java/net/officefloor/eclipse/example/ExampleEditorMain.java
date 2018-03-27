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
package net.officefloor.eclipse.example;

import java.util.function.Supplier;

import org.eclipse.gef.fx.nodes.GeometryNode;
import org.eclipse.gef.geometry.planar.Polygon;

import javafx.application.Application;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.officefloor.eclipse.editor.AbstractEditorApplication;
import net.officefloor.eclipse.editor.AdaptedBuilderContext;
import net.officefloor.eclipse.editor.AdaptedChildBuilder;
import net.officefloor.eclipse.editor.AdaptedParentBuilder;
import net.officefloor.eclipse.editor.AdaptedRootBuilder;
import net.officefloor.eclipse.editor.DefaultImages;
import net.officefloor.model.Model;
import net.officefloor.model.impl.officefloor.OfficeFloorChangesImpl;
import net.officefloor.model.officefloor.OfficeFloorChanges;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectSourceFlowModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectSourceFlowModel.OfficeFloorManagedObjectSourceFlowEvent;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectSourceInputDependencyModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectSourceInputDependencyModel.OfficeFloorManagedObjectSourceInputDependencyEvent;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectSourceModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectSourceModel.OfficeFloorManagedObjectSourceEvent;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectSourceTeamModel;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectSourceTeamModel.OfficeFloorManagedObjectSourceTeamEvent;
import net.officefloor.model.officefloor.OfficeFloorManagedObjectSourceTeamToOfficeFloorTeamModel;
import net.officefloor.model.officefloor.OfficeFloorModel;
import net.officefloor.model.officefloor.OfficeFloorModel.OfficeFloorEvent;
import net.officefloor.model.officefloor.OfficeFloorTeamModel;
import net.officefloor.model.officefloor.OfficeFloorTeamModel.OfficeFloorTeamEvent;

/**
 * Main for running example editor.
 * 
 * @author Daniel Sagenschneider
 */
public class ExampleEditorMain extends AbstractEditorApplication {

	/**
	 * Main to run the editor.
	 * 
	 * @param args
	 *            Command line arguments.
	 */
	public static void main(String[] args) {
		Application.launch(args);
	}

	/*
	 * ============ AbstractEditorMain ================
	 */

	@Override
	protected void buildModels(AdaptedBuilderContext builder) {

		// Create connector
		Supplier<GeometryNode<Polygon>> createConnector = () -> {
			GeometryNode<Polygon> node = new GeometryNode<>(new Polygon(2, 3, 5, 3, 5, 1, 10, 4, 5, 7, 5, 5, 2, 5));
			node.setFill(Color.BLACK);
			return node;
		};

		// Specify the root model
		AdaptedRootBuilder<OfficeFloorModel, OfficeFloorChanges> root = builder.root(OfficeFloorModel.class,
				(r) -> new OfficeFloorChangesImpl(r));

		// Managed Object Source
		AdaptedParentBuilder<OfficeFloorModel, OfficeFloorChanges, OfficeFloorManagedObjectSourceModel, OfficeFloorManagedObjectSourceEvent> mos = root
				.parent(OfficeFloorManagedObjectSourceModel.class, (r) -> r.getOfficeFloorManagedObjectSources(),
						(model, context) -> {
							VBox container = new VBox();
							context.label(container);
							HBox dependenciesFlows = context.addNode(container, new HBox());
							context.addNode(dependenciesFlows, context.childGroup("dependencies", new VBox()));
							context.addNode(dependenciesFlows, context.childGroup("flows", new VBox()));
							context.addNode(container, context.childGroup("teams", new VBox()));
							return container;
						}, OfficeFloorEvent.ADD_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE,
						OfficeFloorEvent.REMOVE_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE);
		mos.label((m) -> m.getOfficeFloorManagedObjectSourceName(),
				OfficeFloorManagedObjectSourceEvent.CHANGE_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE_NAME);
		mos.create(new OfficeFloorManagedObjectSourceModel("Managed Object Source", null, null, null),
				(p) -> p.getRootModel().addOfficeFloorManagedObjectSource(p.position(
						new OfficeFloorManagedObjectSourceModel("Created Managed Object Source", null, null, null))));
		mos.action((ctx) -> {
			ctx.getModel().addOfficeFloorManagedObjectSourceFlow(
					new OfficeFloorManagedObjectSourceFlowModel("Added Flow", null));
			ctx.getModel()
					.addOfficeFloorManagedObjectSourceTeam(new OfficeFloorManagedObjectSourceTeamModel("Added Team"));
			ctx.getModel().addOfficeFloorManagedObjectSourceInputDependency(
					new OfficeFloorManagedObjectSourceInputDependencyModel("Added dependency", null));
		}, DefaultImages.EDIT);
		mos.action((ctx) -> {
			if (ctx.getModel().getOfficeFloorManagedObjectSourceFlows().size() > 0) {
				ctx.getModel().removeOfficeFloorManagedObjectSourceFlow(
						ctx.getModel().getOfficeFloorManagedObjectSourceFlows().get(0));
			}
			if (ctx.getModel().getOfficeFloorManagedObjectSourceTeams().size() > 0) {
				ctx.getModel().removeOfficeFloorManagedObjectSourceTeam(
						ctx.getModel().getOfficeFloorManagedObjectSourceTeams().get(0));
			}
			if (ctx.getModel().getOfficeFloorManagedObjectSourceInputDependencies().size() > 0) {
				ctx.getModel().removeOfficeFloorManagedObjectSourceInputDependency(
						ctx.getModel().getOfficeFloorManagedObjectSourceInputDependencies().get(0));
			}
		}, DefaultImages.DELETE);
		mos.action((ctx) -> ctx.execute(ctx.getOperations().removeOfficeFloorManagedObjectSource(ctx.getModel())),
				DefaultImages.DELETE);

		// Managed Object Source Input Dependencies
		AdaptedChildBuilder<OfficeFloorModel, OfficeFloorChanges, OfficeFloorManagedObjectSourceInputDependencyModel, OfficeFloorManagedObjectSourceInputDependencyEvent> mosDependencies = mos
				.children("dependencies", (m) -> m.getOfficeFloorManagedObjectSourceInputDependencies(),
						OfficeFloorManagedObjectSourceEvent.ADD_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE_INPUT_DEPENDENCY,
						OfficeFloorManagedObjectSourceEvent.REMOVE_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE_INPUT_DEPENDENCY)
				.addChild(OfficeFloorManagedObjectSourceInputDependencyModel.class, (model, context) -> {
					HBox container = new HBox();
					context.label(container);
					return container;
				});
		mosDependencies.label((m) -> m.getOfficeFloorManagedObjectSourceInputDependencyName(),
				OfficeFloorManagedObjectSourceInputDependencyEvent.CHANGE_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE_INPUT_DEPENDENCY_NAME);

		// Managed Object Source Flows
		AdaptedChildBuilder<OfficeFloorModel, OfficeFloorChanges, OfficeFloorManagedObjectSourceFlowModel, OfficeFloorManagedObjectSourceFlowEvent> mosFlows = mos
				.children("flows", (m) -> m.getOfficeFloorManagedObjectSourceFlows(),
						OfficeFloorManagedObjectSourceEvent.ADD_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE_FLOW,
						OfficeFloorManagedObjectSourceEvent.REMOVE_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE_FLOW)
				.addChild(OfficeFloorManagedObjectSourceFlowModel.class, (model, context) -> {
					HBox container = new HBox();
					context.label(container);
					return container;
				});
		mosFlows.label((m) -> m.getOfficeFloorManagedObjectSourceFlowName(),
				OfficeFloorManagedObjectSourceFlowEvent.CHANGE_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE_FLOW_NAME);

		// Managed Object Source Teams
		AdaptedChildBuilder<OfficeFloorModel, OfficeFloorChanges, OfficeFloorManagedObjectSourceTeamModel, OfficeFloorManagedObjectSourceTeamEvent> mosTeams = mos
				.children("teams", (m) -> m.getOfficeFloorManagedObjectSourceTeams(),
						OfficeFloorManagedObjectSourceEvent.ADD_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE_TEAM,
						OfficeFloorManagedObjectSourceEvent.REMOVE_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE_TEAM)
				.addChild(OfficeFloorManagedObjectSourceTeamModel.class, (model, context) -> {
					HBox container = new HBox();
					GeometryNode<?> anchor = context.addNode(container, createConnector.get());
					context.connector(anchor, OfficeFloorManagedObjectSourceTeamToOfficeFloorTeamModel.class);
					context.label(container);
					return container;
				});
		mosTeams.label((m) -> m.getOfficeFloorManagedObjectSourceTeamName(),
				OfficeFloorManagedObjectSourceTeamEvent.CHANGE_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE_TEAM_NAME);
		mosTeams.connectOne(OfficeFloorManagedObjectSourceTeamToOfficeFloorTeamModel.class,
				(s) -> s.getOfficeFloorTeam(), (c) -> c.getOfficeFloorManagedObjectSourceTeam(),
				OfficeFloorManagedObjectSourceTeamEvent.CHANGE_OFFICE_FLOOR_TEAM).toMany(OfficeFloorTeamModel.class,
						(t) -> t.getOfficeFloorManagedObjectSourceTeams(), (c) -> c.getOfficeFloorTeam(),
						(s, t, ctx) -> ctx.execute(
								ctx.getOperations().linkOfficeFloorManagedObjectSourceTeamToOfficeFloorTeam(s, t)),
						(ctx) -> ctx.execute(ctx.getOperations()
								.removeOfficeFloorManagedObjectSourceTeamToOfficeFloorTeam(ctx.getModel())),
						OfficeFloorTeamEvent.ADD_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE_TEAM,
						OfficeFloorTeamEvent.REMOVE_OFFICE_FLOOR_MANAGED_OBJECT_SOURCE_TEAM);

		// Team
		AdaptedParentBuilder<OfficeFloorModel, OfficeFloorChanges, OfficeFloorTeamModel, OfficeFloorTeamEvent> team = root
				.parent(OfficeFloorTeamModel.class, (r) -> r.getOfficeFloorTeams(), (model, context) -> {
					HBox container = new HBox();
					context.label(container);
					GeometryNode<?> anchor = context.addNode(container, createConnector.get());
					context.connector(anchor, OfficeFloorManagedObjectSourceTeamToOfficeFloorTeamModel.class);
					return container;
				}, OfficeFloorEvent.ADD_OFFICE_FLOOR_TEAM, OfficeFloorEvent.REMOVE_OFFICE_FLOOR_TEAM);
		team.label((m) -> m.getOfficeFloorTeamName(), OfficeFloorTeamEvent.CHANGE_OFFICE_FLOOR_TEAM_NAME);
		team.create(new OfficeFloorTeamModel("Team", null),
				(p) -> p.getRootModel().addOfficeFloorTeam(p.position(new OfficeFloorTeamModel("Created Team", null))));
		team.action((ctx) -> ctx.execute(ctx.getOperations().removeOfficeFloorTeam(ctx.getModel())),
				DefaultImages.DELETE);
	}

	@Override
	protected Model createRootModel() {

		// Create the OfficeFloor model
		OfficeFloorModel root = new OfficeFloorModel();

		// Managed Object Source
		OfficeFloorManagedObjectSourceModel mos = new OfficeFloorManagedObjectSourceModel("Managed Object Source",
				"net.example.ManagedObjectSource", "net.example.Type", "0", 100, 100);
		root.addOfficeFloorManagedObjectSource(mos);
		mos.addOfficeFloorManagedObjectSourceInputDependency(
				new OfficeFloorManagedObjectSourceInputDependencyModel("dependency", "net.example.Dependency"));
		mos.addOfficeFloorManagedObjectSourceFlow(
				new OfficeFloorManagedObjectSourceFlowModel("flow", "net.example.Flow"));
		OfficeFloorManagedObjectSourceTeamModel mosTeam = new OfficeFloorManagedObjectSourceTeamModel("Team");
		mos.addOfficeFloorManagedObjectSourceTeam(mosTeam);
		mos.addOfficeFloorManagedObjectSourceTeam(new OfficeFloorManagedObjectSourceTeamModel("Another"));

		// Team
		OfficeFloorTeamModel team = new OfficeFloorTeamModel("Team", "net.example.TeamSource", 100, 300);
		root.addOfficeFloorTeam(team);

		// Another Team
		root.addOfficeFloorTeam(new OfficeFloorTeamModel("Team", null, 200, 300));

		// Connection
		OfficeFloorManagedObjectSourceTeamToOfficeFloorTeamModel mosTeamToTeam = new OfficeFloorManagedObjectSourceTeamToOfficeFloorTeamModel(
				team.getOfficeFloorTeamName(), mosTeam, team);
		mosTeamToTeam.connect();

		// Return the OfficeFloor model
		return root;
	}

}