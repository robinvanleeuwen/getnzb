
/**
 * This file is a part of GetNZB
 * 
 * GetNZB - http://code.google.com/p/getnzb
 * "Android NZB Search and HellaNZB client."
 * 
 * Copyright (C) 2010: Robin van Leeuwen (robinvanleeuwen@gmail.com)
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
**/

package com.rvl.android.getnzb;

import com.rvl.android.getnzb.Tags;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class Preferences extends PreferenceActivity {
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(Tags.LOG, "Starting preference acticity");
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(Tags.PREFS);
		addPreferencesFromResource(R.layout.preferences);
	}
	protected void onDestroy() {
		Log.d(Tags.LOG,"Leaving preference activity.");
		super.onDestroy();
	}
	
}