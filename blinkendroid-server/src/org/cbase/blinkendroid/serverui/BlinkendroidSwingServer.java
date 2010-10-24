package org.cbase.blinkendroid.serverui;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.cbase.blinkendroid.BlinkendroidApp;
import org.cbase.blinkendroid.network.ConnectionListener;
import org.cbase.blinkendroid.network.broadcast.ReceiverThread;
import org.cbase.blinkendroid.network.udp.ClientSocket;
import org.cbase.blinkendroid.player.bml.BLMHeader;
import org.cbase.blinkendroid.player.bml.BLMManager;
import org.cbase.blinkendroid.player.image.ImageHeader;
import org.cbase.blinkendroid.player.image.ImageManager;
import org.cbase.blinkendroid.server.BlinkendroidServer;
import org.cbase.blinkendroid.server.TicketManager;

public final class BlinkendroidSwingServer implements ConnectionListener {

    private ReceiverThread receiverThread;
    private TicketManager ticketManager;
    private BlinkendroidServer blinkendroidServer;

    private BLMManager blmManager;
    private ImageManager imageManager;
    private BlinkendroidFrame serverUI;
    
    public ImageManager getImageManager() {
	return imageManager;
    }

    public BLMManager getBlmManager() {
	return blmManager;
    }

    public BlinkendroidFrame getUI() {
	return serverUI;
    }

    public void setMaxClients(int maxClients) {
	if(maxClients < 0) {
	    return;
	}
	
	ticketManager.setMaxClients(maxClients);
	System.out.println("Max clients changed to " + maxClients);
    }

    public int getMaxClients() {
	return ticketManager.getMaxClients();
    }

    public void setUI(BlinkendroidFrame serverUI) {
	this.serverUI = serverUI;
    }

    public boolean isRunning() {
	if (blinkendroidServer != null) {
	    return blinkendroidServer.isRunning();
	} else {
	    return false;
	}
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

	SwingUtilities.invokeLater(new Runnable() {
	    public void run() {
		JFrame.setDefaultLookAndFeelDecorated(true);
		BlinkendroidSwingServer server = new BlinkendroidSwingServer();

		JFrame.setDefaultLookAndFeelDecorated(true);
		BlinkendroidFrame frame = new BlinkendroidFrame(server);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		server.setUI(frame);
		frame.setVisible(true);

		server.loadMedia();
	    }
	});
    }

    public BlinkendroidSwingServer() {
	super();
	ticketManager = new TicketManager("BlinkendroidSwingServer");
	ticketManager.setMaxClients(20);
    }

    public void loadMedia() {
	blmManager = new BLMManager();
	getBlmManager().readMovies(getUI(),  "c:"+File.separator+"blinkendroid");

	System.out.println("Setting " + getUI() + "as listener for movies");

	imageManager = new ImageManager();
	getImageManager().readImages(getUI(), "c:"+File.separator+"blinkendroid");
    }

    public void switchMovie(BLMHeader movieHeader) {
	blinkendroidServer.switchMovie(movieHeader);
    }
    
    public void switchImage(ImageHeader imgHeader) {
	blinkendroidServer.switchImage(imgHeader);
    }
    
    public void start() {
	if (null == blinkendroidServer) {
	    // start recieverthread
	    receiverThread = new ReceiverThread(
		    BlinkendroidApp.BROADCAST_ANNOUCEMENT_SERVER_PORT,
		    BlinkendroidApp.CLIENT_BROADCAST_COMMAND);
	    receiverThread.addHandler(ticketManager);
	    receiverThread.start();

	    blinkendroidServer = new BlinkendroidServer(
		    BlinkendroidApp.BROADCAST_SERVER_PORT);
	    blinkendroidServer.addConnectionListener(this);
	    blinkendroidServer.addConnectionListener(ticketManager);
	    blinkendroidServer.addConnectionListener(getUI());

	    blinkendroidServer.start();
	} else {
	    receiverThread.shutdown();
	    receiverThread = null;

	    blinkendroidServer.shutdown();
	    blinkendroidServer = null;

	    ticketManager.reset();
	}
    }

	public void connectionClosed(ClientSocket clientSocket) {
		// TODO Auto-generated method stub
		
	}

	public void connectionOpened(ClientSocket clientSocket) {
		// TODO Auto-generated method stub
		
	}

}
