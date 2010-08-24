package com.rvl.android.getnzb;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MonitorHellaNZB extends Activity{
	public static HellaNZB HELLACONNECTION = new HellaNZB();
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.monitorhellanzb);
		if(HELLACONNECTION.CONNECTED == false) HELLACONNECTION.connect();
		
	}
	
		
}