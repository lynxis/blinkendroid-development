package org.cbase.blinkendroid.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.cbase.blinkendroid.BlinkendroidApp;
import org.cbase.blinkendroid.network.ConnectionListener;
import org.cbase.blinkendroid.network.tcp.DataServer;
import org.cbase.blinkendroid.network.udp.BlinkendroidProtocol;
import org.cbase.blinkendroid.network.udp.ClientSocket;
import org.cbase.blinkendroid.network.udp.CommandHandler;
import org.cbase.blinkendroid.network.udp.ConnectionState;
import org.cbase.blinkendroid.network.udp.UDPDirectConnection;
import org.cbase.blinkendroid.player.bml.BLMHeader;
import org.cbase.blinkendroid.player.image.ImageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Color;

public class PlayerManager implements ConnectionListener {

    private static final Logger logger = LoggerFactory.getLogger(PlayerManager.class);

    private PlayerClient[][] mMatrixClients = new PlayerClient[20][20];
    private List<PlayerClient> mClients = new ArrayList<PlayerClient>();
    private ConnectionListener connectionListenerManager;
    /*
     * mMatrixClient are active Clients in the matrix mClients are all Clients
     * which are connected
     */

    private int maxX = 1, maxY = 1;
    private long startTime = System.currentTimeMillis();
    private boolean running = true;
    private String filename = null;
    private TimeouterThread timeouter = new TimeouterThread();
    private AtomicInteger arrowColorIndex = new AtomicInteger(new Random().nextInt(ARROW_COLORS.length));
    // private long serverPlayer = System.currentTimeMillis();
    private DataServer videoServer;
    private int runningMediaType = BlinkendroidProtocol.OPTION_PLAY_TYPE_IMAGE;

    public DataServer getVideoServer() {
	return videoServer;
    }

    public void setVideoServer(DataServer videoServer) {
	this.videoServer = videoServer;
    }

    private static int[] ARROW_COLORS = new int[] { Color.RED, Color.BLUE, Color.GREEN, Color.GRAY, Color.YELLOW,
	    Color.TRANSPARENT };

    public PlayerManager(ConnectionListener connectionListenerManager) {
	this.connectionListenerManager = connectionListenerManager;
	this.videoServer = null;
	timeouter.start();
    }

    public synchronized PlayerClient addClient(ClientSocket clientSocket) {
	PlayerClient client = null;
	for (PlayerClient pPlayerClient : mClients) {
	    if (pPlayerClient.getClientSocketAddress().equals(clientSocket)) {
		client = pPlayerClient;
		break;
	    }
	}
	if (client == null) {
	    client = new PlayerClient(this, clientSocket);
	    mClients.add(client);
	}
	return client;
    }

    public synchronized PlayerClient addClientToMatrix(ClientSocket clientSocket) {
	PlayerClient newPlayer = getPlayerClientByClientSocket(clientSocket);
	if (newPlayer == null) {
	    return null;
	} else {
	    return addClientToMatrix(newPlayer);
	}
    }

    public synchronized PlayerClient addClientToMatrix(PlayerClient playerClient) {
	if (!running) {
	    logger.error("PlayerManager not running ignore addClient ");
	    return null;
	}
	if (startTime == 0)
	    startTime = System.currentTimeMillis();

	// TODO finde freien Platz in der Matrix
	boolean found = false;
	for (int i = 0; i < maxY; i++) {
	    for (int j = 0; j < maxY; j++) {
		// wenn freier platz dann nehmen
		if (mMatrixClients[i][j] == null) {
		    playerClient.y = i;
		    playerClient.x = j;
		    found = true;
		    break;
		}
		if (found)
		    break;
	    }
	}
	// wenn nicht gefunden dann erweitern, erst dann y
	if (!found) {
	    // wenn maxX>maxY -> neuen client an maxY+1
	    if (maxX > maxY) {
		playerClient.y = maxY;
		playerClient.x = 0;
		maxY++;
	    } else {
		// else -> neuen client an maxX+1
		playerClient.y = 0;
		playerClient.x = maxX;
		maxX++;
	    }
	}
	logger.info("added Client at pos " + playerClient.x + ":" + playerClient.y);
	mMatrixClients[playerClient.y][playerClient.x] = playerClient;

	playerClient.getBlinkenProtocol().play(startTime, runningMediaType);
	arrow(playerClient);

	if (!found) {
	    clip(true);
	} else {
	    clip(false);
	    playerClient.getBlinkenProtocol().clip(playerClient.startX, playerClient.startY, playerClient.endX,
		    playerClient.endY);
	}

	return playerClient;
	/*
	 * playerClient.play(filename); arrow(pClient); if (!found) clip(true);
	 * else { clip(false); pClient.clip(); }
	 */
	// server starts thread to send globaltime
    }

