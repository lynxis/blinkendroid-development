package org.cbase.blinkendroid.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InverseEffect implements ITouchEffect {

    private static final Logger logger = LoggerFactory.getLogger(InverseEffect.class);
    private PlayerManager playerManager = null;
    private PlayerClient playerClient = null;

    public InverseEffect(PlayerManager pMgr, PlayerClient pClient) {
	this.playerManager = pMgr;
	this.playerClient = pClient;
    }

    public void showEffect() {
	try {
	    // light up row and column
	    int x = playerClient.x;
	    int y = playerClient.y;
	    int maxX = playerManager.getMaxX();
	    int maxY = playerManager.getMaxY();

	    logger.error("touch " + x + "," + y);
	    int i = 1;
	    do {
		if ((x + i < maxX))
		    blink(x + i, y);
		if (y + i < maxY)
		    blink(x, y + i);
		if (x - i >= 0)
		    blink(x - i, y);
		if (y - i >= 0)
		    blink(x, y - i);
		i++;
	    } while ((x - i > 0) || (x + i < maxX) || (y - i > 0) || (y + i < maxY));
	} catch (Exception e) {
	    logger.error("touch failed", e);
	}
    }

    private void blink(int x, int y) {
	PlayerClient pc = playerManager.getPlayer(x, y);
	if (null != pc)
	    pc.getBlinkenProtocol().blink(1);
    }

}
