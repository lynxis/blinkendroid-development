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

import java.util.ArrayList;
import java.util.List;

import org.cbase.blinkendroid.network.broadcast.IPeerHandler;
import org.cbase.blinkendroid.network.broadcast.ReceiverThread;
import org.cbase.blinkendroid.network.broadcast.SenderThread;
import org.cbase.blinkendroid.utils.NetworkUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.test.UiThreadTest;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

/**
 * @author Andreas Schildbach
 */
public class LoginActivity extends Activity implements Runnable {

  private final List<ListEntry> serverList = new ArrayList<ListEntry>();
  private ServerListAdapter serverListAdapter;
  private ListView serverListView;
  private ReceiverThread receiverThread;
  private Handler handler = new Handler();
  private SenderThread senderThread;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

	super.onCreate(savedInstanceState);

	requestWindowFeature(Window.FEATURE_LEFT_ICON);
	setTitle(getTitle() + " - " + getString(R.string.select_server_to_connect));
	setContentView(R.layout.login_content);
	setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon);

	serverListView = (ListView) findViewById(R.id.login_server_list);

	serverListAdapter = new ServerListAdapter(serverList);

	serverListView.setAdapter(serverListAdapter);
	serverListView.setOnItemClickListener(new OnItemClickListener() {

	  @Override
	  public void onItemClick(AdapterView<?> parent, View v, int position,
		  long id) {
		final ListEntry entry = serverList.get(position);
		final Intent intent = new Intent(LoginActivity.this,
			PlayerActivity.class);
		intent.putExtra(PlayerActivity.INTENT_EXTRA_IP, entry.ip);
		intent.putExtra(PlayerActivity.INTENT_EXTRA_PORT,
			Constants.BROADCAST_SERVER_PORT);
		startActivity(intent);
	  }
	});

	final String owner = PreferenceManager.getDefaultSharedPreferences(this)
		.getString("owner", null);
	if (owner == null)
	  Toast.makeText(this, getString(R.string.name_hint), Toast.LENGTH_LONG)
		  .show();
  }

  @Override
  protected void onResume() {

	super.onResume();
	// send Broadcast
	String ownerName = PreferenceManager.getDefaultSharedPreferences(this)
		.getString("owner", null);
	if (ownerName == null)
	  ownerName = System.currentTimeMillis() + "";
	senderThread = new SenderThread(ownerName);
	senderThread.start();

	// recieve Tickets
	receiverThread = new ReceiverThread(
		Constants.BROADCAST_ANNOUCEMENT_CLIENT_TICKET_PORT,
		Constants.SERVER_TICKET_COMMAND);
	receiverThread.addHandler(new IPeerHandler() {

	  @Override
	  public void foundPeer(final String serverName, final String serverIp,
		  final int protocolVersion) {
		Log.i(Constants.LOG_TAG, "recieved Ticket");
		runOnUiThread(new Runnable() {
		  @Override
		  public void run() {
			ListEntry entry = findServerEntry(serverList, serverName, serverIp);
			if (entry == null) {
			  entry = new ListEntry(serverName, serverIp, System
				  .currentTimeMillis());
			  serverList.add(entry);
			  serverListAdapter.notifyDataSetChanged();
			  serverListView.setVisibility(View.VISIBLE);
			} else {
			  entry.lastFound = System.currentTimeMillis();
			}

		  }
		});
	  }

	  public void foundUnknownServer(int protocolVersion) {
		runOnUiThread(new Runnable() {

		  @Override
		  public void run() {
			new AlertDialog.Builder(LoginActivity.this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(getString(R.string.warning))
				.setMessage(getString(R.string.new_release_text))
				.setPositiveButton(getString(R.string.check_for_update),
					new OnClickListener() {

					  @Override
					  public void onClick(DialogInterface dialog, int which) {
						startActivity(new Intent(Intent.ACTION_VIEW, Uri
							.parse(Constants.DOWNLOAD_URL)));
						finish();

					  }
					})
				.setNegativeButton(getString(R.string.ignore),
					new OnClickListener() {

					  @Override
					  public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					  }
					}).create().show();
		  }
		});

	  }
	});
	receiverThread.start();

	handler.post(this);
  }

  @Override
  public void run() {

	// TODO geht nich mehr , da ticket nur einmal kommt
	// remove timed-out servers
	// for (final Iterator<ListEntry> i = serverList.iterator();
	// i.hasNext();) {
	// final ListEntry entry = i.next();
	// if (entry.lastFound + Constants.BROADCAST_IDLE_THRESHOLD <
	// System.currentTimeMillis()) {
	// i.remove();
	// serverListAdapter.notifyDataSetChanged();
	// }
	// }

	handler.postDelayed(this, 1000);
  }

  @Override
  protected void onPause() {

	handler.removeCallbacks(this);
	if (receiverThread != null) {
	  receiverThread.shutdown();
	  receiverThread = null;
	}
	if (senderThread != null) {
	  senderThread.shutdown();
	  senderThread = null;
	}
	super.onPause();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.login_options, menu);
	return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {

	case R.id.login_options_start_server: {
	  startActivity(new Intent(LoginActivity.this, ServerActivity.class));
	  return true;
	}

	case R.id.login_options_connect_to_ip: {
	  final Dialog dialog = new Dialog(this);
	  dialog.setTitle(getString(R.string.enter_server_ip));
	  dialog.setContentView(R.layout.login_connect_to_ip_dialog_content);
	  dialog.show();
	  final EditText ip = (EditText) dialog
		  .findViewById(R.id.login_connect_to_ip_dialog_ip);
	  ip.setText(NetworkUtils.getLocalIpAddress());
	  ip.setOnEditorActionListener(new OnEditorActionListener() {

		@Override
		public boolean onEditorAction(final TextView v, final int actionId,
			final KeyEvent event) {
		  dialog.dismiss();
		  final Intent intent = new Intent(LoginActivity.this,
			  PlayerActivity.class);
		  intent.putExtra(PlayerActivity.INTENT_EXTRA_IP, ip.getText()
			  .toString());
		  intent.putExtra(PlayerActivity.INTENT_EXTRA_PORT,
			  Constants.BROADCAST_SERVER_PORT);
		  startActivity(intent);
		  return true;
		}
	  });
	  return true;
	}

	case R.id.login_options_preferences: {
	  startActivity(new Intent(getBaseContext(), PreferencesActivity.class));
	  return true;
	}

	case R.id.login_options_instructions: {
	  startActivity(new Intent(Intent.ACTION_VIEW,
		  Uri.parse(Constants.ABOUT_URL)));
	  return true;
	}

	case R.id.login_options_wireless_settings: {
	  final Intent intent = new Intent();
	  intent.setClassName("com.android.settings",
		  "com.android.settings.wifi.WifiSettings");
	  startActivity(intent);
	  return true;
	}
	}

	return false;
  }

  private class ServerListAdapter extends BaseAdapter {

	private final List<ListEntry> serverList;

	public ServerListAdapter(List<ListEntry> serverList) {
	  this.serverList = serverList;
	}

	@Override
	public View getView(int position, View row, ViewGroup parent) {

	  if (row == null)
		row = getLayoutInflater().inflate(android.R.layout.two_line_list_item,
			null);

	  final ListEntry entry = serverList.get(position);

	  ((TextView) row.findViewById(android.R.id.text1)).setText(entry.name
		  .length() > 0 ? entry.name : "<unnamed>");

	  ((TextView) row.findViewById(android.R.id.text2)).setText(entry.ip);

	  return row;
	}

	@Override
	public long getItemId(int position) {
	  return position;
	}

	@Override
	public Object getItem(int position) {
	  return serverList.get(position);
	}

	@Override
	public int getCount() {
	  return serverList.size();
	}
  }

  private static class ListEntry {

	public final String name;
	public final String ip;
	public long lastFound;

	public ListEntry(final String name, final String ip, final long lastFound) {
	  this.name = name;
	  this.ip = ip;
	  this.lastFound = lastFound;
	}

	@Override
	public boolean equals(Object o) {
	  final ListEntry other = (ListEntry) o;
	  return other.name.equals(this.name) && other.ip.equals(this.ip);
	}

	@Override
	public int hashCode() {
	  return name.hashCode() * 9 + ip.hashCode();
	}
  }

  private static ListEntry findServerEntry(final List<ListEntry> serverList,
	  final String name, final String ip) {
	for (final ListEntry entry : serverList)
	  if (entry.ip.equals(ip) && entry.name.equals(name))
		return entry;

	return null;
  }
}
