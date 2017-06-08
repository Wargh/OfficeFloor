/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2013 Daniel Sagenschneider
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
package net.officefloor.compile.impl.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import net.officefloor.compile.governance.GovernanceLoader;
import net.officefloor.compile.governance.GovernanceType;
import net.officefloor.compile.impl.util.CompileUtil;
import net.officefloor.compile.impl.util.LinkUtil;
import net.officefloor.compile.internal.structure.AutoWire;
import net.officefloor.compile.internal.structure.AutoWireLink;
import net.officefloor.compile.internal.structure.AutoWirer;
import net.officefloor.compile.internal.structure.CompileContext;
import net.officefloor.compile.internal.structure.GovernanceNode;
import net.officefloor.compile.internal.structure.LinkTeamNode;
import net.officefloor.compile.internal.structure.ManagedObjectNode;
import net.officefloor.compile.internal.structure.Node;
import net.officefloor.compile.internal.structure.NodeContext;
import net.officefloor.compile.internal.structure.OfficeNode;
import net.officefloor.compile.internal.structure.OfficeObjectNode;
import net.officefloor.compile.internal.structure.OfficeTeamNode;
import net.officefloor.compile.properties.PropertyList;
import net.officefloor.compile.section.TypeQualification;
import net.officefloor.compile.spi.governance.source.GovernanceSource;
import net.officefloor.compile.spi.office.GovernerableManagedObject;
import net.officefloor.compile.spi.office.OfficeGovernance;
import net.officefloor.frame.api.build.GovernanceBuilder;
import net.officefloor.frame.api.build.OfficeBuilder;
import net.officefloor.frame.api.governance.Governance;
import net.officefloor.frame.api.manage.Office;

/**
 * Implementation of the {@link GovernanceNode}.
 * 
 * @author Daniel Sagenschneider
 */
public class GovernanceNodeImpl implements GovernanceNode {

	/**
	 * Name of this {@link OfficeGovernance}.
	 */
	private final String governanceName;

	/**
	 * {@link PropertyList} to source the {@link Governance}.
	 */
	private final PropertyList properties;

	/**
	 * {@link OfficeNode} of the {@link Office} containing this
	 * {@link Governance}.
	 */
	private final OfficeNode officeNode;

	/**
	 * {@link NodeContext}.
	 */
	private final NodeContext context;

	/**
	 * Initialised state.
	 */
	private InitialisedState state;

	/**
	 * Initialised state.
	 */
	private static class InitialisedState {

		/**
		 * Class name of the {@link GovernanceSource}.
		 */
		private final String governanceSourceClassName;

		/**
		 * {@link GovernanceSource} instance to use. Should this be specified it
		 * overrides the {@link Class}.
		 */
		private final GovernanceSource<?, ?> governanceSource;

		/**
		 * Instantiate.
		 * 
		 * @param governanceSourceClassName
		 *            Class name of the {@link GovernanceSource}.
		 * @param governanceSource
		 *            {@link GovernanceSource} instance to use. Should this be
		 *            specified it overrides the {@link Class}.
		 */
		public InitialisedState(String governanceSourceClassName, GovernanceSource<?, ?> governanceSource) {
			this.governanceSourceClassName = governanceSourceClassName;
			this.governanceSource = governanceSource;
		}
	}

	/**
	 * {@link OfficeObjectNode} instances being governed by this
	 * {@link Governance}.
	 */
	private final List<OfficeObjectNode> governedOfficeObjects = new LinkedList<>();

	/**
	 * {@link ManagedObjectNode} instances being governed by this
	 * {@link Governance}.
	 */
	private final List<ManagedObjectNode> governedManagedObjects = new LinkedList<>();

	/**
	 * {@link GovernanceSource} used to source this {@link GovernanceNode}.
	 */
	private GovernanceSource<?, ?> usedGovernanceSource = null;

	/**
	 * Initiate.
	 * 
	 * @param governanceName
	 *            Name of this {@link OfficeGovernance}.
	 * @param officeNode
	 *            {@link OfficeNode} of the {@link Office} containing this
	 *            {@link Governance}.
	 * @param context
	 *            {@link NodeContext}.
	 */
	public GovernanceNodeImpl(String governanceName, OfficeNode officeNode, NodeContext context) {
		this.governanceName = governanceName;
		this.officeNode = officeNode;
		this.context = context;

		// Create additional objects
		this.properties = this.context.createPropertyList();
	}

	/*
	 * ========================== Node ==============================
	 */

	@Override
	public String getNodeName() {
		return this.governanceName;
	}

	@Override
	public String getNodeType() {
		return TYPE;
	}

	@Override
	public String getLocation() {
		return null;
	}

	@Override
	public Node getParentNode() {
		return this.officeNode;
	}

	@Override
	public Node[] getChildNodes() {
		return NodeUtil.getChildNodes();
	}

	@Override
	public boolean isInitialised() {
		return (this.state != null);
	}

	@Override
	public void initialise(String governanceSourceClassName, GovernanceSource<?, ?> governanceSource) {
		this.state = NodeUtil.initialise(this, this.context, this.state,
				() -> new InitialisedState(governanceSourceClassName, governanceSource));
	}

	/*
	 * ======================== GovernanceNode ======================
	 */

