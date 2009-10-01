package artnet4j.packets;


public class ArtDmxPacket extends ArtNetPacket {

	private int numChannels;
	private int sequenceID;
	private int subnetID;
	private int universeID;

	public ArtDmxPacket() {
		super(PacketType.ART_OUTPUT);
		setData(new byte[530]);
		setHeader();
		setProtocol();
	}

	@Override
	public boolean parse(byte[] raw) {
		return false;
	}

	public void setDMX(byte[] dmxData, int numChannels) {
		logger.info("setting DMX data for: " + numChannels + " channels");
		this.numChannels = numChannels;
		data.setByteChunk(dmxData, 18, numChannels);
		data.setInt16(numChannels, 16);
	}

	public void setSequenceID(int id) {
		sequenceID = id;
		data.setInt8(id, 12);
	}

	public void setUniverse(int subnetID, int universeID) {
		this.subnetID = subnetID & 0x0f;
		this.universeID = universeID & 0x0f;
		data.setInt16LE(subnetID << 4 | universeID, 14);
		logger.info("universe ID set to: subnet: " + ByteUtils.hex(subnetID, 2)
				+ "/" + ByteUtils.hex(universeID, 2));
	}

}