package org.cbase.blinkendroid;

import org.cbase.blinkendroid.network.ConnectionListener;
import org.cbase.blinkendroid.network.broadcast.ReceiverThread;
import org.cbase.blinkendroid.network.udp.ClientSocket;
import org.cbase.blinkendroid.player.bml.BLMManager;
import org.cbase.blinkendroid.player.bml.BLMManager.BLMManagerListener;
import org.cbase.blinkendroid.server.BlinkendroidServer;
import org.cbase.blinkendroid.server.TicketManager;
import org.cbase.blinkendroid.utils.NetworkUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class ServerActivity extends Activity implements ConnectionListener, BLMManagerListener {

    private ReceiverThread recieverThread;
    private TicketManager ticketManager;
    private BlinkendroidServer blinkendroidServer;
    private BLMManager blmManager;
    private ArrayAdapter<String> movieAdapter;
    private ArrayAdapter<String> clientAdapter;
    private ArrayAdapter<Integer> ticketSizeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	setContentView(R.layout.server_content);

	final TextView serverNameView = (TextView) findViewById(R.id.server_name);
	final Spinner movieSpinner = (Spinner) findViewById(R.id.server_movie);
	final Button startStopButton = (Button) findViewById(R.id.server_start_stop);
	final Button clientButton = (Button) findViewById(R.id.server_client);
	final ListView clientList = (ListView) findViewById(R.id.server_client_list);
	final Spinner ticketSizeSpinner = (Spinner) findViewById(R.id.ticket_size);

	serverNameView.setText(PreferenceManager.getDefaultSharedPreferences(this).getString("owner", null));

	movieAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
	movieAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	movieSpinner.setAdapter(movieAdapter);
	// add default video
	// movieAdapter.add("Blinkendroid");
	// movieAdapter.add("Random");//#7
	blmManager = new BLMManager();
	blmManager.readMovies(this);

	// Ticketmanager
	ticketManager = new TicketManager();

	clientAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
	clientList.setAdapter(clientAdapter);

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

	startStopButton.setOnClickListener(new OnClickListener() {

	    public void onClick(View v) {
		if (null == blinkendroidServer) {
		    // start recieverthread
		    recieverThread = new ReceiverThread(Constants.BROADCAST_ANNOUCEMENT_SERVER_PORT,
			    Constants.CLIENT_BROADCAST_COMMAND);
		    recieverThread.addHandler(ticketManager);
		    recieverThread.start();

		    blinkendroidServer = new BlinkendroidServer(Constants.BROADCAST_SERVER_PORT);
		    blinkendroidServer.addConnectionListener(ServerActivity.this);
		    blinkendroidServer.addConnectionListener(ticketManager);

		    blinkendroidServer.start();
		    // TODO schtief warum hier kein thread in server ui?
		    startStopButton.setText(getString(R.string.stop));
		    clientButton.setEnabled(true);
		} else {
		    recieverThread.shutdown();
		    recieverThread = null;

		    blinkendroidServer.shutdown();
		    blinkendroidServer = null;

		    ticketManager.reset();

		    startStopButton.setText(getString(R.string.start));
		    clientButton.setEnabled(false);
		}
	    }
	});

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
	// ticketSizeAdapter.getPosition(ticketManager.getMaxClients());
	clientButton.setOnClickListener(new OnClickListener() {

	    public void onClick(View v) {

		final Intent intent = new Intent(ServerActivity.this, PlayerActivity.class);
		intent.putExtra(PlayerActivity.INTENT_EXTRA_IP, NetworkUtils.getLocalIpAddress());
		intent.putExtra(PlayerActivity.INTENT_EXTRA_PORT, Constants.BROADCAST_SERVER_PORT);
		startActivity(intent);
	    }
	});
    }

    @Override
    protected void onDestroy() {

	if (recieverThread != null) {
	    recieverThread.shutdown();
	    recieverThread = null;
	}

	if (blinkendroidServer != null) {
	    blinkendroidServer.shutdown();
	    blinkendroidServer = null;
	}

	super.onDestroy();
    }

    public void connectionOpened(final ClientSocket clientSocket) {
	Log.d(Constants.LOG_TAG, "ServerActivity connectionOpened " + clientSocket.getDestinationAddress().toString());
	runOnUiThread(new Runnable() {

	    public void run() {
		clientAdapter.add(clientSocket.getDestinationAddress().toString());
	    }
	});
    }

    public void connectionClosed(final ClientSocket clientSocket) {
	Log.d(Constants.LOG_TAG, "ServerActivity connectionClosed " + clientSocket.getDestinationAddress().toString());
	runOnUiThread(new Runnable() {

	    public void run() {
		clientAdapter.remove(clientSocket.getDestinationAddress().toString());
	    }
	});
    }

    public void moviesReady() {
	runOnUiThread(new Runnable() {

	    public void run() {
		blmManager.fillArrayAdapter(movieAdapter);
		Toast.makeText(ServerActivity.this, "Movies ready", Toast.LENGTH_SHORT).show();
	    }
	});
    }
}
