package uk.ac.cam.cl.group_project.delta.algorithm;

import uk.ac.cam.cl.group_project.delta.*;
import uk.ac.cam.cl.group_project.delta.algorithm.communications.Communications;
import uk.ac.cam.cl.group_project.delta.algorithm.communications.ControlLayer;

public abstract class Algorithm {

	public static final int ALGORITHM_LOOP_DURATION = 50000000; // 50ms
	public static final int MAXIMUM_MESSAGE_AGE = ALGORITHM_LOOP_DURATION*4; //200ms

	public AlgorithmData algorithmData = new AlgorithmData();
	protected FrontVehicleRoute frontVehicleRoute;

	protected Algorithm(DriveInterface driveInterface,
			SensorInterface sensorInterface,
			NetworkInterface networkInterface,
			BeaconInterface beacons,
			FrontVehicleRoute.RouteNumber routeNumber) {
		algorithmData.controlLayer = new ControlLayer(networkInterface, beacons);
		algorithmData.commsInterface = new Communications(algorithmData.controlLayer);
		algorithmData.driveInterface = driveInterface;
		algorithmData.sensorInterface = sensorInterface;
		frontVehicleRoute = new FrontVehicleRoute(algorithmData, ALGORITHM_LOOP_DURATION, routeNumber);
	}

	/**
	 * Default constructor, uses ROUTE_ZERO
	 */
	protected Algorithm(DriveInterface driveInterface,
						SensorInterface sensorInterface,
						NetworkInterface networkInterface,
						BeaconInterface beacons) {
		this(driveInterface,
				sensorInterface,
				networkInterface,
				beacons,
				FrontVehicleRoute.RouteNumber.ROUTE_ZERO);
	}

	/**
	 *Builds and returns algorithm of type specified by AlgorithmEnum input
	 */
	public static Algorithm createAlgorithm(
			AlgorithmEnum algorithmEnum,
			DriveInterface driveInterface,
			SensorInterface sensorInterface,
			NetworkInterface networkInterface,
			BeaconInterface beacons,
			FrontVehicleRoute.RouteNumber routeNumber) {
		switch (algorithmEnum) {
		case Naive1:
			return new NaiveAlgorithm1(driveInterface, sensorInterface, networkInterface, beacons, routeNumber);
		case Naive2:
			return new NaiveAlgorithm2(driveInterface, sensorInterface, networkInterface, beacons, routeNumber);
		case Naive3:
			return new NaiveAlgorithm3(driveInterface, sensorInterface, networkInterface, beacons, routeNumber);
		case Adaptive_Cruise_Control:
			return new ACC_Algorithm(driveInterface, sensorInterface, networkInterface, beacons, routeNumber);
		case Cooperative_Adaptive_Cruise_Control:
			return new CACC_Algorithm(driveInterface, sensorInterface, networkInterface, beacons, routeNumber);
		}
		return null;
	}

	/**
	 *Builds and returns algorithm of type specified by AlgorithmEnum input
	 *By default, uses ROUTE_ZERO, which makes the front vehicle do nothing
	 */
	public static Algorithm createAlgorithm(
			AlgorithmEnum algorithmEnum,
			DriveInterface driveInterface,
			SensorInterface sensorInterface,
			NetworkInterface networkInterface,
			BeaconInterface beacons) {
		return createAlgorithm(algorithmEnum,
				driveInterface,
				sensorInterface,
				networkInterface,
				beacons,
				FrontVehicleRoute.RouteNumber.ROUTE_ZERO);
	}

	public static AlgorithmEnum[] getAlgorithmList() {
		return AlgorithmEnum.values();
	}

	/** Sets an algorithms parameter.
	 *  Will do nothing if that algorithm does not have the parameter **/
	public abstract void setParameter(ParameterEnum parameterEnum, double value);

	/**
	 * @param parameterEnum enum for parameter
	 * @return if algorithm uses parameter then its value otherwise null
	 */
	public abstract Double getParameter(ParameterEnum parameterEnum);

	/**
	 * @return Array of all parameters this algorithm uses
	 */
	public abstract ParameterEnum[] getParameterList();

	public void initialise() {

	}

