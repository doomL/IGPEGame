package it.unical.igpe.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Utility class for network scanning and IP address discovery
 */
public class NetworkScanner {
	
	/**
	 * Gets the local IP address (non-loopback)
	 */
	public static String getLocalIP() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				// Skip loopback and inactive interfaces
				if (iface.isLoopback() || !iface.isUp()) {
					continue;
				}
				
				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					// Return first non-loopback IPv4 address
					if (!addr.isLoopbackAddress() && addr.getAddress().length == 4) {
						return addr.getHostAddress();
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		// Fallback to localhost
		return "127.0.0.1";
	}
	
	/**
	 * Scans the local network for available game servers
	 * @param port The port to scan
	 * @param timeoutMs Timeout in milliseconds for each scan
	 * @return List of found server IP addresses
	 */
	public static List<String> scanForServers(int port, int timeoutMs) {
		List<String> foundServers = new ArrayList<String>();
		
		try {
			// Get local network prefix (e.g., 192.168.1.x)
			String localIP = getLocalIP();
			String[] parts = localIP.split("\\.");
			if (parts.length != 4) {
				return foundServers;
			}
			
			String networkPrefix = parts[0] + "." + parts[1] + "." + parts[2] + ".";
			
			// Create a socket for scanning
			DatagramSocket scanSocket = new DatagramSocket();
			scanSocket.setSoTimeout(timeoutMs);
			
			// Scan IPs in the local network (1-254)
			for (int i = 1; i < 255; i++) {
				String testIP = networkPrefix + i;
				try {
					InetAddress address = InetAddress.getByName(testIP);
					
					// Try to send a discovery packet
					// For now, we'll just try to connect - in a real implementation,
					// you'd send a specific discovery packet that servers respond to
					byte[] data = "DISCOVER".getBytes();
					DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
					
					try {
						scanSocket.send(packet);
						
						// Try to receive a response
						byte[] buffer = new byte[1024];
						DatagramPacket response = new DatagramPacket(buffer, buffer.length);
						scanSocket.receive(response);
						
						// If we got a response, add it to the list
						foundServers.add(testIP);
					} catch (SocketTimeoutException e) {
						// Timeout is expected for most IPs
					} catch (IOException e) {
						// Ignore IO errors during scanning
					}
				} catch (Exception e) {
					// Skip invalid addresses
				}
			}
			
			scanSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return foundServers;
	}
	
	/**
	 * Simple server discovery - just returns the local IP for now
	 * A full implementation would broadcast discovery packets
	 */
	public static List<String> discoverServers(int port) {
		List<String> servers = new ArrayList<String>();
		// For now, return empty list - full network scanning would require
		// a discovery protocol that servers respond to
		return servers;
	}
}
