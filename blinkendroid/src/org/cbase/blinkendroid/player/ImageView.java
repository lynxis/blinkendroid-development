package org.cbase.blinkendroid.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class ImageView extends View {

    private static final String LOG_TAG = "ImageView".intern();

    private Bitmap image = null;
    private float startX = 0f, startY = 0f, endX = 1f, endY = 1f;

    public ImageView(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    public void setImage(Bitmap image) {
	this.image = image;
    }

    public Bitmap getImage() {
	return image;
    }

    public ImageView(Context context) {
	super(context);
	// setClipping(0.33f, 0.33f, 0.66f, 0.66f);
    }

    public void setClipping(float startX, float startY, float endX, float endY) {
	this.startX = startX;
	this.startY = startY;
	this.endX = endX;
	this.endY = endY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
	super.onDraw(canvas);
	if (image != null) {
	    int absStartX = (int) startX * image.getWidth();
	    int absStartY = (int) startY * image.getHeight();
	    int absEndX = (int) endX * image.getWidth();
	    int absEndY = (int) endY * image.getHeight();

	    int absImgStartX = 0;
	    int absImgStartY = 0;
	    int absImgEndX = canvas.getWidth();
	    int absImgEndY = canvas.getHeight();

	    Rect srcRect = new Rect(absStartX, absStartY, absEndX, absEndY);
	    Rect dstRect = new Rect(absImgStartX, absImgStartY, absImgEndX, absImgEndY);

	    Log.d(LOG_TAG, "*** clip " + absStartX + "," + absStartY + "," + absEndX + "," + absEndY);

	    canvas.drawBitmap(image, srcRect, dstRect, null);
	}
    }

}
