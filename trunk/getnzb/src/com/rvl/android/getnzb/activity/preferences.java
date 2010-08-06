package com.rvl.android.getnzb.activity;

import com.rvl.android.getnzb.tags;

import com.rvl.android.getnzb.*;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class preferences extends PreferenceActivity {
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(tags.LOG, "Starting preference acticity");
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(tags.PREFS);
		addPreferencesFromResource(R.layout.preferences);
	}
	protected void onDestroy() {
		Log.d(tags.LOG,"Leaving preference activity.");
		super.onDestroy();
	}
	
}