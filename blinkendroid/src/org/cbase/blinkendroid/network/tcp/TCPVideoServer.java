package org.cbase.blinkendroid.network.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

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
		System.out.println(" VideoThread ended!!!!!!! ");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void acceptLoop() {
		// TODO Auto-generated method stub

		try {
			while (running) {
				Socket connectionSocket;
				connectionSocket = serverSocket.accept();
				final BlinkendroidVideoServerProtocol blinkenVideoProtocol = new BlinkendroidVideoServerProtocol(
						connectionSocket, videoName);
				

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		 * try { byte[] buffer = new byte[4]; while (running && in.read(buffer)
		 * != -1) { inputLine = ByteBuffer.wrap(buffer).getInt(); if (!running)
		 * // fast exit break;
		 * 
		 * // System.out.println(getMyName() + // " InputThread received: " // +
		 * inputLine);
		 * 
		 * CommandHandler handler = handlers.get(inputLine); if (null !=
		 * handler) handler.handle(in); } } catch (SocketException e) {
		 * System.out.println(getMyName() + " Socket closed."); } catch
		 * (IOException e) { System.out.println(getMyName() +
		 * " InputThread fucked "); e.printStackTrace(); }
		 */
	}

	public void setVideo(String videoName) {
		this.videoName = videoName;
	}

	public void shutdown() {
		System.out.println(" VideoServer shutdown start");
		running = false;
		interrupt();
		System.out.println(" VideoServer shutdown interrupted");
		// try {
		// join();
		// } catch (InterruptedException e) {
		// System.out.println(getMyName() + " ReceiverThread join failed");
		// e.printStackTrace();
		// }
		System.out.println(" ReceiverThread shutdown end");
	}
}
