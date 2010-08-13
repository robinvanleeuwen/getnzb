/* (C) 2010 Robin van Leeuwen (robinvanleeuwen@gmail.com)
 * Licence: GPLv2 or later.
 */

package com.rvl.android.getnzb.activity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.rvl.android.getnzb.R;
import com.rvl.android.getnzb.getnzb;
import com.rvl.android.getnzb.tags;



public class search extends Activity {
	private DefaultHttpClient httpclient = getnzb.httpclient;
	private boolean LOGGEDIN = getnzb.LOGGEDIN;
	public static String HITLIST[][];
	public boolean ENABLE_NEXTBUTTON = true;
	public String SEARCHTERM = "";
	public int CURRENT_PAGE = 1; 	// Start searching on page 1
	
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(tags.LOG, "- Starting search activity -");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		TextView statusbar = (TextView) findViewById(R.id.statusbar);
		statusbar.setText("Enter searchterm.");
		
	}
	public void btn_handler(View v){
	   	switch(v.getId()){
	    	
		   	case R.id.btn_search:
		 			EditText ed = (EditText) findViewById(R.id.searchterm);		
		 			SEARCHTERM = ed.getText().toString().trim().replaceAll(" ", "+");
		 			new searchnzb().execute(SEARCHTERM); 		
	    		break;
	    	case R.id.btn_next:
	    		if(HITLIST.length == 25){
	    			HITLIST = null;
	    			CURRENT_PAGE++;
	    			new searchnzb().execute(SEARCHTERM);
	    		}
	    		else{
	    			TextView statusbar = (TextView) findViewById(R.id.statusbar);
	    			statusbar.setText("No more matches.");
	    		}
	    		break;
	    	case R.id.btn_previous:
	    		HITLIST = null;
	    		if(CURRENT_PAGE > 1) CURRENT_PAGE--;
	    		new searchnzb().execute(SEARCHTERM);
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
	
	// searchnzb() searches nzbs.org and reads the supplied HTML page with HTMLCleaner
	// to build a list of nzb-files (links) that can be sent to the HellaNZB server.	
	private class searchnzb extends AsyncTask<String, Void, Void>{
		    
	    ProgressDialog search_dialog = new ProgressDialog(search.this);
	    private int n=-1;
	    	
	    protected void onPreExecute(){
	    	this.search_dialog.setMessage("Searching on nzbs.org and building list...");
	    	this.search_dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    	this.search_dialog.show();
	    	Log.d(tags.LOG, "Searching nzbs.org...");
	    }
	    protected Void doInBackground(final String... args){
	    	Log.d(tags.LOG,"* Starting searchnzb():");
   			if(!LOGGEDIN){
				Log.d(tags.LOG, "Search: Not logged in...");
				return null;
			}
			String url = "";			
			url  = "http://www.nzbs.org/index.php?action=search&q=";
			url += args[0];
			url += "&catid=0&age=&page="+Integer.toString(CURRENT_PAGE);
			Log.d(tags.LOG,"Constructed URL:"+url);
			try {
				int progresscounter = 5;
				this.search_dialog.setProgress(progresscounter);
	    		
				HtmlCleaner cleaner = new HtmlCleaner();
				CleanerProperties props = cleaner.getProperties();
				props.setAllowHtmlInsideAttributes(true);
				props.setAllowMultiWordAttributes(true);
				props.setRecognizeUnicodeChars(true);
				props.setOmitComments(true);
    
				HttpGet httpget = new HttpGet(url);
				HttpResponse httpresponse = httpclient.execute(httpget);
				HttpEntity entity = httpresponse.getEntity();
				
			    progresscounter += 5;
				this.search_dialog.setProgress(progresscounter);
	
				Log.d(tags.LOG,"Data retrieved, parsing items...");
				TagNode node = cleaner.clean(new InputStreamReader(entity.getContent()));
				
				String xpath_link = "";
				String xpath_rows = "//table[@id='nzbtable']/tbody";			
				Object[] nzbtable = node.evaluateXPath(xpath_rows);
				this.n = nzbtable.length;
				
				TagNode row = (TagNode) nzbtable[0];
				
				int numhits = row.getChildren().size() - 3;
				
				// Check if there is a next-page...
				// if so, the button is enabled in build_item_list()
				// since we can't do it in this thread (non-UI-operations only)...
				Log.d(tags.LOG,"Checking if next button needs to be disabled.");
				
				if(numhits < 25){
					Log.d(tags.LOG,"Disabling next-button");
					ENABLE_NEXTBUTTON = false;
				}
				else{
					Log.d(tags.LOG,"Enabling next-button");
					ENABLE_NEXTBUTTON = true;							
				}
				
				Log.d(tags.LOG,"Found "+Integer.toString(numhits)+" items. Adding them to list.");
				this.n = numhits;
				int counterincrement = 88 / numhits;
				String[][] hit = new String[numhits][4];
			
				TagNode a;
				TagNode b;
				TagNode atag;
				
				progresscounter += 5;
				this.search_dialog.setProgress(progresscounter);
				for(int c=0;c<numhits;c++){
					xpath_rows = "//table[@id='nzbtable']/tbody/tr["+Integer.toString(c+3)+"]";
					nzbtable = node.evaluateXPath(xpath_rows);				
	
					// Name
					row = (TagNode) nzbtable[0];
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
					xpath_link = xpath_rows+"/td[8]/b/a";
					Object[] link     = node.evaluateXPath(xpath_link);
					atag = (TagNode) link[0];
					hit[c][3] = atag.getAttributeByName("href").replaceAll("&amp;", "&"); 
					progresscounter += counterincrement;
					this.search_dialog.setProgress(progresscounter);
					Log.d(tags.LOG,"Item "+Integer.toString(c+1)+" added...");
				}
				
				HITLIST = hit;
			
			} catch (MalformedURLException e) {
				Log.d(tags.LOG,"searchnzb: malformed url exception: "+e.getMessage());
			} catch (IOException e) {
				Log.d(tags.LOG,"searchnzb: IO exception: "+e.getMessage());
			} catch (XPatherException e) {
				Log.d(tags.LOG,"searchnzb: XPatherException: "+e.getMessage());
			}
   			return null;
	    }
		protected void onPostExecute(final Void unused){
    		this.search_dialog.dismiss();
    		TextView statusbar = (TextView) findViewById(R.id.statusbar);
    		// String num_hits = Integer.toString(hitlist.length);
    		// String num_hits = "test";
    		String status = "";
    		if(!LOGGEDIN){
    			status = "Not logged in! Check NZB account settings..."; 
    			statusbar.setText(status);
    			Log.d(tags.LOG,"* searchnzb() ended.");
    		}
    		else{
    			Log.d(tags.LOG,"* searchnzb() ended.");
       			build_item_list();
    		}		
		}
	}
    public void build_item_list(){
    	String hits[][] = HITLIST;
    	
    	setContentView(R.layout.links);
    	String item = "";
    	Log.d(tags.LOG, "* build_item_list()");
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
					Log.d(tags.LOG,"Sending download command for position "+pos);
					new downloadfile().execute(pos); 					
			}
    	});
    	   	
    	itemlist.setAdapter(aa);
    	// --
		Log.d(tags.LOG, "Building hitlist...");

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
		Log.d(tags.LOG,"Leaving search activity.");
		super.onDestroy();
	}
    
    private class downloadfile extends AsyncTask<String, Void, Void>{
    	ProgressDialog sdc_dialog = new ProgressDialog(search.this);
    
    	protected void onPreExecute(){
    		this.sdc_dialog.setMessage("Downloading .nzb file...");
    		this.sdc_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    		this.sdc_dialog.show();
    	}
    	
    	protected Void doInBackground(final String... args){
    		
	    	SharedPreferences settings = getSharedPreferences(tags.PREFS, 0);
  	
	    	int position = Integer.parseInt(args[0]);
    	    		
    		try{
    			  URI uri = new URI("http://www.nzbs.org/"+HITLIST[position][3]);
    			  
    			  HttpGet getter = new HttpGet(uri);
    			  getter.setHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT5.1; en-US; rv:1.8.1.20) Gecko/20081217 Firefox/2.0.0.20");
    			  
    			  HttpResponse r = httpclient.execute(getter);
    			  Header[] headers = r.getAllHeaders();
    			  
    			  for(Header header: headers) Log.d(tags.LOG, header.getName()+": "+header.getValue());
    			
    			  HttpEntity entity = r.getEntity();
    			  String filename = HITLIST[position][0]+".nzb";
    			  if(entity != null){
    		
    				  InputStream is = entity.getContent();
    				  
    				  Log.d(tags.LOG, "Saving file:"+filename); 
    				  Log.d(tags.LOG, "In directory:"+getFilesDir());
    				  FileOutputStream out = openFileOutput(filename,Activity.MODE_WORLD_WRITEABLE);
    		
    				  Log.d(tags.LOG, "Created output file...");
    				  byte buf[] = new byte[1024];
    				  int len;
    				  int i=0;
    				  while((len=is.read(buf))>0){
    					  i++;
    					  out.write(buf, 0, len);
    				  }
    				  Log.d(tags.LOG, "Done writing (wrote "+i*1024+" bytes)...");
    				  out.close();
    				  is.close();
    				  String list[] = fileList();
    				  for(int c=0;c<list.length;c++){
    					 Log.d(tags.LOG,"List of local files: "+list[c]); 
    					 
    				  }
    			  }
    
    		} catch (UnsupportedEncodingException e) {
    			Log.d(tags.LOG,"Unsupported Encoding Exception: "+e.getMessage());
    		} catch (ClientProtocolException e) {
    			Log.d(tags.LOG, "Client Protocol Exception: "+e.getMessage());
    		} catch (IOException e) {	
    			Log.d(tags.LOG, "IO Exception: "+e.getMessage());
    		} catch (URISyntaxException e) {
    			Log.d(tags.LOG, "URI Syntax exception: "+e.getMessage());
			}
    		return null;
    	}
		protected void onPostExecute(final Void unused){
    		this.sdc_dialog.dismiss();
    		return;
		}
    	
    }
	
}