package org.cbase.blinkendroid.network.udp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class ClientSocket {
  private UDPDirectConnection m_Socket;
  private InetSocketAddress m_SocketAddr;

  public UDPDirectConnection getDirectConnection() {
	return m_Socket;
  }

  public int getDestinationPort() {
	return m_SocketAddr.getPort();
  }

  public InetAddress getDestinationAddress() {
	return m_SocketAddr.getAddress();
  }

  public InetSocketAddress getInetSocketAddress() {
	return m_SocketAddr;
  }

  // create socket between localhost:<port> - addr:<port>
  public ClientSocket(UDPDirectConnection socket, InetSocketAddress socketAddr) throws SocketException {
	this.m_Socket = socket;
	this.m_SocketAddr = socketAddr;
  }

  public void send(ByteBuffer out) throws IOException {

	m_Socket.send(m_SocketAddr, out);
  }
}
