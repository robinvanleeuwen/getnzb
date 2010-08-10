package com.rvl.android.getnzb.activity;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import com.rvl.android.getnzb.getnzb;
import com.rvl.android.getnzb.tags;
import com.rvl.android.getnzb.R;
import android.app.Activity;
import android.content.SharedPreferences;
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
	
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		Log.d(tags.LOG,"- Starting HellaNZB Activity!");
		setContentView(R.layout.hellanzb);
		TextView statusbar = (TextView) findViewById(R.id.hellastatus);

		statusbar.setText("Starting HellaNZB routine...");	
	}
	
    public void button_handler_hellanzb(View v){
    	switch(v.getId()){
    	case R.id.button_connecthella:
    		Log.d(tags.LOG,"Going into connect()");
    		connect();
    		break;
    	case R.id.button_listfiles:
    		listfiles();
    		break;
    	}
    }
	
	public void connect(){
		Log.d(tags.LOG,"Getting preferences");
		SharedPreferences prefs = getnzb.preferences;
		String hellahost = prefs.getString("hellanzb_hostname", "");
		TextView statusbar = (TextView) findViewById(R.id.hellastatus);
		
		if(!hellahost.matches("(https?)://.+")) 
			hellahost = "http://" + hellahost;
		try {
			Log.d(tags.LOG,"Creating URI: "+hellahost + ":" + prefs.getString("hellanzb_port", "8760"));
			uri = URI.create(hellahost + ":" + prefs.getString("hellanzb_port", "8760"));
			Log.d(tags.LOG,"CReating Client");
			client = new XMLRPCClient(uri.toURL());
			Log.d(tags.LOG,"Authenticating with:'"+prefs.getString("hellanzbpassword", "")+"'");
			
			client.setBasicAuthentication("hellanzb", prefs.getString("hellanzbpassword",""));
			Log.d(tags.LOG,"Calling aolsay");
			if(client.call("aolsay") != ""){
				String message = "Connected";
				statusbar.setText(message);
				CONNECTED = true;
			}
			
		} catch (MalformedURLException e) {
			Log.d(tags.LOG,"connect() failed: MalformedURLException:"+e.getMessage());
		} catch (XMLRPCException e) {
			Log.d(tags.LOG,"connect() failed: XMLRPCException:"+e.getMessage());
		}
	
	}
	public void hellastatus(){
		TextView statusbar = (TextView) findViewById(R.id.hellastatus);
		Log.d(tags.LOG,"Calling status");
		hellareturn = (HashMap<String, Object>) hellanzbcall("status","");
		statusbar.setText(hellareturn.get("is_paused").toString());
	}
	
    public void listfiles(){
    	Log.d(tags.LOG, "* listfiles()");
    	setContentView(R.layout.hellanzb);
    	TextView statusbar = (TextView) findViewById(R.id.hellastatus);
    	statusbar.setText("List of local .nzb files. Click to upload to HellaNZB:");
    	// -- Bind the itemlist to the itemarray with the arrayadapter
    	ArrayList<String> items = new ArrayList<String>();
    	ArrayAdapter<String> aa = new ArrayAdapter<String>(this,com.rvl.android.getnzb.R.layout.itemslist,items);
   
    	ListView itemlist = (ListView) findViewById(R.id.filelist01);
    	
    	itemlist.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
					String pos = Integer.toString(position);
					Log.d(tags.LOG,"Upload file number "+pos);	
					
					// PUT UPLOAD FILE TO HELLANZB CODE HERE!
			
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
	
	public Object hellanzbcall(String command, String extra) {
		try {
			if(CONNECTED) {
				if(extra != "") return client.call(command, extra);
				else return client.call(command);
			} else{
				Log.d(tags.LOG,"hellanzbcall(): Not connected, connecting first.");
				connect();
				if(extra != "") return client.call(command, extra);
				else return client.call(command);
			}
		} catch(XMLRPCException e) {
			Log.e(tags.LOG, "hellanzbcall(): "+e.getMessage());
			CONNECTED = false;
		}
		return null;
	}
}