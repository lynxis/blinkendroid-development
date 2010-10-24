package org.cbase.blinkendroid.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

import org.cbase.blinkendroid.BlinkendroidApp;
import org.cbase.blinkendroid.network.ConnectionListener;
import org.cbase.blinkendroid.network.broadcast.IPeerHandler;
import org.cbase.blinkendroid.network.udp.ClientSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicketManager implements IPeerHandler, ConnectionListener {

    private static final Logger logger = LoggerFactory.getLogger(TicketManager.class);

    int maxClients = 2;
    int clients = 0;
    private Set<String> tickets = new HashSet<String>();
    private Set<String> waitingQueue = new HashSet<String>();
    private String ownerName;
    DatagramSocket socket = null;
    private ClientQueueListener clientQueueListener = null;

    public TicketManager(String ownerName) {
        this.ownerName = ownerName;
        try {
            socket = new DatagramSocket(BlinkendroidApp.BROADCAST_ANNOUCEMENT_SERVER_TICKET_PORT);
            socket.setReuseAddress(true);
        } catch (SocketException e) {
            logger.error("new DatagramSocket(Constants.BROADCAST_ANNOUCEMENT_SERVER_TICKET_PORT) failed "
                    + e.getMessage());
        }
    }

    public void foundPeer(String name, String ip, int protocolVersion) {
        // noch platz frei?
        if (clients < maxClients || tickets.contains(ip)) {
            // send ticket to ip
            try {
                InetSocketAddress socketAddr = new InetSocketAddress(ip,
                        BlinkendroidApp.BROADCAST_ANNOUCEMENT_CLIENT_TICKET_PORT);
                String message = BlinkendroidApp.BROADCAST_PROTOCOL_VERSION + " "
                        + BlinkendroidApp.SERVER_TICKET_COMMAND + " " + ownerName;
                final byte[] messageBytes = message.getBytes("UTF-8");
                final DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, socketAddr);
                socket.send(packet);
                if (!tickets.contains(ip)) {
                    clients++;
                    tickets.add(ip);
                    waitingQueue.remove(ip);
                    if (clientQueueListener != null) {
                        clientQueueListener.clientNoLongerWaiting(ip);
                        logger.debug("send Ticket for " + name + " " + ip);
                    }
                } else {
                    logger.debug("resend sent ticket for " + name + " " + ip);
                }
            } catch (Exception e) {
                logger.error("Exception in TicketManager", e);
            }
        } else {
            if (!waitingQueue.contains(ip)) {
                waitingQueue.add(ip);
                if (clientQueueListener != null) {
                    clientQueueListener.clientWaiting(ip);
                }
            }
            logger.debug("Server is full, adding to queue");
        }
        // pech jehabt
    }

    public void setClientQueueListener(ClientQueueListener clientQueueListener) {
        this.clientQueueListener = clientQueueListener;
    }

    public void reset() {
        tickets.clear();
    }

    public void connectionClosed(ClientSocket clientSocket) {
        String ip = clientSocket.getDestinationAddress().getHostAddress();
        clients--;
        tickets.remove(ip);
        clientQueueListener.clientNoLongerWaiting(ip);
        waitingQueue.remove(ip);
    }

//    /**
//     * @return the waitingQueue
//     */
//    public Set<String> getWaitingQueue() {
//        return waitingQueue;
//    }

    public void clientStateChangedFromWaitingToConnected(String ip) {
        tickets.add(ip);
        waitingQueue.remove(ip);
        clients++;
        // maxClients++;
    }

    public void connectionOpened(ClientSocket clientSocket) {
        // TODO clients merken und abhaken
    }

    public int getMaxClients() {
        return maxClients;
    }

    public void setMaxClients(int maxClients) {
        this.maxClients = maxClients;
    }
}
