package com.rvl.android.getnzb;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import com.rvl.android.getnzb.Search.downloadfile;
import com.rvl.android.getnzb.Search.searchNZB;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.CursorJoiner.Result;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class MySearch extends Activity{
	
	public static String HITLIST[][];
	public String[][] MYSEARCHES;
	private boolean LOGGEDIN = GetNZB.LOGGEDIN;
	private DefaultHttpClient httpclient = GetNZB.httpclient;
	public boolean ENABLE_NEXTBUTTON = true;
	public int CURRENT_PAGE = 1; 
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mysearches);
		new getMySearches().execute();
	}
	
	private class getMySearches extends AsyncTask<String,Void,Void>{
		ProgressDialog mysearchesDialog = new ProgressDialog(MySearch.this);
		int n = 0;
		
		protected void onPreExecute(){
			this.mysearchesDialog.setTitle("Please wait...");
			this.mysearchesDialog.setMessage("Retrieving My Searches");
			this.mysearchesDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			this.mysearchesDialog.show();
		}
		
		protected Void doInBackground(String... args) {
			int progresscounter = 0;
			Log.d(Tags.LOG,"Retrieving My Searches.");
			if(!LOGGEDIN){
				Log.d(Tags.LOG, "My Searches(): Not logged in...");
				return null;
			}
			this.mysearchesDialog.setProgress(progresscounter);
			
			try {
				String url = "http://nzbs.org/user.php?action=mysearches";
				// Construct parser
				HtmlCleaner cleaner = new HtmlCleaner();
				CleanerProperties clProp = cleaner.getProperties();
				clProp.setAllowHtmlInsideAttributes(true);
				clProp.setAllowMultiWordAttributes(true);
				clProp.setRecognizeUnicodeChars(true);
				clProp.setOmitComments(true);
				
				progresscounter += 5;
				this.mysearchesDialog.setProgress(progresscounter);

				// Get page
				HttpGet httpget = new HttpGet(url);
				HttpResponse httpresponse;			
				httpresponse = httpclient.execute(httpget);
				HttpEntity entity = httpresponse.getEntity();

				progresscounter += 5;
				this.mysearchesDialog.setProgress(progresscounter);
				
				// Constuct the XPath stuff...
				
				String xpath = "//div[@class='content']/table[1]/tbody";
				TagNode node = cleaner.clean(new InputStreamReader(entity.getContent()));
				Object[] mysearchTable = node.evaluateXPath(xpath);
				
				this.n = mysearchTable.length;
				TagNode row = (TagNode) mysearchTable[0];
				int numhits = row.getChildren().size() - 1;
				
				progresscounter += 5;
				
				this.mysearchesDialog.setProgress(progresscounter);
				
				TagNode a;
				TagNode[] b;
				String searchvalue;
				String excludelist;
				String groups;
				String[][] foundMySearches = new String[numhits][2];
				
				for(int c=0;c<numhits;c++){
					int counterincrement = 85 / numhits / 2;
					
					xpath = "//div[@class='content']/table[1]/tbody/tr["+Integer.toString(c+2)+"]";
					mysearchTable = node.evaluateXPath(xpath);
					row = (TagNode) mysearchTable[0];	
					a = (TagNode) row.getChildren().get(0);
					searchvalue = a.getText().toString();
					searchvalue = searchvalue.substring(0, searchvalue.length()-36); // Trim &&[search][rss][..][..]
			
					foundMySearches[c][0] = searchvalue;
					
					progresscounter += counterincrement;
					this.mysearchesDialog.setProgress(progresscounter);

					
					xpath = "//div[@class='content']/table[1]/tbody/tr["+Integer.toString(c+2)+"]/td/small";
					mysearchTable = node.evaluateXPath(xpath);
					row = (TagNode) mysearchTable[0];	
					b = row.getAllElements(true);
					Log.d(Tags.LOG,"FOUND:"+b[1].getText().toString());
					Log.d(Tags.LOG,"VALUE:"+b[1].getAttributeByName("href").toString());
					
					foundMySearches[c][1] = b[0].getAttributeByName("href").toString().replace("&amp;", "&");
					
					progresscounter += counterincrement;
					this.mysearchesDialog.setProgress(progresscounter);

				}
				MYSEARCHES = foundMySearches;
				} catch (ClientProtocolException e) {
					Log.d(Tags.LOG,"getMySearches(): "+e.getMessage());
				} catch (IOException e) {
					Log.d(Tags.LOG,"getMySearches(): "+e.getMessage());
				} catch (XPatherException e) {
					Log.d(Tags.LOG,"getMySearches(): "+e.getMessage());
				}
			
			
			return null;
		}
		protected void onProgressUpdate(String message){
			this.mysearchesDialog.setMessage(message);
		}
	
		protected void onPostExecute(final Void unused){
			Log.d(Tags.LOG,"Finishing...");
			this.mysearchesDialog.dismiss();
			buildMySearchList();
		}


	}
	
	public void buildMySearchList(){
    	String item = "";
    	String foundMySearches[][] = MYSEARCHES;
    	Log.d(Tags.LOG, "* buildItemList()");
    	
    	// -- Bind the itemlist to the itemarray with the arrayadapter
    	ArrayList<String> items = new ArrayList<String>();
    	ArrayAdapter<String> aa = new MySearchRowAdapter(this,items);
       	ListView mySearchList = (ListView) findViewById(R.id.mysearcheslist);
    	mySearchList.setCacheColorHint(00000000);
    	mySearchList.setAdapter(aa);
    	registerForContextMenu(mySearchList);
    	
    	mySearchList.setOnItemClickListener(new OnItemClickListener(){   
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position,
                            long id) {
            				Log.d(Tags.LOG,"SEARCH:"+MYSEARCHES[position][1]);
            				String[] values = MYSEARCHES[position][1].split("&");
            				String q   = values[1].substring(2, values[1].length());
            				String cat = values[2].substring(6, values[2].length());
            				Search.SEARCHTERM = q;
            				Search.SEARCHCATEGORY = cat;
            				startSearch();
            }       
    	});
      	
    	for(int i=0;i<foundMySearches.length;i++){
    		item += foundMySearches[i][0];
    		items.add(item);
    		Log.d(Tags.LOG,"item:"+item);
    		item = "";
    		
    	}
    	aa.notifyDataSetChanged();
	}
	public void startSearch(){
		startActivity(new Intent(this,Search.class));
	}
	
	public int getlastMatch(String searchPattern,String textString) {
		int index = -1;
	    Pattern pattern = Pattern.compile(searchPattern);
	    Matcher matcher = pattern.matcher(textString);

	    while(matcher.find()) {
           index = matcher.start();
	    }
	    return index;
	}
	   

}