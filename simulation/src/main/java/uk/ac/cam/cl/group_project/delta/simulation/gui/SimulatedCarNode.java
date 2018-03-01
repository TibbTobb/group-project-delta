package uk.ac.cam.cl.group_project.delta.simulation.gui;

import javafx.beans.property.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import uk.ac.cam.cl.group_project.delta.Log;
import uk.ac.cam.cl.group_project.delta.algorithm.Algorithm;
import uk.ac.cam.cl.group_project.delta.algorithm.AlgorithmData;
import uk.ac.cam.cl.group_project.delta.algorithm.CommsInterface;
import uk.ac.cam.cl.group_project.delta.algorithm.communications.Communications;
import uk.ac.cam.cl.group_project.delta.algorithm.communications.ControlLayer;
import uk.ac.cam.cl.group_project.delta.simulation.SimulatedCar;
import uk.ac.cam.cl.group_project.delta.simulation.SimulatedSensorModule;

import java.io.IOException;
import java.lang.reflect.Field;

public class SimulatedCarNode extends SimulatedBodyNode implements Paneable {

	/**
	 * Ratio of axle width to the wheel base.
	 */
	private static final double ASPECT_RATIO = 0.7;

	/**
	 * Ratio of car body width to axle width.
	 */
	private static final double BODY_WIDTH_RATIO = 0.95;

	/**
	 * Ratio of car body height to wheel base.
	 */
	private static final double BODY_HEIGHT_RATIO = 1.4;

	/**
	 * Opacity of circles drawn to present platoons.
	 */
	public static final double PLATOON_CIRCLE_OPACITY = 0.2;

	/**
	 * X-component of the car's velocity. Updated by a call to `update()`.
	 */
	private final DoubleProperty velX;

	/**
	 * Y-component of the car's velocity. Updated by a call to `update()`.
	 */
	private final DoubleProperty velY;

	/**
	 * Heading of the car. Updated by a call to `update()`.
	 */
	private final DoubleProperty heading;

	/**
	 * Angle of the car's front wheels. Updated by a call to `update()`.
	 */
	private final DoubleProperty wheelAngle;

	/**
	 * Car's engine power. Updated by a call to `update()`.
	 */
	private final DoubleProperty enginePower;

	/**
	 * Is this car the leader of its platoon?
	 */
	private final BooleanProperty isLeader;

	/**
	 * Is this car in emergency mode?
	 */
	//private final BooleanProperty isEmergency;

	/**
	 * This vehicle's ID.
	 */
	private final IntegerProperty vehicleId;

	/**
	 * The current platoon.
	 */
	private final IntegerProperty platoonId;

	/**
	 * The car's current position within its platoon.
	 */
	private final IntegerProperty platoonPosition;

	/**
	 * The ID of the leader of the car's platoon.
	 */
	private final IntegerProperty platoonLeaderId;

	/**
	 * The hashed colour of this platoon.
	 */
	private final ObjectProperty<Paint> platoonColour;

	private final DoubleProperty frontProximity;

	/**
	 * Construct a representation of the given car.
	 * @param car    Car to represent.
	 */
	public SimulatedCarNode(SimulatedCar car) {

		super(car);

		// Construct properties
		velX = new SimpleDoubleProperty(car.getVelocity().getX());
		velY = new SimpleDoubleProperty(car.getVelocity().getY());
		wheelAngle = new SimpleDoubleProperty(Math.toDegrees(-car.getWheelAngle()));
		heading = new SimpleDoubleProperty(Math.toDegrees(-car.getHeading()));
		enginePower = new SimpleDoubleProperty(car.getEnginePower());
		isLeader = new SimpleBooleanProperty(false);
		vehicleId = new SimpleIntegerProperty(0);
		platoonId = new SimpleIntegerProperty(0);
		platoonPosition = new SimpleIntegerProperty(0);
		platoonLeaderId = new SimpleIntegerProperty(0);
		platoonColour = new SimpleObjectProperty<>(Color.TRANSPARENT);
		frontProximity = new SimpleDoubleProperty(Double.POSITIVE_INFINITY);

		rotateProperty().bind(headingProperty());

		constructAlgorithmInstrumentation(car);
		constructSimpleVisualRepresentation(car);

	}

