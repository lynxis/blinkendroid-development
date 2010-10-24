package org.cbase.blinkendroid;

import java.io.File;

import org.cbase.blinkendroid.network.ConnectionListener;
import org.cbase.blinkendroid.network.broadcast.ReceiverThread;
import org.cbase.blinkendroid.network.udp.ClientSocket;
import org.cbase.blinkendroid.player.bml.BLMHeader;
import org.cbase.blinkendroid.player.bml.BLMManager;
import org.cbase.blinkendroid.player.bml.BLMManager.BLMManagerListener;
import org.cbase.blinkendroid.player.image.ImageHeader;
import org.cbase.blinkendroid.player.image.ImageManager;
import org.cbase.blinkendroid.player.image.ImageManager.ImageManagerListener;
import org.cbase.blinkendroid.server.BlinkendroidServer;
import org.cbase.blinkendroid.server.ClientQueueListener;
import org.cbase.blinkendroid.server.TicketManager;
import org.cbase.blinkendroid.utils.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class ServerActivity extends Activity implements ConnectionListener, BLMManagerListener, ImageManagerListener,
	ClientQueueListener {

    private static final Logger logger = LoggerFactory.getLogger(ServerActivity.class);

    private ReceiverThread receiverThread;
    private TicketManager ticketManager;
    private BlinkendroidServer blinkendroidServer;
    private BlinkendroidApp app;
    private BLMManager blmManager;
    private ImageManager imageManager;
    private ArrayAdapter<String> movieAdapter;
    private ArrayAdapter<String> imageAdapter;
    private ArrayAdapter<String> clientAdapter;
    private ArrayAdapter<String> clientQueueAdapter;
    private ArrayAdapter<Integer> ticketSizeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	app = (BlinkendroidApp) getApplication();
	app.wantWakeLock(true);

	setContentView(R.layout.server_content);

	final TabHost tabHost = (TabHost) this.findViewById(android.R.id.tabhost);
	tabHost.setup();
	tabHost.addTab(tabHost.newTabSpec("settings").setIndicator("Settings").setContent(R.id.tabcontent_settings));
	tabHost.addTab(tabHost.newTabSpec("clients").setIndicator("Connected Clients").setContent(
		R.id.tabcontent_clients_connected));
	tabHost.addTab(tabHost.newTabSpec("clients_queue").setIndicator("Waiting Clients").setContent(
		R.id.tabcontent_clients_waiting));

	String ownerName = PreferenceManager.getDefaultSharedPreferences(this).getString("owner", null);
	if (ownerName == null)
	    ownerName = System.currentTimeMillis() + "";
	ticketManager = new TicketManager(ownerName);
	ticketManager.setClientQueueListener(this);

	movieAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
	movieAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

	imageAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
	imageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

	final TextView serverNameView = (TextView) findViewById(R.id.server_name);
	serverNameView.setText(PreferenceManager.getDefaultSharedPreferences(this).getString("owner", null));

	final Button clientButton = (Button) findViewById(R.id.server_client);
	clientButton.setOnClickListener(new OnClickListener() {

	    public void onClick(View v) {

		final Intent intent = new Intent(ServerActivity.this, PlayerActivity.class);
		intent.putExtra(PlayerActivity.INTENT_EXTRA_IP, NetworkUtils.getLocalIpAddress());
		intent.putExtra(PlayerActivity.INTENT_EXTRA_PORT, BlinkendroidApp.BROADCAST_SERVER_PORT);
		startActivity(intent);
	    }
	});

	final ToggleButton serverSwitchButton = (ToggleButton) findViewById(R.id.server_switch);
	serverSwitchButton.setOnClickListener(new OnClickListener() {

	    public void onClick(final View v) {
		if (serverSwitchButton.isChecked()) {

		    // start recieverthread
		    receiverThread = new ReceiverThread(BlinkendroidApp.BROADCAST_ANNOUCEMENT_SERVER_PORT,
			    BlinkendroidApp.CLIENT_BROADCAST_COMMAND);
		    receiverThread.addHandler(ticketManager);
		    receiverThread.start();

		    blinkendroidServer = new BlinkendroidServer(BlinkendroidApp.BROADCAST_SERVER_PORT);
		    blinkendroidServer.addConnectionListener(ServerActivity.this);
		    blinkendroidServer.addConnectionListener(ticketManager);
		    blinkendroidServer.start();

		    clientButton.setEnabled(true);
		    serverNameView.setEnabled(false);
		} else {
		    receiverThread.shutdown();
		    receiverThread = null;

		    blinkendroidServer.shutdown();
		    blinkendroidServer = null;

		    ticketManager.reset();

		    clientButton.setEnabled(false);
		    serverNameView.setEnabled(true);
		}
	    }
	});

	final Spinner movieSpinner = (Spinner) findViewById(R.id.server_movie);
	movieSpinner.setAdapter(movieAdapter);

	movieSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

	    public void onItemSelected(AdapterView<?> arg0, View arg1, int listElement, long arg3) {
		// already running?
		if (null != blinkendroidServer) {
		    blinkendroidServer.switchMovie(blmManager.getBLMHeader(listElement));
		}
	    }

	    public void onNothingSelected(AdapterView<?> arg0) {
	    }
	});

	final Spinner imageSpinner = (Spinner) findViewById(R.id.image_selector);
	imageSpinner.setAdapter(imageAdapter);

	imageSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

	    public void onItemSelected(AdapterView<?> arg0, View arg1, int listElement, long arg3) {
		// already running?
		if (null != blinkendroidServer) {
		    blinkendroidServer.switchImage(imageManager.getImageHeader(listElement));
		}
	    }

	    public void onNothingSelected(AdapterView<?> arg0) {
	    }
	});

	final Spinner ticketSizeSpinner = (Spinner) findViewById(R.id.ticket_size);
	ticketSizeAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item);
	ticketSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

	// TODO change to int i = 20; i <= 200; i += 20 for productive version
	for (int i = 1; i <= 200; i++) {
	    ticketSizeAdapter.add(i);
	}

	ticketSizeSpinner.setAdapter(ticketSizeAdapter);

	ticketSizeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

	    public void onItemSelected(AdapterView<?> arg0, View arg1, int maxClients, long arg3) {
		if (ticketManager != null) {
		    ticketManager.setMaxClients(maxClients);
		}
	    }

	    public void onNothingSelected(AdapterView<?> arg0) {
	    }
	});

	ticketSizeSpinner.setSelection(ticketManager.getMaxClients() - 1);

	final ListView clientList = (ListView) findViewById(R.id.server_client_list);
	clientAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
	clientList.setAdapter(clientAdapter);

	final ListView clientQueueList = (ListView) findViewById(R.id.server_client_list_waiting);
	clientQueueAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
	clientQueueList.setAdapter(clientQueueAdapter);
	clientQueueList.setOnItemClickListener(new OnItemClickListener() {

	    public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
		if (ticketManager != null) {
		    String ip = (String) adapter.getItemAtPosition(position);
		    ticketManager.clientStateChangedFromWaitingToConnected(ip);
		    // ticketSizeSpinner.setSelection(ticketSizeSpinner.getSelectedItemPosition()
		    // + 1);
		    clientNoLongerWaiting(ip);

		}
	    }

	});

	// ticketSizeAdapter.getPosition(ticketManager.getMaxClients());

	blmManager = new BLMManager();
	blmManager.readMovies(this, Environment.getExternalStorageDirectory().getPath() + File.separator
		+ "blinkendroid");

	imageManager = new ImageManager();
	imageManager.readImages(this, Environment.getExternalStorageDirectory().getPath() + File.separator
		+ "blinkendroid");
    }

    @Override
    protected void onDestroy() {

	if (receiverThread != null) {
	    receiverThread.shutdown();
	    receiverThread = null;
	}

	if (blinkendroidServer != null) {
	    blinkendroidServer.shutdown();
	    blinkendroidServer = null;
	}
	app.wantWakeLock(false);

	super.onDestroy();
    }

    public void connectionOpened(final ClientSocket clientSocket) {
	logger.debug("ServerActivity connectionOpened " + clientSocket.getDestinationAddress().toString());
	runOnUiThread(new Runnable() {

	    public void run() {
		clientAdapter.add(clientSocket.getDestinationAddress().toString());
		checkClientListEmpty();
	    }
	});
    }

    public void connectionClosed(final ClientSocket clientSocket) {
	logger.debug("ServerActivity connectionClosed " + clientSocket.getDestinationAddress().toString());
	runOnUiThread(new Runnable() {

	    public void run() {
		clientAdapter.remove(clientSocket.getDestinationAddress().toString());
		checkClientListEmpty();
	    }
	});
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cbase.blinkendroid.ClientQueueListener#clientWaiting(org.cbase.
     * blinkendroid.network.udp.ClientSocket)
     */
    public void clientWaiting(final String ip) {
	logger.debug("ServerActivity clientWaiting " + ip);
	runOnUiThread(new Runnable() {

	    public void run() {
		clientQueueAdapter.add(ip);
		checkWaitingQueueEmpty();
	    }
	});
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cbase.blinkendroid.ClientQueueListener#clientNoLongerWaiting(org.
     * cbase.blinkendroid.network.udp.ClientSocket)
     */
    public void clientNoLongerWaiting(final String ip) {
	logger.debug("ServerActivity clientNoLongerWaiting " + ip);
	runOnUiThread(new Runnable() {

	    public void run() {
		clientQueueAdapter.remove(ip);
		checkWaitingQueueEmpty();
	    }
	});
    }

    private void checkClientListEmpty() {
	findViewById(R.id.server_client_list_empty).setVisibility(
		clientAdapter.isEmpty() ? View.VISIBLE : View.INVISIBLE);
    }

    private void checkWaitingQueueEmpty() {
	findViewById(R.id.server_client_list_empty_waiting).setVisibility(
		clientQueueAdapter.isEmpty() ? View.VISIBLE : View.INVISIBLE);
    }

    public void moviesReady() {
	runOnUiThread(new Runnable() {

	    public void run() {
		for (BLMHeader header : blmManager.getBlmHeader()) {

		    if (null == header) {
			// TODO null check
		    } else if (null == header.filename && null == header.title) {
			// TODO null check
		    } else {
			String title = header.title + "(" + header.width + "*" + header.height + ")";
			if (null == header.title) {
			    title = header.filename.substring(20) + "(" + header.width + "*" + header.height + ")";
			}
			logger.debug("added " + title);
			movieAdapter.add(title);
		    }
		}
		Toast.makeText(ServerActivity.this, "Movies ready", Toast.LENGTH_SHORT).show();
	    }
	});
    }

    public void imagesReady() {
	runOnUiThread(new Runnable() {

	    public void run() {
		for (ImageHeader header : imageManager.getImageHeader()) {

		    if (null == header) {
		    } else if (null == header.filename && null == header.title) {
		    } else {
			String title = header.title + "(" + header.width + "*" + header.height + ")";
			if (null == header.title) {
			    title = header.filename.substring(20) + "(" + header.width + "*" + header.height + ")";
			}
			logger.debug("added " + title);
			imageAdapter.add(title);
		    }
		}
		Toast.makeText(ServerActivity.this, "Images ready", Toast.LENGTH_SHORT).show();
	    }
	});
    }
}
