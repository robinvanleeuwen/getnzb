package com.rvl.android.getnzb;

import java.util.ArrayList;
import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SearchResultRowAdapter extends ArrayAdapter<String>{
	Activity context;
	private ArrayList<String> items = new ArrayList<String>();
	
	public SearchResultRowAdapter(Activity activity, ArrayList<String> items){
		super(activity, R.layout.searchresultlist, items);
		this.context = activity;
		this.items = items;
	}

	public View getView(int position, View convertView, ViewGroup parent){
		LayoutInflater inflater = context.getLayoutInflater();
		View row = inflater.inflate(R.layout.searchresultlist, null);
		String[] values = items.get(position).split("#");
		
		Log.d(Tags.LOG,"Setting age:"+values[1]);
		((TextView) row.findViewById(R.id.age)).setText(values[1]);		
		Log.d(Tags.LOG,"Setting size:"+values[2]);
		((TextView) row.findViewById(R.id.size)).setText(values[2]);
		Log.d(Tags.LOG,"Setting name:"+values[0]);
		((TextView) row.findViewById(R.id.name)).setText(values[0]);
		Log.d(Tags.LOG,"Setting category:"+values[3]);
		((TextView) row.findViewById(R.id.category)).setText(values[3]);

		return row;
	}

	
}