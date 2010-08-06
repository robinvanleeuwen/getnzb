package com.rvl.android.getnzb;

/* (C) 2010 Robin van Leeuwen
 * Licence: GPLv2 or later.
 */


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.rvl.android.getnzb.activity.*;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


public class getnzb extends Activity {
	public static final int MENU_PREFS = 0;
	public static final int MENU_QUIT = 1;
	HttpClient httpclient = new DefaultHttpClient();
	CookieStore cookies = new BasicCookieStore();
	ProgressDialog pd = null;
	
	public static SharedPreferences preferences;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(tags.LOG,"------ STARTING GETNZB ------");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
    }
    
    public boolean onCreateOptionsMenu(Menu menu){
		menu.add(0, MENU_PREFS, 0, "Preferences");
		menu.add(0, MENU_QUIT, 0, "Quit");
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item){
    	switch (item.getItemId()){
    	case MENU_PREFS:
    		startpreferences();
    		return true;
    	case MENU_QUIT:
    		quit();
    		return true;
    	}
    	return false;
    }
    public void button_handler(View v){
    	switch(v.getId()){
    	case R.id.button_login:
    		pd = ProgressDialog.show(getnzb.this, "http://www.nzbs.org", "Logging in, please wait...");
 			login();
    		break;
    	}
    }
    
    
    public void login(){
    	// Login to www.nzbs.org and go to search-activity. 
    	// The return values from nzbs.org are stored in the cookiestore for later use.
    	
    	new Thread() {
			public void run(){
	
				Log.d(tags.LOG,"Login function started.");
				SharedPreferences pref = getSharedPreferences(tags.PREFS, 0);
				Log.d(tags.LOG,"Using login name:"+pref.getString("nzbsusername", "No value given."));
							
				HttpPost httppost = new HttpPost(tags.NZBS_LOGINPAGE);
				
				List<NameValuePair> nvp = new ArrayList<NameValuePair>(2);
				nvp.add(new BasicNameValuePair("username",pref.getString("nzbsusername", "")));
				nvp.add(new BasicNameValuePair("password",pref.getString("nzbspassword", "")));
				nvp.add(new BasicNameValuePair("action","dologin"));

				try {
					httppost.setEntity(new UrlEncodedFormEntity(nvp));
					HttpResponse httpresponse = httpclient.execute(httppost);
					HttpEntity entity = httpresponse.getEntity();
					if(entity != null){
						entity.consumeContent();
						
					}
	    
				} catch (UnsupportedEncodingException e) {
				
				} catch (ClientProtocolException e) {
				
				} catch (IOException e) {
				
				}
			
				pd_handler.sendEmptyMessage(0);
			}
    	}.start();
    }
	final Handler pd_handler = new Handler(){
		public void handleMessage(Message msg){
			pd.dismiss();
		}
	};
    
    public void startpreferences(){
        startActivity(new Intent(this,preferences.class));
    }

    public void quit(){
    	Log.d(tags.LOG,"Ending application.");
    	this.finish();
    }

}