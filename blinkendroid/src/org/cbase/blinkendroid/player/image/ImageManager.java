package org.cbase.blinkendroid.player.image;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageManager {

    private List<ImageHeader> imageHeader;

    public List<ImageHeader> getImageHeader() {
	return imageHeader;
    }

    ImageManagerListener listener;
    private static final Logger logger = LoggerFactory.getLogger(ImageManager.class);
    private String dir;

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

    public void readImages(final ImageManagerListener listener, String dir) {
	this.listener = listener;
	this.dir = dir;
	new Thread() {
	    @Override
	    public void run() {
		File blinkendroidDir = new File(ImageManager.this.dir);
		if (!blinkendroidDir.exists()) {
		    logger.error(ImageManager.this.dir + " dir does not exist");
		    return;
		}
		File[] files = blinkendroidDir.listFiles();
		if (null != files) {
		    logger.debug("found files " + files.length);
		    for (int i = 0; i < files.length; i++) {
			if (!(files[i].getName().endsWith(".png") || files[i].getName().endsWith(".jpg")))
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

    private ImageHeader getImageHeader(File f) {
	try {
	    ImageHeader defaultMovie = new ImageHeader();
	    defaultMovie.filename = null;
	    defaultMovie.title = f.getName();
	    defaultMovie.height = 32;
	    defaultMovie.width = 32;
	    return defaultMovie;
	} catch (Exception e) {
	    logger.error("could not get ImageHeader", e);
	}
	return null;
    }

    public ImageHeader getImageHeader(int pos) {
	if (null != imageHeader.get(pos))
	    return imageHeader.get(pos);
	return null;
    }
}
