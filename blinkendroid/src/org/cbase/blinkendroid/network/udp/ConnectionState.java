package org.cbase.blinkendroid.network.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.cbase.blinkendroid.Constants;
import org.cbase.blinkendroid.network.ConnectionListener;

import android.util.Log;

/*
 * datapacket:
 * PROTO [Connection]
 * COMMAND [SYN|ACK|SYNACK|..]
 * ConnectionID [random connection id]
 */

public class ConnectionState implements CommandHandler {

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
  private int directTimerThreadRequestCounter	=	3;
  /**
   * This thread sends the global time to connected devices.
   */
  class DirectTimerThread extends Thread {

	volatile private boolean running = true;

	@Override
	public void run() {
	  this.setName("SRV Send DirectTimer");
	  Log.d(Constants.LOG_TAG, "DirectTimerThread started");
	  while (running) {
		try {
		  Thread.sleep(1000);
		} catch (InterruptedException e) {
		  // swallow
		}
		if (!running) // fast exit
		  break;

		Log.d(Constants.LOG_TAG, "DirectTimerThread running for " + m_connId);
		ByteBuffer out = ByteBuffer.allocate(128);
		out.putInt(Constants.PROTOCOL_CONNECTION);
		out.putInt(ConnectionState.Command.HEARTBEAT.ordinal());
		out.putLong(System.currentTimeMillis());
		try {
		  ConnectionState.this.send(out);
		} catch (IOException e) {
		  Log.e(Constants.LOG_TAG, "", e);
		}
	  }
	  Log.d(Constants.LOG_TAG, "DirectTimerThread stopped");
	}
  }

  /**
   * 
   * @param connection
   *          interface Connection which used to send packets
   * @param socketAddr
   *          contains destination ip + port
   * @param listener
   *          will be informed about connection state changes
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
	 System.out.println("handle "+iCommand+" "+connId);
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
	Log.d(Constants.LOG_TAG, "ConnectionStateChanged " + newState);
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
	Log.d(Constants.LOG_TAG, "receivedSynAck");
	if (m_state == Connstate.SYNACKWAIT) {
	  stateChange(Connstate.ESTABLISHED);
	  sendAck();
	}
  }

  protected void receivedSyn(int connId) {
	if (m_state == Connstate.NONE) {
	  m_connId = connId;
	  Log.d(Constants.LOG_TAG, "receivedSyn");
	  sendSynAck();
	  stateChange(Connstate.ACKWAIT);
	}
  }

  protected void receivedAck() {
	if (m_state == Connstate.ACKWAIT) {
	  Log.d(Constants.LOG_TAG, "receivedAck");
	  stateChange(Connstate.ESTABLISHED);
	}
  }

  protected void receivedReset() {
	Log.d(Constants.LOG_TAG, "receivedReset");
	stateChange(Connstate.NONE);
  }

  protected void receivedDirectHeartbeatRequest() {
	Log.d(Constants.LOG_TAG, "receivedDirectHeartbeatRequest");
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
	Log.d(Constants.LOG_TAG, "sendSyn");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.SYN.ordinal());
	out.putInt(7);
	m_connId = 7;
	stateChange(Connstate.SYNACKWAIT);
	try {
	  send(out);
	} catch (IOException e) {
	  Log.e(Constants.LOG_TAG, "sendSyn caused an Exception", e);
	}
  }

  protected void sendSynAck() {
	Log.d(Constants.LOG_TAG, "sendSynAck " + m_connId);
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.SYNACK.ordinal());
	out.putInt(m_connId);
	try {
	  send(out);
	} catch (IOException e) {
	  Log.e(Constants.LOG_TAG, "sendSynAck failed", e);
	}
  }

  protected void sendAck() {
	Log.d(Constants.LOG_TAG, "sendAck");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.ACK.ordinal());
	out.putInt(m_connId);
	try {
	  send(out);
	} catch (IOException e) {
	  Log.e(Constants.LOG_TAG, "sendAck failed", e);
	}
  }

  protected void sendReset() {
	Log.d(Constants.LOG_TAG, "sendReset");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.RESET.ordinal());
	out.putInt(m_connId);
	try {
	  send(out);
	} catch (IOException e) {
	  Log.e(Constants.LOG_TAG, "sendReset failed", e);
	}

	stateChange(Connstate.NONE);
  }

  protected void sendDirectHeartbeatRequest() {
	Log.d(Constants.LOG_TAG, "sendDirectHeartbeatRequest");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.REQUEST_DIRECT_HEARTBEAT.ordinal());
	out.putInt(m_connId);
	try {
	  send(out);
	} catch (IOException e) {
	  Log.e(Constants.LOG_TAG, "requestDirectHeartbeat failed", e);
	}
  }

  protected void sendHeartbeat() {
	Log.d(Constants.LOG_TAG, "sendHeartbeat");
	ByteBuffer out = ByteBuffer.allocate(1024);
	out.putInt(Command.HEARTBEAT.ordinal());
	out.putInt(m_connId);
	try {
	  send(out);
	} catch (IOException e) {
	  Log.e(Constants.LOG_TAG, "sendHeartbeat failed", e);
	}
  }

  /**
   * 
   * @param timeout
   *          in seconds
   */
  public void checkTimeout(int timeout) {
	Log.d(Constants.LOG_TAG, "checkTimeout");
	if (m_state != Connstate.ESTABLISHED) {
	  return;
	}
	long time = System.currentTimeMillis();
	if (m_LastSeen + timeout * 1000 < time) {
	  System.out.printf("Timeout %s\n", this.getClass().getName());
	  //request 3 times directHeartBeat
	  if(directTimerThreadRequestCounter>0)
	  {
		sendDirectHeartbeatRequest();
		directTimerThreadRequestCounter--;
		//reset last_seen
		m_LastSeen	=	System.currentTimeMillis();
	  }
	  else
		sendReset();
	}
  }

  protected void send(ByteBuffer command) throws IOException {
	ByteBuffer out = ByteBuffer.allocate(command.position() + Integer.SIZE);
	out.putInt(Constants.PROTOCOL_CONNECTION); /* protocol header */
	out.put(command.array(), 0, command.position());
	mClientSocket.send(out);
  }
}