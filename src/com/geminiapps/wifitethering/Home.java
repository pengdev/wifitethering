package com.geminiapps.wifitethering;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
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
import android.widget.TextView;
import android.widget.Toast;

public class Home extends Activity {
	Button button;
	CheckBox checkbox;
	Button button2;
	CheckBox checkbox2;
	SharedPreferences prefs;

	String ratedKey;
	String autoRunKey;
	String usageCountKey;
	int usagecount = 0;
	String TAG = "wifitethering";

	private AdView adView;
	/* Your ad unit id. Replace with your actual ad unit id. */
	private static final String AD_UNIT_ID = "ca-app-pub-9576274567421261/5644398036";

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
		if (usagecount == 20) {
			prefs.edit().putBoolean(ratedKey, true).commit();
		}
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

		button2 = (Button) findViewById(R.id.button2);
		checkbox2 = (CheckBox) findViewById(R.id.checkBox2);

		TextView text = (TextView) findViewById(R.id.textView1);

		ratedKey = getApplicationContext().getPackageName() + ".israted";
		autoRunKey = getApplicationContext().getPackageName()
				+ ".automatestartoption";
		usageCountKey = getApplicationContext().getPackageName()
				+ ".usagecount";

		// get shared preference setting
		prefs = this.getSharedPreferences(getApplicationContext()
				.getPackageName(), Context.MODE_PRIVATE);

		// use a default value using new Date()
		int autostart = prefs.getInt(autoRunKey, 0);
		usagecount = prefs.getInt(usageCountKey, 0);
		boolean israted = prefs.getBoolean(ratedKey, false);
		if (!israted) {
			if (usagecount == 20) {
				Toast.makeText(
						getApplicationContext(),
						"You have been using our app for some time, please click the star icon above and give us ratings, thanks :)",
						Toast.LENGTH_LONG).show();
				text.setTextColor(Color.YELLOW);
				text.setText("You have been using our app for some time, please click the star icon above and give us ratings, thanks :) \n *This will only show once if you give us rating now");
				autostart = 0;
				prefs.edit().putInt(autoRunKey, 0).commit();
				prefs.edit().putInt(usageCountKey, 0).commit();
			} else {
				System.out.println("usage count:" + (usagecount + 1));
				prefs.edit().putInt(usageCountKey, usagecount + 1).commit();
			}
		}

		if (autostart == 1)
			openTetheringSettings();
		else if (autostart == 2)
			tetherWirelessAP();

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
		AdRequest adRequest = new AdRequest.Builder()
				.addTestDevice(AdRequest.DEVICE_ID_EMULATOR) // 所有模拟器
				.addTestDevice("1D93C8FC4113388A66A6936BE2F7EE67") // 我的Galaxy
																	// Nexus测试手机
				.build();

		// Start loading the ad in the background.
		adView.loadAd(adRequest);

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (checkbox.isChecked()) {
					prefs.edit().putInt(autoRunKey, 1).commit();
				}
				openTetheringSettings();
			}

		});

		button2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (checkbox2.isChecked()) {
					prefs.edit().putInt(autoRunKey, 2).commit();
				}
				tetherWirelessAP();
			}

		});

	}

	private void tetherWirelessAP() {
		if (IsWifiApEnabled()) {
			enableAP(false);
			Toast.makeText(getApplicationContext(), "Closing Wireless AP",
					Toast.LENGTH_SHORT).show();
		} else {
			enableAP(true);
			Toast.makeText(getApplicationContext(), "Opening Wireless AP",
					Toast.LENGTH_SHORT).show();
		}

		finish();
	}

	private void openTetheringSettings() {
		Intent tetherSettings = new Intent();
		tetherSettings.setClassName("com.android.settings",
				"com.android.settings.TetherSettings");
		try {
			startActivity(tetherSettings);
			finish();
		} catch (android.content.ActivityNotFoundException e) {
			Toast.makeText(getApplicationContext(),
					"Sorry could not find TetherSettings in your system",
					Toast.LENGTH_LONG).show();
		}
	}

	private void enableAP(boolean enable) {
		WifiManager wifi_manager = (WifiManager) this
				.getSystemService(this.WIFI_SERVICE);

		WifiConfiguration wifi_configuration = null;
		wifi_manager.setWifiEnabled(false);

		try {
			// USE REFLECTION TO GET METHOD "SetWifiAPEnabled"
			Method method = wifi_manager.getClass().getMethod(
					"setWifiApEnabled", WifiConfiguration.class, boolean.class);
			method.invoke(wifi_manager, wifi_configuration, enable);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean IsWifiApEnabled() {
		boolean isWifiAPEnabled = false;
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		Method[] wmMethods = wifi.getClass().getDeclaredMethods();
		for (Method method : wmMethods) {
			if (method.getName().equals("isWifiApEnabled")) {
				try {
					isWifiAPEnabled = (Boolean) method.invoke(wifi);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
		return isWifiAPEnabled;
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