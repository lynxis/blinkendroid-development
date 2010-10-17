/*
 * Copyright 2010 the original author or authors.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cbase.blinkendroid.network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import org.cbase.blinkendroid.BlinkendroidApp;
import org.cbase.blinkendroid.network.udp.BlinkendroidClientProtocol;
import org.cbase.blinkendroid.network.udp.ClientConnectionState;
import org.cbase.blinkendroid.network.udp.ClientSocket;
import org.cbase.blinkendroid.network.udp.UDPClientProtocolManager;

import android.util.Log;

public class BlinkendroidClient extends Thread {

  private final InetSocketAddress socketAddress;
  private final BlinkendroidListener listener;
  private DatagramSocket socket;
  private UDPClientProtocolManager protocol;
  private ClientConnectionState m_connstate;
  private BlinkendroidClientProtocol blinkenProto;

  public BlinkendroidClient(final InetSocketAddress socketAddress, final BlinkendroidListener listener) {
	this.socketAddress = socketAddress;
	this.listener = listener;
  }

  @Override
  public void start() {
	Log.d(BlinkendroidApp.LOG_TAG, "trying to connect to server: " + socketAddress);
	try {
	  socket = new DatagramSocket(BlinkendroidApp.BROADCAST_CLIENT_PORT);
	  System.out.printf("UDP SOCKET CREATED");
	  socket.setReuseAddress(true);
	  long t = System.currentTimeMillis();
	  protocol = new UDPClientProtocolManager(socket, socketAddress);

	  m_connstate = new ClientConnectionState(new ClientSocket(protocol, socketAddress), listener);
	  protocol.registerHandler(BlinkendroidApp.PROTOCOL_CONNECTION, m_connstate);
	  m_connstate.openConnection();

	  blinkenProto = new BlinkendroidClientProtocol(listener);
	  protocol.registerHandler(BlinkendroidApp.PROTOCOL_PLAYER, blinkenProto);
	  Log.i(BlinkendroidApp.LOG_TAG, "connected " + (System.currentTimeMillis() - t));

	} catch (final IOException x) {
	  Log.e(BlinkendroidApp.LOG_TAG, "connection failed");
	  x.printStackTrace();
	  listener.connectionFailed(x.getClass().getName() + ": " + x.getMessage());
	}
  }

  public void shutdown() {
	if (null != m_connstate)
	  m_connstate.shutdown();
	if (null != protocol)
	  protocol.shutdown();
	if (null != socket) {
	  if (!socket.isClosed())
		socket.close();
	}
	Log.d(BlinkendroidApp.LOG_TAG, "client shutdown completed");
  }

  public void locateMe() {
	// TODO Auto-generated method stub
  }
}