	/**
	 * Construct GUI representation of algorithm state from instrumentation.
	 * @param car    Car to instrument.
	 */
	private void constructAlgorithmInstrumentation(SimulatedCar car) {

		Algorithm controller = car.getController();

		// Setup properties
		isLeader.set(controller.isLeader());
		vehicleId.set(controller.getVehicleId());
		platoonId.set(controller.getPlatoonId());
		platoonPosition.set(controller.getPlatoonPosition());
		platoonLeaderId.set(controller.getLeaderId());
		platoonColour.set(toPaint(platoonId.get()));

		double base = car.getWheelBase() * Controller.UNITS_PER_METRE;

		// Create circles for platoon representation
		Circle leaderCircle = new Circle(base * 1.6);
		leaderCircle.setFill(Color.TRANSPARENT);
		leaderCircle.setStrokeWidth(base * 0.1);
		leaderCircle.setOpacity(PLATOON_CIRCLE_OPACITY);
		leaderCircle.setMouseTransparent(true);
		leaderCircle.strokeProperty().bind(platoonColour);
		leaderCircle.visibleProperty().bind(isLeader);

		Circle platoonCircle = new Circle(base * 1.5);
		platoonCircle.setMouseTransparent(true);
		platoonCircle.setOpacity(PLATOON_CIRCLE_OPACITY);
		platoonCircle.fillProperty().bind(platoonColour);

		getChildren().addAll(leaderCircle, platoonCircle);

		// Add tooltip for vehicle and platoon ID, and platoon position
		Tooltip tooltip = new Tooltip();
		tooltip.textProperty().bind(
			(new SimpleStringProperty("Vehicle ID: "))
				.concat(vehicleId.asString())
				.concat("\nPlatoon ID: ")
				.concat(platoonId.asString())
				.concat("\nPlatoon position: ")
				.concat(platoonPosition.asString())
		);
		Tooltip.install(this, tooltip);

	}

	/**
	 * Generates a {@link Paint} based on input. This function is designed to be
	 * determinate, but not to be continuous; this could be considered to be a
	 * hashing function.
	 * @param value    Value to convert to a colour.
	 * @return         Hash output, a colour.
	 */
	public static Paint toPaint(int value) {
		double h = (Integer.hashCode(value) * 360.0) / Integer.MAX_VALUE;
		return Color.hsb(h, 0.5, 0.7); // h = h % 360, 0 <= s <= 1, 0 <= b <= 1
	}

	/**
	 * Construct visual representation of car's physical state.
	 * @param car    Car to represent.
	 */
	private void constructSimpleVisualRepresentation(SimulatedCar car) {

		double length = car.getWheelBase() * Controller.UNITS_PER_METRE;
		double width = length * ASPECT_RATIO;
		double hw = width / 2.0;
		double hl = length / 2.0;

		Rectangle rect = makeRect(
			-hw * BODY_WIDTH_RATIO,
			-hl * BODY_HEIGHT_RATIO,
			width * BODY_WIDTH_RATIO,
			length * BODY_HEIGHT_RATIO
		);

		double wheelLength = length / 5.0;
		double wheelWidth = wheelLength / 3.0;

		Rectangle frontLeftWheel = makeRect(
			-hw,
			hl - 0.5 * wheelLength,
			wheelWidth,
			wheelLength
		);
		Rectangle frontRightWheel = makeRect(
			hw - wheelWidth,
			hl - 0.5 * wheelLength,
			wheelWidth,
			wheelLength
		);
		frontLeftWheel.rotateProperty().bind(wheelAngle);
		frontRightWheel.rotateProperty().bind(wheelAngle);

		final double VIEW_ARC_DISTANCE = length * 5;
		Arc viewArc = new Arc(
			0,
			0,
			VIEW_ARC_DISTANCE,
			VIEW_ARC_DISTANCE,
			-90 - Math.toDegrees(SimulatedSensorModule.VIEW_HALF_ANGLE),
			Math.toDegrees(SimulatedSensorModule.VIEW_HALF_ANGLE) * 2
		);
		viewArc.setFill(Color.TRANSPARENT);
		viewArc.setOpacity(0.5);
		viewArc.setStrokeWidth(2);
		viewArc.setStroke(new RadialGradient(
			0,
			0,
			0,
			0,
			VIEW_ARC_DISTANCE,
			false,
			CycleMethod.NO_CYCLE,
			new Stop(0, Color.GREY),
			new Stop(VIEW_ARC_DISTANCE, Color.TRANSPARENT)
		));
		viewArc.setType(ArcType.ROUND);
		viewArc.setMouseTransparent(true);

		Circle transformationCorrectionCircle = new Circle(VIEW_ARC_DISTANCE);
		transformationCorrectionCircle.setOpacity(0);
		transformationCorrectionCircle.setMouseTransparent(true);

		getChildren().addAll(
			transformationCorrectionCircle,
			viewArc,
			frontLeftWheel,
			frontRightWheel,
			makeRect(
				-hw,
				-hl - 0.5 * wheelLength,
				wheelWidth,
				wheelLength
			),
			makeRect(
				hw - wheelWidth,
				-hl - 0.5 * wheelLength,
				wheelWidth,
				wheelLength
			),
			rect
		);

	}

