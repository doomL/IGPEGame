package it.unical.igpe.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.badlogic.gdx.math.Vector2;

import it.unical.igpe.game.IGPEGame;
import it.unical.igpe.logic.AbstractDynamicObject;
import it.unical.igpe.net.packet.Packet;
import it.unical.igpe.net.packet.Packet.PacketTypes;
import it.unical.igpe.net.packet.Packet00Login;
import it.unical.igpe.net.packet.Packet01Disconnect;
import it.unical.igpe.net.packet.Packet02Move;
import it.unical.igpe.net.packet.Packet03Fire;
import it.unical.igpe.net.packet.Packet04Death;
import it.unical.igpe.net.packet.Packet05GameOver;
import it.unical.igpe.net.packet.Packet06MapData;

public class GameClient extends Thread {
	private InetAddress ipAddress;
	private DatagramSocket socket;
	private int port;

	public GameClient(String ipAddress, int port) {
		try {
			it.unical.igpe.utils.DebugUtils.showMessage("Connecting to server: " + ipAddress + ":" + port);
			this.socket = new DatagramSocket();
			this.ipAddress = InetAddress.getByName(ipAddress);
			this.port = port;
			System.out.println("Connected to server " + ipAddress);
			it.unical.igpe.utils.DebugUtils.showMessage("GameClient created successfully");
		} catch (UnknownHostException e) {
			it.unical.igpe.utils.DebugUtils.showError("Unknown host: " + ipAddress, e);
			e.printStackTrace();
		} catch (SocketException e1) {
			it.unical.igpe.utils.DebugUtils.showError("Socket error creating GameClient", e1);
			e1.printStackTrace();
		} catch (Exception e) {
			it.unical.igpe.utils.DebugUtils.showError("Unexpected error creating GameClient", e);
			e.printStackTrace();
		}
	}
	
	public void run() {
		it.unical.igpe.utils.DebugUtils.showMessage("GameClient thread started");
		while(true) {
			byte[] data = new byte[1024];
			DatagramPacket packet = new DatagramPacket(data, data.length);
			try {
				socket.receive(packet);
			} catch (IOException e) {
				it.unical.igpe.utils.DebugUtils.showError("Error receiving packet in GameClient", e);
				e.printStackTrace();
				break; // Exit loop on error
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Unexpected error in GameClient.run()", e);
				e.printStackTrace();
				break;
			}
			this.parsePacket(packet.getData(), packet.getAddress(), packet.getPort());
		}
		it.unical.igpe.utils.DebugUtils.showMessage("GameClient thread ended");
	}
	
	public void sendData(byte[] data) {
		DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void parsePacket(byte[] data, InetAddress address, int port) {
		String message = new String(data).trim();
		PacketTypes type = Packet.lookupPacket(message.substring(0, 2));
		Packet packet = null;
		switch (type) {
		default:
		case INVALID:
			System.out.println("INVALID PACKET");
			break;
		case LOGIN:
			packet = new Packet00Login(data);
			handleLogin((Packet00Login) packet, address, port);
			break;
		case DISCONNECT:
			packet = new Packet01Disconnect(data);
            System.out.println("[" + address.getHostAddress() + ":" + port + "] "
                    + ((Packet01Disconnect) packet).getUsername() + " has left the world...");
            IGPEGame.game.worldMP.removePlayerMP(((Packet01Disconnect) packet).getUsername());
			break;
		case MOVE:
			packet = new Packet02Move(data);
			handleMove((Packet02Move) packet);
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
			handleGameOver((Packet05GameOver) packet);
			break;
		case MAPDATA:
			packet = new Packet06MapData(data);
			handleMapData((Packet06MapData) packet);
			break;
		}
	}
	
	private void handleMapData(Packet06MapData packet) {
		String mapName = packet.getMapName();
		String mapContent = packet.getMapContent();
		
		it.unical.igpe.utils.DebugUtils.showMessage("Received map data from server: " + mapName + (mapContent != null ? " (with content, length: " + mapContent.length() + ")" : ""));
		
		// Store map content directly (don't save to file)
		if (mapContent != null && !mapContent.isEmpty()) {
			// Store map content for use when creating MultiplayerWorld
			MultiplayerWorld.serverMapName = mapName; // Store filename
			MultiplayerWorld.serverMapContent = mapContent; // Store content
			it.unical.igpe.utils.DebugUtils.showMessage("Stored map content for multiplayer world creation");
		} else {
			// No map content, just use the map name (try to load from assets)
			MultiplayerWorld.serverMapName = mapName;
			MultiplayerWorld.serverMapContent = null;
		}
		
		// If worldMP doesn't exist yet, it will be created with this map
		// If it exists, we need to reload it with the new map
		if (IGPEGame.game.worldMP == null) {
			// World will be created with this map
			it.unical.igpe.utils.DebugUtils.showMessage("World not created yet, will use map: " + mapName + (mapContent != null ? " (from content)" : ""));
		} else {
			// World already exists, need to reload with new map
			it.unical.igpe.utils.DebugUtils.showMessage("Reloading world with map: " + mapName);
			try {
				// Save player state before reloading
				String username = IGPEGame.game.worldMP.player != null ? IGPEGame.game.worldMP.player.getUsername() : MultiplayerWorld.username;
				Vector2 playerPos = IGPEGame.game.worldMP.player != null ? 
					new Vector2(IGPEGame.game.worldMP.player.getBoundingBox().x, IGPEGame.game.worldMP.player.getBoundingBox().y) : null;
				
				IGPEGame.game.worldMP = new MultiplayerWorld(MultiplayerWorld.serverMapName, MultiplayerWorld.serverMapContent, false);
				it.unical.igpe.utils.DebugUtils.showMessage("World reloaded with map: " + mapName + (mapContent != null ? " (from content)" : ""));
				it.unical.igpe.utils.DebugUtils.showMessage("World reloaded with map: " + mapName);
			} catch (Exception e) {
				it.unical.igpe.utils.DebugUtils.showError("Failed to reload world with map: " + mapName, e);
			}
		}
	}

	private void handleGameOver(Packet05GameOver packet) {
		IGPEGame.game.worldMP.handleGameOver(packet.getUsernameWinner(), packet.getKillsWinner());
	}

	private void handleDeath(Packet04Death packet) {
		IGPEGame.game.worldMP.handleDeath(packet.getUsernameKiller(), packet.getUsernameKilled());
	}

	private void handleFire(Packet03Fire packet) {
		IGPEGame.game.worldMP.fireBullet(packet.getUsername(), packet.getX(), packet.getY(), packet.getAngle(), packet.getWeapon());
	}

	private void handleLogin(Packet00Login packet, InetAddress address, int port) {
		synchronized (IGPEGame.game.worldMP.getEntities()) {
			for (AbstractDynamicObject e: IGPEGame.game.worldMP.getEntities()) {
				if (e instanceof PlayerMP && ((PlayerMP)e).username.equalsIgnoreCase(packet.getUsername())) {
					return;
				}
			}
		}
		System.out.println("[" + address.getHostAddress() + ":" + port + "]"
				+ packet.getUsername() + " has joined the game");
		PlayerMP player = new PlayerMP(new Vector2(packet.getX(), packet.getY()), IGPEGame.game.worldMP, packet.getUsername(), address, port);
		IGPEGame.game.worldMP.addEntity(player);
	}
	
	private void handleMove(Packet02Move packet) {
		IGPEGame.game.worldMP.movePlayer(packet.getUsername(), packet.getX(), packet.getY(), packet.getAngle(), packet.getState(), packet.getWeapon());
	}
}
