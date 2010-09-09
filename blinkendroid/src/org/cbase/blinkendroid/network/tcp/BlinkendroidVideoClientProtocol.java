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
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import org.cbase.blinkendroid.Constants;
import org.cbase.blinkendroid.player.bml.BBMZParser;
import org.cbase.blinkendroid.player.bml.BLM;

import android.util.Log;

public class BlinkendroidVideoClientProtocol {

	public static final Integer PROTOCOL_PLAYER = 42;
	public static final Integer COMMAND_PLAYER_TIME = 23;
	public static final Integer COMMAND_CLIP = 17;
	public static final Integer COMMAND_PLAY = 11;
	public static final Integer COMMAND_INIT = 77;

	public static BLM receiveMovie(InetSocketAddress socketAddress) {
		BLM blm = null;
		
		try {
			Socket socket = new Socket();
			socket.connect(socketAddress);
//			socket.setSoTimeout(1000);
			BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
			BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			writeInt(out, 2353);
			out.flush();
			long length = readLong(in); // TODO checking racecondition with setSoTimeout
			if (length == 0) {
				Log.i(Constants.LOG_TAG, "Play default video ");
			} else {
				BBMZParser parser = new BBMZParser();
				blm = parser.parseBBMZ(in, length);
			}
			out.close();
			in.close();
			socket.close();
		} catch (SocketTimeoutException e) {
			// setSoTimeout outer
			int ooops = 0;
		} catch (IOException e) {
		}
		return blm;
	}
	
	

	protected static long readLong(BufferedInputStream in) throws IOException {
		byte[] buffer = new byte[8];
		// try {
		in.read(buffer);
		// } catch (IOException e) {
		// Log.e(Constants.LOG_TAG,"readLong failed ",e);
		// }
		return ByteBuffer.wrap(buffer).getLong();
	}

	protected static int readInt(BufferedInputStream in) throws IOException {
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

	protected static void writeInt(BufferedOutputStream out, int i) throws IOException {
		byte[] buffer = new byte[4];
		ByteBuffer.wrap(buffer).putInt(i);
		// try {
		out.write(buffer);
		// } catch (IOException e) {
		// Log.e(Constants.LOG_TAG,"writeInt failed ",e);
		// }
	}

	protected static void writeFloat(BufferedOutputStream out, float f)
			throws IOException {
		byte[] buffer = new byte[16];
		ByteBuffer.wrap(buffer).putFloat(f);
		// try {
		out.write(buffer);
		// } catch (IOException e) {
		// Log.e(Constants.LOG_TAG,"writeFloat failed ",e);
		// }
	}

	protected static void writeLong(BufferedOutputStream out, long l)
			throws IOException {
		byte[] buffer = new byte[8];
		ByteBuffer.wrap(buffer).putLong(l);
		// try {
		out.write(buffer);
		// } catch (IOException e) {
		// Log.e(Constants.LOG_TAG,"writeLong failed ",e);
		// }
	}
}