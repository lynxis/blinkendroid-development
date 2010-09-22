package org.cbase.blinkendroid.network.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.cbase.blinkendroid.Constants;
import org.cbase.blinkendroid.network.ConnectionListener;

/*
 * datapacket:
 * PROTO [Connection]
 * COMMAND [SYN|ACK|SYNACK|..]
 * ConnectionID [random connection id]
 */

public class ConnectionState implements CommandHandler {

    public static enum Command {
	SYN, ACK, SYNACK, RESET, HEARTBEAT
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

    /**
     * 
     * @param connection
     *            interface Connection which used to send packets
     * @param socketAddr
     *            contains destination ip + port
     * @param listener
     *            will be informed about connection state changes
     */
    public ConnectionState(ClientSocket clientSocket,
	    ConnectionListener listener) {
	m_state = Connstate.NONE;
	m_connId = 0;
	mClientSocket = clientSocket;
	m_Listener = listener;
    }

    public void shutdown() {
	sendReset();
    }

    @Override
    public void handle(SocketAddress socketAddr, ByteBuffer bybuff)
	    throws IOException {
	final int iCommand = bybuff.getInt();
	final int connId = bybuff.getInt();
	// System.out.println("handle "+iCommand+" "+connId);
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
	}
    }

    public void openConnection() {
	sendSyn();
    }

    protected void stateChange(Connstate newState) {
	System.out.println("stateChanged " + newState);
	if (newState == m_state) {
	    return;
	}

	if (newState == Connstate.NONE) {
	    m_Listener.connectionClosed(mClientSocket);
	}

	if (newState == Connstate.ESTABLISHED) {
	    m_LastSeen = System.currentTimeMillis();
	    m_Listener.connectionOpened(mClientSocket);
	}

	m_state = newState;

    }

    protected void receivedSynAck() throws IOException {
	System.out.println("receivedSynAck");
	if (m_state == Connstate.SYNACKWAIT) {
	    stateChange(Connstate.ESTABLISHED);
	    sendAck();
	}
    }

    protected void receivedSyn(int connId) {
	if (m_state == Connstate.NONE) {
	    m_connId = connId;
	    System.out.println("receivedSyn");
	    sendSynAck();
	    stateChange(Connstate.ACKWAIT);
	}
    }

    protected void receivedAck() {
	if (m_state == Connstate.ACKWAIT) {
	    System.out.println("receivedAck");
	    stateChange(Connstate.ESTABLISHED);
	}
    }

    protected void receivedReset() {
	System.out.println("receivedReset");
	stateChange(Connstate.NONE);
    }

    protected void receivedHeartbeat() {
	m_LastSeen = System.currentTimeMillis();
	// System.out.printf("Received Heartbeat %d - I'm %s\n", m_LastSeen,
	// this.getClass().getName());
    }

    protected void sendSyn() {
	System.out.println("sendSyn");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.SYN.ordinal());
	out.putInt(7);
	m_connId = 7;
	stateChange(Connstate.SYNACKWAIT);
	try {
	    send(out);
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	// TODO Auto-generated method stub
    }

    protected void sendSynAck() {
	System.out.println("sendSynAck " + m_connId);
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.SYNACK.ordinal());
	out.putInt(m_connId);
	try {
	    send(out);
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	// TODO Auto-generated method stub
    }

    protected void sendAck() {
	System.out.println("sendAck");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.ACK.ordinal());
	out.putInt(m_connId);
	try {
	    send(out);
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	// TODO Auto-generated method stub
    }

    protected void sendReset() {
	System.out.println("sendReset");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.RESET.ordinal());
	out.putInt(m_connId);
	try {
	    send(out);
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	stateChange(Connstate.NONE);
	// TODO Auto-generated method stub
    }

    protected void sendHeartbeat() {
	// System.out.println("sendHeartbeat");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.HEARTBEAT.ordinal());
	out.putInt(m_connId);
	try {
	    send(out);
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	// TODO Auto-generated method stub
    }

    /**
     * 
     * @param timeout
     *            in seconds
     */
    public void checkTimeout(int timeout) {
	if (m_state != Connstate.ESTABLISHED) {
	    return;
	}
	long time = System.currentTimeMillis();
	if (m_LastSeen + timeout * 1000 < time) {
	    System.out.printf("Timeout %s\n", this.getClass().getName());
	    sendReset();
	    // 10 seconds
	}
    }

    protected void send(ByteBuffer command) throws IOException {
	ByteBuffer out = ByteBuffer.allocate(command.position() + Integer.SIZE);
	out.putInt(Constants.PROTOCOL_CONNECTION); /* protocol header */
	out.put(command.array(), 0, command.position());
	mClientSocket.send(out);
    }
}