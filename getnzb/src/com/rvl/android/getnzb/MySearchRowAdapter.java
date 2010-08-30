package com.rvl.android.getnzb;

import java.util.ArrayList;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MySearchRowAdapter extends ArrayAdapter<String>{
	Activity context;
	private ArrayList<String> items = new ArrayList<String>();
	
	public MySearchRowAdapter(Activity activity, ArrayList<String> items){
		super(activity, R.layout.mysearchlist, items);
		this.context = activity;
		this.items = items;
	}
	// Takes a string in items: <searchterm>#<exludelist>#<category>#<searchId>
	public View getView(int position, View convertView, ViewGroup parent){
		LayoutInflater inflater = context.getLayoutInflater();
		View row = inflater.inflate(R.layout.mysearchlist, null);
		String values = items.get(position);
		((TextView) row.findViewById(R.id.mysearchContent)).setText(values);
		//((TextView) row.findViewById(R.id.mysearchExclude)).setText("Exclude: "+values[1]);
		//((TextView) row.findViewById(R.id.mysearchGroups)).setText(values[2]);

		return row;
	}

}