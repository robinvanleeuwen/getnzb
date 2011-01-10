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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.rvl.android.getnzb.R;
import com.rvl.android.getnzb.GetNZB;
import com.rvl.android.getnzb.Tags;

public class Search extends Activity {
	private DefaultHttpClient httpclient = GetNZB.httpclient;
	private boolean LOGGEDIN = GetNZB.LOGGEDIN;
	public static String HITLIST[][];
	public boolean ENABLE_NEXTBUTTON = true;
	public static String SEARCHTERM;
	public static String SEARCHCATEGORY;
	public static String SEARCHAGE;
	public int NUMSEARCHHITS;
	public int CURRENT_PAGE = 1; 	// Start searching on page 1
	public static final int MENU_GETCART = 0;
    public NZBDatabase LocalNZBMetadata = new NZBDatabase(this);
    public static HashMap<String,String> SEARCHCATEGORYHASHMAP = new HashMap<String,String>();
    public static HashMap<String,String> SEARCHAGEHASHMAP = new HashMap<String,String>();
    
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(Tags.LOG, "- Starting search activity -");
		super.onCreate(savedInstanceState);
	 	this.setRequestedOrientation(
    			ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.search);
		if(!SEARCHTERM.equals("")){
			new searchNZB().execute(SEARCHTERM, SEARCHCATEGORY,SEARCHAGE);
			return;
		}
		
