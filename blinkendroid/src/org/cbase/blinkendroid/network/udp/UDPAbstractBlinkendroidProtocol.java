package org.cbase.blinkendroid.network.udp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.cbase.blinkendroid.Constants;
import org.cbase.blinkendroid.network.ConnectionListener;

import android.util.Log;

public class UDPAbstractBlinkendroidProtocol implements UDPDirectConnection {

  protected BufferedOutputStream out;
  protected BufferedInputStream in;
  protected DatagramSocket m_Socket;
  protected ReceiverThread receiverThread;
  protected final HashMap<Integer, CommandHandler> handlers = new HashMap<Integer, CommandHandler>();
  protected List<ConnectionListener> connectionListener = new ArrayList<ConnectionListener>();
  protected boolean server;

  protected UDPAbstractBlinkendroidProtocol(final DatagramSocket socket) throws IOException {
	this.m_Socket = socket;
	receiverThread = new ReceiverThread();
	receiverThread.start();
  }

  public void addConnectionListener(ConnectionListener connectionListener) {
	this.connectionListener.add(connectionListener);
  }

  public void registerHandler(Integer proto, CommandHandler handler) {
	handlers.put(proto, handler);
  }

  public void unregisterHandler(CommandHandler handler) {
	handlers.remove(handler);
  }

  public void close() {
	try {
	  out.close();
	  if (!server)// TODO ugly hack, server needs to long
		in.close();
	  m_Socket.close();
	  Log.d(Constants.LOG_TAG, getMyName() + " BlinkendroidProtocol: Socket closed.");
	} catch (IOException e) {
	  Log.e(Constants.LOG_TAG, getMyName() + " BlinkendroidProtocol: closed failed ");
	}
  }

  public void shutdown() {
	if (null != receiverThread) {
	  receiverThread.shutdown();
	}
	handlers.clear();
	Log.d(Constants.LOG_TAG, getMyName() + " Protocol shutdown.");
	// close();
  }

  protected void receive(DatagramPacket packet) throws IOException {

	InetSocketAddress socketAddress = (InetSocketAddress) packet.getSocketAddress();
	ByteBuffer in = ByteBuffer.wrap(packet.getData());
	int proto = in.getInt();
	Log.d(Constants.LOG_TAG, "BlinkendroidClient received Protocol: " + proto);
	if (proto == Constants.PROTOCOL_HEARTBEAT) {
	  for (CommandHandler h : handlers.values()) {
		h.handle(socketAddress, in);
	  }
	  return;
	}

	CommandHandler handler = handlers.get(proto);
	System.out.println("recieve proto "+proto+" handler "+handler.getClass().toString());
	if (null != handler)
	  handler.handle(socketAddress, in);
  }

  // Inner classes:
  /**
   * A thread that receives information
   */
  class ReceiverThread extends Thread {

	volatile private boolean running = true;

	@Override
	public void run() {
	  this.setName("--- ReceiverThread");
	  running = true;
	  Log.d(Constants.LOG_TAG, "InputThread started");
	  byte[] receiveData;
	  DatagramPacket receivePacket;
	  try {
		m_Socket.setSoTimeout(1000);
		while (running) {
		  receiveData = new byte[1024];
		  receivePacket = new DatagramPacket(receiveData, receiveData.length);
		  Log.d(Constants.LOG_TAG, this.getName() + " received " + receivePacket.toString());
		  try {
			m_Socket.receive(receivePacket);
			receive(receivePacket);
		  } catch (InterruptedIOException e) {
			// timeout happened - just a normal case, swallowing
//			Log.i(Constants.LOG_TAG, "ReceiverThread timeout");
		  }
		}
	  } catch (SocketException e) {
		Log.e(Constants.LOG_TAG, "InputThread Socket closed", e);
	  } catch (IOException e) {
		Log.e(Constants.LOG_TAG, "InputThread IOException", e);
		e.printStackTrace();
	  }
	}

	public void shutdown() {
	  Log.d(Constants.LOG_TAG, getMyName() + " ReceiverThread shutdown start");
	  running = false;
	  interrupt();
	  Log.d(Constants.LOG_TAG, getMyName() + " ReceiverThread shutdown interrupted");
	  try {
		join();
	  } catch (InterruptedException e) {
		Log.e(Constants.LOG_TAG, getMyName() + " ReceiverThread join failed", e);
	  }
	  Log.d(Constants.LOG_TAG, getMyName() + " ReceiverThread shutdown joined & end");
	}
  }

  public void send(InetSocketAddress socketAddr, ByteBuffer out) throws IOException {
	m_Socket.send(new DatagramPacket(out.array(), out.position(), socketAddr));
  }

  protected String getMyName() {
	if (server)
	  return "Server ";// + socket.getRemoteSocketAddress();
	else
	  return "Client ";// + socket.getRemoteSocketAddress();
  }
}