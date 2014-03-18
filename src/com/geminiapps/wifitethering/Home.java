package com.geminiapps.wifitethering;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

public class Home extends Activity {
	Button button;
	CheckBox checkbox;
	SharedPreferences prefs;

	private AdView adView;
	/* Your ad unit id. Replace with your actual ad unit id. */
	private static final String AD_UNIT_ID = "ca-app-pub-5800761622766190/2875368460";

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.actionbar_custom, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_rateme:
			openRatingPage();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void openRatingPage() {
		Uri uri = Uri.parse("market://details?id="
				+ getApplicationContext().getPackageName());
		Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
		try {
			startActivity(goToMarket);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(getApplicationContext(),
					"Couldn't launch the market", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		button = (Button) findViewById(R.id.button1);
		checkbox = (CheckBox) findViewById(R.id.checkBox1);

		// get shared preference setting
		prefs = this.getSharedPreferences(getApplicationContext()
				.getPackageName(), Context.MODE_PRIVATE);
		final String autoRunKey = getApplicationContext().getPackageName()
				+ ".automatestart";

		// use a default value using new Date()
		boolean autostart = prefs.getBoolean(autoRunKey, false);

		if (autostart)
			openTetheringSettings();
		// Create an ad.
		adView = new AdView(this);
		adView.setAdSize(AdSize.BANNER);
		adView.setAdUnitId(AD_UNIT_ID);

		// Add the AdView to the view hierarchy. The view will have no size
		// until the ad is loaded.
		LinearLayout layout = (LinearLayout) findViewById(R.id.ad);
		layout.addView(adView);

		// Create an ad request. Check logcat output for the hashed device ID to
		// get test ads on a physical device.
		AdRequest adRequest = new AdRequest.Builder().build();

		// Start loading the ad in the background.
		adView.loadAd(adRequest);

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (checkbox.isChecked()) {
					prefs.edit().putBoolean(autoRunKey, true).commit();
				}
				openTetheringSettings();
			}

		});

	}

	private void openTetheringSettings() {
		Intent tetherSettings = new Intent();
		tetherSettings.setClassName("com.android.settings",
				"com.android.settings.TetherSettings");
		startActivity(tetherSettings);
		finish();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (adView != null) {
			adView.resume();
		}
	}

	@Override
	public void onPause() {
		if (adView != null) {
			adView.pause();
		}
		super.onPause();
	}

	/** Called before the activity is destroyed. */
	@Override
	public void onDestroy() {
		// Destroy the AdView.
		if (adView != null) {
			adView.destroy();
		}
		super.onDestroy();
		System.out.println("Home activity destroyed");
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this); // Add this method.
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			System.out.println("back pressed");
			finish();
		}
		return super.dispatchKeyEvent(event);
	}

}