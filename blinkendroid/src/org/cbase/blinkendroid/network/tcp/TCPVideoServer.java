package org.cbase.blinkendroid.network.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

public class TCPVideoServer extends Thread {

    private static final String LOG_TAG = "TCPVideoServer".intern();
    volatile private boolean running = true;
    ServerSocket serverSocket;
    private String videoName;

    private String imageName;

    private int videoPort;

    public TCPVideoServer(int videoPort) {
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
	    Log.d(LOG_TAG, "VideoThread ended!!!!!!! ");

	} catch (IOException e) {
	    Log.e(LOG_TAG, "VideoThread fuckup", e);
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
	    Log.e(LOG_TAG, "AcceptLoop issue", e);
	}
    }

    public void setVideoName(String videoName) {
	this.videoName = videoName;
    }

    public void setImageName(String imageName) {
	this.imageName = imageName;
    }

    public void shutdown() {
	Log.d(LOG_TAG, " VideoServer shutdown start");
	running = false;
	interrupt();
	Log.d(LOG_TAG, " VideoServer shutdown interrupted");
	// try {
	// join();
	// } catch (InterruptedException e) {
	// System.out.println(getMyName() + " ReceiverThread join failed");
	// e.printStackTrace();
	// }
	Log.d(LOG_TAG, " ReceiverThread shutdown end");
    }

    public String getVideoName() {
	return videoName;
    }

    public String getImageName() {
	return imageName;
    }

}
