package org.cbase.blinkendroid.network.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.cbase.blinkendroid.BlinkendroidApp;
import org.cbase.blinkendroid.network.BlinkendroidListener;
import org.cbase.blinkendroid.network.tcp.BlinkendroidDataClientProtocol;
import org.cbase.blinkendroid.player.bml.BLM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;

public class BlinkendroidClientProtocol extends BlinkendroidProtocol implements CommandHandler {

    private BlinkendroidListener mListener;
    private static final Logger logger = LoggerFactory.getLogger(BlinkendroidClientProtocol.class);
    private ClientSocket serverSocket;

    public BlinkendroidClientProtocol(BlinkendroidListener listener, ClientSocket serverSocket) {
	mListener = listener;
	this.serverSocket = serverSocket;
    }

    public void locateMe() {
	ByteBuffer out = ByteBuffer.allocate(1024);
	try {
	    out.putInt(COMMAND_LOCATEME);
	    send(out);
	} catch (IOException e) {
	    e.printStackTrace();
	    logger.error("play failed", e);
	}
    }

    public void handle(final SocketAddress from, ByteBuffer in) throws IOException {
	int command = in.getInt();

	// logger.info("received: " + command);
	if (mListener != null) {
	    if (command == COMMAND_HEARTBEAT) {
		// read byte
		in.getInt();// timerstyl
		mListener.serverTime(in.getLong());
	    } else if (command == COMMAND_CLIP) {
		final float startX = in.getFloat();
		final float startY = in.getFloat();
		final float endX = in.getFloat();
		final float endY = in.getFloat();
		logger.info("clip: " + startX + "," + startY + "," + endX + "," + endY);
		mListener.clip(startX, startY, endX, endY);
	    } else if (command == COMMAND_PLAY) {
		int dataType = in.getInt();
		switch (dataType) {
		case OPTION_PLAY_TYPE_MOVIE:
		    final long startTime = in.getLong();
		    Thread t = new Thread() {
			public void run() {
			    BLM blm = BlinkendroidDataClientProtocol.receiveMovie((InetSocketAddress) from);
			    mListener.playBLM(startTime, blm);
			}
		    };
		    t.start();
		    break;
		case OPTION_PLAY_TYPE_IMAGE:
		    in.getLong();// we dont need startime for the image
		    Thread t2 = new Thread() {
			public void run() {
			    Bitmap bmp = BlinkendroidDataClientProtocol.receiveImage((InetSocketAddress) from);
			    mListener.showImage(bmp);
			}
		    };
		    t2.start();
		    break;
		default:
		    break;
		}

	    } else if (command == COMMAND_INIT) {
		final int degrees = in.getInt();
		final int color = in.getInt();
		mListener.arrow(4000, degrees, color);
	    }
	}
    }

    protected void send(ByteBuffer command) throws IOException {
	ByteBuffer out = ByteBuffer.allocate(command.position() + Integer.SIZE);
	out.putInt(BlinkendroidApp.PROTOCOL_CLIENT); /* protocol header */
	out.put(command.array(), 0, command.position());
	serverSocket.send(out);
    }
}
