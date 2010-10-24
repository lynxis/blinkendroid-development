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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.cbase.blinkendroid.BlinkendroidApp;
import org.cbase.blinkendroid.network.ConnectionListener;
import org.cbase.blinkendroid.network.udp.ConnectionState.Command;
import org.cbase.blinkendroid.server.PlayerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UDPServerProtocolManager extends UDPAbstractBlinkendroidProtocol implements ConnectionListener {

    private static final Logger logger = LoggerFactory.getLogger(UDPServerProtocolManager.class);

    protected GlobalTimerThread globalTimerThread;
    private PlayerManager mPlayerManager;

    public void setPlayerManager(PlayerManager mPlayerManager) {
	this.mPlayerManager = mPlayerManager;
    }

    public UDPServerProtocolManager(final DatagramSocket socket) throws IOException {
	super(socket);
    }

    public void startTimerThread() {
	if (globalTimerThread != null) {
	    globalTimerThread.shutdown();
	    // TODO where is the join?
	}
	globalTimerThread = new GlobalTimerThread();
	globalTimerThread.start();
    }

    @Override
    public void shutdown() {
	// TODO where is the join?
	if (null != globalTimerThread)
	    globalTimerThread.shutdown();
	super.shutdown();
    }

    @Override
    protected void receive(DatagramPacket packet) throws IOException {
	InetSocketAddress from = new InetSocketAddress(packet.getAddress().getHostAddress(), packet.getPort());

	/* every Client has his own connectionHandler ! */

	ByteBuffer in = ByteBuffer.wrap(packet.getData());
	int proto = in.getInt();

	CommandHandler handler = handlers.get(proto);
	if (null != handler) {
	    handler.handle(from, in);
	} else {
	    if (mPlayerManager != null) {
		mPlayerManager.handle(this, from, proto, in);
	    }
	}
    }

    public void sendBroadcast(ByteBuffer out) {
	try {
	    // TODO: view SenderThread in broadcast and increase performance by
	    // removing constant creation of InetSocketAddresses
	    send(new InetSocketAddress(InetAddress.getAllByName("255.255.255.255")[0],
		    BlinkendroidApp.BROADCAST_CLIENT_PORT), out);
	} catch (UnknownHostException e) {
	    logger.error("Don't know where to send the broadcast", e);
	} catch (IOException e) {
	    logger.error("IOException", e);
	}
    }

    /**
     * This thread sends the global time to connected devices.
     */
    class GlobalTimerThread extends Thread {

	volatile private boolean running = true;

	@Override
	public void run() {
	    this.setName("SRV Send GlobalTimer");
	    logger.debug("GlobalTimerThread started");
	    while (running) {
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException e) {
		    // swallow
		}
		if (!running) // fast exit
		    break;

		ByteBuffer out = ByteBuffer.allocate(128);
		out.putInt(BlinkendroidApp.PROTOCOL_HEARTBEAT);
		out.putInt(Command.HEARTBEAT.ordinal());
		out.putLong(System.currentTimeMillis());
		sendBroadcast(out);
		// logger.debug( "GlobalTimerThread Broadcast sent: " + out);
	    }
	    logger.debug("GlobalTimerThread stopped");
	}

	public void shutdown() {
	    running = false;
	    interrupt();
	    logger.debug("GlobalTimerThread initiating shutdown");
	}
    }

    public void connectionClosed(ClientSocket clientSocket) {
	for (ConnectionListener connListener : connectionListener) {
	    connListener.connectionClosed(clientSocket);
	}
    }

    public void connectionOpened(ClientSocket clientSocket) {
	for (ConnectionListener connListener : connectionListener) {
	    connListener.connectionOpened(clientSocket);
	}
    }
}
