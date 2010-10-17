package org.cbase.blinkendroid.player.bml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.ZipInputStream;

import org.cbase.blinkendroid.BlinkendroidApp;

import android.util.Log;

public class BBMZParser {
  public BLM parseBBMZ(ByteBuffer input, long length) {
	return parseBBMZ(new ByteArrayInputStream(input.array()), length);
  }

  public BLM parseBBMZ(InputStream openRawResource, long length) {
	long time = System.currentTimeMillis();

	ByteArrayOutputStream os = new ByteArrayOutputStream();
	byte inbuf[] = new byte[1];
	int n;
	try {
	  while ((n = openRawResource.read(inbuf, 0, 1)) != -1) {
		os.write(inbuf, 0, n);
		length -= n;
		if (length == 0)
		  break;
	  }
	} catch (Exception e) {
	  Log.e(BlinkendroidApp.LOG_TAG, "invalid bbmz", e);
	}

	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	System.gc();
	uncompress(new ByteArrayInputStream(os.toByteArray()), baos);
	os = null;
	try {
	  ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
	  Object o = objIn.readObject();
	  Log.d(BlinkendroidApp.LOG_TAG, "decompression and parsing time :" + (System.currentTimeMillis() - time));
	  baos = null;
	  if (o instanceof BLM) {
		return (BLM) o;
	  } else {
		Log.e(BlinkendroidApp.LOG_TAG, "invalid bbmz");
	  }
	} catch (Exception e) {
	  Log.e(BlinkendroidApp.LOG_TAG, "invalid bbmz", e);
	}
	baos = null;
	return null;
  }

  public static void uncompress(InputStream fis, OutputStream fos) {
	try {
	  ZipInputStream zis = new ZipInputStream(fis);
	  zis.getNextEntry();
	  final int BUFSIZ = 4096;
	  byte inbuf[] = new byte[BUFSIZ];
	  int length = 0;
	  int n;
	  while ((n = zis.read(inbuf, 0, BUFSIZ)) != -1) {
		fos.write(inbuf, 0, n);
		length += n;
	  }
	  Log.d(BlinkendroidApp.LOG_TAG, "BBMZParser read bytes " + length);
	  // zis.close();
	  // fis = null;
	  // fos.close();
	  // fos = null;
	} catch (IOException e) {
	  Log.e(BlinkendroidApp.LOG_TAG, "invalid bbmz", e);
	} finally {
	  try {
		// if (fis != null)
		// fis.close();
		if (fos != null)
		  fos.close();
	  } catch (IOException e) {
	  }
	}
  }
}
