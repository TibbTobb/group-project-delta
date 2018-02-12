package uk.ac.cam.cl.group_project.delta.algorithm.communications;

import java.nio.ByteBuffer;

import uk.ac.cam.cl.group_project.delta.MessageReceipt;
import uk.ac.cam.cl.group_project.delta.NetworkInterface;
import uk.ac.cam.cl.group_project.delta.algorithm.VehicleData;

/**
 * This class handles the parsing and creating of the packets from their bytes.
 * Any changes to the packet structure should only need to be reflected here.
 * An instance of this class represents a received packet.
 * 
 * @author Aaron Hutton
 */
public class Packet {
	/**
	 * The size in bytes of the header of the packet
	 */
	public static int SIZE_OF_HEADER = 12;
	
	/**
	 * These fields are generated from a packet received from the network.
	 * So the vehicleId is the vehicle which sent the packet, the type is the
	 * type of packet etc.
	 */
	public final int vehicleId;
	public final int platoonId;
	public final int length;
	public final MessageType type;
	
	public final VehicleData message;
	public final byte[] payload;
	
	/**
	 * This constructor parses a packet and updates the fields appropriately.
	 * Only one of message and payload will be defined, depending on the type.
	 * 
	 * @param receipt - the packet receipt to be parsed
	 */
	public Packet(MessageReceipt receipt) {
		ByteBuffer bytes = ByteBuffer.wrap(receipt.getData());
		int packedInt = bytes.getInt();							// Contains the type and length
		type = MessageType.valueOf((packedInt >> 24) & 0x000000FF);
		length = packedInt & 0x00FFFFFF;
		
		platoonId = bytes.getInt();
		vehicleId = bytes.getInt();
		
		if(type.equals(MessageType.Data)) {
			message = VehicleData.generateDataFromBytes(bytes);
			message.setStartTime(receipt.getTime());
			payload = null;
		} else {
			payload = new byte[length - SIZE_OF_HEADER];
			bytes.get(payload, 0, length - SIZE_OF_HEADER);
			
			message = null;
		}
	}
	
	/**
	 * Creates a new data packet which contains the MessageData which is passed to it
	 * 
	 * @param message - the data to be sent
	 * @param vehicleId - the current vehicle id
	 * @param platoonId - the current platoon id
	 * @return the packet to be sent
	 */
	public static byte[] createDataPacket(VehicleData message, int vehicleId, int platoonId) {
		ByteBuffer bytes = createHeader(vehicleId, platoonId);
		message.appendToBuffer(bytes);
		updateLengthAndType(bytes, MessageType.Data);
		return bytes.array();
	}
	
	/**
	 * Creates a new packet which contains the data passed to is as a payload
	 * and has a type of the type MessageType
	 * 
	 * @param data - the data which makes up the payload
	 * @param vehicleId - the current vehicle id
	 * @param platoonId - the current platoon id
	 * @return the packet to be sent
	 */
	public static byte[] createPacket(byte[] data, int vehicleId, int platoonId, MessageType type) {
		ByteBuffer bytes = createHeader(vehicleId, platoonId);
		bytes.put(data);
		updateLengthAndType(bytes, type);
		return bytes.array();
	}
	
	/**
	 * Create the byte buffer and add the header
	 * 
	 * @param vehicleId
	 * @param platoonId
	 * @return the byte buffer used to create the packet
	 */
	private static ByteBuffer createHeader(int vehicleId, int platoonId) {
		ByteBuffer bytes = ByteBuffer.allocate(NetworkInterface.MAXIMUM_PACKET_SIZE);
		bytes.putInt(0);					// Initially the length is unknown
		bytes.putInt(platoonId);
		bytes.putInt(vehicleId);
		return bytes;
	}
	
	/**
	 * Update the length and type of the packet. This needs to be done after the data
	 * has been added so the length is known.
	 * 
	 * @param bytes
	 * @param type
	 */
	private static void updateLengthAndType(ByteBuffer bytes, MessageType type) {
		int length = bytes.position();
		bytes.rewind();					// Go back to start and put in the type and length
		bytes.putInt((type.getValue() << 24) | (0x00FFFFFF & length));
	}
	
	/**
	 * Tests whether the data passed in contains an emergency message
	 * 
	 * @param msg - the message to be tested
	 * @return whether the message is an emergency
	 */
	public static boolean isEmergencyMessage(byte[] data) {
		return MessageType.valueOf(data[0]).equals(MessageType.Emergency);
	}
}
