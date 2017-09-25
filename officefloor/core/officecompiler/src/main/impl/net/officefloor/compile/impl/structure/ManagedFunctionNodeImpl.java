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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.officefloor.compile.impl.office.OfficeFunctionTypeImpl;
import net.officefloor.compile.impl.util.CompileUtil;
import net.officefloor.compile.impl.util.LinkUtil;
import net.officefloor.compile.internal.structure.AdministrationNode;
import net.officefloor.compile.internal.structure.AutoWire;
import net.officefloor.compile.internal.structure.AutoWireLink;
import net.officefloor.compile.internal.structure.AutoWirer;
import net.officefloor.compile.internal.structure.BoundManagedObjectNode;
import net.officefloor.compile.internal.structure.CompileContext;
import net.officefloor.compile.internal.structure.FunctionFlowNode;
import net.officefloor.compile.internal.structure.FunctionNamespaceNode;
import net.officefloor.compile.internal.structure.FunctionObjectNode;
import net.officefloor.compile.internal.structure.GovernanceNode;
import net.officefloor.compile.internal.structure.LinkFlowNode;
import net.officefloor.compile.internal.structure.LinkTeamNode;
import net.officefloor.compile.internal.structure.ManagedFunctionNode;
import net.officefloor.compile.internal.structure.ManagedFunctionVisitor;
import net.officefloor.compile.internal.structure.Node;
import net.officefloor.compile.internal.structure.NodeContext;
import net.officefloor.compile.internal.structure.OfficeNode;
import net.officefloor.compile.internal.structure.OfficeTeamNode;
import net.officefloor.compile.internal.structure.ResponsibleTeamNode;
import net.officefloor.compile.internal.structure.SectionNode;
import net.officefloor.compile.internal.structure.SectionOutputNode;
import net.officefloor.compile.managedfunction.FunctionNamespaceType;
import net.officefloor.compile.managedfunction.ManagedFunctionEscalationType;
import net.officefloor.compile.managedfunction.ManagedFunctionFlowType;
import net.officefloor.compile.managedfunction.ManagedFunctionObjectType;
import net.officefloor.compile.managedfunction.ManagedFunctionType;
import net.officefloor.compile.object.ObjectDependencyType;
import net.officefloor.compile.section.OfficeFunctionType;
import net.officefloor.compile.section.OfficeSubSectionType;
import net.officefloor.compile.spi.office.AugmentedFunctionObject;
import net.officefloor.compile.spi.office.OfficeAdministration;
import net.officefloor.compile.spi.office.OfficeGovernance;
import net.officefloor.compile.spi.office.OfficeSectionFunction;
import net.officefloor.compile.spi.office.ResponsibleTeam;
import net.officefloor.compile.spi.section.FunctionFlow;
import net.officefloor.compile.spi.section.FunctionObject;
import net.officefloor.compile.spi.section.SectionFunction;
import net.officefloor.frame.api.build.ManagedFunctionBuilder;
import net.officefloor.frame.api.build.OfficeBuilder;
import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.api.function.ManagedFunctionFactory;
import net.officefloor.frame.api.governance.Governance;

/**
 * {@link ManagedFunctionNode} implementation.
 * 
 * @author Daniel Sagenschneider
 */
public class ManagedFunctionNodeImpl implements ManagedFunctionNode {

	/**
	 * Name of this {@link SectionFunction}.
	 */
	private final String functionName;

	/**
	 * {@link NodeContext}.
	 */
	private final NodeContext context;

	/**
	 * {@link InitialisedState} for this {@link ManagedFunctionNode}.
	 */
	private InitialisedState state;

	/**
	 * Initialised state of the {@link ManagedFunctionNode}.
	 */
	private class InitialisedState {

		/**
		 * Name of the {@link ManagedFunctionType} for this
		 * {@link SectionFunction}.
		 */
		private final String functionTypeName;

