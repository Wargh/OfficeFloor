package net.officefloor.compile.impl.structure;

import net.officefloor.compile.impl.util.LinkUtil;
import net.officefloor.compile.internal.structure.EscalationNode;
import net.officefloor.compile.internal.structure.LinkFlowNode;
import net.officefloor.compile.internal.structure.Node;
import net.officefloor.compile.internal.structure.NodeContext;
import net.officefloor.compile.internal.structure.OfficeNode;
import net.officefloor.frame.api.escalate.Escalation;

/**
 * {@link EscalationNode} implementation.
 * 
 * @author Daniel Sagenschneider
 */
public class EscalationNodeImpl implements EscalationNode {

	/**
	 * {@link Escalation} type.
	 */
	private final String escalationType;

	/**
	 * Parent {@link OfficeNode}.
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
	}

	/**
	 * Initiate.
	 * 
	 * @param escalationType
	 *            {@link Escalation} type.
	 * @param officeNode
	 *            Parent {@link OfficeNode}.
	 * @param context
	 *            {@link NodeContext}.
	 */
	public EscalationNodeImpl(String escalationType, OfficeNode officeNode, NodeContext context) {
		this.escalationType = escalationType;
		this.officeNode = officeNode;
		this.context = context;
	}

	/*
	 * ======================== Node =======================
	 */

	@Override
	public String getNodeName() {
		return this.escalationType;
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
	public void initialise() {
		this.state = NodeUtil.initialise(this, this.context, this.state, () -> new InitialisedState());
	}

	/*
	 * ======================== OfficeEscalation =======================
	 */

	@Override
	public String getOfficeEscalationType() {
		return this.escalationType;
	}

	/*
	 * ======================== LinkFlowNode =======================
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

}