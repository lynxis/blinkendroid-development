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

package org.cbase.blinkendroid.network.broadcast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.cbase.blinkendroid.Constants;

import android.util.Log;

/**
 * A multicast sender that sends a server name to blinkendroid clients.
 */
public class SenderThread extends Thread {

  final private String message;
  private InetAddress group;
  volatile private boolean running = true;
  private DatagramSocket socket;

  /**
   * Creates a new {@link SenderThread}
   * 
   * @param serverName
   *          The server's name.
   */
  public SenderThread(String name) {
	// workaround: remove spaces, as those currently break the protocol
	name = name.replaceAll("\\s", "");
	message = Constants.BROADCAST_PROTOCOL_VERSION + " " + Constants.CLIENT_BROADCAST_COMMAND + " " + name;
  }

  @Override
  public void run() {
	try {
	  this.setName("SRV Send Annouce");
	  socket = new DatagramSocket(Constants.BROADCAST_ANNOUCEMENT_CLIENT_PORT);
	  socket.setReuseAddress(true);
	  socket.setBroadcast(true);
	  Log.d(Constants.LOG_TAG, "Sender thread started.");
	  // TODO have to figure out whether
	  // getAllByName("255.255.255.255")[0]; or
	  // getByName("255.255.255.255"); is more useful.
	  group = InetAddress.getAllByName("255.255.255.255")[0];
	  Log.i(Constants.LOG_TAG, "Server ip: " + group.toString());

	  while (running) {
		final byte[] messageBytes = message.getBytes("UTF-8");
		final DatagramPacket initPacket = new DatagramPacket(messageBytes, messageBytes.length, group,
			Constants.BROADCAST_ANNOUCEMENT_SERVER_PORT);
		Log.d(Constants.LOG_TAG, "Broadcasting Packet");
		socket.send(initPacket);

		Log.d(Constants.LOG_TAG, "Broadcasting: '" + message + "'");
		try {
		  Thread.sleep(5000);
		} catch (final InterruptedException x) {
		  // swallow, this is expected when being interrupted
		}
	  }

	  socket.close();

	} catch (final IOException x) {
	  Log.e(Constants.LOG_TAG, "problem sending", x);
	}
  }

  public void shutdown() {
	Log.d(Constants.LOG_TAG, "SenderThread: initiating shutdown");
	running = false;

	if (socket != null) {
	  socket.close();
	}
	interrupt();
	try {
	  join();
	} catch (final InterruptedException x) {
	  throw new RuntimeException(x);
	}
  }
}