    private void arrow(final PlayerClient pClient) {
	// TODO remove in final for puzzle
	arrow(pClient, 0, 1, 0);
	arrow(pClient, -1, 1, 45);
	arrow(pClient, -1, 0, 90);
	arrow(pClient, -1, -1, 135);
	arrow(pClient, 0, -1, 180);
	arrow(pClient, +1, -1, 225);
	arrow(pClient, +1, 0, 270);
	arrow(pClient, +1, 1, 315);
    }

    private void arrow(final PlayerClient pClient, final int dx, final int dy, final int deg) {

	final int color = ARROW_COLORS[arrowColorIndex.getAndIncrement() % ARROW_COLORS.length];

	if (pClient.y + dy >= 0 && pClient.x + dx >= 0 && null != mMatrixClients[pClient.y + dy][pClient.x + dx]) {
	    mMatrixClients[pClient.y + dy][pClient.x + dx].getBlinkenProtocol().arrow(deg, color);
	    final int inverseDeg = (deg + 180) % 360;
	    mMatrixClients[pClient.y][pClient.x].getBlinkenProtocol().arrow(inverseDeg, color);
	}
    }

    private void clip(boolean clipAll) {
	// clipping fuer alle berechnen
	// Log.i(Constants.LOG_TAG, "clip maxX "+i+":"+j);
	float startY = 0;
	for (int i = 0; i < maxY; i++) {
	    float startX = 0;
	    for (int j = 0; j < maxX; j++) {
		if (mMatrixClients[i][j] != null) {
		    PlayerClient playerClient = mMatrixClients[i][j];
		    playerClient.startX = startX;
		    playerClient.startY = startY;
		    playerClient.endX = startX + (float) (1.0 / maxX);
		    playerClient.endY = startY + (float) (1.0 / maxY);
		    if (clipAll)
			playerClient.getBlinkenProtocol().clip(playerClient.startX, playerClient.startY,
				playerClient.endX, playerClient.endY);
		}
		startX = startX + (float) (1.0 / maxX);
	    }
	    startY = startY + (float) (1.0 / maxY);
	}
    }

    public synchronized void shutdown() {
	running = false;
	logger.info("PlayerManager.shutdown() start");
	for (int i = 0; i < maxY; i++) {
	    for (int j = 0; j < maxX; j++) {
		if (null != mMatrixClients[i][j]) {
		    logger.info("shutdown PlayerClient " + j + ":" + i);
		    mMatrixClients[i][j].shutdown();
		}
	    }
	}
	timeouter.shutdown();
	logger.info("PlayerManager.shutdown() end!!!");

    }

    public synchronized void removeClientFromMatrix(ClientSocket clientSocket) {
	PlayerClient client = getMatrixClientBySocketAddress(clientSocket.getInetSocketAddress());
	if (client == null) {
	    return;
	} else {
	    removeClientFromMatrix(client);
	}
    }

    public synchronized void removeClientFromMatrix(PlayerClient playerClient) {
	if (!running) {
	    logger.error("PlayerManager not running ignore removeClient");
	    return;
	}

	logger.info("removeClient " + playerClient.x + ":" + playerClient.y);
	mMatrixClients[playerClient.y][playerClient.x] = null;

	boolean newMaxX = true;
	for (int i = 0; i < maxY; i++) {
	    if (mMatrixClients[i][maxX - 1] != null) {
		newMaxX = false;
		break;
	    }
	}
	if (newMaxX && maxX > 1) {
	    maxX--;
	    logger.info("newMaxX " + maxX);
	}

	boolean newMaxY = true;
	for (int i = 0; i < maxX; i++) {
	    if (mMatrixClients[maxY - 1][i] != null) {
		newMaxY = false;
		break;
	    }
	}
	if (newMaxY && maxY > 1) {
	    maxY--;
	    logger.info("newMaxY " + maxY);
	}
	clip(true);
    }

    public void switchMovie(BLMHeader blmHeader) {
	if (videoServer == null) {
	    logger.error("videoserver is null");
	    return;
	} else {
	    runningMediaType = BlinkendroidProtocol.OPTION_PLAY_TYPE_MOVIE;
	    filename = blmHeader.filename;
	    videoServer.setVideoName(filename);
	    logger.info("switch to movie " + blmHeader.title);
	    for (int i = 0; i < maxY; i++) {
		for (int j = 0; j < maxX; j++) {
		    if (null != mMatrixClients[i][j]) {
			logger.info("play PlayerClient " + j + ":" + i + " " + filename);
			mMatrixClients[i][j].getBlinkenProtocol().play(startTime,
				BlinkendroidProtocol.OPTION_PLAY_TYPE_MOVIE);
		    }
		}
	    }
	}
    }

