package uk.ac.cam.cl.group_project.delta.algorithm.communications;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Random;

import uk.ac.cam.cl.group_project.delta.MessageReceipt;
import uk.ac.cam.cl.group_project.delta.NetworkInterface;
import uk.ac.cam.cl.group_project.delta.algorithm.MessageData;

public class MessageReceiver {
	
	/**
	 * The current position in the platoon, 0 indicates the leader.
	 * Begin life as the leader of a platoon.
	 */
	private int position = 0;
	
	/**
	 * The mapping from positions to messages read by {@link Communications}
	 * and updated by this class.
	 */
	private PlatoonLookup messageLookup;
	
	/**
	 * The network interface used to send and receive data.
	 */
	private NetworkInterface network;

	/**
	 * The current id of this vehicle.
	 */
	private int vehicleId;
	
	/**
	 * The current id for the platoon this vehicle belongs to.
	 */
	private int platoonId;
	
	/**
	 * Create a new platoon instance by making a new MessageReceiver Object
	 * 
	 * @param network - the network interface to be used
	 * @param map - the position to message map to be used
	 */
	public MessageReceiver(NetworkInterface network, PlatoonLookup map) {
		messageLookup = map;
		Random r = new Random();
		vehicleId = r.nextInt();
		platoonId = r.nextInt();
	}

	/**
	 * Send the specific message across the network
	 * 
	 * @param message - the message to be sent
	 */
	public void sendMessage(MessageData message) {
		ByteBuffer bytes = ByteBuffer.allocate(NetworkInterface.MAXIMUM_PACKET_SIZE);
		bytes.putInt(0);					// Initially the length is unknown
		bytes.putInt(platoonId);
		bytes.putInt(vehicleId);
		message.toBytes(bytes);
		
		int length = bytes.position();
		bytes.position(0);					// Go back to start and put in the type and length
		bytes.putInt((MessageType.Data.getValue() << 24) | (0x00FFFFFF & length));
		network.sendData(bytes.array());
	}

	public int getCurrentPosition() {
		return position;
	}

	public void notifyEmergency() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * This updates the messages in the messageLookup to reflect the most
	 * recent messages
	 */
	public void updateMessages() {
		for(MessageReceipt message : network.pollData()) {
			
		}
	}
}
