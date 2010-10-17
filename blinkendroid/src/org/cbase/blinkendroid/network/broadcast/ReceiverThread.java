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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Log;

/**
 * Receives Server announcements
 */
public class ReceiverThread extends Thread {

    private final String LOG_TAG = "ReceiverThread".intern();
  volatile private boolean running = true;
  private List<IPeerHandler> handlers = Collections.synchronizedList(new ArrayList<IPeerHandler>());
  private DatagramSocket socket;
  private int port;
  private String command;

  public ReceiverThread(int port, String command) {
	super();
	this.port = port;
	this.command = command;
  }

  /**
   * Adds a handler to the {@link ReceiverThread}.
   */
  public void addHandler(IPeerHandler handler) {
	handlers.add(handler);
  }

  public void removeHandler(IPeerHandler handler) {
	handlers.remove(handler);
  }

  /**
   * Notifies the registered handlers
   */
  private void notifyHandlers(final int protocolVersion, String name, String ip) {
	for (IPeerHandler h : handlers) {
	  h.foundPeer(name, ip, protocolVersion);
	}
  }

  @Override
  public void run() {
	this.setName("CLI Annoucement Receiver");
	try {
	  socket = new DatagramSocket(port);
	  socket.setReuseAddress(true);
	  socket.setBroadcast(true);
	  Log.d(LOG_TAG, "Receiver thread started.");
	  while (running) {

		final byte[] buf = new byte[512];
		final DatagramPacket packet = new DatagramPacket(buf, buf.length);
		Log.d(LOG_TAG, "Receiving.");
		receive(packet);
		Log.d(LOG_TAG, "Received.");
		if (!running) // fast exit
		  break;

		final String receivedString = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
		Log.d(LOG_TAG, "received via broadcast: '" + receivedString + "'");
		final String[] receivedParts = receivedString.split(" ");

		final int protocolVersion = Integer.parseInt(receivedParts[0]);
		// if (protocolVersion <= Constants.BROADCAST_PROTOCOL_VERSION)
		// {

		if (!receivedParts[1].equals(command)) {
		  continue;
		}

		final InetAddress address = packet.getAddress();
		final String name = receivedParts.length >= 3 ? receivedParts[2] : "";

		notifyHandlers(protocolVersion, name, address.getHostAddress());

		Log.d(LOG_TAG, receivedString + " " + packet.getAddress() + " Thread: "
			+ Thread.currentThread().getId());
		// } else {
		// notifyHandlers(protocolVersion);
		// }
	  }
	  socket.close();
	  Log.d(LOG_TAG, "ReceiverThread: shutdown complete");
	} catch (final IOException x) {
	  Log.e(LOG_TAG, "problem receiving", x);
	}
  }

  private void receive(final DatagramPacket packet) throws IOException {
	try {
	  socket.receive(packet);
	} catch (final SocketException x) {
	  // swallow, this is expected when being interrupted by
	  // socket.close()
	  x.printStackTrace();
	}
  }

  public void shutdown() {
	Log.d(LOG_TAG, "ReceiverThread: initiating shutdown");
	running = false;
	handlers.clear();
	socket.close(); // interrupt
	interrupt();
	try {
	  join();
	} catch (final InterruptedException x) {
	  throw new RuntimeException(x);
	}
  }
}