		/**
		 * {@link FunctionNamespaceNode} containing this
		 * {@link ManagedFunctionNode}.
		 */
		private final FunctionNamespaceNode namespaceNode;

		/**
		 * Initialise the state.
		 * 
		 * @param functionTypeName
		 *            Name of the {@link ManagedFunctionType} for this
		 *            {@link SectionFunction}.
		 * @param namespaceNode
		 *            {@link FunctionNamespaceNode} containing this
		 *            {@link ManagedFunctionNode}.
		 */
		private InitialisedState(String functionTypeName, FunctionNamespaceNode namespaceNode) {
			this.functionTypeName = functionTypeName;
			this.namespaceNode = namespaceNode;
		}
	}

	/**
	 * {@link FunctionFlowNode} instances by their {@link FunctionFlow} names.
	 */
	private final Map<String, FunctionFlowNode> functionFlows = new HashMap<String, FunctionFlowNode>();

	/**
	 * {@link FunctionObjectNode} instances by their {@link FunctionObject}
	 * names.
	 */
	private final Map<String, FunctionObjectNode> functionObjects = new HashMap<String, FunctionObjectNode>();

	/**
	 * {@link FunctionFlowNode} instances by their {@link FunctionFlow} names.
	 */
	private final Map<String, FunctionFlowNode> functionEscalations = new HashMap<String, FunctionFlowNode>();

	/**
	 * Listing of {@link OfficeAdministration} instances to do before this
	 * {@link OfficeSectionFunction}.
	 */
	private final List<AdministrationNode> preFunctionAdministration = new LinkedList<AdministrationNode>();

	/**
	 * Listing of {@link OfficeAdministration} instances to do after this
	 * {@link OfficeSectionFunction}.
	 */
	private final List<AdministrationNode> postFunctionAdministration = new LinkedList<AdministrationNode>();

	/**
	 * Listing of {@link OfficeGovernance} instances providing
	 * {@link Governance} over this {@link ManagedFunction}.
	 */
	private final List<GovernanceNode> governances = new LinkedList<GovernanceNode>();

	/**
	 * {@link ResponsibleTeam} responsible for this
	 * {@link OfficeSectionFunction}.
	 */
	private final ResponsibleTeamNode teamResponsible;

	/**
	 * Initiate.
	 * 
	 * @param functionName
	 *            Name of this {@link SectionFunction}.
	 * @param context
	 *            {@link NodeContext}.
	 */
	public ManagedFunctionNodeImpl(String functionName, NodeContext context) {
		this.functionName = functionName;
		this.context = context;

		// Create additional objects
		this.teamResponsible = this.context.createResponsibleTeamNode("Team for function " + this.functionName, this);
	}

	/**
	 * Obtains the {@link FunctionObjectNode}.
	 * 
	 * @param functionObjectName
	 *            Name of the {@link FunctionObjectNode}.
	 * @return {@link FunctionObjectNode}.
	 */
	private FunctionObjectNode getFunctionObjectNode(String functionObjectName) {
		return NodeUtil.getNode(functionObjectName, this.functionObjects,
				() -> this.context.createFunctionObjectNode(functionObjectName, this));
	}

	/*
	 * ========================== Node ===================================
	 */

	@Override
	public String getNodeName() {
		return this.functionName;
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
		return (this.state != null ? this.state.namespaceNode : null);
	}

	@Override
	public Node[] getChildNodes() {
		return NodeUtil.getChildNodes(this.functionFlows, this.functionEscalations, this.functionObjects);
	}

	@Override
	public boolean isInitialised() {
		return (this.state != null);
	}

	@Override
	public void initialise(String managedFunctionTypeName, FunctionNamespaceNode namespaceNode) {
		this.state = NodeUtil.initialise(this, this.context, this.state,
				() -> new InitialisedState(managedFunctionTypeName, namespaceNode));
	}

	/*
	 * ========================== ManagedFunctionNode ==========================
	 */

	@Override
	public String getQualifiedFunctionName() {
		return this.state.namespaceNode.getSectionNode().getSectionQualifiedName(this.functionName);
	}

