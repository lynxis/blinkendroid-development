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

package org.cbase.blinkendroid;

/**
 * @author Andreas Schildbach
 */
public final class Constants {

  public static final String LOG_TAG = "blinkendroid";
  public static final int BROADCAST_ANNOUCEMENT_SERVER_PORT = 6998;
  public static final int BROADCAST_ANNOUCEMENT_CLIENT_PORT = 6999;
  public static final int BROADCAST_ANNOUCEMENT_CLIENT_TICKET_PORT = 7000;
  public static final int BROADCAST_ANNOUCEMENT_SERVER_TICKET_PORT = 7001;
  public static final int BROADCAST_CLIENT_PORT = 6789;
  public static final int BROADCAST_SERVER_PORT = 6790;
  public static final int BROADCAST_IDLE_THRESHOLD = 5000;
  public static final String MULTICAST_GROUP = "230.0.0.1";
  public static final String CLIENT_BROADCAST_COMMAND = "BLINKENDROID_CLIENT";
  public static final String SERVER_TICKET_COMMAND = "BLINKENDROID_TICKET";
  // public static final int SERVER_PORT = 9876;
  public static final int SERVER_SOCKET_CONNECT_TIMEOUT = 5000;
  public static final int SHOW_OWNER_DURATION = 1500;
  public static final int BROADCAST_PROTOCOL_VERSION = 4;
  public static final String DOWNLOAD_URL = "market://details?id=org.cbase.blinkendroid";
  public static final String ABOUT_URL = "http://code.google.com/p/blinkendroid";

  public static final Integer PROTOCOL_CONNECTION = 1;
  public static final Integer PROTOCOL_PLAYER = 42;
  public static final Integer PROTOCOL_HEARTBEAT = 23;
}
