package net.officefloor.gef.editor;

import org.eclipse.gef.fx.nodes.GeometryNode;
import org.eclipse.gef.geometry.planar.Ellipse;
import org.eclipse.gef.geometry.planar.Point;
import org.eclipse.gef.geometry.planar.Polygon;

import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Default {@link GeometryNode} instances for {@link AdaptedConnector}
 * instances.
 * 
 * @author Daniel Sagenschneider
 */
public class DefaultConnectors {

	/**
	 * {@link AdaptedConnectorVisualFactory} for flow connector.
	 */
	public static AdaptedConnectorVisualFactory<Region> FLOW = (context) -> {
		final double X_LEFT = 0;
		final double X_STEM = 5;
		final double X_TIP = 9;
		final double Y_TOP = 0;
		final double Y_BOTTOM = 10;
		final double Y_STEM_INSET = 2;
		final double Y_STEM_TOP = Y_TOP + Y_STEM_INSET;
		final double Y_STEM_BOTTOM = Y_BOTTOM - Y_STEM_INSET;
		final double Y_TIP = (Y_BOTTOM - Y_TOP) / 2;
		GeometryNode<Polygon> node = new GeometryNode<>(new Polygon(new Point(X_LEFT, Y_STEM_TOP),
				new Point(X_STEM, Y_STEM_TOP), new Point(X_STEM, Y_TOP), new Point(X_TIP, Y_TIP),
				new Point(X_STEM, Y_BOTTOM), new Point(X_STEM, Y_STEM_BOTTOM), new Point(X_LEFT, Y_STEM_BOTTOM)));
		node.setFill(Color.BLACK);
		return node;
	};

	/**
	 * {@link AdaptedConnectorVisualFactory} for object connector.
	 */
	public static AdaptedConnectorVisualFactory<Region> OBJECT = (context) -> {
		GeometryNode<Ellipse> node = new GeometryNode<Ellipse>(new Ellipse(0, 0, 6, 6));
		node.setFill(Color.BLACK);
		return node;
	};

	/**
	 * TODO provide specific connector.
	 * 
	 * {@link AdaptedConnectorVisualFactory} for team connector.
	 */
	public static AdaptedConnectorVisualFactory<Region> TEAM = FLOW;

	/**
	 * {@link AdaptedConnectorVisualFactory} for derivative connector.
	 */
	public static AdaptedConnectorVisualFactory<Region> DERIVE = (context) -> {
		final double Y_TOP = 0;
		final double Y_STEM = 5;
		final double Y_BOTTOM = 9;
		final double X_LEFT = 0;
		final double X_RIGHT = 10;
		final double X_STEM_INSET = 2;
		final double X_STEM_LEFT = X_LEFT + X_STEM_INSET;
		final double X_STEM_RIGHT = X_RIGHT - X_STEM_INSET;
		final double X_TIP = (X_RIGHT - X_LEFT) / 2;
		GeometryNode<Polygon> node = new GeometryNode<>(new Polygon(new Point(X_STEM_LEFT, Y_BOTTOM),
				new Point(X_STEM_LEFT, Y_STEM), new Point(X_LEFT, Y_STEM), new Point(X_TIP, Y_TOP),
				new Point(X_RIGHT, Y_STEM), new Point(X_STEM_RIGHT, Y_STEM), new Point(X_STEM_RIGHT, Y_BOTTOM)));
		node.setFill(Color.LIGHTGRAY);
		return node;
	};

	/**
	 * All access via static methods.
	 */
	private DefaultConnectors() {
	}

}