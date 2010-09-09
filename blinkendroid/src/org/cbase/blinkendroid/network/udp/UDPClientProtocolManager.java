package org.cbase.blinkendroid.network.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.cbase.blinkendroid.Constants;
import org.cbase.blinkendroid.network.BlinkendroidListener;
import org.cbase.blinkendroid.player.bml.BBMZParser;
import org.cbase.blinkendroid.player.bml.BLM;

import android.util.Log;

public class UDPClientProtocolManager extends UDPAbstractBlinkendroidProtocol
		implements UDPDirectConnection {
	
	private InetSocketAddress m_SocketAddr;

	public UDPClientProtocolManager(DatagramSocket socket, InetSocketAddress serverAddr) throws IOException {
		super(socket);
		this.m_SocketAddr = serverAddr;
	}
	
	@Override
	public void receive(DatagramPacket packet) throws IOException {
		/* drop datapackets from other servers */
		if(packet.getSocketAddress() == m_SocketAddr) {
			super.receive(packet);
		}
	}

	public void send(ByteBuffer out) throws IOException {
		send(m_SocketAddr, out);
	}
}