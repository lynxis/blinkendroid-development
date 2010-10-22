package org.cbase.blinkendroid.network.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.cbase.blinkendroid.network.tcp.BlinkendroidDataServerProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.util.Log;

public class UDPClientProtocolManager extends UDPAbstractBlinkendroidProtocol implements UDPDirectConnection {

    private static final Logger logger = LoggerFactory.getLogger(UDPClientProtocolManager.class);

    private InetSocketAddress mSocketAddr;

    public UDPClientProtocolManager(DatagramSocket socket, InetSocketAddress serverAddr) throws IOException {
	super(socket);
	this.mSocketAddr = serverAddr;
    }

    @Override
    public void receive(DatagramPacket packet) throws IOException {
	/* drop datapackets from other servers */
	logger.debug( "Received packet " + packet.toString());
	if (packet.getAddress().equals(mSocketAddr.getAddress())) {
	    super.receive(packet);
	}
    }

    public void send(ByteBuffer out) throws IOException {
	send(mSocketAddr, out);
    }
}