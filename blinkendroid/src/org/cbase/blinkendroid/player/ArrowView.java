package org.cbase.blinkendroid.player;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class ArrowView extends View {

    private int width;
    private int height;
    private int arrowId = 0;
    private Map<Integer, Arrow> arrows = new HashMap<Integer, Arrow>();
    private float scale = 1f;

    private static final Path PATH = new Path();
    private static final Paint ARROW_STROKE = new Paint();

    static {
	PATH.moveTo(0f, -5f);
	PATH.lineTo(-2f, 1f);
	PATH.lineTo(2f, 1f);
	PATH.close();

	ARROW_STROKE.setStyle(Style.STROKE);
	ARROW_STROKE.setColor(Color.WHITE);
	ARROW_STROKE.setStrokeWidth(0.1f);
	ARROW_STROKE.setAntiAlias(true);
    }

    public ArrowView(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    @Override
    protected void onDraw(final Canvas canvas) {

	for (final Arrow arrow : arrows.values()) {

	    canvas.save();

	    final Paint arrowPaint = new Paint();
	    arrowPaint.setColor(arrow.color);
	    arrowPaint.setAntiAlias(true);

	    canvas.translate(width / 2f, height / 2f);
	    canvas.rotate(arrow.angle);
	    canvas.scale(width / 10f * scale, height / 10f * scale);
	    canvas.drawPath(PATH, arrowPaint);
	    canvas.drawPath(PATH, ARROW_STROKE);

	    canvas.restore();
	}
    }

    public int addArrow(final float angle, final int color) {
	final int id = arrowId++;
	final Arrow arrow = new Arrow();
	arrow.angle = angle;
	arrow.color = color;
	arrows.put(id, arrow);
	postInvalidate();
	return id;
    }

    public void removeArrow(final int id) {
	arrows.remove(id);
	postInvalidate();
    }

    public void setScale(final float scale) {
	this.scale = scale;
	postInvalidate();
    }

    @Override
    protected void onMeasure(final int wMeasureSpec, final int hMeasureSpec) {
	final int wMode = MeasureSpec.getMode(wMeasureSpec);
	final int wSize = MeasureSpec.getSize(wMeasureSpec);

	if (wMode == MeasureSpec.EXACTLY) {
	    width = wSize;
	} else if (wMode == MeasureSpec.AT_MOST) {
	    width = Math.min(width, wSize);
	}
	final int hMode = MeasureSpec.getMode(hMeasureSpec);
	final int hSize = MeasureSpec.getSize(hMeasureSpec);

	if (hMode == MeasureSpec.EXACTLY) {
	    height = hSize;
	} else if (hMode == MeasureSpec.AT_MOST) {
	    height = Math.min(height, hSize);
	}

	setMeasuredDimension(this.width, this.height);
    }

    private class Arrow {
	public float angle;
	public int color;
    }
}
