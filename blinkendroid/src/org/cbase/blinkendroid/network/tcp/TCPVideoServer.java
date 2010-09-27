package org.cbase.blinkendroid.network.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.cbase.blinkendroid.Constants;

import android.util.Log;

public class TCPVideoServer extends Thread {

  volatile private boolean running = true;
  ServerSocket serverSocket;
  private String videoName;
  private int videoPort;

  public TCPVideoServer(int videoPort) {
	this.videoPort = videoPort;
	this.videoName = null;
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
	  Log.d(Constants.LOG_TAG, "VideoThread ended!!!!!!! ");

	} catch (IOException e) {
	  Log.e(Constants.LOG_TAG, "VideoThread fuckup", e);
	}
  }

  //TODO do we need this method any longer?
  private void acceptLoop() {

	try {
	  while (running) {
		Socket connectionSocket;
		connectionSocket = serverSocket.accept();
		final BlinkendroidVideoServerProtocol blinkenVideoProtocol = new BlinkendroidVideoServerProtocol(
			connectionSocket, videoName);

	  }
	} catch (IOException e) {
	  Log.e(Constants.LOG_TAG, "AcceptLoop issue", e);
	}
	/*
	 * try { byte[] buffer = new byte[4]; while (running && in.read(buffer) !=
	 * -1) { inputLine = ByteBuffer.wrap(buffer).getInt(); if (!running) // fast
	 * exit break;
	 * 
	 * // System.out.println(getMyName() + // " InputThread received: " // +
	 * inputLine);
	 * 
	 * CommandHandler handler = handlers.get(inputLine); if (null != handler)
	 * handler.handle(in); } } catch (SocketException e) {
	 * System.out.println(getMyName() + " Socket closed."); } catch (IOException
	 * e) { System.out.println(getMyName() + " InputThread fucked ");
	 * e.printStackTrace(); }
	 */
  }

  public void setVideo(String videoName) {
	this.videoName = videoName;
  }

  public void shutdown() {
	Log.d(Constants.LOG_TAG, " VideoServer shutdown start");
	running = false;
	interrupt();
	Log.d(Constants.LOG_TAG, " VideoServer shutdown interrupted");
	// try {
	// join();
	// } catch (InterruptedException e) {
	// System.out.println(getMyName() + " ReceiverThread join failed");
	// e.printStackTrace();
	// }
	Log.d(Constants.LOG_TAG, " ReceiverThread shutdown end");
  }
}
