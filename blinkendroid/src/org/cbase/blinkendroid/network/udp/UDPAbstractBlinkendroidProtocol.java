package org.cbase.blinkendroid.network.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.cbase.blinkendroid.BlinkendroidApp;
import org.cbase.blinkendroid.network.ConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UDPAbstractBlinkendroidProtocol implements UDPDirectConnection {

    private static final Logger logger = LoggerFactory.getLogger(UDPAbstractBlinkendroidProtocol.class);

    protected DatagramSocket mSocket;
    protected ReceiverThread receiverThread;
    protected final HashMap<Integer, CommandHandler> handlers = new HashMap<Integer, CommandHandler>();
    protected List<ConnectionListener> connectionListener = new ArrayList<ConnectionListener>();
    protected boolean server;

    protected UDPAbstractBlinkendroidProtocol(final DatagramSocket socket) throws IOException {
	this.mSocket = socket;
	receiverThread = new ReceiverThread();
	receiverThread.start();
    }

    public void addConnectionListener(ConnectionListener connectionListener) {
	this.connectionListener.add(connectionListener);
    }

    public void registerHandler(Integer proto, CommandHandler handler) {
	handlers.put(proto, handler);
    }

    public void unregisterHandler(CommandHandler handler) {
	handlers.remove(handler);
    }

    public void close() {
	mSocket.close();
	logger.info(getMyName() + " BlinkendroidProtocol: Socket closed.");
    }

    public void shutdown() {
	if (null != receiverThread) {
	    receiverThread.shutdown();
	}
	handlers.clear();
	logger.info(getMyName() + " Protocol shutdown.");
	close();
    }

    protected void receive(DatagramPacket packet) throws IOException {
	InetAddress inetAddress = packet.getAddress();
	int port = packet.getPort();
	InetSocketAddress socketAddress = new InetSocketAddress(inetAddress.getHostAddress(), port);
	ByteBuffer in = ByteBuffer.wrap(packet.getData());
	int proto = in.getInt();
	int pos = in.position();
	// logger.info("BlinkendroidClient received Protocol: " + proto);
	if (proto == BlinkendroidApp.PROTOCOL_HEARTBEAT) {
	    for (CommandHandler h : handlers.values()) {
		h.handle(socketAddress, in);
		in.position(pos);
	    }
	    return;
	}

	CommandHandler handler = handlers.get(proto);

	if (null != handler) {
	    handler.handle(socketAddress, in);
	}
    }

    // Inner classes:
    /**
     * A thread that receives information
     */
    class ReceiverThread extends Thread {

	volatile private boolean running = true;

	@Override
	public void run() {
	    this.setName("--- ReceiverThread");
	    running = true;
	    logger.info("InputThread started");
	    byte[] receiveData;
	    DatagramPacket receivePacket;
	    try {
		mSocket.setSoTimeout(1000);
		while (running) {
		    receiveData = new byte[1024];
		    receivePacket = new DatagramPacket(receiveData, receiveData.length);
		    // logger.info(this.getName() + " received " +
		    // receivePacket.toString());
		    try {
			mSocket.receive(receivePacket);
			receive(receivePacket);
		    } catch (SocketTimeoutException e) {
			// swallow every second
		    } catch (Exception e) {
			logger.error("mSocket.receive failed. go on", e);
		    }
		}
	    } catch (SocketException e) {
		logger.error("ReceiverThread Socket closed", e);
	    } catch (IOException e) {
		logger.error("ReceiverThread IOException", e);
	    }
	}

	public void shutdown() {
	    logger.info(getMyName() + " ReceiverThread shutdown start");
	    running = false;
	    interrupt();
	    logger.info(getMyName() + " ReceiverThread shutdown interrupted");
	    try {
		join();
	    } catch (InterruptedException e) {
		logger.error(getMyName() + " ReceiverThread join failed", e);
	    }
	    logger.info(getMyName() + " ReceiverThread shutdown joined & end");
	}
    }

    public void send(InetSocketAddress socketAddr, ByteBuffer out) throws IOException {
	mSocket.send(new DatagramPacket(out.array(), out.position(), socketAddr));
    }

    protected String getMyName() {
	if (server) {
	    return "Server ";// + socket.getRemoteSocketAddress();
	} else {
	    return "Client ";// + socket.getRemoteSocketAddress();
	}
    }
}