package com.rvl.android.getnzb;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;

public class NZBHandler{
	ProgressDialog uploaddialog;
	Activity context;
	
	public NZBHandler(Activity context){
		this.context = context;
	}
	
	void upload(final String directory, final String filename){
		final String file = directory + "/" + filename;
		uploaddialog = ProgressDialog.show(context,"Please wait...","Uploading '"+filename+"' to HellaNZB server.");
		Log.d(Tags.LOG,"nzbhandler.upload():"+file);
		new Thread(){
			public void run(){
				
				File nzbfile = new File(file);
				String filedata;
				try {
					filedata = LocalNZB.readFile(nzbfile);
					@SuppressWarnings("unused")
					HashMap<String, Object> response = (HashMap<String, Object>) LocalNZB.hellaNZBCall("enqueue", nzbfile.getName(), filedata);

				} catch (IOException e) {
					Log.d(Tags.LOG,"uploadLocalFile(): IOException: "+e.getMessage());
				}
				Log.d(Tags.LOG,"Deleting file:"+file);
				context.deleteFile(filename);
				uploaddialog.dismiss();
				LocalNZB l = new LocalNZB();
				l.listLocalFiles();
			}
			
		}.start();
		
		
	}
}