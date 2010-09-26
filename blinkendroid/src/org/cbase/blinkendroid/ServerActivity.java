package org.cbase.blinkendroid;

import org.cbase.blinkendroid.network.ConnectionListener;
import org.cbase.blinkendroid.network.broadcast.SenderThread;
import org.cbase.blinkendroid.network.udp.ClientSocket;
import org.cbase.blinkendroid.player.bml.BLMManager;
import org.cbase.blinkendroid.player.bml.BLMManager.BLMManagerListener;
import org.cbase.blinkendroid.server.BlinkendroidServer;
import org.cbase.blinkendroid.utils.NetworkUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ServerActivity extends Activity implements ConnectionListener, BLMManagerListener {

    private SenderThread senderThread;
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
		if (!blinkendroidServer.isRunning()) {
		    senderThread = new SenderThread(serverNameView.getText().toString());
		    senderThread.start();

		    blinkendroidServer = new BlinkendroidServer(ServerActivity.this, Constants.BROADCAST_SERVER_PORT);
		    blinkendroidServer.start();
		    // TODO schtief warum hier kein thread in server ui?
		    startStopButton.setText(getString(R.string.stop));
		    clientButton.setEnabled(true);
		} else {
		    senderThread.shutdown();
		    senderThread = null;

		    blinkendroidServer.shutdown();
		    blinkendroidServer = null;

		    startStopButton.setText(getString(R.string.stop));
		    clientButton.setEnabled(false);
		}
	    }
	});
	ticketSizeAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_list_item_1);
	// TODO change to int i = 20; i <= 200; i += 20 for productive version
	for (int i = 1; i <= 200; i++) {
	    ticketSizeAdapter.add(i);
	}
	ticketSizeSpinner.setAdapter(ticketSizeAdapter);
	ticketSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	ticketSizeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

	    @Override
	    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO ben: set selected item to ticketSize
	    }

	    @Override
	    public void onNothingSelected(AdapterView<?> arg0) {
	    }
	});

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

	if (senderThread != null) {
	    senderThread.shutdown();
	    senderThread = null;
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
