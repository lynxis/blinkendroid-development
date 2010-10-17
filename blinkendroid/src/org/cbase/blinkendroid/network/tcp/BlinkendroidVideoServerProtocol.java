/*
 * Copyright 2010 the original author or authors.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cbase.blinkendroid.network.tcp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

import android.util.Log;

public class BlinkendroidVideoServerProtocol {

  private final String LOG_TAG = "BlinkendroidVideoServerProtocol".intern();
  
  public static final Integer PROTOCOL_PLAYER = 42;
  public static final Integer COMMAND_PLAYER_TIME = 23;
  public static final Integer COMMAND_CLIP = 17;
  public static final Integer COMMAND_PLAY = 11;
  public static final Integer COMMAND_INIT = 77;

  protected BufferedOutputStream out;
  protected BufferedInputStream in;
  protected Socket socket;
  protected String movieFile;
  protected ReceiverThread receiver;

  public BlinkendroidVideoServerProtocol(final Socket socket, String movieFile) throws IOException {
	this.out = new BufferedOutputStream(socket.getOutputStream());
	this.in = new BufferedInputStream(socket.getInputStream());
	this.socket = socket;
	this.movieFile = movieFile;
	receiver = new ReceiverThread();
	receiver.start();
  }

  protected void sendMovie() {
	try {
	  if (null == movieFile) {
		writeLong(out, 0);
		Log.d(LOG_TAG, "Play default video ");
	  } else {
		File movie = new File(movieFile);
		if (null != movie && movie.exists()) {

		  try {
			writeLong(out, movie.length());
			Log.d(LOG_TAG, "try to read file with bytes " + movie.length());
			InputStream is = new FileInputStream(movie);
			byte[] buffer = new byte[1024];
			int allLen = 0;
			int len;
			while ((len = is.read(buffer)) != -1) {
			  out.write(buffer, 0, len);
			  allLen += len;
			}
			is.close();
			Log.d(LOG_TAG, "send movie bytes " + movie.length());
			writeLong(out, movie.length());
		  } catch (IOException ioe) {
			Log.e(LOG_TAG, "sending movie failed", ioe);
		  }
		} else {
		  Log.e(LOG_TAG, "movie not found" + movieFile);
		}
	  }

	  out.flush();
	} catch (IOException e) {
	}
  }

  protected long readLong(BufferedInputStream in) throws IOException {
	byte[] buffer = new byte[8];
	// try {
	in.read(buffer);
	// } catch (IOException e) {
	// Log.e(Constants.LOG_TAG,"readLong failed ",e);
	// }
	return ByteBuffer.wrap(buffer).getLong();
  }

  protected int readInt(BufferedInputStream in) throws IOException {
	byte[] buffer = new byte[4];
	// try {
	in.read(buffer);
	// } catch (IOException e) {
	// Log.e(Constants.LOG_TAG,"readLong failed ",e);
	// }
	return ByteBuffer.wrap(buffer).getInt();
  }

  protected float readFloat(BufferedInputStream in) throws IOException {
	byte[] buffer = new byte[16];
	// try {
	in.read(buffer);
	// } catch (IOException e) {
	// Log.e(Constants.LOG_TAG,"readLong failed ",e);
	// }
	return ByteBuffer.wrap(buffer).getFloat();
  }

  protected void writeInt(BufferedOutputStream out, int i) throws IOException {
	byte[] buffer = new byte[4];
	ByteBuffer.wrap(buffer).putInt(i);
	// try {
	out.write(buffer);
	// } catch (IOException e) {
	// Log.e(Constants.LOG_TAG,"writeInt failed ",e);
	// }
  }

  protected void writeFloat(BufferedOutputStream out, float f) throws IOException {
	byte[] buffer = new byte[16];
	ByteBuffer.wrap(buffer).putFloat(f);
	// try {
	out.write(buffer);
	// } catch (IOException e) {
	// Log.e(Constants.LOG_TAG,"writeFloat failed ",e);
	// }
  }

  protected void writeLong(BufferedOutputStream out, long l) throws IOException {
	byte[] buffer = new byte[8];
	ByteBuffer.wrap(buffer).putLong(l);
	// try {
	out.write(buffer);
	// } catch (IOException e) {
	// Log.e(Constants.LOG_TAG,"writeLong failed ",e);
	// }
  }

  // Inner classes:
  /**
   * A thread that receives information
   */
  class ReceiverThread extends Thread {

	volatile private boolean running = true;

	@Override
	public void run() {
	  running = true;
	  int requiredCommand = 0; // we need a 2353 to send it
	  try {
		byte[] buffer = new byte[64];
		while (running && in.read(buffer) != -1) {
		  if (!running) { // fast exit
			break;
		  }
		  requiredCommand = ByteBuffer.wrap(buffer).getInt();
		  if (requiredCommand == 2353) {
			sendMovie();
		  } else {
			running = false;
			socket.close();
		  }
		}
	  } catch (SocketException e) {
		Log.e(LOG_TAG, "Socket closed", e);
	  } catch (IOException e) {
		Log.e(LOG_TAG, "InputThread fucked", e);
		e.printStackTrace();
	  }
	  Log.d(LOG_TAG, "InputThread ended!!!!!!!");

	}

	// TODO shutdown ? howto do it
	/*
	 * @Override public void run() { this.setName("SRV VideoReceiverThread");
	 * running = true; System.out.println(" InputThread started"); byte[]
	 * receiveData; DatagramPacket receivePacket; try { while (running) {
	 * receiveData = new byte[1024]; receivePacket = new
	 * DatagramPacket(receiveData, receiveData.length); try {
	 * m_Socket.receive(receivePacket); receive(receivePacket); } catch
	 * (InterruptedIOException e) { // timeout happened - just a normal case } }
	 * } catch (SocketException e) { System.out.println(" Socket closed."); }
	 * catch (IOException e) { System.out.println(" InputThread IOExeception");
	 * e.printStackTrace(); } }
	 */

	public void shutdown() {
	  Log.d(LOG_TAG, " ReceiverThread shutdown start");
	  running = false;
	  interrupt();
	  Log.d(LOG_TAG, " ReceiverThread shutdown interrupted");
	  try {
		join();
	  } catch (InterruptedException e) {
		Log.e(LOG_TAG, "ReceiverThread join failed", e);
		e.printStackTrace();
	  }
	  Log.i(LOG_TAG, "ReceiverThread shutdown joined & end");
	}
  }
}