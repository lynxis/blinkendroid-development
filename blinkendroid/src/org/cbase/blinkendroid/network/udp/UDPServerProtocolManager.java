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

package org.cbase.blinkendroid.network.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.cbase.blinkendroid.Constants;
import org.cbase.blinkendroid.network.ConnectionListener;
import org.cbase.blinkendroid.server.PlayerManager;

import android.util.Log;

public class UDPServerProtocolManager extends UDPAbstractBlinkendroidProtocol
	implements CommandHandler, ConnectionListener {

  protected GlobalTimerThread globalTimerThread;
  private PlayerManager m_PlayerManager;

  public void setPlayerManager(PlayerManager mPlayerManager) {
	m_PlayerManager = mPlayerManager;
  }

  public UDPServerProtocolManager(final DatagramSocket socket)
	  throws IOException {
	super(socket);
  }

  public void startTimerThread() {
	if (globalTimerThread != null) {
	  globalTimerThread.shutdown();
	}
	globalTimerThread = new GlobalTimerThread();
	globalTimerThread.start();
  }

  @Override
  public void shutdown() {
	if (null != globalTimerThread)
	  globalTimerThread.shutdown();
	super.shutdown();
  }

  @Override
  protected void receive(DatagramPacket packet) throws IOException {
	InetSocketAddress from = new InetSocketAddress(packet.getAddress(),
		packet.getPort());

	/* every Client has his own connectionHandler ! */

	ByteBuffer in = ByteBuffer.wrap(packet.getData());
	int proto = in.getInt();
	// System.out.println("server recieve "+proto);

	CommandHandler handler = handlers.get(proto);
	if (null != handler) {
	  handler.handle(from, in);
	} else {
	  if (m_PlayerManager != null) {
		m_PlayerManager.handle(this, from, proto, in);
	  }
	}
  }

  public void sendBroadcast(ByteBuffer out) {
	try {
	  // TODO: view SenderThread in broadcast and increase performance by
	  // removing constant creation of InetSocketAddresses
	  send(new InetSocketAddress(
		  InetAddress.getAllByName("255.255.255.255")[0],
		  Constants.BROADCAST_CLIENT_PORT), out);
	} catch (UnknownHostException e) {
	  Log.e(Constants.LOG_TAG, "Don't know where to send the broadcast", e);
	} catch (IOException e) {
	  Log.e(Constants.LOG_TAG, "IOException", e);
	}
  }

  @Override
  public void handle(SocketAddress socketAddr, ByteBuffer bybuff)
	  throws IOException {
	// Why doesn't this method do anything?
	throw new IOException(
		"This method does nothing else but throwing this Exception.");
	// System.out.println("handle nothing");
  }

  /**
   * This thread sends the global time to connected devices.
   */
  class GlobalTimerThread extends Thread {

	volatile private boolean running = true;

	@Override
	public void run() {
	  this.setName("SRV Send GlobalTimer");
	  Log.d(Constants.LOG_TAG, "GlobalTimerThread started");
	  while (running) {
		try {
		  Thread.sleep(1000);
		} catch (InterruptedException e) {
		  // swallow
		}
		if (!running) // fast exit
		  break;

		long t = System.currentTimeMillis();
		ByteBuffer out = ByteBuffer.allocate(128);
		out.putInt(Constants.PROTOCOL_CONNECTION);
		out.putInt(ConnectionState.Command.HEARTBEAT.ordinal());
		out.putInt(0); // Connection ID
		out.putLong(t);
		sendBroadcast(out);
		out.position(0);
		out.putInt(Constants.PROTOCOL_PLAYER);
		out.putInt(BlinkendroidProtocol.COMMAND_PLAYER_TIME);
		out.putLong(System.currentTimeMillis());
		sendBroadcast(out);
	  }
	  Log.d(Constants.LOG_TAG, "GlobalTimerThread stopped");
	}

	public void shutdown() {
	  running = false;
	  interrupt();
	  Log.d(Constants.LOG_TAG, "GlobalTimerThread initiating shutdown");
	}
  }

  @Override
  public void connectionClosed(ClientSocket clientSocket) {
	for (ConnectionListener connListener : connectionListener) {
	  connListener.connectionClosed(clientSocket);
	}
  }

  @Override
  public void connectionOpened(ClientSocket clientSocket) {
	for (ConnectionListener connListener : connectionListener) {
	  connListener.connectionOpened(clientSocket);
	}
  }
}
