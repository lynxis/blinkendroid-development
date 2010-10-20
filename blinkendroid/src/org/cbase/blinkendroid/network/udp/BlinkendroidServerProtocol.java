package org.cbase.blinkendroid.network.udp;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.cbase.blinkendroid.BlinkendroidApp;

import android.util.Log;

public class BlinkendroidServerProtocol extends BlinkendroidProtocol {

    private ClientSocket mClientSocket;
    private final String LOG_TAG = "BlinkendroidServerProtocol".intern();

    public BlinkendroidServerProtocol(ClientSocket clientSocket) {

	mClientSocket = clientSocket;
    }

    public void play(long startTime, int dataType) {
	/**
	 * PLAY length of string (0 for default) string
	 */

	ByteBuffer out = ByteBuffer.allocate(1024);
	try {
	    out.putInt(COMMAND_PLAY);
	    out.putInt(dataType);
	    out.putLong(startTime); // TODO we need only one start
	    send(out);
	} catch (IOException e) {
	    e.printStackTrace();
	    Log.e(LOG_TAG, "play failed", e);
	}
    }

    public void arrow(int degrees, int color) {
	try {
	    ByteBuffer out = ByteBuffer.allocate(1024);
	    out.putInt(COMMAND_INIT);
	    out.putInt(degrees);
	    out.putInt(color);

	    send(out);
	} catch (IOException e) {
	    Log.e(LOG_TAG, "arrow failed ", e);
	}
    }

    public void clip(float startX, float startY, float endX, float endY) {
	try {

	    ByteBuffer out = ByteBuffer.allocate(1024);
	    out.putInt(COMMAND_CLIP);
	    out.putFloat(startX);
	    out.putFloat(startY);
	    out.putFloat(endX);
	    out.putFloat(endY);
	    send(out);
	    Log.d(LOG_TAG, "clip flushed ");
	} catch (IOException e) {
	    Log.e(LOG_TAG, "clip failed ", e);
	}
    }

    protected void send(ByteBuffer command) throws IOException {
	ByteBuffer out = ByteBuffer.allocate(command.position() + Integer.SIZE);
	out.putInt(BlinkendroidApp.PROTOCOL_PLAYER); /* protocol header */
	out.put(command.array(), 0, command.position());
	mClientSocket.send(out);
    }
}
