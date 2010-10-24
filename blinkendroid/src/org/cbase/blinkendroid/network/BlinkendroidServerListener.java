package org.cbase.blinkendroid.network;

import java.net.SocketAddress;

public interface BlinkendroidServerListener {
    public void locateMe(SocketAddress from);
}
