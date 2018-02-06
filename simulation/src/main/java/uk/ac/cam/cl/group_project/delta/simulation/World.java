package uk.ac.cam.cl.group_project.delta.simulation;

import java.util.List;

/**
 * Represents a physically simulated collection of physics objects.
 */
public class World {

	/**
	 * A list of bodies contain within this environment.
	 */
	private List<PhysicsBody> bodies;

	/**
	 * Update all objects with this environment.
	 * @param dt                      Timestep in seconds.
	 * @throws SimulationException    An error occurred during simulation.
	 */
	public void update(double dt) throws SimulationException {
		for (PhysicsBody body : bodies) {
			body.update(dt);
		}
	}

	/**
	 * Fetch the list of bodies contained within this world.
	 * @return    List of bodies.
	 */
	public List<PhysicsBody> getBodies() {
		return this.bodies;
	}

}
