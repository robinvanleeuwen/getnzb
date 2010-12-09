package com.rvl.android.getnzb;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class MySearch extends Activity{
	
	public static String HITLIST[][];
	public String[][] MYSEARCHES;
	private boolean LOGGEDIN = GetNZB.LOGGEDIN;
	private DefaultHttpClient httpclient = GetNZB.httpclient;
	public boolean ENABLE_NEXTBUTTON = true;
	public static final int MENU_ADDMYSEARCH = 0;
	public int CURRENT_PAGE = 1; 
	public static ProgressDialog pd = null;
	public static String ADDSEARCHQUERY = "";
	public static String MODIFYSEARCH = "";
	public static String MODIFYTERM = "";
	public static String MODIFYEXCLUDE = "";
	public static String MODIFYGROUP = "";
	public static HashMap<String,String> MYSEARCHCATEGORYHASHMAP = new HashMap<String,String>();
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	 	this.setRequestedOrientation(
    			ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.mysearches);	
		createSearchCategoryMapping();

		new getMySearches().execute();
	}
	
	    public boolean onCreateOptionsMenu(Menu menu){
	    	menu.add(0, MENU_ADDMYSEARCH, 0, "Add Search");
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item){
    	switch (item.getItemId()){
    	case MENU_ADDMYSEARCH:
    		addOrModifySearch();
    		return true;
    	}
    	return false;
    }
    
    public void addOrModifySearch(){
    	setContentView(R.layout.addmysearch);
    	// If it's a modify, fill the fields.
    	if(!MODIFYSEARCH.equals("")){
    		EditText searchTerm = (EditText) findViewById(R.id.editTextSearchTerm);
    		searchTerm.setText(MODIFYTERM);
    		   		
    		EditText excludeTerm = (EditText) findViewById(R.id.editTextExcludeTerm);   		
    		excludeTerm.setText(MODIFYEXCLUDE);
    	}
   
    }
    
    public void saveMySearch(){
    	EditText searchTerm = (EditText) findViewById(R.id.editTextSearchTerm);
		String   term = searchTerm.getText().toString();
		
		EditText excludeTerm = (EditText) findViewById(R.id.editTextExcludeTerm);
		String exclude = excludeTerm.getText().toString();
		
		
		Spinner searchGroup = (Spinner) findViewById(R.id.spinnerAddMySearchGroup);
		String catid = MYSEARCHCATEGORYHASHMAP.get(searchGroup.getSelectedItem().toString());
		
		DefaultHttpClient httpclient = GetNZB.httpclient;
		HttpPost post = new HttpPost(Tags.NZBS_LOGINPAGE);
		
		List<NameValuePair> nvp = new ArrayList<NameValuePair>(2);
		
		if(!MODIFYSEARCH.equals("")){
			nvp.add(new BasicNameValuePair("action","doeditsearch"));
			nvp.add(new BasicNameValuePair("searchID",MODIFYSEARCH));				
		}
		else{
			nvp.add(new BasicNameValuePair("action","doaddsearch"));
		}
		nvp.add(new BasicNameValuePair("searchText",term));
		nvp.add(new BasicNameValuePair("searchFilter",exclude));
		nvp.add(new BasicNameValuePair("catid",catid));

		try {
			post.setEntity(new UrlEncodedFormEntity(nvp));
			HttpResponse response = httpclient.execute(post);
			HttpEntity entity = response.getEntity();
			
				if(entity != null) entity.consumeContent();					
			
		} catch (UnsupportedEncodingException e) {
			Log.d(Tags.LOG,"addMySearch(): UnsupportedEncodingException: "+e.getMessage());
		} catch (ClientProtocolException e) {
			Log.d(Tags.LOG,"addMySearch(): ClientProtocolException: "+e.getMessage());
		} catch (IOException e) {
			httpclient = new DefaultHttpClient();
			Log.d(Tags.LOG,"addMySearch(): IO Exception: "+e.getMessage());
			Log.d(Tags.LOG,"addMySearch(): "+e.toString());
		}
		
	    
    }
    
    public void MySearchButtonHandler(View v){
    	switch(v.getId()){
    		case R.id.buttonAddMySearchSave:
    			saveMySearch();
    			setContentView(R.layout.mysearches);
    			new getMySearches().execute();
    		break;
    		case R.id.buttonAddMySearchCancel:
    			MODIFYSEARCH = "";
    			MODIFYTERM = "";
    			MODIFYEXCLUDE = "";
    			MODIFYGROUP = "";
    			setContentView(R.layout.mysearches);
    			new getMySearches().execute();    			
    		break;
    	}
    }
  
	public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mysearchcontextmenu, menu);
		super.onCreateContextMenu(menu, view, menuInfo);
	}
	
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId()){

		case R.id.deleteMySearch:
            deleteMySearch(info.id);
            return true;
		}
		
		return false;
	}
	public void deleteMySearch(long id){
		
		// We get the URL of the delete page. So strip the searchID number of it.
		
		String[] values = MYSEARCHES[(int) id][2].split("&");
		String[] searchidvalues = values[1].split("=");
		String searchid = searchidvalues[1];
		
		Log.d(Tags.LOG,"deleteMySearch(): deleting value:"+searchid);
	
		HttpPost post = new HttpPost(Tags.NZBS_LOGINPAGE);
		
		List<NameValuePair> nvp = new ArrayList<NameValuePair>(2);
		nvp.add(new BasicNameValuePair("searchID",searchid));
		nvp.add(new BasicNameValuePair("action","dodeletesearch"));

		try {
			post.setEntity(new UrlEncodedFormEntity(nvp));
			HttpResponse response = httpclient.execute(post);
			HttpEntity entity = response.getEntity();
		} catch (UnsupportedEncodingException e) {
			Log.d(Tags.LOG, "deleteMySearch():"+e.getMessage());
		} catch (ClientProtocolException e) {
			Log.d(Tags.LOG, "deleteMySearch():"+e.getMessage());
		} catch (IOException e) {
			Log.d(Tags.LOG, "deleteMySearch():"+e.getMessage());
		}
		
		
		
		// Reload MySearches list.
		new getMySearches().execute();
	}
	public void editMySearch(long id){
		// Get ID
		String[] values = MYSEARCHES[(int) id][2].split("&");
		String[] searchidvalues = values[1].split("=");
		MODIFYSEARCH = searchidvalues[1];
		
		
		
	}
	
	
	class getMySearches extends AsyncTask<String,Void,Void>{
		ProgressDialog mysearchesDialog = new ProgressDialog(MySearch.this);
		
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
			MYSEARCHES = null;
			this.mysearchesDialog.setProgress(progresscounter);
			
			try {
				String url = "http://nzbs.org/user.php?action=mysearches";
				Log.d(Tags.LOG,"Retrieving MySearches...proxy");
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
				String xpath = "//table[@id='msList']/tbody";
				
				TagNode node = cleaner.clean(new InputStreamReader(entity.getContent()));
				Object[] mysearchTable = node.evaluateXPath(xpath);
				
				TagNode row = (TagNode) mysearchTable[0];
				int numhits = row.getChildren().size();
	
				progresscounter += 5;
				
				this.mysearchesDialog.setProgress(progresscounter);
				TagNode a,a2;
				TagNode[] b;
	
				String[][] foundMySearches = new String[numhits][3];
				xpath = "//table[@id='msList']/tbody"; //"+Integer.toString(c+1)+"]";
				mysearchTable = node.evaluateXPath(xpath);
				row = (TagNode) mysearchTable[0];
				Object[] mysearchRow;
				String[] searchTermValues;
				String searchTerm;
				for(int c=0;c<numhits;c++){
					int counterincrement = 85 / numhits / 2;
					
					a = (TagNode) row.getChildren().get(c);
					searchTerm = a.getText().toString();
					searchTermValues = searchTerm.split("&nbsp");
					searchTerm = searchTermValues[0];
					searchTerm = searchTerm.substring(12);
					xpath = "//td/small";
					mysearchRow = a.evaluateXPath(xpath);
					
					a2 = (TagNode) mysearchRow[0];
					
					b = a2.getAllElements(true);
					progresscounter += counterincrement;
					this.mysearchesDialog.setProgress(progresscounter);
							
					foundMySearches[c][0] = searchTerm;
					foundMySearches[c][1] = b[0].getAttributeByName("href").toString().replace("&amp;", "&");
					// Get the ID (which is in the Delete field URL)				
					foundMySearches[c][2] = b[3].getAttributeByName("href").toString().replace("&amp;", "&");
					
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
            				Search.SEARCHTERM = values[1].substring(2, values[1].length());
            				Search.SEARCHCATEGORY = values[2].substring(6, values[2].length());
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

    
    public void createSearchCategoryMapping(){
    	MYSEARCHCATEGORYHASHMAP.clear();
    	// Create search mapping for Category Spinner.
		MYSEARCHCATEGORYHASHMAP.put("All Categories", "0");	
		//MYSEARCHCATEGORYHASHMAP.put("Movies","t2");
		MYSEARCHCATEGORYHASHMAP.put("Movies-DVD","9");
		MYSEARCHCATEGORYHASHMAP.put("Movies-WMV-HD","12");
		MYSEARCHCATEGORYHASHMAP.put("Movies-x264","4");
		MYSEARCHCATEGORYHASHMAP.put("Movies-XviD","2");
		//MYSEARCHCATEGORYHASHMAP.put("TV","t1");
		MYSEARCHCATEGORYHASHMAP.put("TV-DVD","11");
		MYSEARCHCATEGORYHASHMAP.put("TV-FGN","24");
		MYSEARCHCATEGORYHASHMAP.put("TV-H264","22");
		MYSEARCHCATEGORYHASHMAP.put("TV-x264","14");
		MYSEARCHCATEGORYHASHMAP.put("TV-XviD","1");
		//MYSEARCHCATEGORYHASHMAP.put("XXX","t4");
		MYSEARCHCATEGORYHASHMAP.put("XXX-DVD","13");
		MYSEARCHCATEGORYHASHMAP.put("XXX-Pack","25");
		MYSEARCHCATEGORYHASHMAP.put("XXX-WMV","21");
		MYSEARCHCATEGORYHASHMAP.put("XXX-x264","23");
		MYSEARCHCATEGORYHASHMAP.put("XXX-XviD","3");
		//MYSEARCHCATEGORYHASHMAP.put("PC","t5");
		MYSEARCHCATEGORYHASHMAP.put("PC-0day","7");
		MYSEARCHCATEGORYHASHMAP.put("PC-ISO","6");
		MYSEARCHCATEGORYHASHMAP.put("PC-Mac","15");
		//MYSEARCHCATEGORYHASHMAP.put("Music","t3");
		MYSEARCHCATEGORYHASHMAP.put("Music-MP3","5");
		MYSEARCHCATEGORYHASHMAP.put("Music-Video","10");
		//MYSEARCHCATEGORYHASHMAP.put("Console","t6");
		MYSEARCHCATEGORYHASHMAP.put("Console-NDS","19");
		MYSEARCHCATEGORYHASHMAP.put("Console-PSP","16");
		MYSEARCHCATEGORYHASHMAP.put("Console-Wii","17");
		MYSEARCHCATEGORYHASHMAP.put("Console-XBox","8");
		MYSEARCHCATEGORYHASHMAP.put("Console-XBox360","20");
    }
	

}