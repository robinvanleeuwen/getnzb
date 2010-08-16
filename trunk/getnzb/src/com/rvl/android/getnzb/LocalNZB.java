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

package com.rvl.android.getnzb;

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
import java.util.List;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import com.rvl.android.getnzb.R;

import com.rvl.android.getnzb.GetNZB;
import com.rvl.android.getnzb.Tags;
import android.app.Activity;
import android.app.ProgressDialog;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class LocalNZB extends Activity {

	public static URI uri;
	public static XMLRPCClient client;	
	public static boolean CONNECTED = false;
	public static HashMap<String,Object> hellareturn = null;
	public static final int MENU_PREFS = 0;
	public static final int MENU_QUIT = 1;
	public static final int ITEM_DELETE = 0;
	public static final int CONNECT_OK = 1;
	public static final int CONNECT_FAILED_NO_SETTINGS = 2;
	public static final int CONNECT_FAILED_OTHER = 3;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(Tags.LOG,"- Starting HellaNZB Activity!");	
		setContentView(R.layout.hellanzb);
		
		if(!CONNECTED) hellaConnect();
		listLocalFiles();
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.filelistmenu, menu);
		super.onCreateContextMenu(menu, view, menuInfo);
	}
	
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId()){
		case R.id.deleteLocalFile:
			deleteLocalFile(info.id);
			return true;
		// case R.id.infofile:
		// ToDo parse .nzb and display info.
		// return true;
		}
		return false;
	}
	
	public void deleteLocalFile(long id){
		String localFiles[] = fileList();
		deleteFile(localFiles[(int) id]);
		Log.d(Tags.LOG,"deleteLocalFile(): "+localFiles[(int) id]);
		Toast.makeText(this, "File Deleted", Toast.LENGTH_SHORT).show();
		listLocalFiles();
	}
	
	public int hellaConnect(){
 		Log.d(Tags.LOG,"- hellanzb.hellaConnect()");
		Log.d(Tags.LOG,"hellaConnect(): Getting preferences");
		SharedPreferences prefs = GetNZB.preferences;
		String hellaHost = prefs.getString("hellanzb_hostname", "");
		TextView statusbar = (TextView) findViewById(R.id.hellaStatus);
		
		if(!hellaHost.matches("(https?)://.+")) 
			hellaHost = "http://" + hellaHost;
		try {
			Log.d(Tags.LOG,"hellaConnecty(): Creating URI: "+hellaHost + ":" + prefs.getString("hellanzb_port", "8760"));
			uri = URI.create(hellaHost + ":" + prefs.getString("hellanzb_port", "8760"));
			Log.d(Tags.LOG,"hellaConnect(): Creating Client");
			client = new XMLRPCClient(uri.toURL());
			client.setBasicAuthentication("hellanzb", prefs.getString("hellanzbpassword",""));			
			Log.d(Tags.LOG,"hellaConnecty(): Calling 'aolsay'");
			if(client.call("aolsay") != ""){
				String message = "Connected";
				statusbar.setText(message);
				CONNECTED = true;
				return CONNECT_OK;
			}
			
		} catch (MalformedURLException e) {
			Log.d(Tags.LOG,"hellaConnect() failed: MalformedURLException:"+e.getMessage());
			return CONNECT_FAILED_OTHER;
		} catch (XMLRPCException e) {
			Log.d(Tags.LOG,"hellaConnect() failed: XMLRPCException:"+e.getMessage());
			return CONNECT_FAILED_OTHER;
		}
		return CONNECT_FAILED_OTHER;
	}
		
    public void listLocalFiles(){
    	if(!CONNECTED) hellaConnect();
    	Log.d(Tags.LOG, "- hellanzb.listLocalFiles()");
    	setContentView(R.layout.hellanzb);
    	TextView statusbar = (TextView) findViewById(R.id.hellaStatus);
    	statusbar.setText("Local files. Click to upload:");
    	
    
    	// -- Bind the itemlist to the itemarray with the arrayadapter
    	ArrayList<String> items = new ArrayList<String>();
    	ArrayAdapter<String> aa = new ArrayAdapter<String>(this,com.rvl.android.getnzb.R.layout.itemslist,items);
    	
  	
    	ListView itemlist = (ListView) findViewById(R.id.localFileList);
    	itemlist.setCacheColorHint(00000000);
    	itemlist.setOnCreateContextMenuListener(this);
   
    	itemlist.setOnItemClickListener(new OnItemClickListener(){
	
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
					String localFiles[] = fileList();	
					new uploadLocalFile(localFiles[position]).execute(localFiles[position]);
			}	
    	});
    	   	
    	itemlist.setAdapter(aa);
    	String localFiles[] = fileList();
		for(int c=0;c<localFiles.length;c++){
			items.add(localFiles[c]);
		}
		Log.d(Tags.LOG,"Number of files in list: "+localFiles.length); 
    	aa.notifyDataSetChanged();
    }
    
	public Object hellaNZBCall(String command) {
 		Log.d(Tags.LOG,"- hellanzb.hellaNZBCall(c)");
		try {
			if(CONNECTED) return client.call(command);
			else{
				Log.d(Tags.LOG,"hellaNZBCall(): Not hellaConnected, hellaConnecting first.");
				hellaConnect();
				return client.call(command);
			}
		} catch(XMLRPCException e) {
			Log.e(Tags.LOG, "hellaNZBCall(): "+e.getMessage());
			CONNECTED = false;
		}
		return null;
	}

	public Object hellaNZBCall(String command, String extra1) {
 		Log.d(Tags.LOG,"- hellanzb.hellaNZBCall(c,e)"); 	
		try {
			if(CONNECTED) return client.call(command, extra1);
			else{
				Log.d(Tags.LOG,"hellaNZBCall(): Not connected, connecting first.");
				hellaConnect();
				return client.call(command, extra1);
			}
		} catch(XMLRPCException e) {
			Log.e(Tags.LOG, "hellaNZBCall(): "+e.getMessage());
			CONNECTED = false;
		}
		return null;
	}
	
	public Object hellaNZBCall(String command, String extra1, String extra2) {
 		Log.d(Tags.LOG,"- hellanzb.hellaNZBCall(c,e,e)");
		try {
			if(CONNECTED) {
				return client.call(command, extra1, extra2);
				
			} else{
				Log.d(Tags.LOG,"hellaNZBCall(): Not connected, connecting first.");
				hellaConnect();
				return client.call(command, extra1, extra2);
			}
		} catch(XMLRPCException e) {
			Log.e(Tags.LOG, "hellaNZBCall(): "+e.getMessage());
			CONNECTED = false;
		}
		return null;
	}

	private class uploadLocalFile extends AsyncTask<String, Void, Void>{
 		
		ProgressDialog uploadDialog = new ProgressDialog(LocalNZB.this);
		String filename;
		uploadLocalFile(final String name){
			this.filename = name;
		}
		
	   	protected void onPreExecute(){
			Log.d(Tags.LOG,"- hellanzb.uploadLocalFile.preExecute()");
    		this.uploadDialog.setMessage("Uploading '"+this.filename+"' to HellaNZB server...");
    		this.uploadDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    		this.uploadDialog.show();
    	}
		
		@SuppressWarnings("unchecked")
		@Override
		protected Void doInBackground(String... params) {
			Log.d(Tags.LOG,"- hellanzb.uploadLocalFile.doInBackground()");
			String filename = params[0];
			Log.d(Tags.LOG,"Complete filename: "+getFilesDir()+"/"+filename);
			File nzbfile = new File(getFilesDir()+"/"+filename);
			String filedata;
			try {
				filedata = readFile(nzbfile);
				@SuppressWarnings("unused")
				HashMap<String, Object> response = (HashMap<String, Object>) hellaNZBCall("enqueue", nzbfile.getName(), filedata);

			} catch (IOException e) {
				Log.d(Tags.LOG,"uploadLocalFile(): IOException: "+e.getMessage());
			}
			// Delete file after uploading...
			Log.d(Tags.LOG,"Deleting file:"+filename);
			deleteFile(filename);
			return null;
		}
		protected void onPostExecute(final Void unused){
			Log.d(Tags.LOG,"- hellanzb.uploadLocalFile.onPostExecute()");
			this.uploadDialog.dismiss();
    		// Reload file list...
    		listLocalFiles();
    		return;
		}

	}
	
	public static String readFile(File file) throws IOException {
		Log.d(Tags.LOG,"readfile(): Converting file to string. ("+file.getAbsolutePath()+")");
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