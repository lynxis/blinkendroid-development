package org.cbase.blinkendroid.network.udp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.cbase.blinkendroid.Constants;
import org.cbase.blinkendroid.network.ConnectionListener;

import android.util.Log;

public class ClientConnectionState extends ConnectionState implements CommandHandler {
  private ClientConnectionHeartbeat mHeartbeater;

  public ClientConnectionState(ClientSocket clientSocket, ConnectionListener listener) {
	super(clientSocket, listener);
	mHeartbeater = new ClientConnectionHeartbeat();
  }

  @Override
  protected void stateChange(Connstate state) {
	if (state == Connstate.ESTABLISHED) {
	  startHeartbeat();
	}
	if (state == Connstate.NONE) {
	  stopHeartbeat();
	}
	super.stateChange(state);
  }

  public void startHeartbeat() {
	mHeartbeater.start();
  }

  public void stopHeartbeat() {
	mHeartbeater.shutdown();
  }

  // only for debugging client
  @Override
  public void handle(SocketAddress socketAddr, ByteBuffer bybuff) throws IOException {
	int pos = bybuff.position();

//	System.out.printf("CliState received %s", Connstate.values()[bybuff.getInt()].toString());
	bybuff.position(pos);

	super.handle(socketAddr, bybuff);
  }

  /**
   * This thread sends heartbeat to the server
   */
  class ClientConnectionHeartbeat extends Thread {

	volatile private boolean running = true;

	@Override
	public void run() {
	  this.setName("CLI Heartbeat");
	  Log.d(Constants.LOG_TAG, "ClientConnectionState started");
	  while (running) {
		try {
		  Thread.sleep(1000);
		} catch (InterruptedException e) {
		  // swallow
		}
		if (!running) // fast exit
		  break;
		sendHeartbeat();
		checkTimeout(20);
	  }
	  Log.d(Constants.LOG_TAG, "ClientConnectionState stopped");
	}

	public void shutdown() {
	  running = false;
	  interrupt();
	  //TODO where is the join???
	  Log.d(Constants.LOG_TAG, "ClientConnectionState initiating shutdown");
	}
  }
}
