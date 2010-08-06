package com.rvl.android.getnzb.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.rvl.android.getnzb.R;
import com.rvl.android.getnzb.tags;


public class search extends Activity {
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(tags.LOG, "Starting search activity");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		TextView statusbar = (TextView) findViewById(R.id.statusbar);
		statusbar.setText("Enter searchterm.");
	}
	protected void onDestroy() {
		Log.d(tags.LOG,"Leaving search activity.");
		super.onDestroy();
	}
	
}