package com.rvl.android.getnzb;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class AddMySearchDialog extends Dialog{

	

	public AddMySearchDialog(Context context, int theme){
		super(context, theme);
	}
	
	public AddMySearchDialog(Context context){
		super(context);
	}
	
	public static class Builder{
		private Context context;
	
		public Builder(Context context){
			this.context = context;
		}
				

		public AddMySearchDialog create(){
			LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			final AddMySearchDialog dialog = new AddMySearchDialog(context,
					R.style.Dialog);
			final View layout = inflater.inflate(R.layout.addmysearchdialog, null);
			
			dialog.addContentView(layout, new LayoutParams(
					LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
			
			
			((Button) layout.findViewById(R.id.buttonAddMySearch)).setOnClickListener(
				new View.OnClickListener(){
	    		@Override
				public void onClick(View v) {   			
	    			
	    			// Get the search query
	    			EditText searchTerm = (EditText) layout.findViewById(R.id.addMySearchTerm);
	    			String term = searchTerm.getText().toString();
	    			
	    			
	    			// Get the filter
	    			EditText searchFilter = (EditText) layout.findViewById(R.id.addMySearchWithout);
	    			String filter = searchFilter.getText().toString();
	    			
	    			// Get the category
			 		Spinner categorySpinner = (Spinner) layout.findViewById(R.id.addMySearchGroup);
			 		String catid = Search.SEARCHCATEGORYHASHMAP.get(categorySpinner.getSelectedItem().toString());
	    				    			
	    			//------------------------
	    			DefaultHttpClient httpclient = GetNZB.httpclient;
	    				HttpPost post = new HttpPost(Tags.NZBS_LOGINPAGE);
	    				
	    				List<NameValuePair> nvp = new ArrayList<NameValuePair>(2);
	    				nvp.add(new BasicNameValuePair("action","doaddsearch"));
	    				nvp.add(new BasicNameValuePair("searchText",term));
	    				nvp.add(new BasicNameValuePair("searchFilter",filter));
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
	    				
	    				dialog.dismiss();
	    				
	    		}
	    	
			});
			
			((Button) layout.findViewById(R.id.buttonCancelMySearch)).setOnClickListener(
					new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							dialog.dismiss();
						}
					}
			);
			
			dialog.setContentView(layout);
			return dialog;
		}
	}

	
}