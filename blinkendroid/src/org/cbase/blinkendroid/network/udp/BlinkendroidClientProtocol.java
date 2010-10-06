package org.cbase.blinkendroid.network.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.cbase.blinkendroid.Constants;
import org.cbase.blinkendroid.network.BlinkendroidListener;
import org.cbase.blinkendroid.network.tcp.BlinkendroidVideoClientProtocol;
import org.cbase.blinkendroid.player.bml.BLM;

import android.util.Log;

public class BlinkendroidClientProtocol extends BlinkendroidProtocol implements CommandHandler {
  private BlinkendroidListener mListener;

  public BlinkendroidClientProtocol(BlinkendroidListener listener) {
	mListener = listener;
  }

  public void handle(SocketAddress from, ByteBuffer in) throws IOException {
	int command = in.getInt();

	Log.d(Constants.LOG_TAG, "received: " + command);
	if (mListener != null) {
	  if (command == COMMAND_PLAYER_TIME) {
		mListener.serverTime(in.getLong());
	  } else if (command == COMMAND_CLIP) {
		final float startX = in.getFloat();
		final float startY = in.getFloat();
		final float endX = in.getFloat();
		final float endY = in.getFloat();
		System.out.println("clip: " + startX + "," + startY + "," + endX + "," + endY);
		mListener.clip(startX, startY, endX, endY);
	  } else if (command == COMMAND_PLAY) {
		final long startTime = in.getLong();

		BLM blm = BlinkendroidVideoClientProtocol.receiveMovie((InetSocketAddress) from);
		// if length == 0 play default
		/*
		 * if (length == 0) { } // else read BLM else { blm =
		 * parser.parseBBMZ(in, length); // while (in.read(buffer) != -1) { //
		 * inputLine= ByteBuffer.wrap(buffer).getInt(); // if (!running) // fast
		 * exit // break; // } long length2 = in.getLong();
		 * System.out.println("play length1 " + length + " length2:" + length2);
		 * }
		 */
		mListener.play(0, 0, startTime, blm);
	  } else if (command == COMMAND_INIT) {
		final int degrees = in.getInt();
		final int color = in.getInt();
		mListener.arrow(4000, degrees, color);
	  }
	}
  }

}
