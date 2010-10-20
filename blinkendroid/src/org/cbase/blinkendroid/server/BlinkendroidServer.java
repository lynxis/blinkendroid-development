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

package org.cbase.blinkendroid.server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.cbase.blinkendroid.network.ConnectionListener;
import org.cbase.blinkendroid.network.tcp.TCPVideoServer;
import org.cbase.blinkendroid.network.udp.UDPServerProtocolManager;
import org.cbase.blinkendroid.player.bml.BLMHeader;
import org.cbase.blinkendroid.player.image.ImageHeader;

import android.util.Log;

public class BlinkendroidServer {
    // TODO schtief warum hier kein thread in server ui?

    private static final String LOG_TAG = "BlinkendroidServer".intern();
    volatile private boolean running = false;
    volatile private DatagramSocket serverSocket;
    private int port = -1;
    private PlayerManager playerManager;
    private List<ConnectionListener> connectionListeners;
    // TODO muss der Server wissen wer der PlayerManager ist ?
    private UDPServerProtocolManager mServerProto;
    private TCPVideoServer videoSocket;

    public BlinkendroidServer(int port) {
	this.connectionListeners = new ArrayList<ConnectionListener>();
	this.port = port;
    }

    public void addConnectionListener(ConnectionListener connectionListener) {
	this.connectionListeners.add(connectionListener);
    }

    public void start() {

	running = true;

	try {
	    videoSocket = new TCPVideoServer(port);
	    videoSocket.start();

	    serverSocket = new DatagramSocket(port);
	    serverSocket.setBroadcast(true);
	    serverSocket.setReuseAddress(true);
	    mServerProto = new UDPServerProtocolManager(serverSocket);

	    playerManager = new PlayerManager(mServerProto);
	    playerManager.setVideoServer(videoSocket);
	    mServerProto.setPlayerManager(playerManager);

	    for (ConnectionListener connectionListener : connectionListeners) {
		mServerProto.addConnectionListener(connectionListener);
	    }

	    mServerProto.startTimerThread();

	    // how is the protocol connected to the logic ?
	} catch (SocketException e) {
	    Log.e(LOG_TAG, "SocketException", e);
	} catch (IOException e) {
	    Log.e(LOG_TAG, "IOException", e);
	}
    }

    // TODO where is the join?
    public void shutdown() {
	videoSocket.shutdown();
	playerManager.shutdown();
	mServerProto.shutdown();
	serverSocket.close();
    }

    public boolean isRunning() {
	return running;
    }

    public void switchMovie(BLMHeader blmHeader) {
	playerManager.switchMovie(blmHeader);
    }

    public void switchImage(ImageHeader imageHeader) {
	playerManager.switchImage(imageHeader);
    }
}
