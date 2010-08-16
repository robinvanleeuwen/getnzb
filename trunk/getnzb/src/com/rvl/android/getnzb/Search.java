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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

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

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.rvl.android.getnzb.R;
import com.rvl.android.getnzb.GetNZB;
import com.rvl.android.getnzb.Tags;



public class Search extends Activity {
	private DefaultHttpClient httpclient = GetNZB.httpclient;
	private boolean LOGGEDIN = GetNZB.LOGGEDIN;
	public static String HITLIST[][];
	public boolean ENABLE_NEXTBUTTON = true;
	public String SEARCHTERM = "";
	public int CURRENT_PAGE = 1; 	// Start searching on page 1
	public static final int MENU_GETCART = 0;
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(Tags.LOG, "- Starting search activity -");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		TextView statusbar = (TextView) findViewById(R.id.statusbar);
		statusbar.setText("Enter searchterm.");
		
	}
	
    public boolean onCreateOptionsMenu(Menu menu){
    	menu.add(0, MENU_GETCART, 0, "Get Cart");
    	return true;
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
		 		new searchNZB().execute(SEARCHTERM); 		
	    		break;
	    	case R.id.btn_next:
	    		if(HITLIST.length == 25){
	    			HITLIST = null;
	    			CURRENT_PAGE++;
	    			new searchNZB().execute(SEARCHTERM);
	    		}
	    		else{
	    			TextView statusbar = (TextView) findViewById(R.id.statusbar);
	    			statusbar.setText("No more matches.");
	    		}
	    		break;
	    	case R.id.btn_previous:
	    		HITLIST = null;
	    		if(CURRENT_PAGE > 1) CURRENT_PAGE--;
	    		new searchNZB().execute(SEARCHTERM);
	    		break;
	    	case R.id.btn_backtosearch:
	    		HITLIST = null;
	    		CURRENT_PAGE = 1;
	    		setContentView(R.layout.search);
	    		TextView statusbar = (TextView) findViewById(R.id.statusbar);
	    		statusbar.setText("Enter searchterm.");
	    		break;

	    	}
	}
	
	// searchNZB() searches nzbs.org and reads the supplied HTML page with HTMLCleaner
	// to build a list of nzb-files (links) that can be sent to the HellaNZB server.	
	private class searchNZB extends AsyncTask<String, Void, Void>{
		    
	    ProgressDialog searchDialog = new ProgressDialog(Search.this);
	    
	    @SuppressWarnings("unused")
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
			url += "&catid=0&age=&page="+Integer.toString(CURRENT_PAGE);
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
				TagNode row = (TagNode) nzbTable[0];
				int numhits = row.getChildren().size() - 3;
				
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
				String[][] hit = new String[numhits][4];
			
				TagNode a;
				TagNode b;
				TagNode atag;
				
				progresscounter += 5;
				this.searchDialog.setProgress(progresscounter);
				for(int c=0;c<numhits;c++){
					xpathRows = "//table[@id='nzbtable']/tbody/tr["+Integer.toString(c+3)+"]";
					nzbTable = node.evaluateXPath(xpathRows);				
	
					// Name
					row = (TagNode) nzbTable[0];
					a = (TagNode) row.getChildren().get(1);
					b = (TagNode) a.getChildren().get(0);
					hit[c][0] = b.getText().toString();
	
					// Days ago
					a = (TagNode) row.getChildren().get(3);
					hit[c][1] = a.getText().toString();
	
					// Size
					a = (TagNode) row.getChildren().get(4);    					
					hit[c][2] = a.getText().toString();

					// Download link
					xpathLink = xpathRows+"/td[8]/b/a";
					Object[] link     = node.evaluateXPath(xpathLink);
					atag = (TagNode) link[0];
					hit[c][3] = atag.getAttributeByName("href").replaceAll("&amp;", "&"); 
					progresscounter += counterincrement;
					this.searchDialog.setProgress(progresscounter);
					Log.d(Tags.LOG,"Item "+Integer.toString(c+1)+" added...");
				}
				
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
    		// String num_hits = Integer.toString(hitlist.length);
    		// String num_hits = "test";
    		String status = "";
    		if(!LOGGEDIN){
    			status = "Not logged in! Check NZB account settings..."; 
    			statusbar.setText(status);
    			Log.d(Tags.LOG,"* searchNZB() ended.");
    		}
    		else{
    			Log.d(Tags.LOG,"* searchNZB() ended.");
       			buildItemList();
    		}		
		}
	}
    public void buildItemList(){
    	String hits[][] = HITLIST;
    	
    	setContentView(R.layout.links);
    	String item = "";
    	Log.d(Tags.LOG, "* buildItemList()");
    	// -- Bind the itemlist to the itemarray with the arrayadapter
    	ArrayList<String> items = new ArrayList<String>();
    	ArrayAdapter<String> aa = new ArrayAdapter<String>(this,com.rvl.android.getnzb.R.layout.itemslist,items);
   
    	ListView itemlist = (ListView) findViewById(R.id.itemlist01);
    	itemlist.setCacheColorHint(00000000);
    	itemlist.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
					String pos = Integer.toString(position);
					Log.d(Tags.LOG,"Sending download command for position "+pos);
					new downloadfile().execute(pos); 					
			}
    	});
    	   	
    	itemlist.setAdapter(aa);
    	// --
		Log.d(Tags.LOG, "Building hitlist...");

    	for(int i=0;i<hits.length;i++){
    		item += hits[i][0] + " / " + hits[i][1] + " / " + hits[i][2];
    		items.add(item);
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
    
    private class downloadfile extends AsyncTask<String, Void, Void>{
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
    			  if(entity != null){
    		
    				  InputStream is = entity.getContent();
    				  
    				  Log.d(Tags.LOG, "Saving file:"+filename); 
    				  Log.d(Tags.LOG, "In directory:"+getFilesDir());
    				  FileOutputStream out = openFileOutput(filename,Activity.MODE_WORLD_WRITEABLE);
    		
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
    				  String list[] = fileList();
    				  for(int c=0;c<list.length;c++){
    					 Log.d(Tags.LOG,"List of local files: "+list[c]); 
    					 
    				  }
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
    		return null;
    	}
		protected void onPostExecute(final Void unused){
    		this.sdc_dialog.dismiss();
    		return;
		}
    	
    }
	
}