package uk.ac.cam.cl.group_project.delta.lego;

import lejos.hardware.ev3.EV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.Port;
import uk.ac.cam.cl.group_project.delta.DriveInterface;

public class Drive implements DriveInterface {

	private EV3LargeRegulatedMotor L;
	private EV3LargeRegulatedMotor R;
	private EV3MediumRegulatedMotor steer;

	public Drive(EV3 ev3) {
		Port portL = ev3.getPort("B");
		Port portR = ev3.getPort("C");
		Port portSteer = ev3.getPort("D");
		L = new EV3LargeRegulatedMotor(portL);
		R = new EV3LargeRegulatedMotor(portR);
		steer = new EV3MediumRegulatedMotor(portSteer);
	}

	public void testDrive() {
		L.setSpeed(0);
		R.setSpeed(0);
		L.backward();
		R.backward();
		setAcceleration(0.1);
	}

	/**
	 * Attempts to set the acceleration of the vehicle to the specified
	 * signed amount, where a negative value indicates deceleration.
	 * The vehicle will, as quickly as possible, try to attain as close
	 * to the given acceleration as it is able.
	 *
	 * @param acceleration in m/s^2
	 */
	@Override
	public void setAcceleration(double acceleration) {
		int degreesPerMetre = 2640;
		int accelerationDegrees = (int) Math.round(acceleration * degreesPerMetre);
		// metres/second/second -> degrees/second/second
		L.setAcceleration(accelerationDegrees);
		R.setAcceleration(accelerationDegrees);
		L.setSpeed(L.getMaxSpeed());
		R.setSpeed(R.getMaxSpeed());
		L.backward();
		R.backward();
	}

	/**
	 * Attempts to set the turn rate to the specified signed amount,
	 * where a negative value indicates a left turn and a positive value
	 * indicates a right turn. The vehicle will, as quickly as possible,
	 * try to attain as close to the given turn rate as it is able.
	 *
	 * @param turnRate in rad/s
	 */
	@Override
	public void setTurnRate(double turnRate) {

	}

	/**
	 * Brings the vehicle to a stop as quickly as possible. Suitable for emergency use.
	 */
	@Override
	public void stop() {
		L.stop();
		R.stop();
	}

}
