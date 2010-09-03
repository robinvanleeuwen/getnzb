package com.rvl.android.getnzb;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AddMySearchDialog extends Dialog{
	
	public AddMySearchDialog(Context context, int theme){
		super(context, theme);
	}
	
	public AddMySearchDialog(Context context){
		super(context);
	}
	
	public static class Builder{
		private Context context;
		private String title;
		private String message;
		private View contentView;
		
		private DialogInterface.OnClickListener addButtonOnClickListener,
												cancelButtonOnClickListener;
		
		public Builder(Context context){
			this.context = context;
		}
				
		public Builder setContentView(View v){
			this.contentView = v;
			return this;
		}
		
		public Builder setAddButton(DialogInterface.OnClickListener listener){
			
			this.addButtonOnClickListener = listener;
			return this;
		}
		
		public Builder setCancelButton(DialogInterface.OnClickListener listener){
			
			this.cancelButtonOnClickListener = listener;
			return this;
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
	    			String[] values;
	    			
	    			// Get the search query
	    			EditText searchTerm = (EditText) layout.findViewById(R.id.addMySearchTerm);
	    			String term = searchTerm.getText().toString().replaceAll(" ", "+");
	    			
	    			// Get the filter
	    			EditText searchFilter = (EditText) layout.findViewById(R.id.addMySearchWithout);
	    			values = searchFilter.getText().toString().split(" ");
	    			String filter = "";
	    			for(int c=0;c<values.length;c++){
	    				filter += "--"+values[c]+"+";
	    			}
	    			if(filter.equals("--+")) filter = ""; // No excludes where given.
	    			if(!filter.equals("")) filter = filter.substring(0, filter.length()-1); // Remove last +
	    			
	    			
	    			Log.d(Tags.LOG,"Filter:"+term+"+"+filter);
	    			
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