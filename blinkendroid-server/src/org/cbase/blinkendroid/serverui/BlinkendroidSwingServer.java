package org.cbase.blinkendroid.serverui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.cbase.blinkendroid.BlinkendroidApp;
import org.cbase.blinkendroid.network.ConnectionListener;
import org.cbase.blinkendroid.network.broadcast.ReceiverThread;
import org.cbase.blinkendroid.network.udp.ClientSocket;
import org.cbase.blinkendroid.player.bml.BLMManager;
import org.cbase.blinkendroid.player.bml.BLMManager.BLMManagerListener;
import org.cbase.blinkendroid.player.image.ImageManager;
import org.cbase.blinkendroid.player.image.ImageManager.ImageManagerListener;
import org.cbase.blinkendroid.server.BlinkendroidServer;
import org.cbase.blinkendroid.server.TicketManager;

public final class BlinkendroidSwingServer implements ConnectionListener, BLMManagerListener, ImageManagerListener {

    private ReceiverThread receiverThread;
    private TicketManager ticketManager;
    private BlinkendroidServer blinkendroidServer;
	private BlinkendroidFrame blinkendroidFrame;
	
    private BLMManager blmManager;
    private ImageManager imageManager;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame.setDefaultLookAndFeelDecorated(true);
				BlinkendroidSwingServer server = new BlinkendroidSwingServer();
				
				 JFrame.setDefaultLookAndFeelDecorated(true);
				    JFrame frame =new BlinkendroidFrame(server);
				    frame.setTitle("My First Swing Application");
				    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				    frame.setVisible(true);
			}
		});
	}

	
	
	public BlinkendroidSwingServer() {
		super();
		ticketManager = new TicketManager("BlinkendroidSwingServer");
		blmManager=	new BLMManager();
		blmManager.readMovies(this, "c:\blinkendroid");
		
		imageManager=	new ImageManager();
		imageManager.readImages(this, "c:\blinkendroid");
	}



	public void startButton() {
		if (null == blinkendroidServer) {
		    // start recieverthread
		    receiverThread = new ReceiverThread(BlinkendroidApp.BROADCAST_ANNOUCEMENT_SERVER_PORT,
			    BlinkendroidApp.CLIENT_BROADCAST_COMMAND);
		    receiverThread.addHandler(ticketManager);
		    receiverThread.start();

		    blinkendroidServer = new BlinkendroidServer(BlinkendroidApp.BROADCAST_SERVER_PORT);
		    blinkendroidServer.addConnectionListener(this);
		    blinkendroidServer.addConnectionListener(ticketManager);

		    blinkendroidServer.start();
		    blinkendroidFrame.getStartStopButton().setText("stop");
		} else {
		    receiverThread.shutdown();
		    receiverThread = null;

		    blinkendroidServer.shutdown();
		    blinkendroidServer = null;

		    ticketManager.reset();
		    blinkendroidFrame.getStartStopButton().setText("start");
		}
	}

	public void setBlinkendroidFrame(BlinkendroidFrame blinkendroidFrame) {
		this.blinkendroidFrame=blinkendroidFrame;
	}

	@Override
	public void connectionClosed(ClientSocket clientSocket) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void connectionOpened(ClientSocket clientSocket) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void moviesReady() {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void imagesReady() {
		// TODO Auto-generated method stub
		
	}
}
