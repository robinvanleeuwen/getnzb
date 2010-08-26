/**
 * This file is a part of GetNZB
 * 
 * GetNZB - http://code.google.com/p/getnzb
 * "Android NZB Search and HellaNZB/FTP client."
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

package com.rvl.android.getnzb;

import java.net.MalformedURLException;
import java.net.URI;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.content.SharedPreferences;
import android.util.Log;



public class HellaNZB{

	public boolean CONNECTED;
	public XMLRPCClient client;	

	public void HellaNZB(){
		this.CONNECTED = false;
	}
	
	public void connect(){
		
		Log.d(Tags.LOG,"- HellaNZB.connect()");
		if(this.CONNECTED) return;
		SharedPreferences prefs = GetNZB.preferences;
		String hellaHost = prefs.getString("hellanzb_hostname", "");
			
		if(!hellaHost.matches("(https?)://.+")) 
			hellaHost = "http://" + hellaHost;
		try {
			Log.d(Tags.LOG,"HellaNZB.connect(): Creating URI: "+hellaHost + ":" + prefs.getString("hellanzb_port", "8760"));
			URI uri = URI.create(hellaHost + ":" + prefs.getString("hellanzb_port", "8760"));

			Log.d(Tags.LOG,"HellaNZB.connect(): Creating Client");
			this.client = new XMLRPCClient(uri.toURL());
			this.client.setBasicAuthentication("hellanzb", prefs.getString("hellanzbpassword",""));			
			
			if(client.call("aolsay") != ""){
				Log.d(Tags.LOG,"HellaNZB.connect() succeeded.");
				this.CONNECTED = true;		
				return;
			}
			
		} catch (MalformedURLException e) {
			Log.d(Tags.LOG,"hellaConnect() failed: MalformedURLException:"+e.getMessage());
			return;
		} catch (XMLRPCException e) {
			Log.d(Tags.LOG,"hellaConnect() failed: XMLRPCException:"+e.getMessage());
			return;
		}
		Log.d(Tags.LOG,"HellaNZB Connection failed, unkown reason.");
		return;
		
	}
	
	public void disconnect(){
		Log.d(Tags.LOG,"- HellaNZB.disconnect()");
		if(!this.CONNECTED) return;
		this.CONNECTED = false;
		this.client = null;
	}
	
	public Object call(String command) {
 		Log.d(Tags.LOG,"- HellaNZB.call("+command+")");
		try {
			if(this.CONNECTED) {
				return this.client.call(command);
				
			} else{
				Log.d(Tags.LOG,"HellaNZB.call(): Not connected, connecting first.");
				connect();
				return this.client.call(command);
			}
		} catch(XMLRPCException e) {
			Log.e(Tags.LOG, "HellaNZB.call(): "+e.getMessage());
			this.CONNECTED = false;
		}
		return null;
	}

	public Object call(String command, String extra1) {
 		Log.d(Tags.LOG,"- HellaNZB.call(c,e)");
		try {
			if(this.CONNECTED) {
				return this.client.call(command, extra1);
				
			} else{
				Log.d(Tags.LOG,"HellaNZB.call(): Not connected, connecting first.");
				connect();
				return this.client.call(command, extra1);
			}
		} catch(XMLRPCException e) {
			Log.e(Tags.LOG, "HellaNZB.call(): "+e.getMessage());
			this.CONNECTED = false;
		}
		return null;
	}

	public Object call(String command, String extra1, String extra2) {
 		Log.d(Tags.LOG,"- HellaNZB.call(c,e,e)");
		try {
			if(this.CONNECTED) {
				return this.client.call(command, extra1, extra2);
				
			} else{
				Log.d(Tags.LOG,"HellaNZB.call(): Not connected, connecting first.");
				connect();
				return this.client.call(command, extra1, extra2);
			}
		} catch(XMLRPCException e) {
			Log.e(Tags.LOG, "HellaNZB.call(): "+e.getMessage());
			this.CONNECTED = false;
		}
		return null;
	}
	
}