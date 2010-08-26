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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;


import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import com.rvl.android.getnzb.R;

import com.rvl.android.getnzb.GetNZB;
import com.rvl.android.getnzb.Tags;
import android.app.Activity;
import android.app.ProgressDialog;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

	public static HellaNZB HELLACONNECTION = new HellaNZB();
	public static HashMap<String,Object> hellareturn = null;
	public static final int MENU_PREFS = 0;
	public static final int MENU_QUIT = 1;
	public static final int ITEM_DELETE = 0;
	public static final int CONNECT_OK = 1;
	public static final int CONNECT_FAILED_NO_SETTINGS = 2;
	public static final int CONNECT_FAILED_OTHER = 3;
	public static String UPLOADFILENAME = "";
    public NZBDatabase LocalNZBMetadata = new NZBDatabase(this);
    
	static ProgressDialog UPLOADDIALOG = null;
	
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(Tags.LOG,"- Starting LocalNZB Activity!");	
		setContentView(R.layout.localnzb);
	
		listLocalFiles();
	}
		
	public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.localnzbcontextmenu, menu);
		super.onCreateContextMenu(menu, view, menuInfo);
	}
	
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		String localFileList[] = fileList();
		switch(item.getItemId()){

		case R.id.deleteLocalFile:
            deleteLocalFileContextItem(info.id);
            return true;
		case R.id.uploadLocalFileHellaNZB:
			uploadLocalFileHellaNZB(localFileList[(int) info.id]);		
			return true;
		case R.id.uploadLocalFileFTP:
			uploadLocalFileFTP(localFileList[(int) info.id]);
			return true;
		}
		
		return false;
	}
	
	public void uploadLocalFileHellaNZB(String filename){
		UPLOADDIALOG = ProgressDialog.show(this, "Please wait...", "Uploading '"+filename+"' to HellaNZB server.");
			   
		UPLOADFILENAME = filename;
		SharedPreferences prefs = GetNZB.preferences;
		if(prefs.getString("hellanzb_hostname", "")==""){
			uploadDialogHandler.sendEmptyMessage(0);
			Log.d(Tags.LOG,"No HellaNZB settings Toast");
			Toast.makeText(this, "Upload to HellaNZB not possible, please check HellaNZB preferences.", Toast.LENGTH_LONG).show();
			return;
			
		}
		
		new Thread(){
			@SuppressWarnings("unchecked")
			public void run(){
				Log.d(Tags.LOG,"- uploadLocalFileHellaNZB():");
				File uploadfile = new File(getFilesDir()+"/"+UPLOADFILENAME);
				String filedata;
				try{
					filedata = readFile(uploadfile);
					@SuppressWarnings("unused")
					HashMap<String, Object> response = (HashMap<String, Object>) HELLACONNECTION.call("enqueue", uploadfile.getName(), filedata);

				}
				catch (IOException e) {
						Log.d(Tags.LOG,"uploadLocalFile(): IOException: "+e.getMessage());
				}
				removeLocalNZBFile(UPLOADFILENAME);
				UPLOADFILENAME = "";
				uploadDialogHandler.sendEmptyMessage(0);
			}
			
		}.start();
	}


	
	public void uploadLocalFileFTP(String filename){
		UPLOADFILENAME = filename;
		
		UPLOADDIALOG = ProgressDialog.show(this, "Please wait...", "Uploading '"+filename+"' to FTP server.");
		   		
		SharedPreferences prefs = GetNZB.preferences;
		if(prefs.getString("FTPHostname", "")==""){
			uploadDialogHandler.sendEmptyMessage(0);
			Toast.makeText(this, "Upload to FTP server not possible. Please check FTP preferences.", Toast.LENGTH_LONG).show();
			return;
		}
		
		new Thread(){
			
		public void run(){
			SharedPreferences prefs = GetNZB.preferences;
			FTPClient ftp = new FTPClient();
			String FTPHostname = prefs.getString("FTPHostname","");
			String FTPUsername = prefs.getString("FTPUsername", "anonymous");
			String FTPPassword = prefs.getString("FTPPassword","my@email.address");
			String FTPPort     = prefs.getString("FTPPort","21");
			String FTPUploadPath = prefs.getString("FTPUploadPath", "~/");
			if(!FTPUploadPath.matches("$/")){
				Log.d(Tags.LOG,"Adding trailing slash");
				FTPUploadPath += "/";
			}
			String targetFile = FTPUploadPath+UPLOADFILENAME;
			
			try {
				ftp.connect(FTPHostname,Integer.parseInt(FTPPort));
				if(ftp.login(FTPUsername, FTPPassword)){

					ftp.setFileType(FTP.BINARY_FILE_TYPE);
					ftp.enterLocalPassiveMode();
					File file = new File(getFilesDir()+"/"+UPLOADFILENAME);
					BufferedInputStream buffIn = new BufferedInputStream(new FileInputStream(file));
					Log.d(Tags.LOG,"Saving file to:"+targetFile);
					if(ftp.storeFile(targetFile, buffIn)){
						Log.d(Tags.LOG,"FTP: File should be uploaded. Replycode: "+Integer.toString(ftp.getReplyCode()));
					}
					else{
						Log.d(Tags.LOG,"FTP: Could not upload file  Replycode: "+Integer.toString(ftp.getReplyCode()));
					}
				
					buffIn.close();
					ftp.logout();
					ftp.disconnect();
				
					}
					else{
						Log.d(Tags.LOG, "No ftp login");
					}
				} catch (SocketException e) {
					Log.d(Tags.LOG,"ftp(): "+e.getMessage());
					return;
				} catch (IOException e) {
					Log.d(Tags.LOG,"ftp(): "+e.getMessage());
					return;
				}
				removeLocalNZBFile(UPLOADFILENAME);
				
				UPLOADFILENAME = "";
				uploadDialogHandler.sendEmptyMessage(0);
			}
		
		}.start();
	}

	public void deleteLocalFileContextItem(long id){
		String localFiles[] = fileList();
		removeLocalNZBFile(localFiles[(int) id]);
		Toast.makeText(this, "File Deleted", Toast.LENGTH_SHORT).show();
		listLocalFiles();
	}

		
    public void listLocalFiles(){
    
    	Log.d(Tags.LOG, "- localnzb.listLocalFiles()");
    	setContentView(R.layout.localnzb);
    	SharedPreferences prefs = GetNZB.preferences;
		String preferredMethod = prefs.getString("preferredUploadMethod", "");
    	TextView statusbar = (TextView) findViewById(R.id.hellaStatus);
    	statusbar.setText("Local files. Click to upload to "+preferredMethod+", long click for options:");
      	Log.d(Tags.LOG,"Opening database.");
    	LocalNZBMetadata.openDatabase();
    	Cursor cur;
    	  	
    	// -- Bind the itemlist to the itemarray with the arrayadapter
    	ArrayList<String> items = new ArrayList<String>();
 		ArrayAdapter<String> localFilesArrayAdapter =  new LocalNZBRowAdapter(this,items);

    	ListView localFilesListView = (ListView) findViewById(R.id.localFileList);
    	localFilesListView.setCacheColorHint(00000000);
    	localFilesListView.setAdapter(localFilesArrayAdapter);   
    	
    	registerForContextMenu(localFilesListView);
    	
    	localFilesListView.setOnItemClickListener(new OnItemClickListener(){   
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position,
                            long id) {
            					String localFilesArray[] = fileList();      
            					SharedPreferences prefs = GetNZB.preferences;
            					String preferredMethod = prefs.getString("preferredUploadMethod", "");
            					
            					if(preferredMethod.equals("HellaNZB")){
            						Log.d(Tags.LOG,"itemclik(): uploading with HellaNZB.");
            						uploadLocalFileHellaNZB(localFilesArray[position]);
            						return;
            					}
            					if(preferredMethod.equals("FTP")){
            						Log.d(Tags.LOG,"itemclick(): uploading with FTP.");
            						uploadLocalFileFTP(localFilesArray[position]);
            						return;
            					}
            				
            }       
    	});
    	
    	// Retrieve files from disc, retrieve metadata from DB,
    	// combine this data and send it to the arrayadapter.
    	Log.d(Tags.LOG,"Retrieving file metadata and building LocalNZB list.");
    	String age = "";
    	String size = "";
    	String category = "";
    	String fileinfo = "";
    	String localFilesArray[] = fileList();
		for(int c=0;c<localFilesArray.length;c++){
		   	cur = LocalNZBMetadata.myDatabase.query("file", new String[] {"_id","name"}, "name = '"+localFilesArray[c]+"'", null, null, null, null);
		    if(cur.moveToFirst()){
		    	//if there is a hit, retrieve metadata
		    	int idIndex = cur.getColumnIndex("_id");
		    	int file_id = cur.getInt(idIndex);
		    	
		    	cur = LocalNZBMetadata.myDatabase.query("meta", new String[] {"age", "size", "category"}, "file_id ='"+file_id+"'", null, null, null, null);
		    	
		    	if(cur.moveToFirst()){
		    		int ageIndex = cur.getColumnIndex("age");
		    		int sizeIndex = cur.getColumnIndex("size");
		    		int catIndex = cur.getColumnIndex("category");
		    		age = cur.getString(ageIndex);
		    		size = cur.getString(sizeIndex);
		    		category = cur.getString(catIndex);
		    	}
		    	else{
		    		// If there is no metadata for file set dummy metadata info.
		    		age = "???";
		    		size = "???";
		    		category = "???";
		    	}
		    }
		    else{
		    	// If there is no file info in database set dummy info.
		    	age = "???";
   				size = "???";
   				category = "???";
	    	 }
			fileinfo = age + "#" + size + "#"+ localFilesArray[c] + "#" + category;
			items.add(fileinfo);
		}
		Log.d(Tags.LOG,"Number of files in list: "+localFilesArray.length);	
		localFilesArrayAdapter.notifyDataSetChanged();
		LocalNZBMetadata.close();
    }
 	
	final  Handler uploadDialogHandler = new Handler(){
		public void handleMessage(Message msg){
			// After uploading refresh the LocalNZB filelist.		
			UPLOADDIALOG.dismiss();
			listLocalFiles();
		}
	};
	
	// Wrapper for deleting file from LocalNZB list.
	public void removeLocalNZBFile(String filename){
		Log.d(Tags.LOG,"Removing file from database.");
		removeFromDatabase(filename);
		Log.d(Tags.LOG, "Removing file from storage.");
		deleteFile(filename);		
	}
	
	public void removeFromDatabase(String filename){
		LocalNZBMetadata.openDatabase();
		// Get file _id
		Cursor cur = LocalNZBMetadata.myDatabase.query("file", new String[] {"_id"}, "name='"+filename+"'",
											 	       null,null,null,null);
		if(cur.moveToFirst()){
			
			int idIndex = cur.getColumnIndex("_id");
			
			Log.d(Tags.LOG,"removeFromDatabase(): Deleting file entry in DB (file).");
			LocalNZBMetadata.myDatabase.delete("file", "_id='"+cur.getString(idIndex)+"'", null);
			
			Log.d(Tags.LOG,"removeFromDatabase(): Deleting file metadata in DB (meta).");
			LocalNZBMetadata.myDatabase.delete("meta", "file_id='"+cur.getString(idIndex)+"'", null);		
		}
		LocalNZBMetadata.close();
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