package com.rvl.android.getnzb;

import android.content.SharedPreferences;

import com.dropbox.client.DropboxAPI;
import com.dropbox.client.DropboxAPI.Config;

public class Dropbox{
	private DropboxAPI API = new DropboxAPI();
	private boolean DROPBOXLOGGEDIN = false;
	private Config DROPBOXCONFIG;
	
	public void login(){
		
		new Thread(){
			public void run(){
				if(DROPBOXLOGGEDIN == true) return;
		
				SharedPreferences prefs = GetNZB.preferences;
				if(DROPBOXCONFIG == null){
					DROPBOXCONFIG = API.getConfig(null, false);
					DROPBOXCONFIG.consumerKey = prefs.getString("dropboxAccount", "");
					DROPBOXCONFIG.consumerSecret = prefs.getString("dropboxPassword", "");
					DROPBOXCONFIG.server = "api.dropbox.com";
					DROPBOXCONFIG.contentServer="api-content.dropbox.com";
					DROPBOXCONFIG.port = 80;
				}
				DROPBOXCONFIG = API.authenticate(DROPBOXCONFIG,prefs.getString("dropboxAccount", ""), 
						prefs.getString("dropboxPassword", "") 
						);
				
			}
		}.start();
	}
	
	
}