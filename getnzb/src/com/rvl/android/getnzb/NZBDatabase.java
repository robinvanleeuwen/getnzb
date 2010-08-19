package com.rvl.android.getnzb;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class NZBDatabase extends SQLiteOpenHelper{
	public SQLiteDatabase myDatabase;  
    public Context myContext;
    
    public NZBDatabase(Context context){
    	super(context, Tags.DBNAME, null, 1);
    	this.myContext = context;
    	
    }
    
    public void createDatabase() throws IOException{
    	boolean dbExist = checkDatabase();
    	if(dbExist){
    		//do nothing - database already exist
    	}else{
 
    		this.getReadableDatabase();
        	try {
    			copyDatabase();
    		} catch (IOException e) {
        		throw new Error("Error copying database");
        	}
    	}
    }
    private boolean checkDatabase(){
    	 
    	SQLiteDatabase checkDB = null;
    	try{
    		String myPath = Tags.DBPATH + Tags.DBNAME;
    		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    	}catch(SQLiteException e){
    		Log.d(Tags.LOG,e.getMessage());
    	}
    	if(checkDB != null){
    		checkDB.close();
 
    	}
    	return checkDB != null ? true : false;
    }
    
    private void copyDatabase() throws IOException{
    	 
    	//Open your local db as the input stream
    	InputStream myInput = myContext.getAssets().open(Tags.DBNAME);
 
    	// Path to the just created empty db
    	String outFileName = Tags.DBPATH + Tags.DBNAME;
 
    	//Open the empty db as the output stream
    	OutputStream myOutput = new FileOutputStream(outFileName);
 
    	//transfer bytes from the inputfile to the outputfile
    	byte[] buffer = new byte[1024];
    	int length;
    	while ((length = myInput.read(buffer))>0){
    		myOutput.write(buffer, 0, length);
    	}
 
    	//Close the streams
    	myOutput.flush();
    	myOutput.close();
    	myInput.close();
 
    }
    public void openDatabase() throws SQLException{
    	//Open the database, create first if needed.
    	try {
			createDatabase();
		} catch (IOException e) {
			Log.d(Tags.LOG,"openDatabase(): creating new database failed.");
		}
        String myPath = Tags.DBPATH + Tags.DBNAME;
    	myDatabase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
    	Log.d(Tags.LOG,"Opened database '"+Tags.DBNAME+"'.");
    }
 
    @Override
	public synchronized void close() {
 
    	    if(myDatabase != null)
    		    myDatabase.close();
 
    	    super.close();
 
	}
 
	@Override
	public void onCreate(SQLiteDatabase db) {
 
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
 
	}
    
    
}