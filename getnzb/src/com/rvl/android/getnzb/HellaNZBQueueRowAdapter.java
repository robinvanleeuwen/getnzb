package com.rvl.android.getnzb;

import java.util.ArrayList;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class HellaNZBQueueRowAdapter extends ArrayAdapter<String>{
	Activity context;
	private ArrayList<String> items = new ArrayList<String>();
	
	public HellaNZBQueueRowAdapter(Activity activity, ArrayList<String> items){
		super(activity, R.layout.hellanzbqueuelist, items);
		this.context = activity;
		this.items = items;
	}
	// Takes a string in items: <filename>#<size>
	public View getView(int position, View convertView, ViewGroup parent){
		LayoutInflater inflater = context.getLayoutInflater();
		View row = inflater.inflate(R.layout.hellanzbqueuelist, null);
		String[] values = items.get(position).split("#");
	
		((TextView) row.findViewById(R.id.filename)).setText(values[0]);
		((TextView) row.findViewById(R.id.size)).setText(values[1]);

		return row;
	}

}