	private void readSensors() {
		// try to get predecessors messages, trying next car infront if message null, upto the front of platoon
		// note: leader check not needed as if leader then getPredecessorMessages() returns an empty list
		//uses timestamp in message to decide which to use
		// note: individual algorithms handle case in which no message ever received
		for (VehicleData message : algorithmData.commsInterface.getPredecessorMessages()) {
			//loop through messages starting with predecessor up to leader
			if(message != null) {
				if (algorithmData.receiveMessageData != null) {
					if (message.getStartTime() > algorithmData.receiveMessageData.getStartTime() + ALGORITHM_LOOP_DURATION) {
						//if message is at least ALGORITHM_LOOP_DURATION time newer than use it instead
						algorithmData.receiveMessageData = message;
					}
				} else {
					algorithmData.receiveMessageData = message;
				}
			}
		}
		if(algorithmData.receiveMessageData != null &&
				Time.getTime() - algorithmData.receiveMessageData.getStartTime() > MAXIMUM_MESSAGE_AGE) {
			//if message age is longer than MAXIMUM_MESSAGE_AGE discard message
			algorithmData.receiveMessageData = null;
		}
		if (algorithmData.receiveMessageData != null) {
			algorithmData.predecessorAcceleration = algorithmData.receiveMessageData.getAcceleration();
			algorithmData.predecessorSpeed = algorithmData.receiveMessageData.getSpeed();
			algorithmData.predecessorTurnRate = algorithmData.receiveMessageData.getTurnRate();
			algorithmData.predecessorChosenAcceleration = algorithmData.receiveMessageData.getChosenAcceleration();
			algorithmData.predecessorChosenSpeed = algorithmData.receiveMessageData.getChosenSpeed();
			algorithmData.predecessorChosenTurnRate = algorithmData.receiveMessageData.getChosenTurnRate();
		}

		// read data from sensors
		algorithmData.acceleration = algorithmData.sensorInterface.getAcceleration();
		algorithmData.speed = algorithmData.sensorInterface.getSpeed();
		algorithmData.turnRate = algorithmData.sensorInterface.getTurnRate();

		algorithmData.beacons = algorithmData.sensorInterface.getBeacons();

		algorithmData.previousAngle = algorithmData.angle;
		//find closest beacon within maximum sensor distance
		double min = Double.POSITIVE_INFINITY;
		for (Beacon beacon : algorithmData.beacons) {
			if (beacon.getDistanceLowerBound() <= min) {
				min = beacon.getDistanceLowerBound();
				algorithmData.closestBeacon = beacon;
				algorithmData.angle = algorithmData.closestBeacon.getAngle();
			}
		}

		//note this could be null
		algorithmData.sensorFrontProximity = algorithmData.sensorInterface.getFrontProximity();

		//combines beacon distance lower bound and sensor front proximity
		if (algorithmData.closestBeacon != null && algorithmData.sensorFrontProximity != null) {
			algorithmData.frontProximity = 0.5 * algorithmData.closestBeacon.getDistanceLowerBound() + 0.5 * algorithmData.sensorFrontProximity;
		} else if (algorithmData.closestBeacon != null) {
			algorithmData.frontProximity = algorithmData.closestBeacon.getDistanceLowerBound();
		} else if (algorithmData.sensorFrontProximity != null) {
			algorithmData.frontProximity = algorithmData.sensorFrontProximity;
		} else {
			algorithmData.frontProximity = null;
		}

		// get initial distance reading from sensor, distance null if no distance reading
		//algorithmData.previousDistance = algorithmData.frontProximity;
		algorithmData.previousSpeed = algorithmData.speed;
		algorithmData.previousAcceleration = algorithmData.acceleration;
	}

	protected abstract void makeDecision();

	private void sendMessage() {
		// create and send message to other cars
		VehicleData sendMessageData;
		if(algorithmData.commsInterface.isLeader()) {
			sendMessageData = new VehicleData(algorithmData.speed, algorithmData.acceleration,
					algorithmData.turnRate, algorithmData.speed, algorithmData.acceleration,
					algorithmData.turnRate);
		} else {
			sendMessageData = new VehicleData(algorithmData.speed, algorithmData.acceleration,
					algorithmData.turnRate, algorithmData.chosenSpeed, algorithmData.chosenAcceleration,
					algorithmData.chosenTurnRate);
		}
		algorithmData.commsInterface.sendMessage(sendMessageData);
	}

	public void emergencyStop() {
		if (!algorithmData.emergencyOccurred) {
			algorithmData.emergencyOccurred = true;
			algorithmData.driveInterface.stop();
			algorithmData.commsInterface.notifyEmergency();
		}
	}

	private void sendInstruction() {
		// send instructions to drive
		algorithmData.driveInterface.setAcceleration(algorithmData.chosenAcceleration);
		algorithmData.driveInterface.setTurnRate(algorithmData.chosenTurnRate);
	}

	/**
	 * Helper function, runs one loop of algorithm
	 * Called by update and run
	 */
	private void runOneLoop() {
		// read data from sensors into data class
		readSensors();

		if (Thread.interrupted()) {
			emergencyStop();
		}

		boolean shouldSendInstruction;
		if(!algorithmData.commsInterface.isLeader()) {
			makeDecision();
			shouldSendInstruction = true;
		} else {
			shouldSendInstruction = frontVehicleRoute.nextStep();
		}

		if (Thread.interrupted()) {
			emergencyStop();
		}

		sendMessage();

		// send instructions to drive if not leader
		if(shouldSendInstruction) {
			sendInstruction();
		}

		if (Thread.interrupted()) {
			emergencyStop();
		}
	}
	/**
	 * Runs one loop of algorithm
	 */
	public void update() {
		if (!algorithmData.emergencyOccurred) {
			runOneLoop();
		}
	}

	/**
	 * Runs algorithm every ALGORITHM_LOOP_DURATION  nanoseconds until an emergency occurs
	 */
	public void run() {
		initialise();
		long startTime = Time.getTime();

		while (!algorithmData.emergencyOccurred) {
			runOneLoop();
			try {
				long nanosToSleep = ALGORITHM_LOOP_DURATION - (Time.getTime() - startTime);
				if(nanosToSleep > 0) {
					// Note: integer division desired
					Thread.sleep(nanosToSleep/1000000);
				} else {
					Log.warn(String.format("LOOP_DURATION is too low, algorithm can't keep up (%dms too slow)", -nanosToSleep/1000000));
				}
			} catch (InterruptedException e) {
				emergencyStop();
				break;
			}
			startTime = Time.getTime();
		}
		Log.debug("Algorithm has finished running");
	}

	public boolean isLeader() {
		return algorithmData.commsInterface.isLeader();
	}

	public int getVehicleId() {
		return algorithmData.controlLayer.getVehicleId();
	}

	public int getPlatoonId() {
		return algorithmData.controlLayer.getPlatoonId();
	}

	public int getPlatoonPosition() {
		return algorithmData.controlLayer.getCurrentPosition();
	}

	public int getLeaderId() {
		return algorithmData.controlLayer.getLeaderId();
	}

}
