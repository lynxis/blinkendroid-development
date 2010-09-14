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
import java.net.Socket;
import java.net.SocketException;

import org.cbase.blinkendroid.Constants;
import org.cbase.blinkendroid.ServerActivity;
import org.cbase.blinkendroid.network.ConnectionListener;
import org.cbase.blinkendroid.network.tcp.TCPVideoServer;
import org.cbase.blinkendroid.network.udp.UDPServerProtocolManager;
import org.cbase.blinkendroid.player.bml.BLMHeader;

import android.util.Log;

public class BlinkendroidServer {//TODO schtief warum hier kein thread in server ui?

	volatile private boolean running = false;
	volatile private DatagramSocket serverSocket;
	private int port = -1;
	private PlayerManager playerManager;
	private ConnectionListener connectionListener; // TODO muss der Server wissen wer der PlayerManager ist ?
	private UDPServerProtocolManager m_ServerProto;
	private TCPVideoServer videoSocket;

	public BlinkendroidServer(ConnectionListener connectionListener, int port) {
		this.connectionListener = connectionListener;
		this.port = port;
	}

	public void start() {

		running = true;

		try {
			videoSocket = new TCPVideoServer(port);
			videoSocket.start();
			
			serverSocket = new DatagramSocket(port);
			serverSocket.setBroadcast(true);
			serverSocket.setReuseAddress(true);
			m_ServerProto = new UDPServerProtocolManager(serverSocket);
			
			playerManager = new PlayerManager(m_ServerProto);
			playerManager.setVideoServer(videoSocket);
			m_ServerProto.setPlayerManager(playerManager);
			
			m_ServerProto.addConnectionListener(this.connectionListener);
			m_ServerProto.startTimerThread();
			
		// how is the protocol connected to the logic ?
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void shutdown() {
		videoSocket.shutdown();
		playerManager.shutdown();
		m_ServerProto.shutdown();
		serverSocket.close();
	}

	public boolean isRunning() {
		return running;
	}

	public void switchMovie(BLMHeader blmHeader) {
		playerManager.switchMovie(blmHeader);
	}
}
