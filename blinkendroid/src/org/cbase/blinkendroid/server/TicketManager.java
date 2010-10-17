package org.cbase.blinkendroid.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

import org.cbase.blinkendroid.BlinkendroidApp;
import org.cbase.blinkendroid.network.ConnectionListener;
import org.cbase.blinkendroid.network.broadcast.IPeerHandler;
import org.cbase.blinkendroid.network.udp.ClientSocket;

import android.util.Log;

public class TicketManager implements IPeerHandler, ConnectionListener {
    int maxClients = 2;
    int clients = 0;
    private Set<String> tickets = new HashSet<String>();
    private String ownerName;
    DatagramSocket socket = null;

    public TicketManager(String ownerName) {
	this.ownerName = ownerName;
	try {
	    socket = new DatagramSocket(BlinkendroidApp.BROADCAST_ANNOUCEMENT_SERVER_TICKET_PORT);
	    socket.setReuseAddress(true);
	} catch (SocketException e) {
	    System.out.println("new DatagramSocket(Constants.BROADCAST_ANNOUCEMENT_SERVER_TICKET_PORT) failed "
		    + e.getMessage());
	}
    }

    public void foundPeer(String name, String ip, int protocolVersion) {
	// noch platz frei?
	if (clients < maxClients || tickets.contains(ip)) {
	    // send ticket to ip
	    try {
		InetSocketAddress socketAddr = new InetSocketAddress(ip,
			BlinkendroidApp.BROADCAST_ANNOUCEMENT_CLIENT_TICKET_PORT);
		String message = BlinkendroidApp.BROADCAST_PROTOCOL_VERSION + " " + BlinkendroidApp.SERVER_TICKET_COMMAND + " "
			+ ownerName;
		final byte[] messageBytes = message.getBytes("UTF-8");
		final DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, socketAddr);
		socket.send(packet);
		Log.d(BlinkendroidApp.LOG_TAG, "send Ticket for " + name + " " + ip);
		if (!tickets.contains(ip)) {
		    clients++;
		    tickets.add(ip);
		} else {
		    Log.d(BlinkendroidApp.LOG_TAG, "reset sent ticket for " + name + " " + ip);
		}
	    } catch (Exception e) {
		Log.e(BlinkendroidApp.LOG_TAG, "Exception in TicketManager", e);
	    }
	} else {
	    Log.d(BlinkendroidApp.LOG_TAG, "Server is full");
	}
	// pech jehabt
    }

    public void reset() {
	tickets.clear();
    }

    public void connectionClosed(ClientSocket clientSocket) {
	String ip = clientSocket.getDestinationAddress().getHostAddress();
	System.out.println("TicketManager connectionClosed " + ip);
	clients--;
	tickets.remove(ip);
    }

    public void connectionOpened(ClientSocket clientSocket) {
	// TODO clients merken und abhaken

    }

    public int getMaxClients() {
	return maxClients;
    }

    public void setMaxClients(int maxClients) {
	this.maxClients = maxClients;
    }
}
