package it.unical.igpe.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;

import it.unical.igpe.game.IGPEGame;
import it.unical.igpe.net.packet.Packet;
import it.unical.igpe.net.packet.Packet.PacketTypes;
import it.unical.igpe.net.packet.Packet00Login;
import it.unical.igpe.net.packet.Packet01Disconnect;
import it.unical.igpe.net.packet.Packet02Move;
import it.unical.igpe.net.packet.Packet03Fire;
import it.unical.igpe.net.packet.Packet04Death;
import it.unical.igpe.net.packet.Packet05GameOver;
import it.unical.igpe.net.packet.Packet06MapData;

public class GameServer extends Thread {
	public MultiplayerWorld worldMP;
	public int MaxKills;
	private DatagramSocket socket;
	private int port;
	private List<PlayerMP> connectedPlayers = new ArrayList<PlayerMP>();
	private String mapName; // Map name sent to clients (filename)
	private String serverMapPath; // Full path for server to load map (Desktop) or filename (Android)
	private String serverMapContent; // Map content for server to load (Android)
	private boolean initialized = false;

	public GameServer(int port) {
		this(port, "arena.map", null); // Default map
	}

	public GameServer(int port, String mapName) {
		this(port, mapName, null);
	}

	public GameServer(int port, String mapName, String mapContent) {
		this.port = port;
		// Store map content if provided (Android), otherwise use path (Desktop)
		this.serverMapContent = mapContent;
		this.serverMapPath = mapName; // Use mapName as path for Desktop, or filename for Android

		// Extract just filename for client communication
		String mapNameForClients = mapName;
		if (mapName.contains("/")) {
			mapNameForClients = mapName.substring(mapName.lastIndexOf("/") + 1);
		}
		if (mapName.contains("\\")) {
			mapNameForClients = mapName.substring(mapName.lastIndexOf("\\") + 1);
		}
		// Keep .map extension
		this.mapName = mapNameForClients;

		// Socket creation and world loading moved to run() to avoid NetworkOnMainThreadException on Android
		it.unical.igpe.utils.DebugUtils.showMessage("GameServer constructor completed (socket will be created in background thread)");
	}

	private void initialize() {
		try {
			it.unical.igpe.utils.DebugUtils.showMessage("Creating server socket on port: " + port + " with map: " + serverMapPath);
			this.socket = new DatagramSocket(port);
			System.out.println("Creating Server...");
			it.unical.igpe.utils.DebugUtils.showMessage("Socket created, loading multiplayer world...");
			try {
				// Use map content if available (Android), otherwise use path (Desktop)
				if (serverMapContent != null && !serverMapContent.isEmpty()) {
					it.unical.igpe.utils.DebugUtils.showMessage("Loading multiplayer world from content (length: " + serverMapContent.length() + ")");
					this.worldMP = new MultiplayerWorld(serverMapPath, serverMapContent, true);
				} else {
					it.unical.igpe.utils.DebugUtils.showMessage("Loading multiplayer world from path: " + serverMapPath);
					this.worldMP = new MultiplayerWorld(serverMapPath, true);
				}
				it.unical.igpe.utils.DebugUtils.showMessage("GameServer initialized successfully");
				this.initialized = true;
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Failed to load multiplayer world", e);
				// Close socket if world creation fails
				if (this.socket != null && !this.socket.isClosed()) {
					this.socket.close();
				}
				this.socket = null;
				throw e; // Re-throw to indicate failure
			}
		} catch (SocketException e1) {
			it.unical.igpe.utils.DebugUtils.showError("Socket error creating GameServer on port: " + port, e1);
			e1.printStackTrace();
			this.socket = null; // Mark as failed
		} catch (Exception e) {
			it.unical.igpe.utils.DebugUtils.showError("Unexpected error initializing GameServer", e);
			e.printStackTrace();
			this.socket = null; // Mark as failed
		}
	}

	public void run() {
		// Initialize socket and world in background thread (avoids NetworkOnMainThreadException on Android)
		if (!initialized) {
			initialize();
		}

		// Don't run if initialization failed
		if (socket == null || !initialized) {
			it.unical.igpe.utils.DebugUtils.showError("GameServer thread cannot start: initialization failed");
			return;
		}

		it.unical.igpe.utils.DebugUtils.showMessage("GameServer thread started");
		while (true) {
			byte[] data = new byte[1024];
			DatagramPacket packet = new DatagramPacket(data, data.length);
			try {
				socket.receive(packet);
			} catch (IOException e) {
				it.unical.igpe.utils.DebugUtils.showError("Error receiving packet in GameServer", e);
				e.printStackTrace();
				break; // Exit loop on error
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Unexpected error in GameServer.run()", e);
				e.printStackTrace();
				break;
			}
			this.parsePacket(packet.getData(), packet.getAddress(), packet.getPort());
			for (PlayerMP p : connectedPlayers) {
				if(p.kills >= MaxKills)  {
					Packet05GameOver packetGO = new Packet05GameOver(p.username, p.kills);
					packetGO.writeData(this);
				}
			}
		}
	}

