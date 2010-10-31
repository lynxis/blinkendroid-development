package org.cbase.blinkendroid.server;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;

import org.cbase.blinkendroid.network.udp.BlinkendroidProtocol;
import org.cbase.blinkendroid.network.udp.BlinkendroidServerProtocol;
import org.cbase.blinkendroid.network.udp.ClientSocket;
import org.cbase.blinkendroid.network.udp.CommandHandler;
import org.cbase.blinkendroid.network.udp.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerClient extends ConnectionState {

    // position
    int x, y;
    // clipping
    float startX, endX, startY, endY;
    // protocol
    long startTime;
    PlayerManager playerManager;
    private ClientSocket mclientSocket;
    private BlinkendroidServerProtocol mBlinkenProtocol;
    private static final Logger logger = LoggerFactory.getLogger(PlayerClient.class);

    protected final HashMap<Integer, CommandHandler> handlers = new HashMap<Integer, CommandHandler>();

    public HashMap<Integer, CommandHandler> getHandlers() {
	return handlers;
    }

    public PlayerClient(PlayerManager playerManager, ClientSocket clientSocket) {
	super(clientSocket, playerManager);
	logger.info("new PlayerClient");
	this.playerManager = playerManager;
	this.mclientSocket = clientSocket;
	// this.registerHandler(BlinkendroidApp.PROTOCOL_CONNECTION, this);
	mBlinkenProtocol = new BlinkendroidServerProtocol(playerManager, clientSocket);
    }

    @Override
	public void handle(SocketAddress socketAddr, ByteBuffer bybuff) throws IOException {
	int pos = bybuff.position();
	final int iCommand = bybuff.getInt();
	if (iCommand == BlinkendroidProtocol.COMMAND_LOCATEME) {
	    this.playerManager.arrow(this);
	} else {
	    bybuff.position(pos);
	    super.handle(socketAddr, bybuff);
	}
    }

    public SocketAddress getClientSocketAddress() {
	return mclientSocket.getInetSocketAddress();
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