	@Override
	public FunctionNamespaceNode getFunctionNamespaceNode() {
		return (this.state != null ? this.state.namespaceNode : null);
	}

	@Override
	public AugmentedFunctionObject getAugmentedFunctionObject(String objectName) {
		return this.getFunctionObjectNode(objectName);
	}

	@Override
	public ManagedFunctionType<?, ?> loadManagedFunctionType(CompileContext compileContext) {

		// Ensure initialised
		if (this.state == null) {
			this.context.getCompilerIssues().addIssue(this,
					"Can not load type as " + TYPE + " " + this.functionName + " is not initialised");
		}

		// Obtain the namespace type
		FunctionNamespaceType namespaceType = compileContext.getOrLoadFunctionNamespaceType(this.state.namespaceNode);
		if (namespaceType == null) {
			return null;
		}

		// Find the managed function type for this managed function node
		for (ManagedFunctionType<?, ?> type : namespaceType.getManagedFunctionTypes()) {
			if (this.state.functionTypeName.equals(type.getFunctionName())) {
				// Found the type for this managed function
				return type;
			}
		}

		// As here, did not find corresponding managed function type
		this.context.getCompilerIssues().addIssue(this, "Can not find function '" + this.state.functionTypeName
				+ "' on namespace " + this.state.namespaceNode.getSectionFunctionNamespaceName());
		return null;
	}

	@Override
	public OfficeFunctionType loadOfficeFunctionType(OfficeSubSectionType parentSubSectionType,
			CompileContext compileContext) {

		// Load the function type
		ManagedFunctionType<?, ?> functionType = this.loadManagedFunctionType(compileContext);
		if (functionType == null) {
			return null;
		}

		// Load the object dependencies
		ObjectDependencyType[] dependencies = CompileUtil.loadTypes(this.functionObjects,
				(object) -> object.getFunctionObjectName(), (object) -> object.loadObjectDependencyType(compileContext),
				ObjectDependencyType[]::new);
		if (dependencies == null) {
			return null;
		}

		// Create and return the type
		return new OfficeFunctionTypeImpl(this.functionName, parentSubSectionType, dependencies);
	}

	@Override
	public boolean souceManagedFunction(ManagedFunctionVisitor visitor, CompileContext compileContext) {

		// Obtain the type for this function
		ManagedFunctionType<?, ?> functionType = this.loadManagedFunctionType(compileContext);
		if (functionType == null) {
			return false; // must have type
		}

		// Initialise the flows
		for (ManagedFunctionFlowType<?> flowType : functionType.getFlowTypes()) {
			String flowName = flowType.getFlowName();
			NodeUtil.getInitialisedNode(flowName, this.functionFlows, this.context,
					() -> this.context.createFunctionFlowNode(flowName, false, this), (flow) -> flow.initialise());
		}

		// Initialise the objects
		for (ManagedFunctionObjectType<?> objectType : functionType.getObjectTypes()) {
			String objectName = objectType.getObjectName();
			NodeUtil.getInitialisedNode(objectName, this.functionObjects, this.context,
					() -> this.context.createFunctionObjectNode(objectName, this), (object) -> object.initialise());
		}

		// Initialise the escalations
		for (ManagedFunctionEscalationType escalationType : functionType.getEscalationTypes()) {
			String escalationName = escalationType.getEscalationType().getName();
			NodeUtil.getInitialisedNode(escalationName, this.functionEscalations, this.context,
					() -> this.context.createFunctionFlowNode(escalationName, true, this),
					(escalation) -> escalation.initialise());
		}

		// Visit this managed function
		if (visitor != null) {
			visitor.visit(functionType, this, compileContext);
		}

		// Successfully sourced
		return true;
	}