	private void parsePacket(byte[] data, InetAddress address, int port) {
		String message = new String(data).trim();
		PacketTypes type = Packet.lookupPacket(message.substring(0, 2));
		Packet packet = null;
		switch (type) {
		default:
		case INVALID:
			break;
		case LOGIN:
			packet = new Packet00Login(data);
			System.out.println("[" + address.getHostAddress() + ":" + port + "]"
					+ ((Packet00Login) packet).getUsername() + " has connected");
			PlayerMP player = new PlayerMP(
					new Vector2(((Packet00Login) packet).getX(), ((Packet00Login) packet).getY()),
					IGPEGame.game.worldMP, ((Packet00Login) packet).getUsername(), address, port);
			this.addConnection(player, (Packet00Login) packet);
			// Send map data to the newly connected client
			// Use stored map content if available (custom maps), otherwise send just map name (default maps in assets)
			try {
				if (serverMapContent != null && !serverMapContent.isEmpty()) {
					// Custom map: send content
					Packet06MapData mapPacket = new Packet06MapData(mapName, serverMapContent);
					sendData(mapPacket.getData(), address, port);
					it.unical.igpe.utils.DebugUtils.showMessage("Sent custom map data to client: " + mapName + " (" + serverMapContent.length() + " bytes)");
				} else {
					// Default map: just send name, client will load from assets
					Packet06MapData mapPacket = new Packet06MapData(mapName);
					sendData(mapPacket.getData(), address, port);
					it.unical.igpe.utils.DebugUtils.showMessage("Sent default map name to client: " + mapName + " (client will load from assets)");
				}
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Failed to send map data to client", e);
				// Fallback: send just map name
				Packet06MapData mapPacket = new Packet06MapData(mapName);
				sendData(mapPacket.getData(), address, port);
			}
			break;
		case DISCONNECT:
			packet = new Packet01Disconnect(data);
			System.out.println("[" + address.getHostAddress() + ":" + port + "] "
					+ ((Packet01Disconnect) packet).getUsername() + " has left...");
			this.removeConnection((Packet01Disconnect) packet);
			break;
		case MOVE:
			packet = new Packet02Move(data);
			this.handleMove((Packet02Move) packet);
			break;
		case FIRE:
			packet = new Packet03Fire(data);
			handleFire((Packet03Fire) packet);
			break;
		case DEATH:
			packet = new Packet04Death(data);
			handleDeath((Packet04Death) packet);
			break;
		case GAMEOVER:
			packet = new Packet05GameOver(data);
			packet.writeData(this);
			break;
		case MAPDATA:
			// Server receives map data (shouldn't happen, but handle gracefully)
			break;
		}
	}
	
	private void handleDeath(Packet04Death packet) {
		if (getPlayerMP(packet.getUsernameKiller()) != null && getPlayerMP(packet.getUsernameKilled()) != null) {
			int index = getPlayerMPIndex(packet.getUsernameKiller());
			if (index >= 0 && index < this.connectedPlayers.size() &&
				!packet.getUsernameKiller().equalsIgnoreCase(MultiplayerWorld.username)) {
				this.connectedPlayers.get(index).kills++;
			}
			index = getPlayerMPIndex(packet.getUsernameKilled());
			if (index >= 0 && index < this.connectedPlayers.size() &&
				!packet.getUsernameKilled().equalsIgnoreCase(MultiplayerWorld.username)) {
				this.connectedPlayers.get(index).deaths++;
			}
			packet.writeData(this);
		}
	}

	public void close() {
		if (this.socket != null && !this.socket.isClosed()) {
			this.socket.close();
		}
	}

	/**
	 * Check if the server was created successfully
	 */
	public boolean isValid() {
		return this.initialized && this.socket != null && !this.socket.isClosed();
	}

