package org.cbase.blinkendroid.network.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class UDPClientProtocolManager extends UDPAbstractBlinkendroidProtocol implements UDPDirectConnection {

  private InetSocketAddress m_SocketAddr;

  public UDPClientProtocolManager(DatagramSocket socket, InetSocketAddress serverAddr) throws IOException {
	super(socket);
	this.m_SocketAddr = serverAddr;
  }

  @Override
  public void receive(DatagramPacket packet) throws IOException {
	/* drop datapackets from other servers */
	if (packet.getAddress().equals(m_SocketAddr.getAddress())) {
	  super.receive(packet);
	}
  }

  public void send(ByteBuffer out) throws IOException {
	send(m_SocketAddr, out);
  }
}