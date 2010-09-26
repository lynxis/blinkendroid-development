package org.cbase.blinkendroid.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.cbase.blinkendroid.Constants;
import org.cbase.blinkendroid.network.ConnectionListener;
import org.cbase.blinkendroid.network.broadcast.IPeerHandler;
import org.cbase.blinkendroid.network.udp.ClientSocket;

public class TicketManager implements IPeerHandler, ConnectionListener {
    int maxClients = 2;
    int clients = 0;
    private Set<String> tickets = new HashSet<String>();

    public void foundPeer(String name, String ip, int protocolVersion) {
	System.out.printf("foundpeer " + name + ip);
	if (tickets.contains(name + ip)) {
	    System.out.printf("already send ticket for " + name + ip);
	    return;
	}
	// noch platz frei?
	if (clients < maxClients) {
	    // send ticket to ip
	    try {
		InetSocketAddress socketAddr = new InetSocketAddress(ip,
			Constants.BROADCAST_ANNOUCEMENT_CLIENT_TICKET_PORT);
		String message = Constants.BROADCAST_PROTOCOL_VERSION + " " + Constants.SERVER_TICKET_COMMAND + " "
			+ name;
		final byte[] messageBytes = message.getBytes("UTF-8");
		final DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, socketAddr);
		System.out.printf("UDP SOCKET CREATED");
		DatagramSocket socket = new DatagramSocket(Constants.BROADCAST_ANNOUCEMENT_SERVER_TICKET_PORT);
		socket.setReuseAddress(true);
		socket.send(packet);
		System.out.println("send Ticket");
		clients++;
		tickets.add(name + ip);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} else {
	    System.out.println("voll");
	}
	// pech jehabt
    }

    public void reset() {
	tickets.clear();
    }

    public void connectionClosed(ClientSocket clientSocket) {
	clients--;
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
