package com.rvl.android.getnzb.activity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
					//this.search_dialog.setMessage("Item "+Integer.toString(c+1)+" added...");
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
    	
    	itemlist.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
					String pos = Integer.toString(position);
					Log.d(tags.LOG,"Sending download command for position "+pos);
					//new senddownloadcommand().execute(pos); 					
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
    }
        		
	protected void onDestroy() {
		Log.d(tags.LOG,"Leaving search activity.");
		super.onDestroy();
	}
	
}