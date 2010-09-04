package com.rvl.android.getnzb;

/**
 * This file is a part of GetNZB
 * 
 * GetNZB - http://code.google.com/p/getnzb
 * "Android NZB Search and HellaNZB client."
 * 
 * Copyright (C) 2010: Robin van Leeuwen (robinvanleeuwen@gmail.com)
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
**/

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class GetNZB extends Activity {
	
	public static boolean LOGGEDIN = false;
	public static boolean HELLACONNECTED = false;

	public static final int MENU_PREFS = 0;
	public static final int MENU_QUIT = 1;
	public static final int MENU_ABOUT = 2;
	
	public static final int DIALOG_NO_NZBS_SETTINGS=1;
	public static final int DIALOG_NO_HELLANZB_SETTINGS=2;
	public static final int DIALOG_ABOUT=3;
	
	public static DefaultHttpClient httpclient = new DefaultHttpClient();
    public NZBDatabase LocalNZBMetadata = new NZBDatabase(this);

	ProgressDialog pd = null;
	AlertDialog.Builder builder;
	AlertDialog alert;
	
	public static SharedPreferences preferences;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(Tags.LOG,"------ STARTING GETNZB ------");
    	super.onCreate(savedInstanceState);
    	this.setRequestedOrientation(
    			ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.login);
        preferences = getSharedPreferences(Tags.PREFS,0);
        builder = new AlertDialog.Builder(this);
       

    }
    
    public boolean onCreateOptionsMenu(Menu menu){
    	menu.add(0, MENU_ABOUT, 0, "About");
		menu.add(0, MENU_PREFS, 0, "Preferences");
		menu.add(0, MENU_QUIT, 0, "Quit");
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item){
    	switch (item.getItemId()){
    	case MENU_PREFS:
    		startPreferences();
    		return true;
    	case MENU_QUIT:
    		quit();
    		return true;
    	case MENU_ABOUT:
    		showDialog(DIALOG_ABOUT);
    		return true;
    	}
    	return false;
    }
    protected Dialog onCreateDialog(int id){
    	switch(id){
    	case DIALOG_NO_NZBS_SETTINGS:
    		/* Clear settings of previous alert dialogs */
    		builder = new AlertDialog.Builder(this);
    		
    		builder.setTitle("No nzbs.org account settings.")
			.setMessage("Do you wish to enter account settings now?")
    		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					startPreferences();
					return;
				}
			})
			.setNegativeButton("No", null);
			alert = builder.create();
			return alert;
		case DIALOG_NO_HELLANZB_SETTINGS:
			/* Clear settings of previous alert dialogs */
    		builder = new AlertDialog.Builder(this);
    		
    		builder.setTitle("No HellaNZB server settings.")
			.setMessage("Do you wish to enter HellaNZB settings now?")
    		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					startPreferences();
					return;
				}
			})
			.setNegativeButton("No", null);
			alert = builder.create();
			return alert;
    	case DIALOG_ABOUT:
    		/* Clear settings of previous alert dialogs */
    		builder = new AlertDialog.Builder(this);
    		
    		builder.setTitle("About GetNZB");
    		View about = getLayoutInflater().inflate(R.layout.about, null);
			((TextView)about.findViewById(R.id.textAboutTitle)).setText("GetNZB v" + Tags.VERSION);
			builder.setView(about);
			alert = builder.create();
    		return alert;
    	}
    	return null;
    }
    

    public void button_handler(View v){
    	switch(v.getId()){
    	case R.id.button_login:
    		if(preferences.getString("nzbsusername", "") == "")
    			showDialog(DIALOG_NO_NZBS_SETTINGS);
    		else{
    			pd = ProgressDialog.show(GetNZB.this, "http://www.nzbs.org", "Logging in, please wait...");
    			login();
    	
    		}
    		break;
    	case R.id.button_localnzb:
    		//if(preferences.getString("hellanzb_hostname", "")=="")
    		//	showDialog(DIALOG_NO_HELLANZB_SETTINGS);
    		//else 
    			startLocalNZB();
    		break;
    	case R.id.button_monitor:
    			startMonitor();
    		break;
    	}
    }
    
    
    public void login(){
    	// Login to www.nzbs.org and go to search-activity. 
    	// The return values from nzbs.org are stored in the cookiestore for later use.
    	
    	new Thread() {
			public void run(){
				if(!LOGGEDIN){
				Log.d(Tags.LOG,"- login()");
				SharedPreferences pref = getSharedPreferences(Tags.PREFS, 0);
				Log.d(Tags.LOG,"Using login name: '"+pref.getString("nzbsusername", "No value given.")+"'");
				HttpPost post = new HttpPost(Tags.NZBS_LOGINPAGE);
				
				List<NameValuePair> nvp = new ArrayList<NameValuePair>(2);
				nvp.add(new BasicNameValuePair("username",pref.getString("nzbsusername", "")));
				nvp.add(new BasicNameValuePair("password",pref.getString("nzbspassword", "")));
				nvp.add(new BasicNameValuePair("action","dologin"));

				try {
						post.setEntity(new UrlEncodedFormEntity(nvp));
						HttpResponse response = httpclient.execute(post);
						HttpEntity entity = response.getEntity();
					
						if(entity != null) entity.consumeContent();					
					
						List<Cookie> cookielist = httpclient.getCookieStore().getCookies();
						// If we are logged in we got three cookies. A a php-sessionid, username and a id-hash.
						if (cookielist.isEmpty()) {
							Log.d(Tags.LOG,"No cookies, not logged in.");
						} else {
							Log.d(Tags.LOG,"Received "+cookielist.size()+" cookies: ");
							for (int i = 0; i < cookielist.size(); i++) {
								Log.d(Tags.LOG,"- " + cookielist.get(i).toString());
							}
							LOGGEDIN = true;
						}
					
						} catch (UnsupportedEncodingException e) {
							Log.d(Tags.LOG,"login(): UnsupportedEncodingException: "+e.getMessage());
						} catch (ClientProtocolException e) {
							Log.d(Tags.LOG,"login(): ClientProtocolException: "+e.getMessage());
						} catch (IOException e) {
							httpclient = new DefaultHttpClient();
							Log.d(Tags.LOG,"login(): IO Exception: "+e.getMessage());
							Log.d(Tags.LOG,"login(): "+e.toString());
						}
				}
				pd_handler.sendEmptyMessage(0);
			}
    	}.start();
    }
	final Handler pd_handler = new Handler(){
		public void handleMessage(Message msg){
			pd.dismiss();
			if(LOGGEDIN) startSearch();		
		}
	};
    
	public void startSearch(){
		Search.SEARCHTERM = "";
		startActivity(new Intent(this,Search.class));
	}
	
	public void startLocalNZB(){
		startActivity(new Intent(this,LocalNZB.class));
	}
	
    public  void startPreferences(){
        startActivity(new Intent(this,Preferences.class));
    }
    
    public void startMonitor(){
    	String preferredNewsgrabber = preferences.getString("preferredNewsgrabber", "None");
    	if(preferredNewsgrabber.equals("None")){
    		Toast.makeText(this, "Select preferred newsgrabber in preferences.", Toast.LENGTH_LONG).show();
    		return;
    	}
    	if(preferredNewsgrabber.equals("HellaNZB")){
    		if(preferences.getString("hellanzb_hostname", "").equals("")){
    			Toast.makeText(this, "Enter HellaNZB settings in preferences.", Toast.LENGTH_LONG).show();
    			return;
    		}
    		else{
    			startActivity(new Intent(this,MonitorHellaNZB.class));	
    			return;
    		}
    	}
    }

    public void quit(){
    	Log.d(Tags.LOG,"- quit(): Ending application.");
    	
    	this.finish();
    }
    

}