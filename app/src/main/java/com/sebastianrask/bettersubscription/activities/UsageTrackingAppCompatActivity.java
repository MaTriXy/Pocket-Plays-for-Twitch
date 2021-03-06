package com.sebastianrask.bettersubscription.activities;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.sebastianrask.bettersubscription.BuildConfig;
import com.sebastianrask.bettersubscription.PocketPlaysApplication;
import com.sebastianrask.bettersubscription.activities.main.MainActivity;
import com.sebastianrask.bettersubscription.service.Service;
import com.sebastianrask.bettersubscription.service.Settings;

/**
 * Created by Sebastian Rask on 07-05-2016.
 */
public abstract class UsageTrackingAppCompatActivity extends AppCompatActivity {
	private String LOG_TAG = getClass().getSimpleName();
	private long startTime;
	protected Tracker mTracker;

	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		if (!PocketPlaysApplication.isCrawlerUpdate) {
			PocketPlaysApplication application = (PocketPlaysApplication) getApplication();
			mTracker = application.getDefaultTracker();
		}

		checkStartdate();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!BuildConfig.DEBUG && !PocketPlaysApplication.isCrawlerUpdate) {
			mTracker.setScreenName(getClass().getSimpleName());
			mTracker.send(new HitBuilders.ScreenViewBuilder().build());
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		startTime = System.currentTimeMillis();
		if (shouldShowDonationPrompt() && Service.isNetWorkConnected(this)) {
			if (this instanceof MainActivity) {
				((MainActivity) UsageTrackingAppCompatActivity.this).navigateToDonationActivity();
			}
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		long timeSpent = System.currentTimeMillis() - startTime;
		new Settings(this).addUsageTimeInApp(timeSpent);
	}

	public void trackEvent(@StringRes int category, @StringRes int action) {
		trackEvent(getString(category), getString(action));
	}

	public void trackEvent(@StringRes int category, @StringRes int action, @Nullable String label) {
		trackEvent(getString(category), getString(action), label, null);
	}

	public void trackEvent(String category, String action) {
		trackEvent(category, action, null, null);
	}

	public void trackEvent(String category, String action, @Nullable String label, @Nullable Long value) {
		HitBuilders.EventBuilder builder = new HitBuilders.EventBuilder().setCategory(category).setAction(action);

		if (label != null) {
			builder.setLabel(label);
		}

		if (value != null) {
			builder.setValue(value);
		}

		if (mTracker != null) {
			mTracker.send(builder.build());
		}
	}

	private void checkStartdate() {
		Settings settings = new Settings(this);
		if (settings.getUsageStartDate() == -1) {
			settings.setUsageStartDate(System.currentTimeMillis());
		}
	}

	private boolean shouldShowDonationPrompt() {
		if (BuildConfig.DEBUG) {
			return false;
		}

		Settings settings = new Settings(getBaseContext());
		Log.d(LOG_TAG, "Donation is shown: " + settings.isDonationPromptShown());
		if (!settings.isDonationPromptShown() && !settings.getShowDonationPrompt()) {
			long usageForDonationPrompt = settings.getUsageTimeBeforeDonation();
			Log.d(LOG_TAG, "Usage in app: " + settings.getUsageTimeInApp());
			Log.d(LOG_TAG, "Usage before donation: " + usageForDonationPrompt);
			if (settings.getUsageTimeInApp() > usageForDonationPrompt) {
				return true;
			}
		}

		return false;
	}
}