    public void switchImage(ImageHeader imageHeader) {
	if (videoServer == null) {
	    return;
	} else {
	    runningMediaType = BlinkendroidProtocol.OPTION_PLAY_TYPE_IMAGE;
	    filename = imageHeader.filename;
	    videoServer.setImageName(filename);
	    logger.info("switch to image " + imageHeader.title);
	    for (int i = 0; i < maxY; i++) {
		for (int j = 0; j < maxX; j++) {
		    if (null != mMatrixClients[i][j]) {
			logger.info("play PlayerClient " + j + ":" + i + " " + filename);
			mMatrixClients[i][j].getBlinkenProtocol().play(startTime,
				BlinkendroidProtocol.OPTION_PLAY_TYPE_IMAGE);
		    }
		}
	    }
	}
    }

    public synchronized PlayerClient getPlayerClientByClientSocket(ClientSocket clientSocket) {
	PlayerClient resultPlayer = null;
	for (PlayerClient clientPlayer : mClients) {// TODO schtief use hashmap
	    if (clientPlayer.getClientSocketAddress().equals(clientSocket.getInetSocketAddress())) {
		resultPlayer = clientPlayer;
		break;
	    }
	}
	return resultPlayer;
    }

    public synchronized PlayerClient getMatrixClientBySocketAddress(SocketAddress socketAddr) {
	for (int i = 0; i < maxY; i++) {
	    for (int j = 0; j < maxX; j++) {
		if (null != mMatrixClients[i][j]) {
		    if (mMatrixClients[i][j].getClientSocketAddress().equals(socketAddr)) {
			return mMatrixClients[i][j];
		    }
		}
	    }
	}
	return null;
    }

    public synchronized PlayerClient getPlayerClientBySocketAddress(SocketAddress socketAddr) {
	for (PlayerClient pClient : mClients) {// TODO schtief use hashmap
	    if (pClient.getClientSocketAddress().equals(socketAddr))
		return pClient;
	}
	return null;
    }

    public void connectionClosed(ClientSocket clientSocket) {
	// TODO Auto-generated method stub
	// TODO remove Client
	// search for the client
	// remove it
	PlayerClient pClient = getPlayerClientByClientSocket(clientSocket);
	if (pClient == null) {
	    return;
	}
	removeClientFromMatrix(pClient);
	mClients.remove(pClient);
	connectionListenerManager.connectionClosed(clientSocket);
    }

    public void connectionOpened(ClientSocket clientSocket) {
	// TODO Auto-generated method stub
	// TODO addClient
	addClientToMatrix(clientSocket);
	connectionListenerManager.connectionOpened(clientSocket);
    }

    public void checkTimeouts() {
	for (PlayerClient player : mClients) {
	    player.checkTimeout(10);
	}
    }

    public void handle(UDPDirectConnection blinkendroidprotocol, InetSocketAddress socketAddr, int proto,
	    ByteBuffer protoData) {
	PlayerClient client = getPlayerClientBySocketAddress(socketAddr);

	if (client != null) {
	    CommandHandler handler = client.getHandlers().get(proto);
	    if (handler != null) {
		try {
		    client.handle(socketAddr, protoData);
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	} else { // no client found
	    if (proto == BlinkendroidApp.PROTOCOL_CONNECTION) {
		int data = protoData.getInt();
		logger.debug("Playermanager data " + data);
		if (ConnectionState.Command.SYN.ordinal() == data) {
		    // new connection
		    try {
			client = addClient(new ClientSocket(blinkendroidprotocol, socketAddr));
			protoData.rewind();
			protoData.getInt(); // protocol
			client.handle(socketAddr, protoData); // dirty direct
			// call of the
			// protocol
			// handler
		    } catch (SocketException e) {
			logger.error("SocketException in PlayerManager", e);
		    } catch (IOException e) {
			logger.error("IOException in PlayerManager", e);
		    }
		}
	    }
	}
    }

    /**
     * Checks for timeouts
     */
    class TimeouterThread extends Thread {

	volatile private boolean running = true;

	@Override
	public void run() {
	    this.setName("SRV PlayerManager Timeouter");
	    logger.info("TimeouterThread started");
	    while (running) {
		try {
		    Thread.sleep(3000);
		} catch (InterruptedException e) {
		    // swallow
		}
		if (!running) // fast exit
		    break;
		checkTimeouts();
	    }
	    logger.debug("TimeouterThread stopped");
	}

	public void shutdown() {
	    running = false;
	    interrupt();
	    logger.debug("TimeouterThread initiating shutdown");
	}
    }
}
