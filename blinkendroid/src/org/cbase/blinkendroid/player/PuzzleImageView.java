package org.cbase.blinkendroid.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class PuzzleImageView extends View {

    private static final Logger logger = LoggerFactory.getLogger(PuzzleImageView.class);

    private Bitmap image = null;
    private float startX = 0f, startY = 0f, endX = 1f, endY = 1f;

    public PuzzleImageView(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    public PuzzleImageView(Context context) {
	super(context);
	// setClipping(0.33f, 0.33f, 0.66f, 0.66f);
    }

    public void setImage(Bitmap image) {
	if (this.image != null) {
	    this.image.recycle();
	}
	this.image = image;
    }

    public Bitmap getImage() {
	return image;
    }

    public void setClipping(float startX, float startY, float endX, float endY) {
	this.startX = startX;
	this.startY = startY;
	this.endX = endX;
	this.endY = endY;

	postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
	if (image != null) {
	    int absStartX = (int) (startX * image.getWidth());
	    int absStartY = (int) (startY * image.getHeight());
	    int absEndX = (int) (endX * image.getWidth());
	    int absEndY = (int) (endY * image.getHeight());

	    int absImgStartX = 0;
	    int absImgStartY = 0;
	    int absImgEndX = canvas.getWidth();
	    int absImgEndY = canvas.getHeight();

	    Rect srcRect = new Rect(absStartX, absStartY, absEndX, absEndY);
	    Rect dstRect = new Rect(absImgStartX, absImgStartY, absImgEndX, absImgEndY);

	    logger.info("*** clip " + absStartX + "," + absStartY + "," + absEndX + "," + absEndY);

	    canvas.drawBitmap(image, srcRect, dstRect, null);
	}
	super.onDraw(canvas);
    }
}
