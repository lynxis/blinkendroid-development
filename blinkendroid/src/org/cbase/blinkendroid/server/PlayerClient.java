package org.cbase.blinkendroid.server;

import java.net.SocketAddress;
import java.util.HashMap;

import org.cbase.blinkendroid.BlinkendroidApp;
import org.cbase.blinkendroid.network.udp.BlinkendroidServerProtocol;
import org.cbase.blinkendroid.network.udp.ClientSocket;
import org.cbase.blinkendroid.network.udp.CommandHandler;
import org.cbase.blinkendroid.network.udp.ConnectionState;

import android.util.Log;

public class PlayerClient extends ConnectionState {

  // position
  int x, y;
  // clipping
  float startX, endX, startY, endY;
  // protocol
  long startTime;
  PlayerManager playerManager;
  private ClientSocket m_clientSocket;
  private BlinkendroidServerProtocol mBlinkenProtocol;

  protected final HashMap<Integer, CommandHandler> handlers = new HashMap<Integer, CommandHandler>();

  public HashMap<Integer, CommandHandler> getHandlers() {
	return handlers;
  }

  public PlayerClient(PlayerManager playerManager, ClientSocket clientSocket) {
	super(clientSocket, playerManager);
	Log.d(BlinkendroidApp.LOG_TAG, "new PlayerClient");
	this.playerManager = playerManager;
	this.m_clientSocket = clientSocket;
	this.registerHandler(BlinkendroidApp.PROTOCOL_CONNECTION, this);
	mBlinkenProtocol = new BlinkendroidServerProtocol(clientSocket);
  }

  public SocketAddress getClientSocketAddress() {
	return m_clientSocket.getInetSocketAddress();
  }

  public void registerHandler(Integer proto, CommandHandler handler) {
	handlers.put(proto, handler);
  }

  public void unregisterHandler(CommandHandler handler) {
	handlers.remove(handler);
  }

  public BlinkendroidServerProtocol getBlinkenProtocol() {
	return mBlinkenProtocol;
  }

}
