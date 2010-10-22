package org.cbase.blinkendroid.player.image;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.cbase.blinkendroid.network.tcp.BlinkendroidDataServerProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;

public class ImageManager {

    private List<ImageHeader> imageHeader;
    ImageManagerListener listener;
    private static final Logger logger = LoggerFactory.getLogger(ImageManager.class);


    public ImageManager() {
	imageHeader = new ArrayList<ImageHeader>();
	/* Adding the default movie */
	ImageHeader defaultMovie = new ImageHeader();
	defaultMovie.filename = null;
	defaultMovie.title = "Blinkendroid - Default";
	defaultMovie.height = 32;
	defaultMovie.width = 32;

	imageHeader.add(defaultMovie);
    }

    public interface ImageManagerListener {
	public void imagesReady();
    }

    public void readImages(final ImageManagerListener listener) {
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
			if (!files[i].getName().endsWith(".png"))
			    continue;
			ImageHeader header = getImageHeader(files[i]);
			if (null != header) {
			    header.filename = files[i].getAbsolutePath();
			    imageHeader.add(header);
			}
		    }
		    listener.imagesReady();
		}
	    }
	}.start();

    }

    public void fillArrayAdapter(ArrayAdapter<String> adapter) {
	for (ImageHeader header : imageHeader) {

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

    private ImageHeader getImageHeader(File f) {
	try {
	    ImageHeader defaultMovie = new ImageHeader();
	    defaultMovie.filename = null;
	    defaultMovie.title = f.getName();
	    defaultMovie.height = 32;
	    defaultMovie.width = 32;
	    return defaultMovie;
	} catch (Exception e) {
	    logger.error( "could not get ImageHeader", e);
	}
	return null;
    }

    public ImageHeader getImageHeader(int pos) {
	if (null != imageHeader.get(pos))
	    return imageHeader.get(pos);
	return null;
    }
}
