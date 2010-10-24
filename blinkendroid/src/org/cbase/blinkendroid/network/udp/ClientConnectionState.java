package org.cbase.blinkendroid.network.udp;

import org.cbase.blinkendroid.network.ConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientConnectionState extends ConnectionState implements CommandHandler {

    private ClientConnectionHeartbeat mHeartbeater;

    private static final Logger logger = LoggerFactory.getLogger(ClientConnectionState.class);

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

    /**
     * This thread sends heartbeat to the server
     */
    class ClientConnectionHeartbeat extends Thread {

	volatile private boolean running = true;

	@Override
	public void run() {
	    this.setName("CLI Heartbeat");
	    logger.debug("ClientConnectionState started");
	    while (running) {
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException e) {
		    // swallow
		}
		if (!running) // fast exit
		    break;
		sendHeartbeat();
		checkTimeout(5);
	    }
	    logger.debug("ClientConnectionState stopped");
	}

	public void shutdown() {
	    running = false;
	    interrupt();
	    // TODO where is the join???
	    logger.debug("ClientConnectionState initiating shutdown");
	}
    }

}
