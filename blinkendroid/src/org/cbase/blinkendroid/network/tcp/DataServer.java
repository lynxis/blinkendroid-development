package org.cbase.blinkendroid.network.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataServer extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(DataServer.class);

    volatile private boolean running = true;
    ServerSocket serverSocket;
    private String videoName;

    private String imageName;

    private int videoPort;

    public DataServer(int videoPort) {
	this.videoPort = videoPort;
	this.videoName = null;
	this.imageName = null;
    }

    @Override
    public void run() {
	this.setName("SRV VideoTCPServer");
	try {
	    serverSocket = new ServerSocket(videoPort);
	    serverSocket.setReuseAddress(true);
	    running = true;

	    acceptLoop();

	    serverSocket.close();
	    logger.debug("VideoThread ended!!!!!!! ");

	} catch (IOException e) {
	    logger.error("VideoThread fuckup", e);
	}
    }

    // TODO do we need this method any longer?
    private void acceptLoop() {

	try {
	    while (running) {
		Socket connectionSocket;
		connectionSocket = serverSocket.accept();

		final BlinkendroidDataServerProtocol blinkenVideoProtocol = new BlinkendroidDataServerProtocol(
			connectionSocket, this);

	    }
	} catch (IOException e) {
	    logger.error("AcceptLoop issue", e);
	}
    }

    public void setVideoName(String videoName) {
	this.videoName = videoName;
    }

    public void setImageName(String imageName) {
	this.imageName = imageName;
    }

    public void shutdown() {
	logger.debug(" VideoServer shutdown start");
	running = false;
	interrupt();
	logger.debug(" VideoServer shutdown interrupted");
	// try {
	// TODO join();
	// } catch (InterruptedException e) {
	// System.out.println(getMyName() + " ReceiverThread join failed");
	// e.printStackTrace();
	// }
	logger.debug(" ReceiverThread shutdown end");
    }

    public String getVideoName() {
	return videoName;
    }

    public String getImageName() {
	return imageName;
    }

}
