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

package com.rvl.android.getnzb.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import com.rvl.android.getnzb.R;
import com.rvl.android.getnzb.getnzb;
import com.rvl.android.getnzb.tags;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class hellanzb extends Activity {

	public static URI uri;
	public static XMLRPCClient client;	
	public static boolean CONNECTED = false;
	public static HashMap<String,Object> hellareturn = null;
	public static final int MENU_PREFS = 0;
	public static final int MENU_QUIT = 1;
	public static final int CONNECT_OK = 1;
	public static final int CONNECT_FAILED_NO_SETTINGS = 2;
	public static final int CONNECT_FAILED_OTHER = 3;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(tags.LOG,"- Starting HellaNZB Activity!");	
		setContentView(R.layout.hellanzb);
		
		if(!CONNECTED) connect();
		listfiles();
	}
		
	public int connect(){
 		Log.d(tags.LOG,"- hellanzb.connect()");
		Log.d(tags.LOG,"connect(): Getting preferences");
		SharedPreferences prefs = getnzb.preferences;
		String hellahost = prefs.getString("hellanzb_hostname", "");
		TextView statusbar = (TextView) findViewById(R.id.hellastatus);
		
		if(!hellahost.matches("(https?)://.+")) 
			hellahost = "http://" + hellahost;
		try {
			Log.d(tags.LOG,"connecty(): Creating URI: "+hellahost + ":" + prefs.getString("hellanzb_port", "8760"));
			uri = URI.create(hellahost + ":" + prefs.getString("hellanzb_port", "8760"));

			Log.d(tags.LOG,"connect(): Creating Client");
			client = new XMLRPCClient(uri.toURL());
			
			Log.d(tags.LOG,"Authenticating with:'"+prefs.getString("hellanzbpassword", "")+"'");
			client.setBasicAuthentication("hellanzb", prefs.getString("hellanzbpassword",""));
			
			Log.d(tags.LOG,"connecty(): Calling 'aolsay'");
			if(client.call("aolsay") != ""){
				String message = "Connected";
				statusbar.setText(message);
				CONNECTED = true;
				return CONNECT_OK;
			}
			
		} catch (MalformedURLException e) {
			Log.d(tags.LOG,"connect() failed: MalformedURLException:"+e.getMessage());
			return CONNECT_FAILED_OTHER;
		} catch (XMLRPCException e) {
			Log.d(tags.LOG,"connect() failed: XMLRPCException:"+e.getMessage());
			return CONNECT_FAILED_OTHER;
		}
		return CONNECT_FAILED_OTHER;
	}
		
    public void listfiles(){
    	if(!CONNECTED) connect();
    	Log.d(tags.LOG, "- hellanzb.listfiles()");
    	setContentView(R.layout.hellanzb);
    	TextView statusbar = (TextView) findViewById(R.id.hellastatus);
    	statusbar.setText("Local files. Click to upload:");
    	
    	// -- Bind the itemlist to the itemarray with the arrayadapter
    	ArrayList<String> items = new ArrayList<String>();
    	ArrayAdapter<String> aa = new ArrayAdapter<String>(this,com.rvl.android.getnzb.R.layout.itemslist,items);
    	
    	ListView itemlist = (ListView) findViewById(R.id.filelist01);
    	itemlist.setCacheColorHint(00000000);
    			
    	itemlist.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
					String list[] = fileList();					
					new uploadfile(list[position]).execute(list[position]);
			}	
    	});
    	   	
    	itemlist.setAdapter(aa);
    	String list[] = fileList();
		for(int c=0;c<list.length;c++){
			items.add(list[c]);
			Log.d(tags.LOG,"List of local files: "+list[c]); 
		}
		
    	aa.notifyDataSetChanged();
    }
    
	public Object hellanzbcall(String command) {
 		Log.d(tags.LOG,"- hellanzb.hellanzbcall(c)");
		try {
			if(CONNECTED) return client.call(command);
			else{
				Log.d(tags.LOG,"hellanzbcall(): Not connected, connecting first.");
				connect();
				return client.call(command);
			}
		} catch(XMLRPCException e) {
			Log.e(tags.LOG, "hellanzbcall(): "+e.getMessage());
			CONNECTED = false;
		}
		return null;
	}

	public Object hellanzbcall(String command, String extra1) {
 		Log.d(tags.LOG,"- hellanzb.hellanzbcall(c,e)"); 	
		try {
			if(CONNECTED) return client.call(command, extra1);
			else{
				Log.d(tags.LOG,"hellanzbcall(): Not connected, connecting first.");
				connect();
				return client.call(command, extra1);
			}
		} catch(XMLRPCException e) {
			Log.e(tags.LOG, "hellanzbcall(): "+e.getMessage());
			CONNECTED = false;
		}
		return null;
	}
	
	public Object hellanzbcall(String command, String extra1, String extra2) {
 		Log.d(tags.LOG,"- hellanzb.hellanzbcall(c,e,e)");
		try {
			if(CONNECTED) {
				return client.call(command, extra1, extra2);
				
			} else{
				Log.d(tags.LOG,"hellanzbcall(): Not connected, connecting first.");
				connect();
				return client.call(command, extra1, extra2);
			}
		} catch(XMLRPCException e) {
			Log.e(tags.LOG, "hellanzbcall(): "+e.getMessage());
			CONNECTED = false;
		}
		return null;
	}

	private class uploadfile extends AsyncTask<String, Void, Void>{
 		
		ProgressDialog uploaddialog = new ProgressDialog(hellanzb.this);
		String filename;
		uploadfile(final String name){
			this.filename = name;
		}
		
	   	protected void onPreExecute(){
			Log.d(tags.LOG,"- hellanzb.uploadfile.preExecute()");
    		this.uploaddialog.setMessage("Uploading '"+this.filename+"' to HellaNZB server...");
    		this.uploaddialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    		this.uploaddialog.show();
    	}
		
		@SuppressWarnings("unchecked")
		@Override
		protected Void doInBackground(String... params) {
			Log.d(tags.LOG,"- hellanzb.uploadfile.doInBackground()");
			String filename = params[0];
			Log.d(tags.LOG,"Complete filename: "+getFilesDir()+"/"+filename);
			File nzbfile = new File(getFilesDir()+"/"+filename);
			String filedata;
			try {
				filedata = readfile(nzbfile);
				@SuppressWarnings("unused")
				HashMap<String, Object> response = (HashMap<String, Object>) hellanzbcall("enqueue", nzbfile.getName(), filedata);

			} catch (IOException e) {
				Log.d(tags.LOG,"uploadfile(): IOException: "+e.getMessage());
			}
			// Delete file after uploading...
			Log.d(tags.LOG,"Deleting file:"+filename);
			deleteFile(filename);
			return null;
		}
		protected void onPostExecute(final Void unused){
			Log.d(tags.LOG,"- hellanzb.uploadfile.onPostExecute()");
			this.uploaddialog.dismiss();
    		// Reload file list...
    		listfiles();
    		return;
		}

	}
	
	public static String readfile(File file) throws IOException {
		Log.d(tags.LOG,"readfile(): Converting file to string. ("+file.getAbsolutePath()+")");
        FileInputStream stream = new FileInputStream(file);
        try {
        	FileChannel fc = stream.getChannel();
        	MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        	/* Instead of using default, pass in a decoder. */
        	return Charset.defaultCharset().decode(bb).toString();
        } 
        finally{     
        	stream.close();
        }
	}
	
}