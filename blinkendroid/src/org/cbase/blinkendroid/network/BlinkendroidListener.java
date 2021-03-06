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

package org.cbase.blinkendroid.network;

import org.cbase.blinkendroid.player.bml.BLM;

import android.graphics.Bitmap;

/**
 * @author Andreas Schildbach
 */
public interface BlinkendroidListener extends ConnectionListener {

    void connectionFailed(String message);

    void serverTime(long serverTime);

    void playBLM(long startTime, BLM blm);

    void clip(float startX, float startY, float endX, float endY);

    void arrow(long duration, float angle, int color);

    void showImage(Bitmap bmp);

    void mole(int type, int moleCounter, int duration, int points);

    void blink(int type);
}