	/**
	 * Update the GUI representation of the simulated object by polling the
	 * simulation state.
	 */
	@Override
	public void update() {

		super.update();

		SimulatedCar car = getCar();
		synchronized (car) {

			velX.set(car.getVelocity().getX());
			velY.set(car.getVelocity().getY());
			heading.set(Math.toDegrees(-car.getHeading()));
			wheelAngle.set(Math.toDegrees(-car.getWheelAngle()));
			enginePower.set(car.getEnginePower());
			frontProximity.set(car.getSensorInterface().getFrontProximity());

			Algorithm controller = car.getController();
			isLeader.set(controller.isLeader());
			vehicleId.set(controller.getVehicleId());
			platoonId.set(controller.getPlatoonId());
			platoonPosition.set(controller.getPlatoonPosition());
			platoonLeaderId.set(controller.getLeaderId());
			platoonColour.set(toPaint(platoonId.get()));

		}

	}

	/**
	 * Fetch the car that this GUI element represents.
	 * @return    A {@link SimulatedCar}.
	 */
	public SimulatedCar getCar() {
		return (SimulatedCar) getBody();
	}

	/**
	 * Make a border-only rectangle - no fill and black borders.
	 * @param x    X-position.
	 * @param y    Y-position.
	 * @param w    Width.
	 * @param h    Height.
	 * @return     Constructed rectangle.
	 */
	private static Rectangle makeRect(double x, double y, double w, double h) {
		Rectangle rect = new Rectangle(x, y, w, h);
		rect.setFill(Color.WHITESMOKE);
		rect.setStroke(Color.BLACK);
		return rect;
	}

	/**
	 * Convert this object to a {@link Pane} for display in a properties panel.
	 * @return    GUI representation of this object.
	 */
	@Override
	public Pane toPane() {
		try {

			FXMLLoader loader = new FXMLLoader(getClass().getResource("car.properties.fxml"));
			Pane pane = loader.load();
			CarPropertiesController controller = loader.getController();

			controller.uuid.setText(Integer.toString(getCar().getUuid()));
			controller.controller.setText(
				getCar().getController().getClass().getSimpleName()
			);
			controller.positionX.textProperty().bind(
				posXProperty().divide(Controller.UNITS_PER_METRE).asString("%.2f")
			);
			controller.positionY.textProperty().bind(
				posYProperty().divide(Controller.UNITS_PER_METRE).asString("%.2f")
			);
			controller.heading.textProperty().bind(
				headingProperty().negate().asString("%.2f°")
			);
			controller.wheelAngle.textProperty().bind(
				wheelAngleProperty().negate().asString("%.2f°")
			);
			controller.enginePower.textProperty().bind(
				enginePowerProperty().asString("%.2f")
			);
			controller.velocityX.textProperty().bind(
				velXProperty().asString("%.2f")
			);
			controller.velocityY.textProperty().bind(
				velYProperty().asString("%.2f")
			);
			controller.vehicleId.textProperty().bind(
				vehicleId.asString()
			);
			controller.platoonId.textProperty().bind(
				platoonId.asString()
			);
			controller.platoonPosition.textProperty().bind(
				platoonPosition.asString()
			);
			controller.isLeader.textProperty().bind(
				isLeader.asString()
			);
			controller.platoonLeaderId.textProperty().bind(
				platoonLeaderId.asString()
			);
			controller.platoonColour.fillProperty().bind(
				platoonColour
			);
			controller.platoonColour.setOpacity(PLATOON_CIRCLE_OPACITY);

			controller.frontProximity.textProperty().bind(
				frontProximity.asString("%.2f")
			);

			return pane;

		}
		catch (IOException e) {
			return new Pane(new Text(e.getMessage()));
		}
	}

	public DoubleProperty velXProperty() {
		return velX;
	}

	public DoubleProperty velYProperty() {
		return velY;
	}

	public DoubleProperty headingProperty() {
		return heading;
	}

	public DoubleProperty wheelAngleProperty() {
		return wheelAngle;
	}

	public DoubleProperty enginePowerProperty() {
		return enginePower;
	}

	public DoubleProperty frontProximityProperty() {
		return frontProximity;
	}
}
