package org.cbase.blinkendroid.network.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.cbase.blinkendroid.BlinkendroidApp;
import org.cbase.blinkendroid.network.ConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * datapacket:
 * PROTO [Connection]
 * COMMAND [SYN|ACK|SYNACK|..]
 * ConnectionID [random connection id]
 */

public class ConnectionState implements CommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionState.class);

    
    public static enum Command {
	SYN, ACK, SYNACK, RESET, HEARTBEAT, REQUEST_DIRECT_HEARTBEAT
    }

    public static enum Connstate {
	NONE, SYNWAIT, SYNACKWAIT, ACKWAIT, ESTABLISHED
    }

    public Connstate m_state;
    private int m_connId;
    private InetSocketAddress m_SocketAddr;
    private ConnectionListener m_Listener;
    private long m_LastSeen;
    private ClientSocket mClientSocket;
    private DirectTimerThread directTimerThread;
    private int directTimerThreadRequestCounter = 3;

    /**
     * This thread sends the global time to connected devices.
     */
    class DirectTimerThread extends Thread {

	volatile private boolean running = true;

	@Override
	public void run() {
	    this.setName("SRV Send DirectTimer");
	    logger.debug( "DirectTimerThread started");
	    while (running) {
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException e) {
		    // swallow
		}
		if (!running) // fast exit
		    break;

		logger.debug( "DirectTimerThread running for " + m_connId);
		ByteBuffer out = ByteBuffer.allocate(128);
		out.putInt(BlinkendroidApp.PROTOCOL_HEARTBEAT);
		out.putInt(ConnectionState.Command.HEARTBEAT.ordinal());
		out.putLong(System.currentTimeMillis());
		try {
		    // ConnectionState.this.send(out); this adds protocol 1
		    mClientSocket.send(out);
		} catch (IOException e) {
		    logger.error( "", e);
		}
	    }
	    logger.debug( "DirectTimerThread stopped");
	}

	public void shutdown() {
	    running = false;
	    interrupt();
	    logger.debug( "DirectTimerThread initiating shutdown");
	}
    }

    /**
     * 
     * @param connection
     *            interface Connection which used to send packets
     * @param socketAddr
     *            contains destination ip + port
     * @param listener
     *            will be informed about connection state changes
     */
    public ConnectionState(ClientSocket clientSocket, ConnectionListener listener) {
	m_state = Connstate.NONE;
	m_connId = 0;
	mClientSocket = clientSocket;
	m_Listener = listener;
    }

    public void shutdown() {
	sendReset();
    }

    public void handle(SocketAddress socketAddr, ByteBuffer bybuff) throws IOException {
	final int iCommand = bybuff.getInt();
	final int connId = bybuff.getInt();
	System.out.println("handle " + iCommand + " " + connId);
	/*
	 * if ( Command.values().length > iCommand || 0 < iCommand ) { // ignore
	 * unknown commands return; }
	 */
	final Command command = Command.values()[iCommand];

	// syn is a special case, because we dont have a connection
	if (command == Command.SYN) {
	    receivedSyn(connId);
	    return;
	}

	// heartbeat is also a special case, because it does not need a connId
	if (command == Command.HEARTBEAT) {
	    receivedHeartbeat();
	    return;
	}

	if (connId != this.m_connId) {
	    return; // TODO send reset - not our connection
	}

	switch (command) {
	case ACK:
	    receivedAck();
	    break;
	case SYNACK:
	    receivedSynAck();
	    break;
	case RESET:
	    receivedReset();
	    break;
	case REQUEST_DIRECT_HEARTBEAT:
	    receivedDirectHeartbeatRequest();
	}
    }

    public void openConnection() {
	sendSyn();
    }

    protected void stateChange(Connstate newState) {
	logger.debug( "ConnectionStateChanged " + newState);
	if (newState == m_state) {
	    return;
	}

	if (newState == Connstate.NONE) {
	    // if there is a directtimerthread running for this client kill him
	    if (null != directTimerThread) {
		directTimerThread.shutdown();
		try {
		    directTimerThread.join(1000);
		} catch (InterruptedException e) {
		    // swallow bitch
		}
	    }
	    m_Listener.connectionClosed(mClientSocket);
	}

	if (newState == Connstate.ESTABLISHED) {
	    m_LastSeen = System.currentTimeMillis();
	    m_Listener.connectionOpened(mClientSocket);
	}

	m_state = newState;

    }

    protected void receivedSynAck() throws IOException {
	logger.debug( "receivedSynAck");
	if (m_state == Connstate.SYNACKWAIT) {
	    stateChange(Connstate.ESTABLISHED);
	    sendAck();
	}
    }

    protected void receivedSyn(int connId) {
	if (m_state == Connstate.NONE) {
	    m_connId = connId;
	    logger.debug( "receivedSyn");
	    sendSynAck();
	    stateChange(Connstate.ACKWAIT);
	}
    }

    protected void receivedAck() {
	if (m_state == Connstate.ACKWAIT) {
	    logger.debug( "receivedAck");
	    stateChange(Connstate.ESTABLISHED);
	}
    }

    protected void receivedReset() {
	logger.debug( "receivedReset");
	stateChange(Connstate.NONE);
    }

    protected void receivedDirectHeartbeatRequest() {
	logger.debug( "receivedDirectHeartbeatRequest " + m_connId);
	if (directTimerThread == null) {
	    directTimerThread = new DirectTimerThread();
	    directTimerThread.start();
	}
    }

    protected void receivedHeartbeat() {
	m_LastSeen = System.currentTimeMillis();
	System.out.printf("Received Heartbeat %d - I'm %s\n", m_LastSeen, this.getClass().getName());
    }

    protected void sendSyn() {
	logger.debug( "sendSyn");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.SYN.ordinal());
	m_connId = (int) System.currentTimeMillis();
	out.putInt(m_connId);// TODO set uuid
	stateChange(Connstate.SYNACKWAIT);
	try {
	    send(out);
	} catch (IOException e) {
	    logger.error( "sendSyn caused an Exception", e);
	}
    }

    protected void sendSynAck() {
	logger.debug( "sendSynAck " + m_connId);
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.SYNACK.ordinal());
	out.putInt(m_connId);
	try {
	    send(out);
	} catch (IOException e) {
	    logger.error( "sendSynAck failed", e);
	}
    }

    protected void sendAck() {
	logger.debug( "sendAck");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.ACK.ordinal());
	out.putInt(m_connId);
	try {
	    send(out);
	} catch (IOException e) {
	    logger.error( "sendAck failed", e);
	}
    }

    protected void sendReset() {
	logger.debug( "sendReset");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.RESET.ordinal());
	out.putInt(m_connId);
	try {
	    send(out);
	} catch (IOException e) {
	    logger.error( "sendReset failed", e);
	}

	stateChange(Connstate.NONE);
    }

    protected void sendDirectHeartbeatRequest() {
	logger.debug( "sendDirectHeartbeatRequest");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.REQUEST_DIRECT_HEARTBEAT.ordinal());
	out.putInt(m_connId);
	try {
	    send(out);
	} catch (IOException e) {
	    logger.error( "requestDirectHeartbeat failed", e);
	}
    }

    protected void sendHeartbeat() {
	logger.debug( "sendHeartbeat");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.HEARTBEAT.ordinal());
	out.putInt(m_connId);
	try {
	    send(out);
	} catch (IOException e) {
	    logger.error( "sendHeartbeat failed", e);
	}
    }

    /**
     * 
     * @param timeout
     *            in seconds
     */
    public void checkTimeout(int timeout) {
	logger.debug( "checkTimeout");
	if (m_state != Connstate.ESTABLISHED) {
	    return;
	}
	long time = System.currentTimeMillis();
	if (m_LastSeen + timeout * 1000 < time) {
	    System.out.printf("Timeout %s\n", this.getClass().getName());
	    // request 3 times directHeartBeat
	    if (directTimerThreadRequestCounter > 0) {
		sendDirectHeartbeatRequest();
		directTimerThreadRequestCounter--;
		// reset last_seen
		m_LastSeen = System.currentTimeMillis();
	    } else
		sendReset();
	}
    }

    protected void send(ByteBuffer command) throws IOException {
	ByteBuffer out = ByteBuffer.allocate(command.position() + Integer.SIZE);
	out.putInt(BlinkendroidApp.PROTOCOL_CONNECTION); /* protocol header */
	out.put(command.array(), 0, command.position());
	mClientSocket.send(out);
    }
}