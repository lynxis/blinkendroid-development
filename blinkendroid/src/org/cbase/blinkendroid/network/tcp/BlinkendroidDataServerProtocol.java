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

import org.cbase.blinkendroid.network.udp.BlinkendroidProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BlinkendroidDataServerProtocol {

    private static final Logger logger = LoggerFactory.getLogger(BlinkendroidDataServerProtocol.class);

    protected BufferedOutputStream out;
    protected BufferedInputStream in;
    protected Socket socket;
    protected ReceiverThread receiver;
    TCPVideoServer dataServer;

    public BlinkendroidDataServerProtocol(final Socket socket, TCPVideoServer dataServer) throws IOException {
	this.out = new BufferedOutputStream(socket.getOutputStream());
	this.in = new BufferedInputStream(socket.getInputStream());
	this.socket = socket;
	this.dataServer = dataServer;
	receiver = new ReceiverThread();
	receiver.start();
    }

    protected void sendMovie() {
	try {
	    if (null == dataServer.getVideoName()) {
		writeLong(out, 0);
		logger.debug( "Play default video ");
	    } else {
		File movie = new File(dataServer.getVideoName());
		if (null != movie && movie.exists()) {

		    try {
			writeLong(out, movie.length());
			logger.debug( "try to read file with bytes " + movie.length());
			InputStream is = new FileInputStream(movie);
			byte[] buffer = new byte[1024];
			// commented because: not referenced
			// int allLen = 0;
			int len;
			while ((len = is.read(buffer)) != -1) {
			    out.write(buffer, 0, len);
			    // allLen += len;
			}
			is.close();
			logger.debug( "send movie bytes " + movie.length());
			writeLong(out, movie.length());
		    } catch (IOException ioe) {
			logger.error( "sending movie failed", ioe);
		    }
		} else {
		    logger.error( "movie not found" + dataServer.getVideoName());
		}
	    }

	    out.flush();
	} catch (IOException e) {
	}
    }

    protected void sendImage() {
	try {
	    if (null == dataServer.getImageName()) {
		writeLong(out, 0);
		logger.debug( "Play default image ");
	    } else {
		File image = new File(dataServer.getImageName());
		if (null != image && image.exists()) {

		    try {
			writeLong(out, image.length());
			logger.debug( "try to read file with bytes " + image.length());
			InputStream is = new FileInputStream(image);
			byte[] buffer = new byte[1024];
			// commented because: not referenced
			// int allLen = 0;
			int len;
			while ((len = is.read(buffer)) != -1) {
			    out.write(buffer, 0, len);
			    // allLen += len;
			}
			is.close();
			logger.debug( "send image bytes " + image.length());
			writeLong(out, image.length());
		    } catch (IOException ioe) {
			logger.error( "sending movie failed", ioe);
		    }
		} else {
		    logger.error( "movie not found" + dataServer.getImageName());
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
	    int requiredCommand = 0; // we need a to send it
	    try {
		byte[] buffer = new byte[64];
		while (running && in.read(buffer) != -1) {
		    if (!running) { // fast exit
			break;
		    }
		    requiredCommand = ByteBuffer.wrap(buffer).getInt();
		    if (requiredCommand == BlinkendroidProtocol.OPTION_PLAY_TYPE_MOVIE) {
			sendMovie();
		    }
		    if (requiredCommand == BlinkendroidProtocol.OPTION_PLAY_TYPE_IMAGE) {
			sendImage();
		    } else {
			running = false;
			socket.close();
		    }
		}
	    } catch (SocketException e) {
		logger.error( "Socket closed", e);
	    } catch (IOException e) {
		logger.error( "InputThread fucked", e);
		e.printStackTrace();
	    }
	    logger.debug( "InputThread ended!!!!!!!");

	}

	// TODO shutdown ? howto do it
	/*
	 * @Override public void run() {
	 * this.setName("SRV VideoReceiverThread"); running = true;
	 * System.out.println(" InputThread started"); byte[] receiveData;
	 * DatagramPacket receivePacket; try { while (running) { receiveData =
	 * new byte[1024]; receivePacket = new DatagramPacket(receiveData,
	 * receiveData.length); try { m_Socket.receive(receivePacket);
	 * receive(receivePacket); } catch (InterruptedIOException e) { //
	 * timeout happened - just a normal case } } } catch (SocketException e)
	 * { System.out.println(" Socket closed."); } catch (IOException e) {
	 * System.out.println(" InputThread IOExeception"); e.printStackTrace();
	 * } }
	 */

	public void shutdown() {
	    logger.debug( " ReceiverThread shutdown start");
	    running = false;
	    interrupt();
	    logger.debug( " ReceiverThread shutdown interrupted");
	    try {
		join();
	    } catch (InterruptedException e) {
		logger.error( "ReceiverThread join failed", e);
		e.printStackTrace();
	    }
	    logger.info( "ReceiverThread shutdown joined & end");
	}
    }
}