		TextView statusbar = (TextView) findViewById(R.id.statusbar);
		statusbar.setText("Enter searchterm.");
		createSearchCategoryMapping();
		createAgeMapping();
		
	}
	
   
	
    public boolean onOptionsItemSelected(MenuItem item){
    	switch (item.getItemId()){
    	case MENU_GETCART:
    		getcart();
    		return true;
    	}
    	return false;
    }
    public void getcart(){
    	
    }
	public void btn_handler(View v){
	   	switch(v.getId()){
	    	
		   	case R.id.btn_search:
		 		EditText ed = (EditText) findViewById(R.id.searchterm);		
		 		SEARCHTERM = ed.getText().toString().trim().replaceAll(" ", "+");
		 		Spinner categorySpinner = (Spinner) findViewById(R.id.spinnerCategory);
		 		SEARCHCATEGORY = SEARCHCATEGORYHASHMAP.get(categorySpinner.getSelectedItem().toString());
		 		Spinner ageSpinner = (Spinner) findViewById(R.id.spinnerAge);
		 		SEARCHAGE = SEARCHAGEHASHMAP.get(ageSpinner.getSelectedItem().toString());
		 		Log.d(Tags.LOG,"Searching in "+SEARCHCATEGORY);
		 		new searchNZB().execute(SEARCHTERM, SEARCHCATEGORY,SEARCHAGE); 		
	    		break;
		
		   	case R.id.buttonMySearches:
		   		startMySearch();
		   		break;
	    	case R.id.btn_next:
	    		if(HITLIST.length == 25){
	    			HITLIST = null;
	    			CURRENT_PAGE++;
	    			new searchNZB().execute(SEARCHTERM, SEARCHCATEGORY,SEARCHAGE);
	    		}
	    		else{
	    			TextView statusbar = (TextView) findViewById(R.id.statusbar);
	    			statusbar.setText("No more matches.");
	    		}
	    		break;
	    	case R.id.btn_previous:
	    		HITLIST = null;
	    		if(CURRENT_PAGE > 1) CURRENT_PAGE--;
	    		new searchNZB().execute(SEARCHTERM, SEARCHCATEGORY, SEARCHAGE);
	    		break;
	    	case R.id.btn_backtosearch:
	    		HITLIST = null;
	    		CURRENT_PAGE = 1;
	    		SEARCHTERM = "";
	    		setContentView(R.layout.search);
	    		TextView statusbar = (TextView) findViewById(R.id.statusbar);
	    		statusbar.setText("Enter searchterm and select category.");
	    		break;

	    	}
	}
	
	// searchNZB() searches nzbs.org and reads the supplied HTML page with HTMLCleaner
	// to build a list of nzb-files (links) that can be sent to the HellaNZB server.	
	public class searchNZB extends AsyncTask<String, Void, Void>{
		    
	    ProgressDialog searchDialog = new ProgressDialog(Search.this);
	    
		private int n = -1;
	      
	    protected void onPreExecute(){
	    	this.searchDialog.setMessage("Searching on nzbs.org and building list...");
	    	this.searchDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    	this.searchDialog.show();
	    	Log.d(Tags.LOG, "Searching nzbs.org...");
	    }
	    
	    protected Void doInBackground(final String... args){
	    	Log.d(Tags.LOG,"* Starting searchNZB():");
   			if(!LOGGEDIN){
				Log.d(Tags.LOG, "Search: Not logged in...");
				return null;
			}
   			String url = "";
   			
   			url  = "http://www.nzbs.org/index.php?action=search&q=";
   			url += args[0];
   			url += "&catid=" + args[1];
   			url += "&age=" +  args[2];
   			url += "&page="+Integer.toString(CURRENT_PAGE);
   			
   			Log.d(Tags.LOG,"Constructed URL:"+url);
			try {
				int progresscounter = 5;
				this.searchDialog.setProgress(progresscounter);
	    		
				HtmlCleaner cleaner = new HtmlCleaner();
				CleanerProperties props = cleaner.getProperties();
				props.setAllowHtmlInsideAttributes(true);
				props.setAllowMultiWordAttributes(true);
				props.setRecognizeUnicodeChars(true);
				props.setOmitComments(true);
				Log.d(Tags.LOG,"URL:"+url.toString());
				HttpGet httpget = new HttpGet(url);
				HttpResponse httpresponse = httpclient.execute(httpget);
				HttpEntity entity = httpresponse.getEntity();
			
				
			    progresscounter += 5;
				this.searchDialog.setProgress(progresscounter);
	
				Log.d(Tags.LOG,"Data rerieved, parsing items...");
				
				String xpathLink = "";
				String xpathRows = "//table[@id='nzbtable']/tbody";			
				TagNode node = cleaner.clean(new InputStreamReader(entity.getContent()));
				Object[] nzbTable = node.evaluateXPath(xpathRows);
				
				this.n = nzbTable.length;
				Log.d(Tags.LOG,"N="+Integer.toString(this.n));
				if(this.n == 0){
					NUMSEARCHHITS = 0;
					return null;
				}
				TagNode row = (TagNode) nzbTable[0];
				
				int numhits = row.getChildren().size() - 3;
				
				NUMSEARCHHITS = numhits;
				// Check if there is a next-page...
				// if so, the button is enabled in buildItemList()
				// since we can't do it in this thread (non-UI-operations only)...
				Log.d(Tags.LOG,"Checking if next button needs to be disabled.");
				
				if(numhits < 25){
					Log.d(Tags.LOG,"Disabling next-button");
					ENABLE_NEXTBUTTON = false;
				}
				else{
					Log.d(Tags.LOG,"Enabling next-button");
					ENABLE_NEXTBUTTON = true;							
				}
				
				Log.d(Tags.LOG,"Found "+Integer.toString(numhits)+" items. Adding them to list.");
				this.n = numhits;
				int counterincrement = 88 / numhits;
				String[][] hit = new String[numhits][5];
							
				progresscounter += 5;
				this.searchDialog.setProgress(progresscounter);
				xpathRows = "//table[@id='nzbtable']/tbody";
				nzbTable = node.evaluateXPath(xpathRows);
				Log.d(Tags.LOG,"1");
				TagNode tbody = (TagNode) nzbTable[0];
				Object[] tempObject;
				for(int c=0;c<numhits;c++){
				
					xpathRows = "//tr["+Integer.toString(c+3)+"]";
					nzbTable = tbody.evaluateXPath(xpathRows);				
					Log.d(Tags.LOG,"2");
					// Name
					tempObject = ((TagNode)nzbTable[0]).evaluateXPath("//td[2]/b/a");				
					hit[c][0] = ((TagNode)tempObject[0]).getText().toString();
					
					// Category
					tempObject = ((TagNode)nzbTable[0]).evaluateXPath("//td[3]/a");
					hit[c][4] = ((TagNode)tempObject[0]).getText().toString();
					
					// Days ago
					tempObject = ((TagNode)nzbTable[0]).evaluateXPath("//td[4]");
					hit[c][1] = ((TagNode)tempObject[0]).getText().toString();
					
					// Size
					tempObject = ((TagNode)nzbTable[0]).evaluateXPath("//td[5]");					
					hit[c][2] = ((TagNode)tempObject[0]).getText().toString();
			
					// Download link
					tempObject = ((TagNode)nzbTable[0]).evaluateXPath("//td[8]/b/a");
					hit[c][3] = ((TagNode)tempObject[0]).getAttributeByName("href").replaceAll("&amp;", "&"); 
					
					progresscounter += counterincrement;
					this.searchDialog.setProgress(progresscounter);
				
				}
				Log.d(Tags.LOG,"Added "+Integer.toString(numhits)+" items to list.");
				HITLIST = hit;
			
			} catch (MalformedURLException e) {
				Log.d(Tags.LOG,"searchNZB: malformed url exception: "+e.getMessage());
			} catch (IOException e) {
				Log.d(Tags.LOG,"searchNZB: IO exception: "+e.getMessage());
			} catch (XPatherException e) {
				Log.d(Tags.LOG,"searchNZB: XPatherException: "+e.getMessage());
			}
   			return null;
	    }
		protected void onPostExecute(final Void unused){
    		this.searchDialog.dismiss();
    		TextView statusbar = (TextView) findViewById(R.id.statusbar);
    		String status = "";
    		if(!LOGGEDIN){
    			status = "Not logged in! Check NZB account settings..."; 
    			statusbar.setText(status);
    			Log.d(Tags.LOG,"* searchNZB() ended.");
    		}
    		else{
    			Log.d(Tags.LOG,"* searchNZB() ended.");
       			buildItemList(NUMSEARCHHITS);
    		}		
		}
	}
    public void buildItemList(int numhits){
    	String hits[][] = HITLIST;
    	
    	setContentView(R.layout.links);
    	String item = "";
    	Log.d(Tags.LOG, "* buildItemList()");
    	
    	if(numhits==0){
    		Toast.makeText(this, "No search result found!", Toast.LENGTH_LONG);
    		return;
    	}
    	
    	// -- Bind the itemlist to the itemarray with the arrayadapter
    	ArrayList<String> items = new ArrayList<String>();
    	ArrayAdapter<String> aa = new SearchResultRowAdapter(this,items);
    	
    	//ArrayAdapter<String> aa = new ArrayAdapter<String>(this,com.rvl.android.getnzb.R.layout.itemslist,items);
   
    	ListView itemlist = (ListView) findViewById(R.id.itemlist01);
    	itemlist.setCacheColorHint(00000000);
    	itemlist.setAdapter(aa);
    	registerForContextMenu(itemlist);
    	
    	itemlist.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
					String pos = Integer.toString(position);
					Log.d(Tags.LOG,"Sending download command for position "+pos);
					new downloadfile().execute(pos); 					
			}
    	});
    	   	
    	
    	// --
		Log.d(Tags.LOG, "Building hitlist...");

    	for(int i=0;i<hits.length;i++){
    		item += hits[i][0] + "#" + hits[i][1] + "#" + hits[i][2] + "#" + hits[i][4];
    		items.add(item);
    		Log.d(Tags.LOG,"item:"+item);
    		item = "";
    		
    	}
    	aa.notifyDataSetChanged();
    
    	// Enable or disable the button for next results page...
    	Button nextbutton = (Button) findViewById(R.id.btn_next);
    	nextbutton.setEnabled(ENABLE_NEXTBUTTON);
    	
    }
        		
	protected void onDestroy() {
		Log.d(Tags.LOG,"Leaving search activity.");
		super.onDestroy();
	}
    
    public class downloadfile extends AsyncTask<String, Void, Void>{
    	//private static final Object[] String a = null;
		ProgressDialog sdc_dialog = new ProgressDialog(Search.this);
		
    	protected void onPreExecute(){
    		this.sdc_dialog.setMessage("Downloading .nzb file...");
    		this.sdc_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    		this.sdc_dialog.show();
    	}
    	
    	protected Void doInBackground(final String... args){
    		  	
	    	int position = Integer.parseInt(args[0]);
    	    		
    		try{
    			  URI uri = new URI("http://www.nzbs.org/"+HITLIST[position][3]);
    			  
    			  HttpGet getter = new HttpGet(uri);
    			  getter.setHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT5.1; en-US; rv:1.8.1.20) Gecko/20081217 Firefox/2.0.0.20");
    			  
    			  HttpResponse r = httpclient.execute(getter);
    			  Header[] headers = r.getAllHeaders();
    			  
    			  for(Header header: headers) Log.d(Tags.LOG, header.getName()+": "+header.getValue());
    			
    			  HttpEntity entity = r.getEntity();
    			  String filename = HITLIST[position][0]+".nzb";
    			  String age  = HITLIST[position][1];
    			  String size = HITLIST[position][2];
    			  String category = HITLIST[position][4];
    			 
    			  Log.d(Tags.LOG,"Inserting filename and metadata in database.");
    			  
    			  LocalNZBMetadata.openDatabase();
    			  
    			  // Insert filename in database.
    			  String query = "INSERT INTO file ('_id','name') VALUES(null,'"+filename+"')";
    			  LocalNZBMetadata.myDatabase.execSQL(query);
    			  
    			  // Retrieve file _id for the new file.
				  Cursor cur = LocalNZBMetadata.myDatabase.query("file", 
						  										new String[] {"_id","name"},
						  										"name='"+filename+"'",
						  										null, null, null, null);
				  int idIndex = cur.getColumnIndex("_id");
    			  cur.moveToFirst();
    			  int fileId = cur.getInt(idIndex);
    			  
    			  // Insert file metadata in database.
    			  query = "INSERT INTO meta (_id,file_id,category,age,size) VALUES (null,'"
    				  	  +fileId+"','"
    				  	  +category+"','"
    				  	  +age+"','"
    				  	  +size+"')";
    			  
    			  LocalNZBMetadata.myDatabase.execSQL(query);
    			  
    			  if(entity != null){
    		
    				  InputStream is = entity.getContent();
    				  Log.d(Tags.LOG, "Saving file:"+filename); 
    				  Log.d(Tags.LOG, "--- In directory:"+Environment.getExternalStorageDirectory().toString());
    				  
    				  
    				  //FileOutputStream out = openFileOutput(filename,Activity.MODE_WORLD_WRITEABLE);
    				  File storageFile = new File("/mnt/sdcard/"+filename);
    				  FileOutputStream out = new FileOutputStream(storageFile);
    				  
    				  // Update the DB with file metadata.
    				  
    			
    				  int nameIndex = cur.getColumnIndex("name");
    				  cur.moveToFirst();
    				  do{
    					  Log.d(Tags.LOG,"Found name: "+cur.getString(nameIndex));  					  
    				  } while(cur.moveToNext());
    				  			  
    				  Log.d(Tags.LOG, "Created output file...");
    				  byte buf[] = new byte[1024];
    				  int len;
    				  int i=0;
    				  while((len=is.read(buf))>0){
    					  i++;
    					  out.write(buf, 0, len);
    				  }
    				  Log.d(Tags.LOG, "Done writing (wrote "+i*1024+" bytes)...");
    				  out.close();
    				  is.close();

    			  }
    
    		} catch (UnsupportedEncodingException e) {
    			Log.d(Tags.LOG,"Unsupported Encoding Exception: "+e.getMessage());
    		} catch (ClientProtocolException e) {
    			Log.d(Tags.LOG, "Client Protocol Exception: "+e.getMessage());
    		} catch (IOException e) {	
    			Log.d(Tags.LOG, "IO Exception: "+e.getMessage());
    		} catch (URISyntaxException e) {
    			Log.d(Tags.LOG, "URI Syntax exception: "+e.getMessage());
			}
    		LocalNZBMetadata.close();
    		return null;
    	}
		protected void onPostExecute(final Void unused){
    		this.sdc_dialog.dismiss();
    		return;
		}
    	
    }
    
	public void startMySearch(){
		startActivity(new Intent(this,MySearch.class));
	}
    
	public void createAgeMapping(){
		SEARCHAGEHASHMAP.put("All Time", "");
		SEARCHAGEHASHMAP.put("1 Day", "1");
		SEARCHAGEHASHMAP.put("7 Days", "7");
		SEARCHAGEHASHMAP.put("14 Days", "14");
		SEARCHAGEHASHMAP.put("21 Days", "21");
		SEARCHAGEHASHMAP.put("50 Days", "50");
		SEARCHAGEHASHMAP.put("100 Days", "100");
		SEARCHAGEHASHMAP.put("140 Days", "140");
		SEARCHAGEHASHMAP.put("260 Days", "260");
		SEARCHAGEHASHMAP.put("365 Days", "365");
		SEARCHAGEHASHMAP.put("700 Days", "700");
	}
	
    public void createSearchCategoryMapping(){
    	// Create search mapping for Category Spinner.
		SEARCHCATEGORYHASHMAP.put("All Categories", "0");	
		SEARCHCATEGORYHASHMAP.put("Movies","t2");
		SEARCHCATEGORYHASHMAP.put("Movies-DVD","9");
		SEARCHCATEGORYHASHMAP.put("Movies-WMV-HD","12");
		SEARCHCATEGORYHASHMAP.put("Movies-x264","4");
		SEARCHCATEGORYHASHMAP.put("Movies-XviD","2");
		SEARCHCATEGORYHASHMAP.put("TV","t1");
		SEARCHCATEGORYHASHMAP.put("TV-DVD","11");
		SEARCHCATEGORYHASHMAP.put("TV-FGN","24");
		SEARCHCATEGORYHASHMAP.put("TV-H264","22");
		SEARCHCATEGORYHASHMAP.put("TV-x264","14");
		SEARCHCATEGORYHASHMAP.put("TV-XviD","1");
		SEARCHCATEGORYHASHMAP.put("XXX","t4");
		SEARCHCATEGORYHASHMAP.put("XXX-DVD","13");
		SEARCHCATEGORYHASHMAP.put("XXX-Pack","25");
		SEARCHCATEGORYHASHMAP.put("XXX-WMV","21");
		SEARCHCATEGORYHASHMAP.put("XXX-x264","23");
		SEARCHCATEGORYHASHMAP.put("XXX-XviD","3");
		SEARCHCATEGORYHASHMAP.put("PC","t5");
		SEARCHCATEGORYHASHMAP.put("PC-0day","7");
		SEARCHCATEGORYHASHMAP.put("PC-ISO","6");
		SEARCHCATEGORYHASHMAP.put("PC-Mac","15");
		SEARCHCATEGORYHASHMAP.put("Music","t3");
		SEARCHCATEGORYHASHMAP.put("Music-MP3","5");
		SEARCHCATEGORYHASHMAP.put("Music-Video","10");
		SEARCHCATEGORYHASHMAP.put("Console","t6");
		SEARCHCATEGORYHASHMAP.put("Console-NDS","19");
		SEARCHCATEGORYHASHMAP.put("Console-PSP","16");
		SEARCHCATEGORYHASHMAP.put("Console-Wii","17");
		SEARCHCATEGORYHASHMAP.put("Console-XBox","8");
		SEARCHCATEGORYHASHMAP.put("Console-XBox360","20");
    }
	
}