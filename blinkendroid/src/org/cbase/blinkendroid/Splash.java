package org.cbase.blinkendroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;

public class Splash extends Activity {

  @Override
  public void onResume() {
	super.onResume();
	setContentView(R.layout.splashscreen_content);

	new Handler().postDelayed(new Runnable() {
	  @Override
	  public void run() {
		try {
		  Thread.sleep(1500);
		} catch (InterruptedException e) {
		}
		try {
		  startActivity(new Intent(Splash.this, LoginActivity.class));
		} finally {
		  finish();
		}
	  }
	}, 2000);
  }
}
