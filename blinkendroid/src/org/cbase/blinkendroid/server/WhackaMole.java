package org.cbase.blinkendroid.server;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhackaMole extends Thread {
    private static final Logger logger = LoggerFactory
	    .getLogger(WhackaMole.class);
    private boolean running = false;
    private int moleCounter;
    private Random random;
    private PlayerManager playerManager;

    public WhackaMole(PlayerManager pM) {
	this.playerManager = pM;
    }

    @Override
    public synchronized void start() {
	moleCounter = 0;
	random = new Random(System.currentTimeMillis());
	running = true;
	logger.info("Whackamole running");
	super.start();
    }

    @Override
    public void run() {
	float x;
	float y;
	int style;
	try {
	    do {

		int moleDuration;
		
		if(moleCounter <= 19) {
		    moleDuration = (int) (10000 - moleCounter * 500);
		} else {
		    moleDuration = 100;
		}

		try {
		    // sleep random time but a little less if moleCounter is
		    // high
		    Thread.sleep(random.nextInt(10000) + 500);
		} catch (InterruptedException ie) {
		    logger.error("WhackaMole interrupt fucked up", ie);
		}
		// send a mole to x=0.0-1.0 y=0.0-1.0
		x = random.nextFloat();
		y = random.nextFloat();
		// set different style
		style = random.nextInt(10);
		playerManager.mole(x, y, style, moleCounter, moleDuration);
		moleCounter++;
	    } while (running);
	} catch (Exception e) {
	    logger.error("WhackaMole Thread crashed", e);
	}
    }

    public void shutdown() {
	logger.info("WhackaMole: initiating shutdown");
	running = false;
	interrupt();
	try {
	    join();
	} catch (final InterruptedException x) {
	}
	logger.info("WhackaMole shutdown");
    }

    public boolean isRunning() {
	return running;
    }
}
