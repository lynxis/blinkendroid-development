package org.cbase.blinkendroid.player.bml;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.cbase.blinkendroid.network.tcp.BlinkendroidDataServerProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;

public class BLMManager {

  private List<BLMHeader> blmHeader;
  BLMManagerListener listener;
  private static final Logger logger = LoggerFactory.getLogger(BLMManager.class);


  public BLMManager() {
	blmHeader = new ArrayList<BLMHeader>();
	/* Adding the default movie */
	BLMHeader defaultMovie = new BLMHeader();
	defaultMovie.filename = null;
	defaultMovie.title = "Blinkendroid - Default";
	defaultMovie.height = 32;
	defaultMovie.width = 32;

	blmHeader.add(defaultMovie);
  }

  public interface BLMManagerListener {
	public void moviesReady();
  }

  public void readMovies(final BLMManagerListener listener) {
	this.listener = listener;
	new Thread() {
	  @Override
	  public void run() {
		File blinkendroidDir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator
			+ "blinkendroid");
		if (!blinkendroidDir.exists()) {
		  logger.debug( "/blinkendroid does not exist");
		  return;
		}
		File[] files = blinkendroidDir.listFiles();
		if (null != files) {
		  logger.debug( "found files " + files.length);
		  for (int i = 0; i < files.length; i++) {
			if (!files[i].getName().endsWith(".info"))
			  continue;
			BLMHeader header = getBLMHeader(files[i]);
			if (null != header) {
			  header.filename = files[i].getAbsolutePath().substring(0, files[i].getAbsolutePath().length() - 5)
				  + ".bbmz";
			  blmHeader.add(header);
			}
		  }
		  listener.moviesReady();
		}
	  }
	}.start();

  }

  public void fillArrayAdapter(ArrayAdapter<String> adapter) {
	for (BLMHeader header : blmHeader) {

	  if (null == header) {
		// TODO null check
	  } else if (null == header.filename && null == header.title) {
		// TODO null check
	  } else {
		String title = header.title + "(" + header.width + "*" + header.height + ")";
		if (null == header.title) {
		  title = header.filename.substring(20) + "(" + header.width + "*" + header.height + ")";
		}
		logger.debug( "added " + title);
		adapter.add(title);
	  }
	}
  }

  private BLMHeader getBLMHeader(File f) {
	try {
	  ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(f));
	  Object receivedObject = objIn.readObject();
	  if (receivedObject instanceof BLMHeader) {
		return (BLMHeader) receivedObject;
	  }
	} catch (Exception e) {
	  logger.error( "could not get BMLHeader", e);
	}
	return null;
  }

  public BLMHeader getBLMHeader(int pos) {
	if (null != blmHeader.get(pos))
	  return blmHeader.get(pos);
	return null;
  }

  // public String getFilename(int arg2) {
  // // TODO This method does nothing, do we need it?
  // return null;
  // }
}