	@Override
	public void autoWireManagedFunctionResponsibility(AutoWirer<LinkTeamNode> autoWirer,
			CompileContext compileContext) {

		// Create the listing of source auto wires
		Set<AutoWire> autoWires = new HashSet<>();
		this.functionObjects.values().forEach((object) -> LinkUtil.loadAllObjectAutoWires(object, autoWires,
				compileContext, this.context.getCompilerIssues()));
		AutoWire[] sourceAutoWires = autoWires.stream().toArray(AutoWire[]::new);

		// Obtain the office
		OfficeNode office = this.state.namespaceNode.getSectionNode().getOfficeNode();

		// Attempt to obtain the responsible team
		AutoWireLink<LinkTeamNode>[] links = autoWirer.findAutoWireLinks(this.teamResponsible, sourceAutoWires);
		if (links.length == 1) {
			LinkUtil.linkTeam(this.teamResponsible, links[0].getTargetNode(office), this.context.getCompilerIssues(),
					this);
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void buildManagedFunction(OfficeBuilder officeBuilder, CompileContext compileContext) {

		// Obtain the type for this function
		ManagedFunctionType<?, ?> functionType = this.loadManagedFunctionType(compileContext);
		if (functionType == null) {
			return; // must have type
		}

		// Ensure the managed function source possibly registered as MBean
		this.state.namespaceNode.registerAsPossibleMbean(compileContext);

		// Obtain the name of the function
		String qualifiedFunctionName = this.getQualifiedFunctionName();

		// Obtain the office team for the function
		OfficeTeamNode officeTeam = LinkUtil.findTarget(this.teamResponsible, OfficeTeamNode.class,
				this.context.getCompilerIssues());

		// Build the function
		ManagedFunctionFactory functionFactory = functionType.getManagedFunctionFactory();
		ManagedFunctionBuilder functionBuilder = officeBuilder.addManagedFunction(qualifiedFunctionName,
				functionFactory);
		if (officeTeam != null) {
			functionBuilder.setResponsibleTeam(officeTeam.getOfficeTeamName());
		}

		// Add annotations (if available)
		Object[] annotations = functionType.getAnnotations();
		if (annotations != null) {
			for (Object annotation : annotations) {
				functionBuilder.addAnnotation(annotation);
			}
		}

		// Build the flows
		ManagedFunctionFlowType<?>[] flowTypes = functionType.getFlowTypes();
		for (int flowIndex = 0; flowIndex < flowTypes.length; flowIndex++) {
			ManagedFunctionFlowType<?> flowType = flowTypes[flowIndex];

			// Obtain type details for linking
			String flowName = flowType.getFlowName();
			Enum<?> flowKey = flowType.getKey();
			Class<?> argumentType = flowType.getArgumentType();

			// Obtain the linked function for the flow
			FunctionFlowNode flowNode = this.functionFlows.get(flowName);
			if (flowNode == null) {
				this.context.getCompilerIssues().addIssue(this,
						"Flow " + flowName + " is not linked to a " + ManagedFunctionNode.class.getSimpleName());
				continue; // must have linked task
			}
			ManagedFunctionNode linkedFunction = LinkUtil.retrieveTarget(flowNode, ManagedFunctionNode.class,
					this.context.getCompilerIssues());
			if (linkedFunction == null) {
				continue; // must have linked function
			}

			// Obtain configured details for linking
			String linkedFunctionName = linkedFunction.getQualifiedFunctionName();
			boolean isSpawnThreadState = flowNode.isSpawnThreadState();

			// Link the function
			if (flowKey != null) {
				functionBuilder.linkFlow(flowKey, linkedFunctionName, argumentType, isSpawnThreadState);
			} else {
				functionBuilder.linkFlow(flowIndex, linkedFunctionName, argumentType, isSpawnThreadState);
			}
		}

		// Build the next function
		if (this.linkedFlowNode != null) {
			// Have next function so link to it
			ManagedFunctionNode nextFunction = LinkUtil.retrieveTarget(this, ManagedFunctionNode.class,
					this.context.getCompilerIssues());
			if (nextFunction != null) {

				// Obtain next details for linking
				String nextFunctionName = nextFunction.getQualifiedFunctionName();
				Class<?> argumentType = functionType.getReturnType();

				// Link to next function
				functionBuilder.setNextFunction(nextFunctionName, argumentType);
			}
		}

		// Build the objects
		ManagedFunctionObjectType<?>[] objectTypes = functionType.getObjectTypes();
		for (int objectIndex = 0; objectIndex < objectTypes.length; objectIndex++) {
			ManagedFunctionObjectType<?> objectType = objectTypes[objectIndex];

			// Obtain the type details for linking
			String objectName = objectType.getObjectName();
			Enum<?> objectKey = objectType.getKey();
			Class<?> objectClass = objectType.getObjectType();

			// Obtain the object node for the function object
			FunctionObjectNode objectNode = this.functionObjects.get(objectName);
			if (objectNode == null) {
				this.context.getCompilerIssues().addIssue(this,
						"Object " + objectName + " is not linked to a " + BoundManagedObjectNode.class.getSimpleName());
				continue;
			}

			// Determine if the object is a parameter
			if ((objectNode != null) && (objectNode.isParameter())) {
				// Link as parameter
				if (objectKey != null) {
					functionBuilder.linkParameter(objectKey, objectClass);
				} else {
					functionBuilder.linkParameter(objectIndex, objectClass);
				}
				continue; // linked as a parameter
			}

			// Obtain the managed object for the object
			BoundManagedObjectNode linkedManagedObject = LinkUtil.retrieveTarget(objectNode,
					BoundManagedObjectNode.class, this.context.getCompilerIssues());
			if (linkedManagedObject == null) {
				continue; // must have linked managed object
			}

			// Link function object to managed object
			String linkedManagedObjectName = linkedManagedObject.getBoundManagedObjectName();
			if (objectKey != null) {
				functionBuilder.linkManagedObject(objectKey, linkedManagedObjectName, objectClass);
			} else {
				functionBuilder.linkManagedObject(objectIndex, linkedManagedObjectName, objectClass);
			}
		}

		// Build the escalations
		ManagedFunctionEscalationType[] escalationTypes = functionType.getEscalationTypes();
		for (int i = 0; i < escalationTypes.length; i++) {
			ManagedFunctionEscalationType escalationType = escalationTypes[i];

			// Obtain the type details for linking
			Class<? extends Throwable> escalationClass = escalationType.getEscalationType();
			String escalationName = escalationClass.getName();

			// Obtain the linked function for the escalation
			FunctionFlowNode escalationNode = this.functionEscalations.get(escalationName);
			if (escalationNode == null) {
				this.context.getCompilerIssues().addIssue(this,
						"Escalation " + escalationName + " not handled by a Function nor propagated to the Office");
				continue;
			}

			// Link escalation
			ManagedFunctionNode linkedFunction = LinkUtil.findTarget(escalationNode, ManagedFunctionNode.class,
					this.context.getCompilerIssues());
			if (linkedFunction != null) {
				// Obtain the configuration details for linking
				String linkedFunctionName = linkedFunction.getQualifiedFunctionName();

				// Link to function
				functionBuilder.addEscalation(escalationClass, linkedFunctionName);

			} else {
				// Ensure the escalation is propagated to the office
				boolean isEscalatedToOffice = false;
				SectionOutputNode sectionOutputNode = LinkUtil.findFurtherestTarget(escalationNode,
						SectionOutputNode.class, this.context.getCompilerIssues());
				if (sectionOutputNode != null) {
					// Determine if object of top level section (the office)
					SectionNode sectionNode = sectionOutputNode.getSectionNode();
					isEscalatedToOffice = (sectionNode.getParentSectionNode() == null);
				}
				if (!isEscalatedToOffice) {
					// Escalation must be propagated to the office
					this.context.getCompilerIssues().addIssue(this, "Escalation " + escalationClass.getName()
							+ " not handled by a Function nor propagated to the Office");
				}
			}
		}

		// Build the pre function administration
		for (AdministrationNode preAdmin : this.preFunctionAdministration) {
			preAdmin.buildPreFunctionAdministration(functionBuilder, compileContext);
		}

		// Build the post function administration
		for (AdministrationNode postAdmin : this.postFunctionAdministration) {
			postAdmin.buildPostFunctionAdministration(functionBuilder, compileContext);
		}

		// Build the governance (first inherited then specific for function)
		SectionNode section = this.state.namespaceNode.getSectionNode();
		GovernanceNode[] sectionGovernances = section.getGoverningGovernances();
		for (GovernanceNode governance : sectionGovernances) {
			functionBuilder.addGovernance(governance.getOfficeGovernanceName());
		}
		for (GovernanceNode governance : this.governances) {
			functionBuilder.addGovernance(governance.getOfficeGovernanceName());
		}
	}

	/*
	 * ====================== SectionFunction =============================
	 */

	@Override
	public String getSectionFunctionName() {
		return this.functionName;
	}

	@Override
	public FunctionFlow getFunctionFlow(String functionFlowName) {
		return NodeUtil.getNode(functionFlowName, this.functionFlows,
				() -> this.context.createFunctionFlowNode(functionFlowName, false, this));
	}

	@Override
	public FunctionObject getFunctionObject(String functionObjectName) {
		return this.getFunctionObjectNode(functionObjectName);
	}

	@Override
	public FunctionFlow getFunctionEscalation(String functionEscalationName) {
		return NodeUtil.getNode(functionEscalationName, this.functionEscalations,
				() -> this.context.createFunctionFlowNode(functionEscalationName, true, this));
	}

	/*
	 * ==================== LinkFlowNode ==================================
	 */

	/**
	 * Linked {@link LinkFlowNode}.
	 */
	private LinkFlowNode linkedFlowNode;

	@Override
	public boolean linkFlowNode(LinkFlowNode node) {
		return LinkUtil.linkFlowNode(this, node, this.context.getCompilerIssues(),
				(link) -> this.linkedFlowNode = link);
	}

	@Override
	public LinkFlowNode getLinkedFlowNode() {
		return this.linkedFlowNode;
	}

	/*
	 * ===================== OfficeSectionFunction =====================
	 */

	@Override
	public String getOfficeFunctionName() {
		return this.functionName;
	}

	@Override
	public ResponsibleTeam getResponsibleTeam() {
		return this.teamResponsible;
	}

	@Override
	public void addPreAdministration(OfficeAdministration administration) {

		// Ensure administration node
		if (!(administration instanceof AdministrationNode)) {
			this.context.getCompilerIssues().addIssue(this, "Invalid administration: " + administration + " ["
					+ (administration == null ? null : administration.getClass().getName()) + "]");
			return; // can not add administration
		}
		AdministrationNode adminNode = (AdministrationNode) administration;

		// Add the pre function administration
		this.preFunctionAdministration.add(adminNode);
	}

	@Override
	public void addPostAdministration(OfficeAdministration administration) {

		// Ensure administration node
		if (!(administration instanceof AdministrationNode)) {
			this.context.getCompilerIssues().addIssue(this, "Invalid administration: " + administration + " ["
					+ (administration == null ? null : administration.getClass().getName()) + "]");
			return; // can not add administration
		}
		AdministrationNode adminNode = (AdministrationNode) administration;

		// Add the post function administration
		this.postFunctionAdministration.add(adminNode);
	}

	@Override
	public void addGovernance(OfficeGovernance governance) {

		// Ensure governance node
		if (!(governance instanceof GovernanceNode)) {
			this.context.getCompilerIssues().addIssue(this, "Invalid governance: " + governance + " ["
					+ (governance == null ? null : governance.getClass().getName()) + "]");
			return; // can not add governance
		}
		GovernanceNode governanceNode = (GovernanceNode) governance;

		// Add the governance
		this.governances.add(governanceNode);
	}

}