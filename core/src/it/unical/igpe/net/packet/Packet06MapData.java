package it.unical.igpe.net.packet;

import it.unical.igpe.net.GameClient;
import it.unical.igpe.net.GameServer;

/**
 * Packet to send map data from server to clients
 * Format: 06,mapName,mapContent
 * mapContent is the actual map file content (base64 encoded if needed, or raw)
 */
public class Packet06MapData extends Packet {
	
	private String mapName;
	private String mapContent;

	public Packet06MapData(byte[] data) {
		super(06);
		String dataStr = readData(data);
		// Format: mapName|||mapContent or just mapName (for backwards compatibility)
		if (dataStr.contains("|||")) {
			String[] parts = dataStr.split("\\|\\|\\|", 2);
			this.mapName = parts[0];
			this.mapContent = parts.length > 1 ? parts[1] : null;
		} else {
			// Backwards compatibility: just map name
			this.mapName = dataStr;
			this.mapContent = null;
		}
	}
	
	public Packet06MapData(String mapName) {
		super(06);
		this.mapName = mapName;
		this.mapContent = null;
	}
	
	public Packet06MapData(String mapName, String mapContent) {
		super(06);
		this.mapName = mapName;
		this.mapContent = mapContent;
	}

	@Override
	public void writeData(GameClient client) {
		client.sendData(getData());
	}

	@Override
	public void writeData(GameServer server) {
		server.sendDataToAllClients(getData());
	}

	@Override
	public byte[] getData() {
		if (mapContent != null) {
			// Send map name and content, using a separator that won't appear in map data
			// Use a special separator like ||| to split name and content
			return ("06" + this.mapName + "|||" + this.mapContent).getBytes();
		} else {
			// Backwards compatibility: just map name
			return ("06" + this.mapName).getBytes();
		}
	}

	public String getMapName() {
		return mapName;
	}
	
	public String getMapContent() {
		return mapContent;
	}
}