	private void handleFire(Packet03Fire packet) {
		if (getPlayerMP(packet.getUsername()) != null) {
			int index = getPlayerMPIndex(packet.getUsername());
			if (index >= 0 && index < this.connectedPlayers.size()) {
				this.connectedPlayers.get(index).getBoundingBox().x = packet.getX();
				this.connectedPlayers.get(index).getBoundingBox().y = packet.getY();
				this.connectedPlayers.get(index).angle = packet.getAngle();
				packet.writeData(this);
			}
		}
	}

	private void removeConnection(Packet01Disconnect packet) {
		int index = getPlayerMPIndex(packet.getUsername());
		if (index >= 0 && index < this.connectedPlayers.size()) {
			this.connectedPlayers.remove(index);
		}
		packet.writeData(this);
	}

	private void handleMove(Packet02Move packet) {
		if (getPlayerMP(packet.getUsername()) != null) {
			int index = getPlayerMPIndex(packet.getUsername());
			if (index >= 0 && index < this.connectedPlayers.size()) {
				PlayerMP plMP = this.connectedPlayers.get(index);
				plMP.getBoundingBox().x = packet.getX();
				plMP.getBoundingBox().y = packet.getY();
				plMP.angle = packet.getAngle();
				plMP.state = packet.getState();
				packet.writeData(this);
			}
		}
	}

	public void addConnection(PlayerMP player, Packet00Login packet) {
		boolean alreadyConnected = false;
		for (PlayerMP p : this.connectedPlayers) {
			if (player.getUsername().equalsIgnoreCase(p.getUsername())) {
				if (p.ipAddress == null)
					p.ipAddress = player.ipAddress;
				if (p.port == -1)
					p.port = player.port;
				alreadyConnected = true;
			}
			// Only send to players with valid address and port
			if (p.ipAddress != null && p.port > 0) {
				sendData(packet.getData(), p.ipAddress, p.port);
			}

			// Only send back if player has valid address and port
			if (player.ipAddress != null && player.port > 0) {
				Packet newPacket = new Packet00Login(p.getUsername(), (int) p.getBoundingBox().x, (int) p.getBoundingBox().y);
				sendData(newPacket.getData(), player.ipAddress, player.port);
			}
		}
		if (!alreadyConnected) {
			this.connectedPlayers.add(player);
		}

	}

	public void sendData(byte[] data, InetAddress ipAddress, int port) {
		// Validate port and address before sending
		if (ipAddress == null || port <= 0 || port > 65535) {
			it.unical.igpe.utils.DebugUtils.showMessage("Skipping sendData to invalid address/port: " + ipAddress + ":" + port);
			return;
		}
		DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendDataToAllClients(byte[] data) {
		for (PlayerMP p : connectedPlayers) {
			// Only send to players with valid address and port
			if (p.ipAddress != null && p.port > 0) {
				sendData(data, p.ipAddress, p.port);
			}
		}
	}

	public PlayerMP getPlayerMP(String username) {
		for (PlayerMP player : this.connectedPlayers) {
			if (player.getUsername().equalsIgnoreCase(username)) {
				return player;
			}
		}
		return null;
	}

	public int getPlayerMPIndex(String username) {
		int index = 0;
		for (PlayerMP player : this.connectedPlayers) {
			if (player.getUsername().equalsIgnoreCase(username)) {
				return index;
			}
			index++;
		}
		return -1;
	}
	
	/**
	 * Load map file content as a string to send to clients
	 */
	private String loadMapContent(String mapPath) throws IOException {
		FileHandle fileHandle = null;
		
		// Check if path is absolute or relative
		if (mapPath.startsWith("/") || mapPath.startsWith("content://") || mapPath.startsWith("file://")) {
			// Absolute path
			if (mapPath.startsWith("/")) {
				fileHandle = Gdx.files.absolute(mapPath);
			} else if (mapPath.startsWith("file://")) {
				String cleanPath = mapPath.substring(7);
				fileHandle = Gdx.files.absolute(cleanPath);
			} else {
				throw new IOException("Cannot load from content URI: " + mapPath);
			}
		} else {
			// Relative path - try internal first
			fileHandle = Gdx.files.internal(mapPath);
			if (!fileHandle.exists()) {
				fileHandle = Gdx.files.external(mapPath);
			}
		}
		
		if (!fileHandle.exists()) {
			throw new IOException("Map file does not exist: " + mapPath);
		}
		
		// Read entire file content
		BufferedReader br = new BufferedReader(fileHandle.reader());
		StringBuilder content = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			if (content.length() > 0) {
				content.append("\n");
			}
			content.append(line);
		}
		br.close();
		
		return content.toString();
	}
}