	@Override
	public GovernanceType<?, ?> loadGovernanceType() {

		// Load the override properties
		PropertyList overrideProperties = this.context.overrideProperties(this,
				this.officeNode.getQualifiedName(this.governanceName), this.properties);

		// Create the governance loader
		GovernanceLoader loader = this.context.getGovernanceLoader(this);

		// Obtain the goverannce source
		GovernanceSource<?, ?> governanceSource = this.state.governanceSource;
		if (governanceSource == null) {

			// Obtain the governance source class
			Class<? extends GovernanceSource<?, ?>> governanceSourceClass = this.context
					.getGovernanceSourceClass(this.state.governanceSourceClassName, this);
			if (governanceSourceClass == null) {
				return null; // must obtain source class
			}

			// Obtain the governance source
			governanceSource = CompileUtil.newInstance(governanceSourceClass, GovernanceSource.class, this,
					this.context.getCompilerIssues());
			if (governanceSource == null) {
				return null; // must obtain source
			}
		}

		// Keep track of used governance source
		this.usedGovernanceSource = governanceSource;

		// Load and return the governance type
		return loader.loadGovernanceType(governanceSource, overrideProperties);
	}

	@Override
	public void autoWireTeam(AutoWirer<LinkTeamNode> autoWirer, CompileContext compileContext) {

		// Ignore if already specified team
		if (this.linkedTeamNode != null) {
			return;
		}

		// Create the listing of source auto-wires
		List<AutoWire> autoWires = new ArrayList<>();
		this.governedOfficeObjects.stream()
				.sorted((a, b) -> CompileUtil.sortCompare(a.getOfficeObjectName(), b.getOfficeObjectName()))
				.forEachOrdered((object) -> autoWires
						.add(new AutoWire(object.getTypeQualifier(), object.getOfficeObjectType())));
		this.governedManagedObjects.stream().sorted((a, b) -> CompileUtil
				.sortCompare(a.getGovernerableManagedObjectName(), b.getGovernerableManagedObjectName()))
				.forEachOrdered((managedObject) -> {
					TypeQualification[] qualifications = managedObject.getTypeQualifications(compileContext);
					Arrays.stream(qualifications).forEach((qualification) -> autoWires
							.add(new AutoWire(qualification.getQualifier(), qualification.getType())));
				});
		AutoWire[] sourceAutoWires = autoWires.stream().toArray(AutoWire[]::new);

		// Attempt to auto-wire this governance
		AutoWireLink<LinkTeamNode>[] links = autoWirer.findAutoWireLinks(this, sourceAutoWires);
		if (links.length == 1) {
			LinkUtil.linkTeamNode(this, links[0].getTargetNode(), this.context.getCompilerIssues(),
					(link) -> this.linkTeamNode(link));
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void buildGovernance(OfficeBuilder officeBuilder, CompileContext compileContext) {

		// Obtain the governance type
		GovernanceType govType = compileContext.getOrLoadGovernanceType(this);
		if (govType == null) {
			return; // must obtain governance type
		}

		// Register as possible MBean
		String qualifiedName = this.officeNode.getQualifiedName(this.governanceName);
		compileContext.registerPossibleMBean(GovernanceSource.class, qualifiedName, this.usedGovernanceSource);

		// Build the governance
		GovernanceBuilder govBuilder = officeBuilder.addGovernance(this.governanceName, govType.getExtensionInterface(),
				govType.getGovernanceFactory());

		// Obtain the office team responsible for this governance
		OfficeTeamNode officeTeam = LinkUtil.findTarget(this, OfficeTeamNode.class, this.context.getCompilerIssues());
		if (officeTeam != null) {
			// Build the team responsible for the governance
			govBuilder.setResponsibleTeam(officeTeam.getOfficeTeamName());
		}
	}

	/*
	 * ======================== OfficeGovernance ======================
	 */

	@Override
	public String getOfficeGovernanceName() {
		return this.governanceName;
	}

	@Override
	public void addProperty(String name, String value) {
		this.properties.addProperty(name).setValue(value);
	}

	@Override
	public void governManagedObject(GovernerableManagedObject managedObject) {

		// Register governance with the managed object
		if (managedObject instanceof ManagedObjectNode) {
			// Register governance with the managed object node
			ManagedObjectNode managedObjectNode = (ManagedObjectNode) managedObject;
			managedObjectNode.addGovernance(this, this.officeNode);
			this.governedManagedObjects.add(managedObjectNode);

		} else if (managedObject instanceof OfficeObjectNode) {
			// Register governance with the office object node
			OfficeObjectNode officeObjectNode = (OfficeObjectNode) managedObject;
			officeObjectNode.addGovernance(this);
			this.governedOfficeObjects.add(officeObjectNode);

		} else {
			// Unknown governable managed object node
			this.context.getCompilerIssues().addIssue(this,
					"Unknown " + GovernerableManagedObject.class.getSimpleName() + " node");
		}
	}

	/*
	 * ========================== LinkTeamNode ========================
	 */

	/**
	 * Linked {@link LinkTeamNode}.
	 */
	private LinkTeamNode linkedTeamNode = null;

	@Override
	public boolean linkTeamNode(LinkTeamNode node) {
		return LinkUtil.linkTeamNode(this, node, this.context.getCompilerIssues(),
				(link) -> this.linkedTeamNode = link);
	}

	@Override
	public LinkTeamNode getLinkedTeamNode() {
		return this.linkedTeamNode;
	}

}