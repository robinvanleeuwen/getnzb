package com.rvl.android.getnzb;



import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MonitorHellaNZB extends Activity{
	public static HellaNZB HELLACONNECTION = new HellaNZB();
	private final Handler handler = new Handler();
	private Timer t;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.monitorhellanzb);
		if(HELLACONNECTION.CONNECTED == false) HELLACONNECTION.connect();
		autoQueueRefresh();
	}
	
	public void updateCurrentDownloadScreen(String status){
		String values[] = status.split("#");

		int remaining = Integer.parseInt(values[2]) - Integer.parseInt(values[3]);

		((TextView) findViewById(R.id.currentFilename)).setText(values[1]);
		((TextView) findViewById(R.id.eta)).setText(values[5]);
		((TextView) findViewById(R.id.sizeRemaining)).setText(Integer.toString(remaining)+" MB / "+values[2]+" MB");
		((TextView) findViewById(R.id.speed)).setText(values[4]+" KB/s");
		((ProgressBar) findViewById(R.id.currentProgress)).setProgress(Integer.parseInt(values[6]));
		
		
	}
	
	
	// Get string with current status of HellaNZB server.
	// makeup:0 <Paused true of false>#
	//		  1 <Name of .nzb file currently downloading>#
	//        2 <Total size in MB of currently downloading>#
	//        3 <MB Remaining>#
	//        4 <Downloadspeed in KB>#
	//        5 <Estimated time of arrival>#
	//        6 <Percent complete of current download>
	
	@SuppressWarnings("unchecked")
	public String getHellaNZBStatus(){
		String status = "";
		Log.d(Tags.LOG,"- getHellaNZBStatus(): retrieving status.");
		HashMap<String,Object> globalinfo = (HashMap<String, Object>) HELLACONNECTION.call("status");
		Object[] tt = (Object[]) globalinfo.get("currently_downloading");
		HashMap<String,Object> currdlinfo = (HashMap<String, Object>) tt[0];
		
		status += globalinfo.get("is_paused").toString() + "#";
		status += currdlinfo.get("nzbName").toString() + "#";
		status += currdlinfo.get("total_mb").toString() + "#";
		status += globalinfo.get("queued_mb").toString() + "#";
		status += globalinfo.get("rate").toString() + "#";
		status += convertEta((Integer) globalinfo.get("eta")) + "#";
		status += globalinfo.get("percent_complete").toString();
		
		Log.d(Tags.LOG,"getHellaNZBStatus(): "+status);
			
		return status;
	}
	
	private static String convertEta(int secs) {
		int hours = secs / 3600,
		remainder = secs % 3600,
		minutes = remainder / 60,
		seconds = remainder % 60; 
		String disHour = (hours < 10 ? "0" : "") + hours,
		disMinu = (minutes < 10 ? "0" : "") + minutes ,
		disSec = (seconds < 10 ? "0" : "") + seconds ;

		return(disHour +":"+ disMinu+":"+disSec);
	}	
	
	private void autoQueueRefresh() {
		t = new Timer();
		t.schedule(new TimerTask() {
			public void run() {
				handler.post(new Runnable() {
					public void run() {
						if(HELLACONNECTION.CONNECTED)
							updateCurrentDownloadScreen(getHellaNZBStatus());
					}
				});
			}
		}, 0, 2000);
	}

}